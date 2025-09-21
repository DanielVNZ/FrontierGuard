package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.PvpAreaManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event listener for PVP area selection tool interactions.
 * Handles right-click events with the selection tool.
 */
public class PvpAreaSelectionListener implements Listener {
    
    private final PvpAreaManager pvpAreaManager;
    
    public PvpAreaSelectionListener(LandClaimerPlugin plugin, PvpAreaManager pvpAreaManager) {
        this.pvpAreaManager = pvpAreaManager;
    }
    
    /**
     * Handles player interaction events for PVP area selection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Only handle right-click events
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Check if player is holding the selection tool
        if (item == null || item.getType() != Material.STICK) {
            return;
        }
        
        // Check if it's the PVP area selection tool
        if (!isSelectionTool(item)) {
            return;
        }
        
        // Check if player has permission
        if (!player.hasPermission("frontierguard.admin.pvparea")) {
            return;
        }
        
        // Handle the selection
        if (event.getClickedBlock() != null) {
            boolean handled = pvpAreaManager.handleSelectionTool(player, event.getClickedBlock().getLocation());
            if (handled) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Checks if an item is the PVP area selection tool
     * @param item The item to check
     * @return true if it's the selection tool, false otherwise
     */
    private boolean isSelectionTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = meta.displayName().toString();
        return displayName.contains("PVP Area Selection Tool");
    }
}
