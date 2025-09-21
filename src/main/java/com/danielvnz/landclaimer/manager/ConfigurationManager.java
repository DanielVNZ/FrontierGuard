package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages plugin configuration loading, validation, and reloading.
 * Handles claim limits, messages, and other configurable settings.
 */
public class ConfigurationManager {
    
    private final LandClaimerPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Configuration values
    private final Map<String, Integer> claimLimits;
    private final Map<String, String> messages;
    private int showRadius;
    
    // Update check configuration
    private boolean updateCheckEnabled;
    private int updateCheckIntervalHours;
    private boolean updateNotifyOnJoin;
    private boolean updateNotifyAdminsOnly;
    private String updateDownloadUrl;
    
    public ConfigurationManager(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.claimLimits = new HashMap<>();
        this.messages = new HashMap<>();
        
        // Initialize configuration
        initializeConfig();
    }
    
    /**
     * Initializes the configuration file and loads values
     */
    private void initializeConfig() {
        // Create config file if it doesn't exist
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        // Load configuration
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load configuration values
        loadClaimLimits();
        loadMessages();
        loadShowRadius();
        loadUpdateCheckConfig();
        
        plugin.getLogger().info("Configuration loaded successfully");
    }
    
    /**
     * Loads claim limits from configuration
     */
    private void loadClaimLimits() {
        claimLimits.clear();
        
        ConfigurationSection claimLimitsSection = config.getConfigurationSection("claim-limits");
        if (claimLimitsSection != null) {
            for (String key : claimLimitsSection.getKeys(false)) {
                int limit = claimLimitsSection.getInt(key, 10);
                claimLimits.put(key, limit);
                plugin.getLogger().info("Loaded claim limit for " + key + ": " + limit);
            }
        } else {
            // Set default claim limits - all start at 1 since players can buy more
            claimLimits.put("default", 1);
            claimLimits.put("vip", 1);
            claimLimits.put("premium", 1);
            plugin.getLogger().warning("No claim-limits section found, using defaults (all start at 1)");
        }
    }
    
