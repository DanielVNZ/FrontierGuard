package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import com.danielvnz.landclaimer.manager.PvpAreaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Event listener for potion protection.
 * Protects peaceful players from potion effects thrown by other players outside of PVP areas.
 */
public class PotionProtectionListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    private final PvpAreaManager pvpAreaManager;
    
    public PotionProtectionListener(LandClaimerPlugin plugin, PlayerModeManager playerModeManager, PvpAreaManager pvpAreaManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
        this.pvpAreaManager = pvpAreaManager;
    }
    
    /**
     * Handles potion splash events to protect peaceful players from player-thrown potions
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionSplash(PotionSplashEvent event) {
        // Only handle if the event isn't already cancelled
        if (event.isCancelled()) {
            return;
        }
        
        // Get the thrown potion
        ThrownPotion potion = event.getPotion();
        
        // Check if the potion was thrown by a player
        if (!(potion.getShooter() instanceof Player)) {
            return; // Not thrown by a player, allow it
        }
        
        Player thrower = (Player) potion.getShooter();
        
        // Check if thrower is in peaceful mode
        boolean throwerIsPeaceful = playerModeManager.isPeacefulPlayer(thrower);
        
        // Check if we're in a PVP area
        boolean inPvpArea = pvpAreaManager.isInPvpArea(potion.getLocation());
        
        // If we're in a PVP area, allow all potion effects
        if (inPvpArea) {
            return;
        }
        
        // Handle potion effects on affected entities
        for (var entity : event.getAffectedEntities()) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                double intensity = event.getIntensity(target);
                
                // Skip if intensity is 0 (no effect)
                if (intensity <= 0) {
                    continue;
                }
                
                // Check if target is in peaceful mode
                boolean targetIsPeaceful = playerModeManager.isPeacefulPlayer(target);
                
                // If thrower is peaceful, they cannot affect other players
                if (throwerIsPeaceful && !target.equals(thrower)) {
                    // Cancel the effect on other players
                    event.setIntensity(target, 0.0);
                    thrower.sendMessage(Component.text("You cannot throw potions at other players as a peaceful player!", NamedTextColor.RED));
                    continue;
                }
                
                // If target is peaceful, they cannot be affected by other players
                if (targetIsPeaceful && !target.equals(thrower)) {
                    // Cancel the effect on peaceful players
                    event.setIntensity(target, 0.0);
                    thrower.sendMessage(Component.text("You cannot affect peaceful players with potions outside of PVP areas!", NamedTextColor.RED));
                    continue;
                }
                
                // If both are normal players, allow the effect
                // (This case is already handled by the default behavior)
            }
        }
    }
    
    /**
     * Handles projectile launch events to prevent peaceful players from throwing potions at others
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Only handle if the event isn't already cancelled
        if (event.isCancelled()) {
            return;
        }
        
        // Only handle thrown potions
        if (!(event.getEntity() instanceof ThrownPotion)) {
            return;
        }
        
        ThrownPotion potion = (ThrownPotion) event.getEntity();
        
        // Check if the potion was thrown by a player
        if (!(potion.getShooter() instanceof Player)) {
            return; // Not thrown by a player, allow it
        }
        
        Player thrower = (Player) potion.getShooter();
        
        // Check if thrower is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(thrower)) {
            return; // Not a peaceful player, allow it
        }
        
        // Check if we're in a PVP area
        if (pvpAreaManager.isInPvpArea(potion.getLocation())) {
            return; // In PVP area, allow it
        }
        
        // Check if there are other players nearby that could be affected
        boolean hasNearbyPlayers = thrower.getWorld().getNearbyEntities(potion.getLocation(), 10, 10, 10).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .anyMatch(p -> !p.equals(thrower));
        
        if (hasNearbyPlayers) {
            // Cancel the potion throw
            event.setCancelled(true);
            thrower.sendMessage(Component.text("You cannot throw potions near other players as a peaceful player!", NamedTextColor.RED));
            plugin.getLogger().info("Prevented peaceful player " + thrower.getName() + " from throwing potion near other players");
        }
    }
}
