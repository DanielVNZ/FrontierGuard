package com.danielvnz.landclaimer;

import com.danielvnz.landclaimer.command.LandClaimerCommandExecutor;
import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.listener.BlockProtectionListener;
import com.danielvnz.landclaimer.listener.ExplosionProtectionListener;
import com.danielvnz.landclaimer.listener.GuiListener;
import com.danielvnz.landclaimer.listener.ModeSelectionListener;
import com.danielvnz.landclaimer.listener.PlayerJoinListener;
import com.danielvnz.landclaimer.listener.PlayerMoveListener;
import com.danielvnz.landclaimer.listener.PvpAreaSelectionListener;
import com.danielvnz.landclaimer.listener.PvpProtectionListener;
import com.danielvnz.landclaimer.manager.ClaimInvitationManager;
import com.danielvnz.landclaimer.manager.ClaimManager;
import com.danielvnz.landclaimer.manager.ConfigurationManager;
import com.danielvnz.landclaimer.manager.ErrorHandler;
import com.danielvnz.landclaimer.manager.GuiManager;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import com.danielvnz.landclaimer.manager.ProtectionManager;
import com.danielvnz.landclaimer.manager.PvpAreaManager;
import com.danielvnz.landclaimer.manager.PvpProtectionManager;
import com.danielvnz.landclaimer.manager.UpdateChecker;
import com.danielvnz.landclaimer.manager.VisualFeedbackManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for the LandClaimer plugin.
 * Handles plugin initialization, configuration loading, and component registration.
 */
