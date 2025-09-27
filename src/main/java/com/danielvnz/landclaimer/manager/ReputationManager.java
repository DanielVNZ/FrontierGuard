package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.dao.PlayerReputationDao;
import com.danielvnz.landclaimer.model.PlayerReputation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player reputation system including tracking, calculations, and regeneration.
 * Handles reputation changes from PVP events and playtime accumulation.
 */
public class ReputationManager {
    
    private final LandClaimerPlugin plugin;
    private final PlayerReputationDao reputationDao;
    private final PlayerModeManager playerModeManager;
    private final PvpAreaManager pvpAreaManager;
    
    // Cache for player reputations to improve performance
    private final Map<UUID, PlayerReputation> reputationCache = new ConcurrentHashMap<>();
    
    // Playtime tracking
    private final Map<UUID, LocalDateTime> playerSessionStart = new ConcurrentHashMap<>();
    private BukkitTask reputationRegenerationTask;
    
    public ReputationManager(LandClaimerPlugin plugin, PlayerReputationDao reputationDao, 
                           PlayerModeManager playerModeManager, PvpAreaManager pvpAreaManager) {
        this.plugin = plugin;
        this.reputationDao = reputationDao;
        this.playerModeManager = playerModeManager;
        this.pvpAreaManager = pvpAreaManager;
        
        // Start the reputation regeneration task
        startReputationRegenerationTask();
    }
    
    /**
     * Gets a player's reputation, loading from database if not cached
     * @param player The player to get reputation for
     * @return CompletableFuture containing the player's reputation
     */
    public CompletableFuture<PlayerReputation> getPlayerReputation(Player player) {
        if (player == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Check cache first
        PlayerReputation cached = reputationCache.get(playerUuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Load from database
        return reputationDao.findById(playerUuid).thenApply(optional -> {
            if (optional.isPresent()) {
                PlayerReputation reputation = optional.get();
                reputationCache.put(playerUuid, reputation);
                return reputation;
            } else {
                // Create new reputation record for new players
                PlayerReputation newReputation = new PlayerReputation(playerUuid);
                savePlayerReputation(newReputation);
                reputationCache.put(playerUuid, newReputation);
                return newReputation;
            }
        });
    }
    
    /**
     * Gets a player's reputation synchronously (from cache only)
     * @param player The player to get reputation for
     * @return The player's reputation from cache, or null if not cached
     */
    public PlayerReputation getPlayerReputationSync(Player player) {
        if (player == null) {
            return null;
        }
        return reputationCache.get(player.getUniqueId());
    }
    
    /**
     * Saves a player's reputation to the database
     * @param reputation The reputation to save
     */
    public void savePlayerReputation(PlayerReputation reputation) {
        if (reputation == null) {
            return;
        }
        
        reputationCache.put(reputation.getUuid(), reputation);
        reputationDao.save(reputation).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player reputation for " + reputation.getUuid(), throwable);
            return null;
        });
    }
    
    /**
     * Handles PVP kill event and applies reputation changes
     * @param killer The player who killed another player
     * @param victim The player who was killed
     * @param location The location where the kill occurred
     */
    public void handlePvpKill(Player killer, Player victim, org.bukkit.Location location) {
        if (killer == null || victim == null || location == null) {
            return;
        }
        
        // Only apply reputation changes to normal players
        if (!playerModeManager.isNormalPlayer(killer)) {
            return;
        }
        
        // Check if both players are normal mode
        if (!playerModeManager.isNormalPlayer(victim)) {
            return;
        }
        
        // Check if the kill happened outside a PVP zone
        if (pvpAreaManager.isInPvpArea(location)) {
            return; // No reputation penalty for kills in PVP zones
        }
        
        // Apply reputation penalty
        getPlayerReputation(killer).thenAccept(reputation -> {
            if (reputation != null) {
                int oldReputation = reputation.getReputation();
                int change = reputation.addReputation(-1);
                
                if (change != 0) {
                    savePlayerReputation(reputation);
                    
                    // Send message to killer
                    killer.sendMessage(Component.text(
                        String.format("You lost %d reputation for killing %s outside a PVP zone! (New reputation: %d)", 
                                    Math.abs(change), victim.getName(), reputation.getReputation()),
                        NamedTextColor.RED));
                    
                    plugin.getLogger().info(String.format("Player %s lost %d reputation for killing %s outside PVP zone. Reputation changed from %d to %d", 
                                                         killer.getName(), Math.abs(change), victim.getName(), oldReputation, reputation.getReputation()));
                }
            }
        });
    }
    
    /**
     * Manually sets a player's reputation (admin command)
     * @param player The player to set reputation for
     * @param newReputation The new reputation value
     * @return CompletableFuture containing the actual reputation set (may be clamped)
     */
    public CompletableFuture<Integer> setPlayerReputation(Player player, int newReputation) {
        if (player == null) {
            return CompletableFuture.completedFuture(0);
        }
        
        return getPlayerReputation(player).thenApply(reputation -> {
            if (reputation != null) {
                int oldReputation = reputation.getReputation();
                reputation.setReputation(newReputation);
                savePlayerReputation(reputation);
                
                plugin.getLogger().info(String.format("Admin set %s's reputation from %d to %d", 
                                                     player.getName(), oldReputation, reputation.getReputation()));
                
                return reputation.getReputation();
            }
            return 0;
        });
    }
    
