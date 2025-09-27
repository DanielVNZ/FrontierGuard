package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.ReputationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Event listener for reputation system.
 * Handles player deaths, joins, and quits for reputation tracking.
 */
public class ReputationListener implements Listener {
    
    private final ReputationManager reputationManager;
    
    public ReputationListener(LandClaimerPlugin plugin, ReputationManager reputationManager) {
        this.reputationManager = reputationManager;
    }
    
    /**
     * Handles player death events to track PVP kills for reputation changes
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Only process if there's a killer and it's a player
        if (killer == null || !(killer instanceof Player)) {
            return;
        }
        
        // Track the PVP kill for reputation changes
        reputationManager.handlePvpKill(killer, victim, victim.getLocation());
    }
    
    /**
     * Handles player join events to start session tracking
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Start tracking the player's session for playtime calculation
        reputationManager.startPlayerSession(player);
    }
    
    /**
     * Handles player quit events to end session tracking and update playtime
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // End the player's session and update playtime
        reputationManager.endPlayerSession(player);
    }
}