public class LandClaimerPlugin extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private ConfigurationManager configurationManager;
    private ErrorHandler errorHandler;
    private PlayerModeManager playerModeManager;
    private ClaimManager claimManager;
    private ClaimInvitationManager invitationManager;
    private ProtectionManager protectionManager;
    private PvpAreaManager pvpAreaManager;
    private PvpProtectionManager pvpProtectionManager;
    private VisualFeedbackManager visualFeedbackManager;
    private GuiManager guiManager;
    private UpdateChecker updateChecker;
    private Economy economy;
    private boolean initialized = false;
    
    @Override
    public void onEnable() {
        try {
            getLogger().info("Enabling FrontierGuard plugin...");
            
            // Load configuration with defaults
            loadConfiguration();
            
            // Initialize database
            initializeDatabase();
            
            // Setup Vault economy (optional)
            if (!setupEconomy()) {
                getLogger().warning("Vault economy not found! Claim purchasing will be disabled.");
                getLogger().warning("To enable claim purchasing, install Vault + an economy plugin (like EssentialsX).");
                economy = null; // Set to null so GUI can handle it gracefully
            }
            
            // Initialize managers
            initializeManagers();
            
            // Start update checker
            updateChecker.start();
            
            // Register event listeners
            registerEventListeners();
            
            // Register command executors
            registerCommandExecutors();
            
            initialized = true;
            
            // Log plugin statistics
            if (errorHandler != null) {
                errorHandler.logPluginStats();
            }
            
            getLogger().info("LandClaimer plugin enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable LandClaimer plugin", e);
            
            // Use error handler if available
            if (errorHandler != null) {
                errorHandler.handlePluginError(null, "plugin enable", e);
            }
            
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling LandClaimer plugin...");
        
        try {
            // Stop update checker
            if (updateChecker != null) {
                updateChecker.stop();
                getLogger().info("Update checker stopped");
            }
            
            // Shutdown database connection
            if (databaseManager != null) {
                databaseManager.shutdown();
                getLogger().info("Database connection closed");
            }
            
            initialized = false;
            getLogger().info("LandClaimer plugin disabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during plugin shutdown", e);
        }
    }
    
    /**
     * Loads the plugin configuration with default values
     */
    private void loadConfiguration() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Set default configuration values
        getConfig().addDefault("claim-limits.default", 10);
        getConfig().addDefault("claim-limits.vip", 25);
        getConfig().addDefault("claim-limits.premium", 50);
        getConfig().addDefault("database.type", "sqlite");
        getConfig().addDefault("database.file", "landclaimer.db");
        getConfig().addDefault("messages.mode-selection-title", "Select Your Game Mode");
        getConfig().addDefault("messages.peaceful-mode-name", "Peaceful Mode");
        getConfig().addDefault("messages.normal-mode-name", "Normal Mode");
        getConfig().addDefault("messages.peaceful-mode-description", "PvE gameplay with land claiming protection");
        getConfig().addDefault("messages.normal-mode-description", "Full PvP survival gameplay");
        getConfig().addDefault("messages.claim-success", "Chunk claimed successfully!");
        getConfig().addDefault("messages.claim-failed-already-claimed", "This chunk is already claimed!");
        getConfig().addDefault("messages.claim-failed-limit-reached", "You have reached your claim limit!");
        getConfig().addDefault("messages.claim-failed-normal-mode", "Only peaceful players can claim chunks!");
        getConfig().addDefault("messages.unclaim-success", "Chunk unclaimed successfully!");
        getConfig().addDefault("messages.unclaim-failed-not-owner", "You don't own this chunk!");
        getConfig().addDefault("messages.protection-block-break", "You cannot break blocks in this protected area!");
        getConfig().addDefault("messages.protection-block-place", "You cannot place blocks in this protected area!");
        getConfig().addDefault("messages.protection-container-access", "You cannot access containers in this protected area!");
        getConfig().addDefault("messages.pvp-area-enter", "Warning: You are entering a PvP area!");
        getConfig().addDefault("messages.pvp-area-exit", "You have left the PvP area. Protection restored.");
        
        // Save config with defaults
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        getLogger().info("Configuration loaded with default values");
    }
    
    /**
     * Initializes the database connection and creates tables
     */
    private void initializeDatabase() {
        try {
            databaseManager = new DatabaseManager(getDataFolder());
            
            // Initialize database asynchronously but wait for completion during startup
            databaseManager.initialize().get();
            
            getLogger().info("Database initialized successfully");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Initializes all manager classes
     */
    private void initializeManagers() {
        configurationManager = new ConfigurationManager(this);
        errorHandler = new ErrorHandler(this);
        playerModeManager = new PlayerModeManager(this);
        visualFeedbackManager = new VisualFeedbackManager(this);
        updateChecker = new UpdateChecker(this);
        claimManager = new ClaimManager(this, playerModeManager);
        invitationManager = new ClaimInvitationManager(this, claimManager);
        pvpAreaManager = new PvpAreaManager(this);
        protectionManager = new ProtectionManager(this, claimManager, playerModeManager, pvpAreaManager);
        pvpProtectionManager = new PvpProtectionManager(this, playerModeManager, pvpAreaManager);
        guiManager = new GuiManager(this, playerModeManager, claimManager, economy);
        getLogger().info("Managers initialized successfully");
    }
    
    /**
     * Registers all event listeners
     */
    private void registerEventListeners() {
        // Register mode selection listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, playerModeManager), this);
        getServer().getPluginManager().registerEvents(new ModeSelectionListener(this, playerModeManager), this);
        
        // Register protection listeners
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this, protectionManager), this);
        getServer().getPluginManager().registerEvents(new PvpProtectionListener(this, pvpProtectionManager), this);
        getServer().getPluginManager().registerEvents(new ExplosionProtectionListener(this), this);
        
        // Register PVP area selection listener
        getServer().getPluginManager().registerEvents(new PvpAreaSelectionListener(this, pvpAreaManager), this);
        
        // Register player movement listener for PVP area entry/exit
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this, playerModeManager, pvpAreaManager, visualFeedbackManager, claimManager), this);
        
        // Register GUI listener
        getServer().getPluginManager().registerEvents(new GuiListener(this, guiManager), this);
        
        getLogger().info("Event listeners registered successfully");
    }
    
    /**
     * Registers all command executors
     */
    private void registerCommandExecutors() {
        LandClaimerCommandExecutor commandExecutor = new LandClaimerCommandExecutor(this);
        
        // Register main command
        getCommand("frontierguard").setExecutor(commandExecutor);
        getCommand("frontierguard").setTabCompleter(commandExecutor);
        
        getLogger().info("Command executors registered successfully");
    }
    
    /**
     * Reloads the plugin configuration
     */
    public void reloadConfiguration() {
        reloadConfig();
        getLogger().info("Configuration reloaded");
    }
    
    /**
     * Gets the database manager instance
     * @return The database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Gets the player mode manager instance
     * @return The player mode manager
     */
    public PlayerModeManager getPlayerModeManager() {
        return playerModeManager;
    }
    
    /**
     * Gets the claim manager instance
     * @return The claim manager
     */
    public ClaimManager getClaimManager() {
        return claimManager;
    }
    
    /**
     * Gets the claim invitation manager instance
     * @return The claim invitation manager
     */
    public ClaimInvitationManager getInvitationManager() {
        return invitationManager;
    }
    
    /**
     * Gets the visual feedback manager instance
     * @return The visual feedback manager
     */
    public VisualFeedbackManager getVisualFeedbackManager() {
        return visualFeedbackManager;
    }
    
    /**
     * Gets the update checker
     * @return The update checker
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    /**
     * Gets the protection manager instance
     * @return The protection manager
     */
    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
    
    /**
     * Gets the PVP area manager instance
     * @return The PVP area manager
     */
    public PvpAreaManager getPvpAreaManager() {
        return pvpAreaManager;
    }
    
    /**
     * Gets the PVP protection manager instance
     * @return The PVP protection manager
     */
    public PvpProtectionManager getPvpProtectionManager() {
        return pvpProtectionManager;
    }
    
    /**
     * Gets the configuration manager instance
     * @return The configuration manager
     */
    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
    
    /**
     * Gets the error handler instance
     * @return The error handler
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Gets a message from the configuration
     * @param key The message key
     * @return The message value, or the key itself if not found
     */
    public String getMessage(String key) {
        if (configurationManager != null) {
            return configurationManager.getMessage(key);
        }
        return key;
    }
    
    /**
     * Gets the GUI manager instance
     * @return The GUI manager
     */
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    /**
     * Gets the economy instance
     * @return The economy
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * Sets up Vault economy integration
     * @return true if economy was found and set up, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Checks if the plugin is fully initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    
}