package com.danielvnz.landclaimer.placeholder;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.ReputationManager;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import com.danielvnz.landclaimer.model.PlayerMode;
import com.danielvnz.landclaimer.model.PlayerReputation;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for FrontierGuard plugin.
 * Provides placeholders for player reputation and player mode.
 */
public class FrontierGuardPlaceholders extends PlaceholderExpansion {
    
    private final LandClaimerPlugin plugin;
    private final ReputationManager reputationManager;
    private final PlayerModeManager playerModeManager;
    
    public FrontierGuardPlaceholders(LandClaimerPlugin plugin, ReputationManager reputationManager, PlayerModeManager playerModeManager) {
        this.plugin = plugin;
        this.reputationManager = reputationManager;
        this.playerModeManager = playerModeManager;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "frontierguard";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "DanielVNZ";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.6"; // Plugin version
    }
    
    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // Handle reputation placeholders
        if (params.equalsIgnoreCase("reputation")) {
            return getPlayerReputation(player);
        }
        
        if (params.equalsIgnoreCase("reputation_number")) {
            return getPlayerReputationNumber(player);
        }
        
        if (params.equalsIgnoreCase("reputation_status")) {
            return getPlayerReputationStatus(player);
        }
        
        if (params.equalsIgnoreCase("reputation_color")) {
            return getPlayerReputationColor(player);
        }
        
        // Handle player mode placeholders
        if (params.equalsIgnoreCase("mode")) {
            return getPlayerMode(player);
        }
        
        if (params.equalsIgnoreCase("mode_display")) {
            return getPlayerModeDisplay(player);
        }
        
        if (params.equalsIgnoreCase("is_normal")) {
            return isPlayerNormal(player);
        }
        
        if (params.equalsIgnoreCase("is_peaceful")) {
            return isPlayerPeaceful(player);
        }
        
        // Handle playtime placeholders
        if (params.equalsIgnoreCase("playtime_hours")) {
            return getPlayerPlaytimeHours(player);
        }
        
        if (params.equalsIgnoreCase("playtime_formatted")) {
            return getPlayerPlaytimeFormatted(player);
        }
        
        // Handle combined placeholders
        if (params.equalsIgnoreCase("reputation_full")) {
            return getPlayerReputationFull(player);
        }
        
        if (params.equalsIgnoreCase("mode_reputation")) {
            return getPlayerModeWithReputation(player);
        }
        
        // Handle coordinate placeholders
        if (params.equalsIgnoreCase("coords")) {
            return getPlayerCoordinates(player);
        }
        
        if (params.equalsIgnoreCase("coordinates")) {
            return getPlayerCoordinates(player);
        }
        
        if (params.equalsIgnoreCase("location")) {
            return getPlayerCoordinates(player);
        }
        
        // Handle noob placeholder
        if (params.equalsIgnoreCase("noob")) {
            return getPlayerNoobStatus(player);
        }
        
