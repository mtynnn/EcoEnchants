package com.willfp.ecoenchants.mechanics

import com.willfp.ecoenchants.enchant.EcoEnchants
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent

object IndestructibleSupport : Listener {
    private val ENCHANT_KEY = NamespacedKey.minecraft("indestructible")

    @EventHandler(priority = EventPriority.LOWEST)
    fun handle(event: PlayerItemDamageEvent) {
        val meta = event.item.itemMeta ?: return

        // Check via EcoEnchants registry first
        val ecoEnchant = EcoEnchants.getByID("indestructible")
        if (ecoEnchant != null && meta.hasEnchant(ecoEnchant.enchantment)) {
            event.isCancelled = true
            return
        }

        // Fallback: check by namespaced key directly from Bukkit registry
        val enchantment = Registry.ENCHANTMENT.get(ENCHANT_KEY) ?: return
        if (meta.hasEnchant(enchantment)) {
            event.isCancelled = true
        }
    }
}
