package com.willfp.ecoenchants.enchant.impl.hardcoded

import com.willfp.ecoenchants.enchant.EcoEnchant
import com.willfp.ecoenchants.enchant.impl.HardcodedEcoEnchant
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent

object EnchantmentIndestructible : HardcodedEcoEnchant(
    "indestructible"
) {
    private val handler = IndestructibleHandler(this)

    override fun onRegister() {
        plugin.eventManager.registerListener(handler)
    }

    override fun onRemove() {
        plugin.eventManager.unregisterListener(handler)
    }

    private class IndestructibleHandler(
        private val enchant: EcoEnchant
    ) : Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        fun handle(event: PlayerItemDamageEvent) {
            if (event.item.itemMeta?.hasEnchant(enchant.enchantment) == true) {
                event.isCancelled = true
            }
        }
    }
}
