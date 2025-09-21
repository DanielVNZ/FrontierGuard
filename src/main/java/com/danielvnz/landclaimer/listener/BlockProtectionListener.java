package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.ProtectionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Event listener for block protection in claimed chunks.
 * Handles block break, block place, and container access protection.
 */
public class BlockProtectionListener implements Listener {
    
    private final ProtectionManager protectionManager;
    
    public BlockProtectionListener(LandClaimerPlugin plugin, ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }
    
    /**
     * Handles block break events for protection
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        // Always run protection logic, even if another plugin cancelled
        protectionManager.handleBlockBreak(event);
    }
    
    /**
     * Handles block place events for protection
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Always run protection logic, even if another plugin cancelled
        protectionManager.handleBlockPlace(event);
    }
    
    /**
     * Handles player interaction events for container and door protection
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Always run protection logic, even if another plugin cancelled
        // Handle container access protection
        protectionManager.handleContainerAccess(event);
        
        // Handle door/gate interaction protection
        protectionManager.handleDoorInteraction(event);
    }
}
