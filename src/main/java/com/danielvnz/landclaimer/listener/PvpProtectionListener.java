package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.PvpProtectionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Event listener for PVP protection.
 * Handles player vs player damage events to enforce PVP rules.
 */
public class PvpProtectionListener implements Listener {
    
    private final PvpProtectionManager pvpProtectionManager;
    
    public PvpProtectionListener(LandClaimerPlugin plugin, PvpProtectionManager pvpProtectionManager) {
        this.pvpProtectionManager = pvpProtectionManager;
    }
    
    /**
     * Handles entity damage events for PVP protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle if the event isn't already cancelled
        if (!event.isCancelled()) {
            pvpProtectionManager.handlePvpProtection(event);
        }
    }
}
