package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.dao.NoobStatusDao;
import com.danielvnz.landclaimer.model.NoobStatus;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages manual noob status for players.
 * Tracks players who have been manually marked as "noob" by admins.
 * Uses database persistence to survive server restarts.
 */
public class NoobManager {
    
    private final LandClaimerPlugin plugin;
    private final NoobStatusDao noobStatusDao;
    
    // Cache for quick lookup of noob statuses
    private final Map<UUID, NoobStatus> noobCache = new ConcurrentHashMap<>();
    
    public NoobManager(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.noobStatusDao = new NoobStatusDao(plugin.getDatabaseManager());
        
        // Load active noob statuses into cache
        loadActiveStatuses();
        
        // Clean up expired statuses
        cleanupExpiredStatuses();
    }
    
    /**
     * Marks a player as a noob for 30 minutes
     * @param player The player to mark as noob
     * @return true if successfully marked, false otherwise
     */
    public boolean markPlayerAsNoob(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        NoobStatus noobStatus = NoobStatus.createForThirtyMinutes(playerUuid);
        
        // Save to database asynchronously
        noobStatusDao.save(noobStatus).thenRun(() -> {
            plugin.getLogger().info(String.format("Player %s has been marked as noob for 30 minutes (saved to database)", player.getName()));
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to save noob status to database for player: " + player.getName());
            throwable.printStackTrace();
            return null;
        });
        
        // Update cache immediately for quick access
        noobCache.put(playerUuid, noobStatus);
        
        plugin.getLogger().info(String.format("Player %s has been marked as noob for 30 minutes", player.getName()));
        
        return true;
    }
    
    /**
     * Removes a player's manual noob status
     * @param player The player to remove noob status from
     * @return true if successfully removed, false otherwise
     */
    public boolean removePlayerNoobStatus(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Remove from database asynchronously
        noobStatusDao.deleteByPlayerUuid(playerUuid).thenAccept(deleted -> {
            if (deleted) {
                plugin.getLogger().info(String.format("Manual noob status removed from player %s (deleted from database)", player.getName()));
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to remove noob status from database for player: " + player.getName());
            throwable.printStackTrace();
            return null;
        });
        
        // Remove from cache immediately
        NoobStatus removed = noobCache.remove(playerUuid);
        
        if (removed != null) {
            plugin.getLogger().info(String.format("Manual noob status removed from player %s", player.getName()));
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a player is manually marked as noob
     * @param player The player to check
     * @return true if player is manually marked as noob and status hasn't expired
     */
    public boolean isPlayerNoob(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        NoobStatus noobStatus = noobCache.get(playerUuid);
        
        if (noobStatus == null) {
            // Check database if not in cache
            loadPlayerNoobStatus(playerUuid);
            noobStatus = noobCache.get(playerUuid);
            
            if (noobStatus == null) {
                return false;
            }
        }
        
        // Check if the noob status has expired
        if (noobStatus.isExpired()) {
            // Remove expired status
            removeExpiredStatus(playerUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the remaining time for a player's manual noob status
     * @param player The player to check
     * @return Minutes remaining, or 0 if not marked as noob or expired
     */
    public long getRemainingNoobTime(Player player) {
        if (player == null) {
            return 0;
        }
        
        UUID playerUuid = player.getUniqueId();
        NoobStatus noobStatus = noobCache.get(playerUuid);
        
        if (noobStatus == null) {
            // Check database if not in cache
            loadPlayerNoobStatus(playerUuid);
            noobStatus = noobCache.get(playerUuid);
            
            if (noobStatus == null) {
                return 0;
            }
        }
        
        return noobStatus.getRemainingMinutes();
    }
    
    /**
     * Loads active noob statuses from database into cache
     */
    private void loadActiveStatuses() {
        noobStatusDao.findAllActive().thenAccept(activeStatuses -> {
            for (NoobStatus status : activeStatuses) {
                noobCache.put(status.getPlayerUuid(), status);
            }
            plugin.getLogger().info(String.format("Loaded %d active noob statuses from database", activeStatuses.size()));
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to load active noob statuses from database");
            throwable.printStackTrace();
            return null;
        });
    }
    
    /**
     * Loads a specific player's noob status from database
     * @param playerUuid The player's UUID
     */
    private void loadPlayerNoobStatus(UUID playerUuid) {
        noobStatusDao.findByPlayerUuid(playerUuid).thenAccept(noobStatus -> {
            if (noobStatus != null && !noobStatus.isExpired()) {
                noobCache.put(playerUuid, noobStatus);
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to load noob status for player: " + playerUuid);
            throwable.printStackTrace();
            return null;
        });
    }
    
    /**
     * Removes an expired status from cache and database
     * @param playerUuid The player's UUID
     */
    private void removeExpiredStatus(UUID playerUuid) {
        noobCache.remove(playerUuid);
        noobStatusDao.deleteByPlayerUuid(playerUuid).thenAccept(deleted -> {
            if (deleted) {
                plugin.getLogger().fine("Removed expired noob status for player: " + playerUuid);
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to remove expired noob status for player: " + playerUuid);
            throwable.printStackTrace();
            return null;
        });
    }
    
    /**
     * Cleans up expired noob statuses from database and cache
     * This method should be called periodically to clean up expired entries
     */
    public void cleanupExpiredStatuses() {
        // Clean up database
        noobStatusDao.deleteExpired().thenAccept(deletedCount -> {
            if (deletedCount > 0) {
                plugin.getLogger().info(String.format("Cleaned up %d expired noob statuses from database", deletedCount));
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to cleanup expired noob statuses from database");
            throwable.printStackTrace();
            return null;
        });
        
        // Clean up cache
        noobCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Gets the number of players currently marked as noob
     * @return Number of active noob players
     */
    public int getActiveNoobCount() {
        // Clean up expired entries first
        cleanupExpiredStatuses();
        return noobCache.size();
    }
    
    /**
     * Gets all players currently marked as noob
     * @return Map of player UUIDs to their NoobStatus objects
     */
    public Map<UUID, NoobStatus> getAllNoobPlayers() {
        // Clean up expired entries first
        cleanupExpiredStatuses();
        return new ConcurrentHashMap<>(noobCache);
    }
    
    /**
     * Clears all manual noob statuses
     * Useful for server restarts or admin commands
     */
    public void clearAllNoobStatuses() {
        noobStatusDao.deleteAll().thenAccept(deletedCount -> {
            plugin.getLogger().info(String.format("Cleared %d manual noob statuses from database", deletedCount));
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to clear noob statuses from database");
            throwable.printStackTrace();
            return null;
        });
        
        int cacheCount = noobCache.size();
        noobCache.clear();
        plugin.getLogger().info(String.format("Cleared %d manual noob statuses from cache", cacheCount));
    }
    
    /**
     * Shuts down the noob manager and performs cleanup
     */
    public void shutdown() {
        // Final cleanup of expired statuses
        cleanupExpiredStatuses();
        
        // Clear cache
        noobCache.clear();
        
        plugin.getLogger().info("NoobManager shutdown complete");
    }
}
