package com.willfp.ecoenchants.enchant.impl.hardcoded

import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.Items
import com.willfp.ecoenchants.enchant.EcoEnchant
import com.willfp.ecoenchants.enchant.impl.HardcodedEcoEnchant
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object EnchantmentSoulbound : HardcodedEcoEnchant(
    "soulbound"
) {
    private val handler = SoulboundHandler(this)

    override fun onRegister() {
        plugin.eventManager.registerListener(handler)
    }

    override fun onRemove() {
        plugin.eventManager.unregisterListener(handler)
    }

    private class SoulboundHandler(
        private val enchant: EcoEnchant
    ) : Listener {
        private val savedSoulboundItems = PersistentDataKey(
            plugin.namespacedKeyFactory.create("soulbound_items"),
            PersistentDataKeyType.STRING_LIST,
            emptyList()
        )

        private val soulboundKey = plugin.namespacedKeyFactory.create("soulbound")
        private val keptItems = mutableMapOf<UUID, Collection<ItemStack>>()
        private val temporaryDebug = true

        @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
        )
        fun handle(event: PlayerDeathEvent) {
            if (event.keepInventory) {
                return
            }

            val player = event.entity
            // The event drops are authoritative: combat-log plugins can manage the player's inventory.
            val items = event.drops
                .filter { !it.type.isAir && it.itemMeta?.hasEnchant(enchant.enchantment) == true }

            if (items.isEmpty()) {
                return
            }

            debug(player, "death detected=${items.describe()} drops-before=${event.drops.describe()}")
            event.drops.removeAll(items)

            // Use native paper method
            if (Prerequisite.HAS_PAPER.isMet) {
                val modifiedItems = if (enchant.config.getBool("single-use")) {
                    items.map {
                        val meta = it.itemMeta ?: return@map it
                        meta.removeEnchant(enchant.enchantment)
                        it.itemMeta = meta
                        it
                    }
                } else {
                    items
                }

                event.itemsToKeep += modifiedItems
                keptItems[player.uniqueId] = modifiedItems
                debug(player, "kept=${modifiedItems.describe()} drops-after=${event.drops.describe()}")
                return
            }

            for (item in items) {
                item.fast().persistentDataContainer.set(soulboundKey, PersistentDataType.INTEGER, 1)

                if (enchant.config.getBool("single-use")) {
                    val meta = item.itemMeta ?: continue
                    meta.removeEnchant(enchant.enchantment)
                    item.itemMeta = meta
                }
            }

            player.profile.write(savedSoulboundItems, items.map { Items.toSNBT(it) })
        }

        @EventHandler(
            ignoreCancelled = true
        )
        fun onJoin(event: PlayerJoinEvent) {
            giveItems(event.player)
        }

        @EventHandler(
            ignoreCancelled = true
        )
        fun onJoin(event: PlayerRespawnEvent) {
            giveItems(event.player)
        }

        private fun giveItems(player: Player) {
            val itemStrings = player.profile.read(savedSoulboundItems)

            if (itemStrings.isEmpty()) {
                return
            }

            val items = itemStrings.map { Items.fromSNBT(it) }

            plugin.scheduler.run {
                DropQueue(player)
                    .addItems(items)
                    .forceTelekinesis()
                    .push()
            }

            player.profile.write(savedSoulboundItems, emptyList())
        }

        @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
        )
        fun preventDroppingSoulboundItems(event: PlayerDeathEvent) {
            val before = event.drops.size
            event.drops.removeIf {
                it.fast().persistentDataContainer.has(soulboundKey, PersistentDataType.INTEGER)
                        && it.itemMeta?.hasEnchant(enchant.enchantment) == true
            }
            if (event.drops.size != before) {
                debug(event.entity, "fallback removed=${before - event.drops.size} drops=${event.drops.describe()}")
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun removeLateDrops(event: PlayerDeathEvent) {
            val kept = keptItems.remove(event.entity.uniqueId) ?: return
            val before = event.drops.size
            event.drops.removeAll(kept)
            debug(event.entity, "final removed=${before - event.drops.size} drops=${event.drops.describe()}")
        }

        private fun debug(player: Player, message: String) {
            if (temporaryDebug) {
                plugin.logger.info("[TEMP-DEBUG] soulbound player=${player.uniqueId} $message")
            }
        }

        private fun Collection<ItemStack>.describe() = joinToString(prefix = "[", postfix = "]") {
            "${it.type}:${it.amount}:${it.getEnchantmentLevel(enchant.enchantment)}"
        }
    }
}
