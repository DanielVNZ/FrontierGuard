package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.model.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Manages PVP protection for peaceful players.
 * Enforces PVP rules based on player modes and locations.
 */
public class PvpProtectionManager {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    private final PvpAreaManager pvpAreaManager;
    
    public PvpProtectionManager(LandClaimerPlugin plugin, PlayerModeManager playerModeManager, PvpAreaManager pvpAreaManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
        this.pvpAreaManager = pvpAreaManager;
    }
    
    /**
     * Checks if PVP is allowed between two players at a specific location
     * @param attacker The player attempting to attack
     * @param target The player being attacked
     * @param location The location where the attack is happening
     * @return true if PVP is allowed, false otherwise
     */
    public boolean canPvP(Player attacker, Player target, org.bukkit.Location location) {
        if (attacker == null || target == null || location == null) {
            return false;
        }
        
        // Players can't attack themselves
        if (attacker.equals(target)) {
            return false;
        }
        
        // Check if player has bypass permission
        if (attacker.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if we're in a designated PVP area
        if (isInPvpArea(location)) {
            return true; // All players can PVP in designated PVP areas
        }
        
        // Get player modes
        PlayerMode attackerMode = playerModeManager.getPlayerMode(attacker);
        PlayerMode targetMode = playerModeManager.getPlayerMode(target);
        
        // If either player hasn't selected a mode, allow PVP (they should select mode first)
        if (attackerMode == null || targetMode == null) {
            return true;
        }
        
        // PVP Rules:
        // 1. Peaceful players cannot attack anyone outside PVP areas
        // 2. Peaceful players cannot be attacked by anyone outside PVP areas
        // 3. Normal players can attack other normal players freely
        // 4. Normal players cannot attack peaceful players outside PVP areas
        
        if (attackerMode == PlayerMode.PEACEFUL) {
            // Peaceful players cannot attack anyone outside PVP areas
            return false;
        }
        
        if (targetMode == PlayerMode.PEACEFUL) {
            // Peaceful players cannot be attacked outside PVP areas
            return false;
        }
        
        // Both players are normal mode - allow PVP
        return true;
    }
    
    /**
     * Handles PVP protection for entity damage events
     * @param event The EntityDamageByEntityEvent
     */
    public void handlePvpProtection(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity entity = event.getEntity();
        
        // Only handle player vs player damage
        if (!(damager instanceof Player) || !(entity instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) damager;
        Player target = (Player) entity;
        
        // Check if PVP is allowed
        if (!canPvP(attacker, target, target.getLocation())) {
            event.setCancelled(true);
            
            // Send appropriate message to attacker
            sendPvpDeniedMessage(attacker, target);
        }
    }
    
    /**
     * Sends a PVP denied message to the attacker
     * @param attacker The player who attempted to attack
     * @param target The player who was targeted
     */
    private void sendPvpDeniedMessage(Player attacker, Player target) {
        PlayerMode attackerMode = playerModeManager.getPlayerMode(attacker);
        PlayerMode targetMode = playerModeManager.getPlayerMode(target);
        
        String message;
        
        if (attackerMode == PlayerMode.PEACEFUL) {
            message = "You cannot attack other players as a peaceful player!";
        } else if (targetMode == PlayerMode.PEACEFUL) {
            message = "You cannot attack peaceful players outside of PVP areas!";
        } else {
            message = "PVP is not allowed in this area!";
        }
        
        attacker.sendMessage(Component.text(message, NamedTextColor.RED));
        
        // Show visual feedback
        plugin.getVisualFeedbackManager().showPvpDenied(attacker, "PVP Denied!");
    }
    
    /**
     * Checks if a location is in a designated PVP area
     * @param location The location to check
     * @return true if the location is in a PVP area, false otherwise
     */
    private boolean isInPvpArea(org.bukkit.Location location) {
        return pvpAreaManager.isInPvpArea(location);
    }
    
    /**
     * Gets the PVP status for a location
     * @param location The location to check
     * @return A string describing the PVP status
     */
    public String getPvpStatus(org.bukkit.Location location) {
        if (location == null) {
            return "Unknown";
        }
        
        if (isInPvpArea(location)) {
            return "PVP Area - All players can fight";
        }
        
        return "Protected - Peaceful players cannot be attacked";
    }
    
    /**
     * Checks if a player can be attacked at a specific location
     * @param player The player to check
     * @param location The location to check
     * @return true if the player can be attacked, false otherwise
     */
    public boolean canBeAttacked(Player player, org.bukkit.Location location) {
        if (player == null || location == null) {
            return false;
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if we're in a PVP area
        if (isInPvpArea(location)) {
            return true; // All players can be attacked in PVP areas
        }
        
        // Get player mode
        PlayerMode playerMode = playerModeManager.getPlayerMode(player);
        
        // If player hasn't selected a mode, they can be attacked
        if (playerMode == null) {
            return true;
        }
        
        // Peaceful players cannot be attacked outside PVP areas
        if (playerMode == PlayerMode.PEACEFUL) {
            return false;
        }
        
        // Normal players can be attacked
        return true;
    }
    
    /**
     * Checks if a player can attack others at a specific location
     * @param player The player to check
     * @param location The location to check
     * @return true if the player can attack others, false otherwise
     */
    public boolean canAttackOthers(Player player, org.bukkit.Location location) {
        if (player == null || location == null) {
            return false;
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if we're in a PVP area
        if (isInPvpArea(location)) {
            return true; // All players can attack in PVP areas
        }
        
        // Get player mode
        PlayerMode playerMode = playerModeManager.getPlayerMode(player);
        
        // If player hasn't selected a mode, they can attack
        if (playerMode == null) {
            return true;
        }
        
        // Peaceful players cannot attack others outside PVP areas
        if (playerMode == PlayerMode.PEACEFUL) {
            return false;
        }
        
        // Normal players can attack others
        return true;
    }
}