    /**
     * Adds to a player's reputation (admin command)
     * @param player The player to add reputation to
     * @param amount The amount to add (can be negative)
     * @return CompletableFuture containing the actual amount added
     */
    public CompletableFuture<Integer> addPlayerReputation(Player player, int amount) {
        if (player == null) {
            return CompletableFuture.completedFuture(0);
        }
        
        return getPlayerReputation(player).thenApply(reputation -> {
            if (reputation != null) {
                int change = reputation.addReputation(amount);
                savePlayerReputation(reputation);
                
                plugin.getLogger().info(String.format("Admin %s %d reputation to %s. New reputation: %d", 
                                                     amount >= 0 ? "added" : "removed", Math.abs(amount), 
                                                     player.getName(), reputation.getReputation()));
                
                return change;
            }
            return 0;
        });
    }
    
    /**
     * Starts tracking a player's session for playtime calculation
     * @param player The player to start tracking
     */
    public void startPlayerSession(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        playerSessionStart.put(playerUuid, LocalDateTime.now());
        
        // Load player reputation if not cached
        getPlayerReputation(player);
    }
    
    /**
     * Stops tracking a player's session and updates playtime
     * @param player The player to stop tracking
     */
    public void endPlayerSession(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        LocalDateTime sessionStart = playerSessionStart.remove(playerUuid);
        
        if (sessionStart != null) {
            // Calculate session duration and update playtime
            double sessionHours = java.time.Duration.between(sessionStart, LocalDateTime.now()).toMinutes() / 60.0;
            
            if (sessionHours > 0) {
                getPlayerReputation(player).thenAccept(reputation -> {
                    if (reputation != null) {
                        int reputationGain = reputation.addPlaytime(sessionHours);
                        reputation.setLastPlaytimeUpdate(LocalDateTime.now());
                        savePlayerReputation(reputation);
                        
                        if (reputationGain > 0) {
                            player.sendMessage(Component.text(
                                String.format("You gained %d reputation from %d hours of playtime! (New reputation: %d)", 
                                            reputationGain, (int) Math.floor(sessionHours), reputation.getReputation()),
                                NamedTextColor.GREEN));
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Starts the reputation regeneration task that runs every hour
     */
    private void startReputationRegenerationTask() {
        reputationRegenerationTask = new BukkitRunnable() {
            @Override
            public void run() {
                processReputationRegeneration();
            }
        }.runTaskTimer(plugin, 20L * 60 * 60, 20L * 60 * 60); // Run every hour
        
        plugin.getLogger().info("Started reputation regeneration task (runs every hour)");
    }
    
    /**
     * Processes reputation regeneration for all online players
     */
    private void processReputationRegeneration() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerModeManager.isNormalPlayer(player)) {
                getPlayerReputation(player).thenAccept(reputation -> {
                    if (reputation != null) {
                        // Check if enough time has passed for reputation gain
                        LocalDateTime lastUpdate = reputation.getLastPlaytimeUpdate();
                        LocalDateTime now = LocalDateTime.now();
                        
                        long hoursSinceUpdate = java.time.Duration.between(lastUpdate, now).toHours();
                        
                        if (hoursSinceUpdate >= 1) {
                            int reputationGain = reputation.addReputation((int) hoursSinceUpdate);
                            reputation.setLastPlaytimeUpdate(now);
                            savePlayerReputation(reputation);
                            
                            if (reputationGain > 0) {
                                player.sendMessage(Component.text(
                                    String.format("You gained %d reputation from %d hours of playtime! (New reputation: %d)", 
                                                reputationGain, (int) hoursSinceUpdate, reputation.getReputation()),
                                    NamedTextColor.GREEN));
                            }
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Gets the top players by reputation
     * @param limit The maximum number of players to return
     * @return CompletableFuture containing the top players
     */
    public CompletableFuture<java.util.List<PlayerReputation>> getTopPlayers(int limit) {
        return reputationDao.getTopPlayers(limit);
    }
    
    /**
     * Gets the bottom players by reputation (most negative)
     * @param limit The maximum number of players to return
     * @return CompletableFuture containing the bottom players
     */
    public CompletableFuture<java.util.List<PlayerReputation>> getBottomPlayers(int limit) {
        return reputationDao.getBottomPlayers(limit);
    }
    
    /**
     * Clears the reputation cache for a player
     * @param playerUuid The player's UUID
     */
    public void clearPlayerCache(UUID playerUuid) {
        reputationCache.remove(playerUuid);
    }
    
    /**
     * Shuts down the reputation manager and stops all tasks
     */
    public void shutdown() {
        if (reputationRegenerationTask != null) {
            reputationRegenerationTask.cancel();
        }
        
        // Save all cached reputations before shutdown
        for (PlayerReputation reputation : reputationCache.values()) {
            reputationDao.save(reputation);
        }
        
        reputationCache.clear();
        playerSessionStart.clear();
        
        plugin.getLogger().info("ReputationManager shutdown complete");
    }
}
