package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages update checking and notifications for the plugin.
 * Checks for updates from SpigotMC and notifies players when updates are available.
 */
public class UpdateChecker {
    
    private final LandClaimerPlugin plugin;
    private final ConfigurationManager configManager;
    
    // SpigotMC API endpoints
    private static final String SPIGOT_API_URL = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final String SPIGOT_RESOURCE_ID = "128966"; // Replace with actual resource ID
    
    // Update checking state
    private boolean updateAvailable = false;
    private String latestVersion = null;
    private String currentVersion;
    private long lastCheckTime = 0;
    private BukkitTask updateCheckTask = null;
    
    // Players who have been notified (to avoid spam)
    private final Set<UUID> notifiedPlayers = new HashSet<>();
    
    public UpdateChecker(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigurationManager();
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }
    
    /**
     * Starts the update checking system
     */
    public void start() {
        if (!configManager.isUpdateCheckEnabled()) {
            plugin.getLogger().info("Update checking is disabled in configuration.");
            return;
        }
        
        plugin.getLogger().info("Starting update checker...");
        
        // Perform initial check
        checkForUpdates();
        
        // Schedule periodic checks
        schedulePeriodicChecks();
    }
    
    /**
     * Stops the update checking system
     */
    public void stop() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }
    }
    
    /**
     * Checks for updates asynchronously
     */
    public void checkForUpdates() {
        if (!configManager.isUpdateCheckEnabled()) {
            return;
        }
        
        // Check if enough time has passed since last check
        long currentTime = System.currentTimeMillis();
        long checkInterval = configManager.getUpdateCheckIntervalHours() * 60 * 60 * 1000L;
        
        if (currentTime - lastCheckTime < checkInterval) {
            return;
        }
        
        plugin.getLogger().info("Checking for updates...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                URL url = URI.create(SPIGOT_API_URL + SPIGOT_RESOURCE_ID).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "FrontierGuard-UpdateChecker");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String latestVersion = reader.readLine();
                    reader.close();
                    
                    if (latestVersion != null && !latestVersion.isEmpty()) {
                        return latestVersion.trim();
                    }
                }
                
                connection.disconnect();
                return null;
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                return null;
            }
        }).thenAccept(latestVersion -> {
            // Run on main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    handleUpdateCheckResult(latestVersion);
                }
            }.runTask(plugin);
        });
    }
    
    /**
     * Handles the result of an update check
     */
    private void handleUpdateCheckResult(String latestVersion) {
        lastCheckTime = System.currentTimeMillis();
        
        if (latestVersion == null) {
            plugin.getLogger().warning("Could not retrieve latest version information.");
            return;
        }
        
        this.latestVersion = latestVersion;
        
        if (isNewerVersion(latestVersion, currentVersion)) {
            this.updateAvailable = true;
            plugin.getLogger().info("Update available! Current: " + currentVersion + ", Latest: " + latestVersion);
            
            // Notify online admins immediately
            notifyAdmins();
        } else {
            this.updateAvailable = false;
            plugin.getLogger().info("Plugin is up to date. Version: " + currentVersion);
        }
    }
    
    /**
     * Schedules periodic update checks
     */
    private void schedulePeriodicChecks() {
        long checkIntervalTicks = configManager.getUpdateCheckIntervalHours() * 60 * 60 * 20L; // Convert hours to ticks
        
        updateCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }.runTaskTimerAsynchronously(plugin, checkIntervalTicks, checkIntervalTicks);
    }
    
    /**
     * Notifies a player about available updates
     */
    public void notifyPlayer(Player player) {
        if (!updateAvailable || !configManager.isUpdateCheckEnabled()) {
            return;
        }
        
        // Check if we should only notify admins
        if (configManager.isUpdateNotifyAdminsOnly() && !player.hasPermission("frontierguard.admin")) {
            return;
        }
        
        // Check if player has already been notified
        if (notifiedPlayers.contains(player.getUniqueId())) {
            return;
        }
        
        // Mark player as notified
        notifiedPlayers.add(player.getUniqueId());
        
        // Send notification
        sendUpdateNotification(player);
    }
    
    /**
     * Notifies all online admins about updates
     */
    private void notifyAdmins() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("frontierguard.admin")) {
                notifyPlayer(player);
            }
        }
    }
    
    /**
     * Sends update notification to a player
     */
    private void sendUpdateNotification(Player player) {
        // Title notification
        Component title = Component.text("🔄 UPDATE AVAILABLE!", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component subtitle = Component.text("v" + latestVersion + " is now available!", NamedTextColor.YELLOW);
        
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(3000), // stay
            Duration.ofMillis(500)   // fade out
        );
        
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
        
        // Chat message
        Component message = Component.text("FrontierGuard Update Available!", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("\nCurrent Version: ", NamedTextColor.GRAY))
            .append(Component.text("v" + currentVersion, NamedTextColor.RED))
            .append(Component.text("\nLatest Version: ", NamedTextColor.GRAY))
            .append(Component.text("v" + latestVersion, NamedTextColor.GREEN))
            .append(Component.text("\nDownload: ", NamedTextColor.GRAY))
            .append(Component.text(configManager.getUpdateDownloadUrl(), NamedTextColor.AQUA, TextDecoration.UNDERLINED));
        
        player.sendMessage(message);
        
        // Action bar notification
        Component actionBar = Component.text("Type /fg update for more info", NamedTextColor.YELLOW);
        player.sendActionBar(actionBar);
        
        // Play notification sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    /**
     * Checks if a version string is newer than another
     */
    private boolean isNewerVersion(String version1, String version2) {
        try {
            // Simple version comparison (assumes semantic versioning)
            String[] v1Parts = version1.split("\\.");
            String[] v2Parts = version2.split("\\.");
            
            int maxLength = Math.max(v1Parts.length, v2Parts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
                
                if (v1Part > v2Part) {
                    return true;
                } else if (v1Part < v2Part) {
                    return false;
                }
            }
            
            return false;
        } catch (NumberFormatException e) {
            // Fallback to string comparison
            return version1.compareTo(version2) > 0;
        }
    }
    
    /**
     * Gets update information for display
     */
    public String getUpdateInfo() {
        if (!updateAvailable) {
            return "No updates available. Current version: " + currentVersion;
        }
        
        return "Update available! Current: " + currentVersion + ", Latest: " + latestVersion;
    }
    
    /**
     * Clears notified players list (useful for testing)
     */
    public void clearNotifiedPlayers() {
        notifiedPlayers.clear();
    }
    
    /**
     * Gets the current version
     */
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    /**
     * Gets the latest version
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * Checks if an update is available
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
