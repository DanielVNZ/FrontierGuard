package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.ClaimManager;
import com.danielvnz.landclaimer.manager.ConfigurationManager;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import com.danielvnz.landclaimer.manager.PvpAreaManager;
import com.danielvnz.landclaimer.manager.VisualFeedbackManager;
import com.danielvnz.landclaimer.model.ClaimData;
import com.danielvnz.landclaimer.model.PlayerMode;
import com.danielvnz.landclaimer.model.PvpArea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for player movement to handle PVP area entry/exit.
 * Provides warnings and status updates when players enter or leave PVP areas.
 */
public class PlayerMoveListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    private final PvpAreaManager pvpAreaManager;
    private final VisualFeedbackManager visualFeedbackManager;
    private final ClaimManager claimManager;
    private final ConfigurationManager configurationManager;
    
    // Track player locations to detect area changes
    private final Map<UUID, Location> playerLocations;
    
    // Track players in PVP areas to avoid spam
    private final Map<UUID, Boolean> playersInPvpAreas;
    
    // Track players in claims to avoid spam
    private final Map<UUID, Boolean> playersInClaims;
    
    // Cooldown for area entry/exit messages (in milliseconds)
    private static final long MESSAGE_COOLDOWN = 2000; // 2 seconds
    private final Map<UUID, Long> lastEntryMessageTime;
    private final Map<UUID, Long> lastExitMessageTime;
    private final Map<UUID, Long> lastClaimEntryMessageTime;
    private final Map<UUID, Long> lastClaimExitMessageTime;
    
    public PlayerMoveListener(LandClaimerPlugin plugin, PlayerModeManager playerModeManager, 
                             PvpAreaManager pvpAreaManager, VisualFeedbackManager visualFeedbackManager,
                             ClaimManager claimManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
        this.pvpAreaManager = pvpAreaManager;
        this.visualFeedbackManager = visualFeedbackManager;
        this.claimManager = claimManager;
        this.configurationManager = plugin.getConfigurationManager();
        this.playerLocations = new HashMap<>();
        this.playersInPvpAreas = new HashMap<>();
        this.playersInClaims = new HashMap<>();
        this.lastEntryMessageTime = new HashMap<>();
        this.lastExitMessageTime = new HashMap<>();
        this.lastClaimEntryMessageTime = new HashMap<>();
        this.lastClaimExitMessageTime = new HashMap<>();
    }
    
    /**
     * Handles player movement events for PVP area detection
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player actually moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location newLocation = event.getTo();
        UUID playerId = player.getUniqueId();
        
        // Initialize player location if not tracked
        Location oldLocation = playerLocations.get(playerId);
        if (oldLocation == null) {
            playerLocations.put(playerId, newLocation);
            return;
        }
        
        // Check if player entered or left a PVP area
        boolean wasInPvpArea = pvpAreaManager.isInPvpArea(oldLocation);
        boolean isInPvpArea = pvpAreaManager.isInPvpArea(newLocation);
        
        // Check if player entered or left a claim
        boolean wasInClaim = isInClaim(oldLocation);
        boolean isInClaim = isInClaim(newLocation);
        
        // Update player location
        playerLocations.put(playerId, newLocation);
        
        // Handle PVP area transitions
        if (!wasInPvpArea && isInPvpArea) {
            handlePvpAreaEntry(player, newLocation);
        } else if (wasInPvpArea && !isInPvpArea) {
            handlePvpAreaExit(player, oldLocation);
        }
        
        // Handle claim transitions
        if (!wasInClaim && isInClaim) {
            handleClaimEntry(player, newLocation);
        } else if (wasInClaim && !isInClaim) {
            handleClaimExit(player, oldLocation);
        } else if (wasInClaim && isInClaim) {
            // Check if player moved between different claims
            handleClaimTransition(player, oldLocation, newLocation);
        }
        
        // Update area status
        playersInPvpAreas.put(playerId, isInPvpArea);
        playersInClaims.put(playerId, isInClaim);
    }
    
    /**
     * Handles when a player enters a PVP area
     * @param player The player entering the area
     * @param location The location where they entered
     */
    private void handlePvpAreaEntry(Player player, Location location) {
        // Check cooldown to prevent spam
        if (isOnEntryCooldown(player)) {
            return;
        }
        
        PlayerMode playerMode = playerModeManager.getPlayerMode(player);
        PvpArea pvpArea = pvpAreaManager.getPvpArea(location);
        
        if (pvpArea == null) {
            return; // Should not happen, but safety check
        }
        
        // Send area entry message
        Component entryMessage = Component.text("[WARNING] ", NamedTextColor.RED, TextDecoration.BOLD)
            .append(Component.text("ENTERING PVP AREA: ", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(pvpArea.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));
        
        player.sendMessage(entryMessage);
        
        // Send mode-specific warnings
        if (playerMode == PlayerMode.PEACEFUL) {
            Component warningMessage = Component.text("[WARNING] ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("WARNING: ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("You are now in a PVP area! ", NamedTextColor.YELLOW))
                .append(Component.text("Other players can attack you here.", NamedTextColor.RED));
            
            player.sendMessage(warningMessage);
            
            // Show visual feedback for peaceful players
            visualFeedbackManager.showPvpAreaWarning(player, pvpArea.getName());
            
        } else if (playerMode == PlayerMode.NORMAL) {
            Component infoMessage = Component.text("[INFO] ", NamedTextColor.BLUE)
                .append(Component.text("You are now in a PVP area. ", NamedTextColor.AQUA))
                .append(Component.text("All players can fight here.", NamedTextColor.WHITE));
            
            player.sendMessage(infoMessage);
        }
        
        // Show area information
        Component areaInfo = Component.text("Area: ", NamedTextColor.GRAY)
            .append(Component.text(pvpArea.getArea() + " blocks", NamedTextColor.WHITE))
            .append(Component.text(" | Y: ", NamedTextColor.GRAY))
            .append(Component.text(location.getWorld().getMinHeight() + " to " + (location.getWorld().getMaxHeight() - 1), NamedTextColor.WHITE));
        
        player.sendMessage(areaInfo);
        
        // Update entry cooldown
        updateEntryCooldown(player);
    }
    
    /**
     * Handles when a player exits a PVP area
     * @param player The player exiting the area
     * @param location The location where they exited
     */
    private void handlePvpAreaExit(Player player, Location location) {
        // Check cooldown to prevent spam
        if (isOnExitCooldown(player)) {
            return;
        }
        
        PlayerMode playerMode = playerModeManager.getPlayerMode(player);
        
        // Send area exit message
        Component exitMessage = Component.text("⚠ ", NamedTextColor.GREEN, TextDecoration.BOLD)
            .append(Component.text("LEAVING PVP AREA", NamedTextColor.GREEN, TextDecoration.BOLD));
        
        player.sendMessage(exitMessage);
        
        // Send mode-specific status restoration messages
        if (playerMode == PlayerMode.PEACEFUL) {
            Component safeMessage = Component.text("⚠ ", NamedTextColor.GREEN)
                .append(Component.text("You are now safe! ", NamedTextColor.GREEN))
                .append(Component.text("PVP protection is restored.", NamedTextColor.AQUA));
            
            player.sendMessage(safeMessage);
            
            // Show visual feedback for peaceful players
            visualFeedbackManager.showPvpAreaExit(player);
            
        } else if (playerMode == PlayerMode.NORMAL) {
            Component infoMessage = Component.text("⚠ ", NamedTextColor.BLUE)
                .append(Component.text("You have left the PVP area. ", NamedTextColor.AQUA))
                .append(Component.text("Standard PVP rules apply.", NamedTextColor.WHITE));
            
            player.sendMessage(infoMessage);
        }
        
        // Update exit cooldown
        updateExitCooldown(player);
    }
    
    /**
     * Checks if a location is within a claim
     * @param location The location to check
     * @return true if the location is in a claim, false otherwise
     */
    private boolean isInClaim(Location location) {
        try {
            ClaimData claim = claimManager.getClaimInfo(location.getChunk());
            return claim != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Handles when a player moves between different claims
     * @param player The player moving between claims
     * @param oldLocation The previous location
     * @param newLocation The new location
     */
    private void handleClaimTransition(Player player, Location oldLocation, Location newLocation) {
        try {
            ClaimData oldClaim = claimManager.getClaimInfo(oldLocation.getChunk());
            ClaimData newClaim = claimManager.getClaimInfo(newLocation.getChunk());
            
            // Check if the claims are different (different owners)
            if (oldClaim != null && newClaim != null && 
                !oldClaim.getOwnerUuid().equals(newClaim.getOwnerUuid())) {
                
                // Player moved from one claim to another - treat as exit then entry
                handleClaimExit(player, oldLocation);
                handleClaimEntry(player, newLocation);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling claim transition for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Handles when a player enters a claim
     * @param player The player entering the claim
     * @param location The location where they entered
     */
    private void handleClaimEntry(Player player, Location location) {
        // Check cooldown to prevent spam
        if (isOnClaimEntryCooldown(player)) {
            return;
        }
        
        try {
            ClaimData claim = claimManager.getClaimInfo(location.getChunk());
            if (claim == null) {
                return; // Should not happen, but safety check
            }
            
            // Check if player owns this claim
            boolean isOwner = claim.getOwnerUuid().equals(player.getUniqueId());
            
            // Send action bar message
            String message;
            if (isOwner) {
                message = configurationManager.getMessage("claim-enter-own", "&a&lEntering your claim");
            } else {
                // Get owner name from UUID
                String ownerName = getPlayerName(claim.getOwnerUuid());
                String baseMessage = configurationManager.getMessage("claim-enter-other", "&e&lEntering {0}'s claim");
                message = baseMessage.replace("{0}", ownerName);
            }
            
            Component actionBarMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            player.sendActionBar(actionBarMessage);
            
            // Update entry cooldown
            updateClaimEntryCooldown(player);
            
        } catch (Exception e) {
            // Silently handle errors to avoid spam
        }
    }
    
    /**
     * Handles when a player exits a claim
     * @param player The player exiting the claim
     * @param location The location where they exited
     */
    private void handleClaimExit(Player player, Location location) {
        // Check cooldown to prevent spam
        if (isOnClaimExitCooldown(player)) {
            return;
        }
        
        // Send action bar message
        String message = configurationManager.getMessage("claim-exit", "&b&lLeaving claimed land");
        Component actionBarMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        
        player.sendActionBar(actionBarMessage);
        
        // Update exit cooldown
        updateClaimExitCooldown(player);
    }
    
    /**
     * Checks if a player is on cooldown for entry messages
     * @param player The player to check
     * @return true if on cooldown, false otherwise
     */
    private boolean isOnEntryCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long lastTime = lastEntryMessageTime.get(playerId);
        
        if (lastTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastTime < MESSAGE_COOLDOWN;
    }
    
    /**
     * Checks if a player is on cooldown for exit messages
     * @param player The player to check
     * @return true if on cooldown, false otherwise
     */
    private boolean isOnExitCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long lastTime = lastExitMessageTime.get(playerId);
        
        if (lastTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastTime < MESSAGE_COOLDOWN;
    }
    
    /**
     * Updates the entry cooldown for a player
     * @param player The player to update cooldown for
     */
    private void updateEntryCooldown(Player player) {
        lastEntryMessageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Updates the exit cooldown for a player
     * @param player The player to update cooldown for
     */
    private void updateExitCooldown(Player player) {
        lastExitMessageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Checks if a player is on cooldown for claim entry messages
     * @param player The player to check
     * @return true if on cooldown, false otherwise
     */
    private boolean isOnClaimEntryCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long lastTime = lastClaimEntryMessageTime.get(playerId);
        
        if (lastTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastTime < MESSAGE_COOLDOWN;
    }
    
    /**
     * Checks if a player is on cooldown for claim exit messages
     * @param player The player to check
     * @return true if on cooldown, false otherwise
     */
    private boolean isOnClaimExitCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long lastTime = lastClaimExitMessageTime.get(playerId);
        
        if (lastTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastTime < MESSAGE_COOLDOWN;
    }
    
    /**
     * Updates the claim entry cooldown for a player
     * @param player The player to update cooldown for
     */
    private void updateClaimEntryCooldown(Player player) {
        lastClaimEntryMessageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Updates the claim exit cooldown for a player
     * @param player The player to update cooldown for
     */
    private void updateClaimExitCooldown(Player player) {
        lastClaimExitMessageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Gets the player name from UUID, with fallback for offline players
     * @param uuid The player's UUID
     * @return The player's name, or "Unknown Player" if not found
     */
    private String getPlayerName(UUID uuid) {
        // First try to get online player
        Player onlinePlayer = org.bukkit.Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        // Try to get offline player
        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        
        // Fallback to UUID string if name not available
        return "Unknown Player";
    }
    
    /**
     * Gets the current PVP area status for a player
     * @param player The player to check
     * @return true if the player is in a PVP area, false otherwise
     */
    public boolean isPlayerInPvpArea(Player player) {
        return playersInPvpAreas.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Gets the PVP area a player is currently in
     * @param player The player to check
     * @return The PVP area if the player is in one, null otherwise
     */
    public PvpArea getPlayerPvpArea(Player player) {
        Location location = playerLocations.get(player.getUniqueId());
        if (location == null) {
            return null;
        }
        
        return pvpAreaManager.getPvpArea(location);
    }
    
    /**
     * Cleans up player data when they leave the server
     * @param player The player who left
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        playerLocations.remove(playerId);
        playersInPvpAreas.remove(playerId);
        playersInClaims.remove(playerId);
        lastEntryMessageTime.remove(playerId);
        lastExitMessageTime.remove(playerId);
        lastClaimEntryMessageTime.remove(playerId);
        lastClaimExitMessageTime.remove(playerId);
    }
    
    /**
     * Initializes a player's location when they join
     * @param player The player who joined
     */
    public void initializePlayer(Player player) {
        Location location = player.getLocation();
        playerLocations.put(player.getUniqueId(), location);
        playersInPvpAreas.put(player.getUniqueId(), pvpAreaManager.isInPvpArea(location));
        playersInClaims.put(player.getUniqueId(), isInClaim(location));
    }
}
