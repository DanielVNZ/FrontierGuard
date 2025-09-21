package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

/**
 * Handles inventory click events for the mode selection GUI.
 * Processes player mode selection and prevents inventory manipulation.
 */
public class ModeSelectionListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    
    public ModeSelectionListener(LandClaimerPlugin plugin, PlayerModeManager playerModeManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
    }
    
    /**
     * Handles inventory click events for mode selection GUI
     * @param event The inventory click event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if this is a mode selection GUI
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is the mode selection GUI
        if (!isModeSelectionGUI(inventory)) {
            return;
        }
        
        // Cancel the event to prevent item manipulation
        event.setCancelled(true);
        
        try {
            int slot = event.getSlot();
            
            // Handle mode selection
            if (playerModeManager.handleModeSelection(player, slot)) {
                plugin.getLogger().info("Player " + player.getName() + " selected mode via slot " + slot);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling mode selection click for player: " + player.getName(), e);
        }
    }
    
    /**
     * Handles inventory close events for mode selection GUI
     * Re-opens the GUI if the player hasn't selected a mode yet
     * @param event The inventory close event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is the mode selection GUI
        if (!isModeSelectionGUI(inventory)) {
            return;
        }
        
        try {
            // If player still hasn't selected a mode, re-open the GUI after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && !playerModeManager.hasSelectedMode(player)) {
                        plugin.getLogger().info("Re-opening mode selection GUI for player: " + player.getName());
                        playerModeManager.promptModeSelection(player);
                    }
                }
            }.runTaskLater(plugin, 10L); // Wait 0.5 seconds (10 ticks)
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling mode selection GUI close for player: " + player.getName(), e);
        }
    }
    
    /**
     * Checks if the given inventory is a mode selection GUI
     * @param inventory The inventory to check
     * @return true if it's a mode selection GUI, false otherwise
     */
    private boolean isModeSelectionGUI(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        // Check inventory size and structure instead of title for better compatibility
        if (inventory.getSize() != 9) {
            return false;
        }
        
        // Check if the inventory has the expected items in the expected slots
        return inventory.getItem(3) != null && inventory.getItem(5) != null &&
               inventory.getItem(3).getType().toString().contains("EMERALD") &&
               inventory.getItem(5).getType().toString().contains("SWORD");
    }
}