    /**
     * Loads messages from configuration
     */
    private void loadMessages() {
        messages.clear();
        
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            loadMessagesRecursively(messagesSection, "");
        } else {
            plugin.getLogger().warning("No messages section found in config");
        }
    }
    
    /**
     * Recursively loads messages from configuration sections
     * @param section The configuration section to load from
     * @param prefix The prefix for the message keys
     */
    private void loadMessagesRecursively(ConfigurationSection section, String prefix) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (section.isConfigurationSection(key)) {
                // Recursively load nested sections
                loadMessagesRecursively(section.getConfigurationSection(key), fullKey);
            } else {
                // Load message value
                String value = section.getString(key);
                if (value != null) {
                    messages.put(fullKey, value);
                }
            }
        }
    }
    
    /**
     * Loads show radius from configuration
     */
    private void loadShowRadius() {
        showRadius = config.getInt("show.radius", 3);
        
        // Validate radius (1-10)
        if (showRadius < 1 || showRadius > 10) {
            plugin.getLogger().warning("Invalid show radius in config: " + showRadius + ". Using default value: 3");
            showRadius = 3;
        }
        
        plugin.getLogger().info("Loaded show radius: " + showRadius);
    }
    
    /**
     * Loads update check configuration from config
     */
    private void loadUpdateCheckConfig() {
        updateCheckEnabled = config.getBoolean("update-check.enabled", true);
        updateCheckIntervalHours = config.getInt("update-check.check-interval-hours", 24);
        updateNotifyOnJoin = config.getBoolean("update-check.notify-on-join", true);
        updateNotifyAdminsOnly = config.getBoolean("update-check.notify-admins-only", false);
        updateDownloadUrl = config.getString("update-check.download-url", "https://spigotmc.org/resources/frontierguard");
        
        // Validate interval (minimum 1 hour)
        if (updateCheckIntervalHours < 1) {
            plugin.getLogger().warning("Invalid update check interval in config: " + updateCheckIntervalHours + ". Using default value: 24");
            updateCheckIntervalHours = 24;
        }
        
        plugin.getLogger().info("Loaded update check config - enabled: " + updateCheckEnabled + ", interval: " + updateCheckIntervalHours + " hours");
    }
    
    /**
     * Reloads the configuration from file
     * @return true if reload was successful, false otherwise
     */
    public boolean reloadConfig() {
        try {
            // Reload the configuration file
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Reload configuration values
            loadClaimLimits();
            loadMessages();
            loadShowRadius();
            loadUpdateCheckConfig();
            
            plugin.getLogger().info("Configuration reloaded successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configuration", e);
            return false;
        }
    }
    
    /**
     * Gets the claim limit for a specific permission group
     * @param permissionGroup The permission group to get the limit for
     * @return The claim limit for the group, or default if not found
     */
    public int getClaimLimit(String permissionGroup) {
        return claimLimits.getOrDefault(permissionGroup, claimLimits.getOrDefault("default", 10));
    }
    
    /**
     * Gets all available claim limit groups
     * @return Set of all claim limit group names
     */
    public Set<String> getClaimLimitGroups() {
        return claimLimits.keySet();
    }
    
    /**
     * Gets the mode change cooldown in hours
     * @return The cooldown hours, defaulting to 24 if not configured
     */
    public int getModeChangeCooldownHours() {
        return config.getInt("mode-change.cooldown-hours", 24);
    }
    
    /**
     * Gets a message from the configuration
     * @param key The message key
     * @return The message value with color codes translated, or the key itself if not found
     */
    public String getMessage(String key) {
        String message = messages.getOrDefault(key, key);
        return translateColorCodes(message);
    }
    
    /**
     * Gets a message from the configuration with a default value
     * @param key The message key
     * @param defaultValue The default value if key is not found
     * @return The message value with color codes translated, or default value
     */
    public String getMessage(String key, String defaultValue) {
        String message = messages.getOrDefault(key, defaultValue);
        return translateColorCodes(message);
    }
    
    /**
     * Gets the show radius for the /fg show command
     * @return The radius in chunks (1-10)
     */
    public int getShowRadius() {
        return showRadius;
    }
    
    /**
     * Sets a claim limit for a specific group
     * @param group The permission group
     * @param limit The claim limit
     */
    public void setClaimLimit(String group, int limit) {
        claimLimits.put(group, limit);
        
        // Update the configuration file
        config.set("claim-limits." + group, limit);
        saveConfig();
    }
    
    /**
     * Sets a message in the configuration
     * @param key The message key
     * @param value The message value
     */
    public void setMessage(String key, String value) {
        messages.put(key, value);
        
        // Update the configuration file
        config.set("messages." + key, value);
        saveConfig();
    }
    
    /**
     * Translates Minecraft color codes (&) to ChatColor codes
     * @param message The message containing color codes
     * @return The message with color codes translated
     */
    private String translateColorCodes(String message) {
        if (message == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Saves the configuration to file
     * @return true if save was successful, false otherwise
     */
    public boolean saveConfig() {
        try {
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration", e);
            return false;
        }
    }
    
    /**
     * Gets the raw configuration object
     * @return The FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Validates the configuration and logs any issues
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateConfig() {
        boolean valid = true;
        
        // Validate claim limits
        if (claimLimits.isEmpty()) {
            plugin.getLogger().warning("No claim limits configured, using defaults");
            valid = false;
        }
        
        // Validate required messages
        String[] requiredMessages = {
            "mode-selection-title",
            "peaceful-mode-name",
            "normal-mode-name",
            "claim-success",
            "claim-failed-already-claimed",
            "protection-block-break",
            "protection-block-place"
        };
        
        for (String messageKey : requiredMessages) {
            if (!messages.containsKey(messageKey)) {
                plugin.getLogger().warning("Missing required message: " + messageKey);
                valid = false;
            }
        }
        
        if (valid) {
            plugin.getLogger().info("Configuration validation passed");
        } else {
            plugin.getLogger().warning("Configuration validation failed - some features may not work correctly");
        }
        
        return valid;
    }
    
    /**
     * Gets configuration statistics for debugging
     * @return A string with configuration statistics
     */
    public String getConfigStats() {
        return String.format("Config Stats - Claim Limits: %d groups, Messages: %d entries", 
            claimLimits.size(), messages.size());
    }
    
    // Update check configuration getters
    
    /**
     * Checks if update checking is enabled
     * @return true if update checking is enabled
     */
    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }
    
    /**
     * Gets the update check interval in hours
     * @return The interval in hours
     */
    public int getUpdateCheckIntervalHours() {
        return updateCheckIntervalHours;
    }
    
    /**
     * Checks if players should be notified about updates when they join
     * @return true if players should be notified on join
     */
    public boolean isUpdateNotifyOnJoin() {
        return updateNotifyOnJoin;
    }
    
    /**
     * Checks if only admins should be notified about updates
     * @return true if only admins should be notified
     */
    public boolean isUpdateNotifyAdminsOnly() {
        return updateNotifyAdminsOnly;
    }
    
    /**
     * Gets the download URL for updates
     * @return The download URL
     */
    public String getUpdateDownloadUrl() {
        return updateDownloadUrl;
    }
}
