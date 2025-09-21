package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.logging.Level;

/**
 * Handles GUI click events for the FrontierGuard plugin.
 * Processes clicks in the main GUI and other plugin interfaces.
 */
public class GuiListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final GuiManager guiManager;
    
    public GuiListener(LandClaimerPlugin plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }
    
    /**
     * Handles inventory click events for GUI interfaces
     * Modern approach: Clean event handling with proper validation
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle player clicks
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Only handle clicks in the top inventory (GUI inventory)
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        
        // Check if this is a plugin GUI
        if (!isPluginGui(inventory)) {
            return;
        }
        
        // Cancel the event immediately to prevent any item manipulation
        event.setCancelled(true);
        
        // Handle the click
        try {
            int slot = event.getSlot();
            boolean isRightClick = event.isRightClick();
            
            // Handle GUI click based on GUI type
            if (isMainGui(inventory)) {
                guiManager.handleGuiClick(player, slot);
            } else if (isInvitationManagementGui(inventory)) {
                guiManager.handleInvitationManagementClick(player, slot, isRightClick);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling GUI click for player: " + player.getName(), e);
            player.sendMessage(net.kyori.adventure.text.Component.text("An error occurred. Please try again.", net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }
    
    /**
     * Handles inventory drag events to prevent item manipulation
     * Modern approach: Clean validation and immediate cancellation
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        
        // Check if this is a plugin GUI and cancel immediately
        if (isPluginGui(inventory)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handles inventory close events
     * Modern approach: Minimal logging, clean state management
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        // Clean up any GUI-related state if needed
        // Using MONITOR priority to ensure this runs after other plugins
    }
    
    /**
     * Checks if the given inventory is any plugin GUI
     * Modern approach: Single method for all GUI types
     */
    private boolean isPluginGui(Inventory inventory) {
        return isMainGui(inventory) || isInvitationManagementGui(inventory);
    }
    
    /**
     * Checks if the given inventory is the main GUI
     * Modern approach: More robust title checking
     */
    private boolean isMainGui(Inventory inventory) {
        if (inventory == null || inventory.getSize() != 27) {
            return false;
        }
        
        // Modern approach: Safe title checking with fallback
        try {
            if (inventory.getViewers().isEmpty()) {
                return false;
            }
            String title = inventory.getViewers().get(0).getOpenInventory().getTitle();
            return title != null && title.contains("FrontierGuard Menu");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if the given inventory is the invitation management GUI
     * Modern approach: More robust title checking
     */
    private boolean isInvitationManagementGui(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        try {
            if (inventory.getViewers().isEmpty()) {
                return false;
            }
            String title = inventory.getViewers().get(0).getOpenInventory().getTitle();
            return title != null && title.contains("Manage Invitations");
        } catch (Exception e) {
            return false;
        }
    }
    
}
