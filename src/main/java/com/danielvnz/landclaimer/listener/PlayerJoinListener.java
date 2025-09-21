package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

/**
 * Handles player join and quit events for mode selection and data management.
 * Triggers mode selection for new players and manages player data caching.
 */
public class PlayerJoinListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    
    public PlayerJoinListener(LandClaimerPlugin plugin, PlayerModeManager playerModeManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
    }
    
    /**
     * Handles player join events to load player data and trigger mode selection for new players
     * @param event The player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        try {
            // Load player data into cache
            playerModeManager.loadPlayerData(player);
            
            // Check if player needs to select a mode (delay to ensure data is loaded)
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (!playerModeManager.hasSelectedMode(player)) {
                            plugin.getLogger().info("New player " + player.getName() + " needs to select a mode");
                            playerModeManager.promptModeSelection(player);
                        } else {
                            plugin.getLogger().info("Player " + player.getName() + " already has mode: " + 
                                playerModeManager.getPlayerMode(player));
                        }
                        
                        // Check for update notifications (delay to avoid spam)
                        if (plugin.getConfigurationManager().isUpdateNotifyOnJoin()) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    plugin.getUpdateChecker().notifyPlayer(player);
                                }
                            }.runTaskLater(plugin, 60L); // Wait 3 seconds (60 ticks) after join
                        }
                        
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error checking mode selection for player: " + player.getName(), e);
                    }
                }
            }.runTaskLater(plugin, 20L); // Wait 1 second (20 ticks) for data to load
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player join for: " + player.getName(), e);
        }
    }
    
    /**
     * Handles player quit events to clean up cached data
     * @param event The player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        try {
            // Remove player data from cache to free memory
            playerModeManager.unloadPlayerData(player);
            plugin.getLogger().info("Unloaded player data for: " + player.getName());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling player quit for: " + player.getName(), e);
        }
    }
}