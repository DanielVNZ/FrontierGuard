package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.database.dao.ClaimDataDao;
import com.danielvnz.landclaimer.model.ClaimData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages chunk claiming functionality exclusively for peaceful players.
 * Handles claim validation, limits, and database operations.
 */
public class ClaimManager {
    
    private final LandClaimerPlugin plugin;
    private final ClaimDataDao claimDataDao;
    private final PlayerModeManager playerModeManager;
    
    public ClaimManager(LandClaimerPlugin plugin, PlayerModeManager playerModeManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
        this.claimDataDao = createClaimDataDao(plugin.getDatabaseManager());
    }
    
    /**
     * Creates the ClaimDataDao instance. Can be overridden for testing.
     * @param databaseManager The database manager
     * @return The ClaimDataDao instance
     */
    protected ClaimDataDao createClaimDataDao(DatabaseManager databaseManager) {
        return new ClaimDataDao(databaseManager);
    }
    
    /**
     * Claims a chunk for a peaceful player
     * @param player The player attempting to claim
     * @param chunk The chunk to claim
     * @return true if the claim was successful, false otherwise
     */
    public boolean claimChunk(Player player, Chunk chunk) {
        if (player == null || chunk == null) {
            plugin.getLogger().warning("Attempted to claim chunk with null player or chunk");
            return false;
        }
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text("Only peaceful players can claim chunks!", NamedTextColor.RED));
            return false;
        }
        
        // Validate claim attempt
        if (!canClaim(player, chunk)) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        try {
            // Create claim data
            ClaimData claimData = new ClaimData(playerUuid, worldName, chunkX, chunkZ, LocalDateTime.now());
            
            // Save to database asynchronously
            claimDataDao.save(claimData).thenRun(() -> {
                plugin.getLogger().info("Successfully claimed chunk (" + chunkX + ", " + chunkZ + ") in world " + worldName + " for player: " + player.getName());
                
                // Send success message and visual feedback to player
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Component message = Component.text("Successfully claimed chunk at ", NamedTextColor.GREEN)
                            .append(Component.text("(" + chunkX + ", " + chunkZ + ")", NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.text("!", NamedTextColor.GREEN));
                        player.sendMessage(message);
                        
                        // Show visual feedback
                        plugin.getVisualFeedbackManager().showClaimSuccess(player, chunk);
                    }
                }.runTask(plugin);
                
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to save claim for player: " + player.getName(), throwable);
                
                // Send error message to player
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(Component.text("Failed to claim chunk. Please try again or contact an administrator.", NamedTextColor.RED));
                    }
                }.runTask(plugin);
                
                return null;
            });
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error claiming chunk for player: " + player.getName(), e);
            player.sendMessage(Component.text("Error claiming chunk. Please try again or contact an administrator.", NamedTextColor.RED));
            return false;
        }
    }
    
    /**
     * Unclaims a chunk for a peaceful player
     * @param player The player attempting to unclaim
     * @param chunk The chunk to unclaim
     * @return true if the unclaim was successful, false otherwise
     */
    public boolean unclaimChunk(Player player, Chunk chunk) {
        if (player == null || chunk == null) {
            plugin.getLogger().warning("Attempted to unclaim chunk with null player or chunk");
            return false;
        }
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text("Only peaceful players can unclaim chunks!", NamedTextColor.RED));
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        try {
            // Check if chunk is claimed by this player
            Optional<ClaimData> existingClaim = claimDataDao.findByChunk(worldName, chunkX, chunkZ).get();
            
            if (!existingClaim.isPresent()) {
                player.sendMessage(Component.text("This chunk is not claimed!", NamedTextColor.RED));
                return false;
            }
            
            ClaimData claimData = existingClaim.get();
            if (!claimData.getOwnerUuid().equals(playerUuid)) {
                player.sendMessage(Component.text("You can only unclaim your own chunks!", NamedTextColor.RED));
                return false;
            }
            
            // Remove from database asynchronously
            claimDataDao.deleteByChunk(worldName, chunkX, chunkZ).thenRun(() -> {
                plugin.getLogger().info("Successfully unclaimed chunk (" + chunkX + ", " + chunkZ + ") in world " + worldName + " for player: " + player.getName());
                
                // Send success message and visual feedback to player
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Component message = Component.text("Successfully unclaimed chunk at ", NamedTextColor.GREEN)
                            .append(Component.text("(" + chunkX + ", " + chunkZ + ")", NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.text("!", NamedTextColor.GREEN));
                        player.sendMessage(message);
                        
                        // Show visual feedback
                        plugin.getVisualFeedbackManager().showUnclaimSuccess(player, chunk);
                    }
                }.runTask(plugin);
                
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to unclaim chunk for player: " + player.getName(), throwable);
                
                // Send error message to player
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(Component.text("Failed to unclaim chunk. Please try again or contact an administrator.", NamedTextColor.RED));
                    }
                }.runTask(plugin);
                
                return null;
            });
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error unclaiming chunk for player: " + player.getName(), e);
            player.sendMessage(Component.text("Error unclaiming chunk. Please try again or contact an administrator.", NamedTextColor.RED));
            return false;
        }
    }
    
    /**
     * Gets the owner of a chunk
     * @param chunk The chunk to check
     * @return The UUID of the owner, or null if unclaimed
     */
    public UUID getChunkOwner(Chunk chunk) {
        if (chunk == null) {
            return null;
        }
        
        try {
            String worldName = chunk.getWorld().getName();
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            
            Optional<ClaimData> claimData = claimDataDao.findByChunk(worldName, chunkX, chunkZ).get();
            return claimData.map(ClaimData::getOwnerUuid).orElse(null);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting chunk owner for chunk (" + chunk.getX() + ", " + chunk.getZ() + ")", e);
            return null;
        }
    }
    
    /**
     * Gets all claims for a player
     * @param player The player whose claims to get
     * @return List of ClaimData objects
     */
    public List<ClaimData> getPlayerClaims(Player player) {
        if (player == null) {
            return List.of();
        }
        
        try {
            return claimDataDao.findByOwner(player.getUniqueId()).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting claims for player: " + player.getName(), e);
            return List.of();
        }
    }
    
    /**
     * Gets all claims owned by a player by UUID
     * @param playerUuid The UUID of the player whose claims to get
     * @return List of ClaimData objects
     */
    public List<ClaimData> getPlayerClaims(UUID playerUuid) {
        if (playerUuid == null) {
            return List.of();
        }
        
        try {
            return claimDataDao.findByOwner(playerUuid).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting claims for player UUID: " + playerUuid, e);
            return List.of();
        }
    }
    
    /**
     * Validates if a player can claim a chunk
     * @param player The player attempting to claim
     * @param chunk The chunk to claim
     * @return true if the claim is valid, false otherwise
     */
    public boolean canClaim(Player player, Chunk chunk) {
        if (player == null || chunk == null) {
            return false;
        }
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text("Only peaceful players can claim chunks!", NamedTextColor.RED));
            return false;
        }
        
        // Check if chunk is already claimed
        try {
            String worldName = chunk.getWorld().getName();
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            
            Optional<ClaimData> existingClaim = claimDataDao.findByChunk(worldName, chunkX, chunkZ).get();
            if (existingClaim.isPresent()) {
                player.sendMessage(Component.text("This chunk is already claimed!", NamedTextColor.RED));
                return false;
            }
            
            // Check claim limits - use async version to get fresh data
            try {
                Boolean canClaim = canClaimMoreAsync(player).get();
                if (!canClaim) {
                    player.sendMessage(Component.text("You have reached your claim limit!", NamedTextColor.RED));
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error checking claim limits for player: " + player.getName(), e);
                // Fallback to sync version
                if (!canClaimMore(player)) {
                    player.sendMessage(Component.text("You have reached your claim limit!", NamedTextColor.RED));
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error validating claim for player: " + player.getName(), e);
            player.sendMessage(Component.text("Error validating claim. Please try again.", NamedTextColor.RED));
            return false;
        }
    }
    
    /**
     * Checks if a player can claim more chunks based on their limits
     * @param player The player to check
     * @return true if they can claim more, false otherwise
     */
    private boolean canClaimMore(Player player) {
        try {
            List<ClaimData> currentClaims = claimDataDao.findByOwner(player.getUniqueId()).get();
            int currentClaimCount = currentClaims.size();
            
            // Check for unlimited claims permission first
            if (player.hasPermission("frontierguard.claims.unlimited")) {
                return true;
            }
            
            // Get claim limit - use GuiManager's system which includes purchased claims
            int claimLimit = getPlayerClaimLimit(player);
            
            plugin.getLogger().info("Player " + player.getName() + " has " + currentClaimCount + " claims, limit is " + claimLimit);
            
            return currentClaimCount < claimLimit;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking claim limits for player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Checks if a player can claim more chunks based on their limits (async version)
     * @param player The player to check
     * @return CompletableFuture containing true if they can claim more, false otherwise
     */
    private CompletableFuture<Boolean> canClaimMoreAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ClaimData> currentClaims = claimDataDao.findByOwner(player.getUniqueId()).get();
                int currentClaimCount = currentClaims.size();
                
                // Check for unlimited claims permission first
                if (player.hasPermission("frontierguard.claims.unlimited")) {
                    return true;
                }
                
                // Get claim limit asynchronously
                return getPlayerClaimLimitAsync(player).thenApply(claimLimit -> {
                    plugin.getLogger().info("Player " + player.getName() + " has " + currentClaimCount + " claims, limit is " + claimLimit);
                    return currentClaimCount < claimLimit;
                }).get();
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error checking claim limits for player: " + player.getName(), e);
                return false;
            }
        });
    }
    
    /**
     * Gets claim information for a chunk
     * @param chunk The chunk to get info for
     * @return ClaimData if claimed, null otherwise
     */
    public ClaimData getClaimInfo(Chunk chunk) {
        if (chunk == null) {
            return null;
        }
        
        try {
            String worldName = chunk.getWorld().getName();
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            
            return claimDataDao.findByChunk(worldName, chunkX, chunkZ).get().orElse(null);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting claim info for chunk (" + chunk.getX() + ", " + chunk.getZ() + ")", e);
            return null;
        }
    }
    
    /**
     * Checks if a location is in a claimed chunk
     * @param location The location to check
     * @return true if the location is in a claimed chunk, false otherwise
     */
    public boolean isLocationClaimed(Location location) {
        if (location == null) {
            return false;
        }
        
        Chunk chunk = location.getChunk();
        return getChunkOwner(chunk) != null;
    }
    
    /**
     * Checks if a player owns the chunk at a location
     * @param player The player to check
     * @param location The location to check
     * @return true if the player owns the chunk, false otherwise
     */
    public boolean isChunkOwnedBy(Player player, Location location) {
        if (player == null || location == null) {
            return false;
        }
        
        UUID owner = getChunkOwner(location.getChunk());
        return player.getUniqueId().equals(owner);
    }
    
    /**
     * Deletes all claims for a specific player
     * @param player The player whose claims to delete
     * @return CompletableFuture that completes when the delete operation is done
     */
    public CompletableFuture<Integer> deleteAllPlayerClaims(Player player) {
        if (player == null) {
            return CompletableFuture.completedFuture(0);
        }
        
        try {
            // Get current claims count for logging
            List<ClaimData> currentClaims = getPlayerClaims(player);
            int claimCount = currentClaims.size();
            
            if (claimCount == 0) {
                return CompletableFuture.completedFuture(0);
            }
            
            // Delete all claims for this player
            return claimDataDao.deleteByOwner(player.getUniqueId()).thenApply(v -> {
                plugin.getLogger().info("Deleted " + claimCount + " claims for player: " + player.getName());
                return claimCount;
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete claims for player: " + player.getName(), throwable);
                return 0;
            });
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting claims for player: " + player.getName(), e);
            return CompletableFuture.completedFuture(0);
        }
    }
    
    /**
     * Gets the claim limit for a player, including purchased claims
     */
    private int getPlayerClaimLimit(Player player) {
        // Check for unlimited claims permission
        if (player.hasPermission("frontierguard.claims.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        // Check for specific claim amount permissions
        for (int i = 1; i <= 1000; i++) { // Check up to 1000 claims
            if (player.hasPermission("frontierguard.claimamount." + i)) {
                return i;
            }
        }
        
        // Check for wildcard permission
        if (player.hasPermission("frontierguard.claimamount.*")) {
            return Integer.MAX_VALUE;
        }
        
        // Base limit (1) + purchased claims from GuiManager
        int baseLimit = 1;
        int purchasedClaims = plugin.getGuiManager().getPurchasedClaims(player);
        return baseLimit + purchasedClaims;
    }
    
    /**
     * Gets the claim limit for a player, including purchased claims (async version)
     */
    private CompletableFuture<Integer> getPlayerClaimLimitAsync(Player player) {
        // Check for unlimited claims permission
        if (player.hasPermission("frontierguard.claims.unlimited")) {
            return CompletableFuture.completedFuture(Integer.MAX_VALUE);
        }
        
        // Check for specific claim amount permissions
        for (int i = 1; i <= 1000; i++) { // Check up to 1000 claims
            if (player.hasPermission("frontierguard.claimamount." + i)) {
                return CompletableFuture.completedFuture(i);
            }
        }
        
        // Check for wildcard permission
        if (player.hasPermission("frontierguard.claimamount.*")) {
            return CompletableFuture.completedFuture(Integer.MAX_VALUE);
        }
        
        // Base limit (1) + purchased claims from GuiManager (async)
        int baseLimit = 1;
        return plugin.getGuiManager().getPurchasedClaimsAsync(player).thenApply(purchasedClaims -> baseLimit + purchasedClaims);
    }
}