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
     * @param event The inventory click event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if this is a player clicking
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is a plugin GUI
        if (!isMainGui(inventory) && !isInvitationManagementGui(inventory)) {
            return;
        }
        
        // Cancel the event to prevent item manipulation
        event.setCancelled(true);
        
        // Prevent any item movement
        event.setResult(org.bukkit.event.Event.Result.DENY);
        
        // Schedule a GUI refresh to restore proper item names and lore
        // Use a small delay to ensure the click handling completes first
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            refreshGui(inventory, player);
        }, 1L);
        
        try {
            int slot = event.getSlot();
            boolean isRightClick = event.isRightClick();
            
            plugin.getLogger().info("GUI click detected - Player: " + player.getName() + ", Slot: " + slot + ", RightClick: " + isRightClick + ", GUI: " + inventory.getViewers().get(0).getOpenInventory().getTitle());
            
            // Handle GUI click based on GUI type
            if (isMainGui(inventory)) {
                plugin.getLogger().info("Handling main GUI click for slot: " + slot);
                if (guiManager.handleGuiClick(player, slot)) {
                    plugin.getLogger().info("Player " + player.getName() + " clicked main GUI slot " + slot + " successfully");
                } else {
                    plugin.getLogger().info("Player " + player.getName() + " clicked main GUI slot " + slot + " but handler returned false");
                }
            } else if (isInvitationManagementGui(inventory)) {
                plugin.getLogger().info("Handling invitation management GUI click for slot: " + slot);
                if (guiManager.handleInvitationManagementClick(player, slot, isRightClick)) {
                    plugin.getLogger().info("Player " + player.getName() + " clicked invitation GUI slot " + slot + " successfully");
                } else {
                    plugin.getLogger().info("Player " + player.getName() + " clicked invitation GUI slot " + slot + " but handler returned false");
                }
            } else {
                plugin.getLogger().info("Unknown GUI type - not handling click");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling GUI click for player: " + player.getName(), e);
        }
    }
    
    /**
     * Handles inventory drag events to prevent item manipulation
     * @param event The inventory drag event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is a plugin GUI
        if (isMainGui(inventory) || isInvitationManagementGui(inventory)) {
            plugin.getLogger().info("Cancelling drag event in plugin GUI for player: " + player.getName());
            // Cancel the event to prevent item manipulation
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }
    
    /**
     * Handles inventory close events to prevent item theft
     * @param event The inventory close event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is a plugin GUI
        if (isMainGui(inventory) || isInvitationManagementGui(inventory)) {
            plugin.getLogger().info("Player " + player.getName() + " closed plugin GUI");
        }
    }
    
    /**
     * Checks if the given inventory is the main GUI
     * @param inventory The inventory to check
     * @return true if it's the main GUI, false otherwise
     */
    private boolean isMainGui(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        // Check inventory size and title
        if (inventory.getSize() != 27) {
            return false;
        }
        
        String title = inventory.getViewers().isEmpty() ? "" : 
            inventory.getViewers().get(0).getOpenInventory().getTitle();
        
        return title.contains("FrontierGuard Menu");
    }
    
    /**
     * Checks if the given inventory is the invitation management GUI
     * @param inventory The inventory to check
     * @return true if it's the invitation management GUI, false otherwise
     */
    private boolean isInvitationManagementGui(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        String title = inventory.getViewers().isEmpty() ? "" : 
            inventory.getViewers().get(0).getOpenInventory().getTitle();
        
        return title.contains("Manage Invitations");
    }
    
    /**
     * Refreshes the GUI to restore proper item names and lore
     * @param inventory The inventory to refresh
     * @param player The player viewing the GUI
     */
    private void refreshGui(org.bukkit.inventory.Inventory inventory, Player player) {
        if (isMainGui(inventory)) {
            // Refresh main GUI
            guiManager.openMainGui(player);
        } else if (isInvitationManagementGui(inventory)) {
            // Refresh invitation management GUI
            guiManager.openInvitationManagementGui(player);
        }
    }
    
    /**
     * Refreshes a specific slot in the GUI
     * @param inventory The inventory containing the slot
     * @param player The player viewing the GUI
     * @param slot The slot to refresh
     */
    private void refreshGuiSlot(org.bukkit.inventory.Inventory inventory, Player player, int slot) {
        if (isMainGui(inventory)) {
            // Refresh specific slot in main GUI
            guiManager.refreshMainGuiSlot(player, slot);
        } else if (isInvitationManagementGui(inventory)) {
            // For invitation GUI, refresh the entire GUI since it's more complex
            guiManager.openInvitationManagementGui(player);
        }
    }
}