        // Unknown placeholder
        return null;
    }
    
    /**
     * Gets the player's reputation as a formatted string with color coding
     */
    private String getPlayerReputation(Player player) {
        if (reputationManager == null) {
            return "§e0";
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation != null) {
            return String.format("%s%d", reputation.getReputationColor(), reputation.getReputation());
        }
        
        return "§e0";
    }
    
    /**
     * Gets the player's reputation number only
     */
    private String getPlayerReputationNumber(Player player) {
        return getPlayerReputation(player);
    }
    
    /**
     * Gets the player's reputation status (Excellent, Good, Neutral, etc.)
     */
    private String getPlayerReputationStatus(Player player) {
        if (reputationManager == null) {
            return "Neutral";
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation != null) {
            return reputation.getReputationStatus();
        }
        
        return "Neutral";
    }
    
    /**
     * Gets the player's reputation color code
     */
    private String getPlayerReputationColor(Player player) {
        if (reputationManager == null) {
            return "§e"; // Yellow for neutral
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation != null) {
            return reputation.getReputationColor();
        }
        
        return "§e"; // Yellow for neutral
    }
    
    /**
     * Gets the player's mode as a string
     */
    private String getPlayerMode(Player player) {
        if (playerModeManager == null) {
            return "unknown";
        }
        
        PlayerMode mode = playerModeManager.getPlayerMode(player);
        if (mode != null) {
            return mode.getDisplayName();
        }
        
        return "unknown";
    }
    
    /**
     * Gets the player's mode with proper capitalization and color coding
     */
    private String getPlayerModeDisplay(Player player) {
        String mode = getPlayerMode(player);
        if (mode.equals("unknown")) {
            return "§7Unknown";
        }
        
        String capitalizedMode = mode.substring(0, 1).toUpperCase() + mode.substring(1);
        
        // Add color coding: green for Peaceful, yellow for Normal
        if (mode.equals("peaceful")) {
            return "§a" + capitalizedMode; // Green
        } else if (mode.equals("normal")) {
            return "§e" + capitalizedMode; // Yellow
        }
        
        return capitalizedMode;
    }
    
    /**
     * Returns "true" if player is in normal mode, "false" otherwise
     */
    private String isPlayerNormal(Player player) {
        if (playerModeManager == null) {
            return "false";
        }
        
        return String.valueOf(playerModeManager.isNormalPlayer(player));
    }
    
    /**
     * Returns "true" if player is in peaceful mode, "false" otherwise
     */
    private String isPlayerPeaceful(Player player) {
        if (playerModeManager == null) {
            return "false";
        }
        
        return String.valueOf(playerModeManager.isPeacefulPlayer(player));
    }
    
    /**
     * Gets the player's total playtime in hours
     */
    private String getPlayerPlaytimeHours(Player player) {
        if (reputationManager == null) {
            return "0.0";
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation != null) {
            return String.format("%.1f", reputation.getTotalPlaytimeHours());
        }
        
        return "0.0";
    }
    
    /**
     * Gets the player's playtime in a formatted string (e.g., "5h 30m")
     */
    private String getPlayerPlaytimeFormatted(Player player) {
        if (reputationManager == null) {
            return "0h 0m";
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation != null) {
            double totalHours = reputation.getTotalPlaytimeHours();
            int hours = (int) Math.floor(totalHours);
            int minutes = (int) Math.floor((totalHours - hours) * 60);
            
            return String.format("%dh %dm", hours, minutes);
        }
        
        return "0h 0m";
    }
    
    /**
     * Gets the player's reputation with color and status
     */
    private String getPlayerReputationFull(Player player) {
        if (reputationManager == null) {
            return "§e0 (Neutral)";
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation != null) {
            return String.format("%s%d (%s)", 
                               reputation.getReputationColor(), 
                               reputation.getReputation(),
                               reputation.getReputationStatus());
        }
        
        return "§e0 (Neutral)";
    }
    
    /**
     * Gets the player's mode with reputation info
     */
    private String getPlayerModeWithReputation(Player player) {
        String mode = getPlayerModeDisplay(player);
        String reputation = getPlayerReputationFull(player);
        
        return String.format("%s - %s", mode, reputation);
    }
    
    /**
     * Gets the player's live coordinates in format (x, y, z)
     * Only returns coordinates if player has -15 reputation, otherwise returns empty string
     */
    private String getPlayerCoordinates(Player player) {
        // Check if player has -15 reputation
        if (reputationManager == null) {
            return "";
        }
        
        PlayerReputation reputation = reputationManager.getPlayerReputationSync(player);
        if (reputation == null || reputation.getReputation() != -15) {
            return "";
        }
        
        // Get player's current location
        org.bukkit.Location location = player.getLocation();
        if (location == null) {
            return "";
        }
        
        // Format coordinates as (x, y, z)
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        return String.format("(%d, %d, %d)", x, y, z);
    }
    
    /**
     * Gets the player's noob status
     * Returns "Noob" if player is within first 30 minutes or manually marked as noob
     * Returns empty string otherwise
     */
    private String getPlayerNoobStatus(Player player) {
        if (plugin == null) {
            return "";
        }
        
        // Check if player is within first 30 minutes of joining
        if (isNewPlayer(player)) {
            return "Noob";
        }
        
        // Check if player is manually marked as noob
        if (plugin.getNoobManager() != null && plugin.getNoobManager().isPlayerNoob(player)) {
            return "Noob";
        }
        
        return "";
    }
    
    /**
     * Checks if a player is within their first 30 minutes on the server
     */
    private boolean isNewPlayer(Player player) {
        if (player == null) {
            return false;
        }
        
        // Get first played time from Bukkit (returns milliseconds since epoch)
        long firstPlayedTime = player.getFirstPlayed();
        
        if (firstPlayedTime == 0) {
            // Player has never played before, consider them new
            return true;
        }
        
        // Calculate minutes since first join
        long currentTime = System.currentTimeMillis();
        long timeDifferenceMs = currentTime - firstPlayedTime;
        long minutesSinceJoin = timeDifferenceMs / (1000 * 60); // Convert to minutes
        
        return minutesSinceJoin < 30;
    }
}
