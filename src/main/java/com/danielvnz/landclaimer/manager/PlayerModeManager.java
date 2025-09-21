package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.database.dao.PlayerDataDao;
import com.danielvnz.landclaimer.model.PlayerData;
import com.danielvnz.landclaimer.model.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player mode selection and persistence.
 * Handles GUI-based mode selection interface and database operations.
 */
public class PlayerModeManager {
    
    private final LandClaimerPlugin plugin;
    private final PlayerDataDao playerDataDao;
    private final Map<UUID, PlayerData> playerCache;
    
    // GUI constants
    private static final String MODE_SELECTION_TITLE = "Select Your Game Mode";
    private static final int PEACEFUL_SLOT = 3;
    private static final int NORMAL_SLOT = 5;
    private static final int GUI_SIZE = 9;
    
    public PlayerModeManager(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.playerDataDao = createPlayerDataDao(plugin.getDatabaseManager());
        this.playerCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates the PlayerDataDao instance. Can be overridden for testing.
     * @param databaseManager The database manager
     * @return The PlayerDataDao instance
     */
    protected PlayerDataDao createPlayerDataDao(DatabaseManager databaseManager) {
        return new PlayerDataDao(databaseManager);
    }
    
    /**
     * Prompts a player to select their game mode using a GUI interface
     * @param player The player to show the mode selection GUI to
     */
    public void promptModeSelection(Player player) {
        if (player == null) {
            plugin.getLogger().warning("Attempted to prompt mode selection for null player");
            return;
        }
        
        try {
            // Create inventory GUI
            String title = plugin.getMessage("mode-selection-title");
            if (title.equals("mode-selection-title")) {
                title = MODE_SELECTION_TITLE;
            }
            
            Inventory gui = Bukkit.createInventory(null, GUI_SIZE, Component.text(title));
            
            // Create peaceful mode item
            ItemStack peacefulItem = createModeItem(
                Material.EMERALD,
                plugin.getMessage("peaceful-mode-name"),
                plugin.getMessage("peaceful-mode-description"),
                "Click to select Peaceful Mode",
                "• Land claiming protection",
                "• PvE gameplay only",
                "• Protected from PvP outside designated areas"
            );
            
            // Create normal mode item
            ItemStack normalItem = createModeItem(
                Material.DIAMOND_SWORD,
                plugin.getMessage("normal-mode-name"),
                plugin.getMessage("normal-mode-description"),
                "Click to select Normal Mode",
                "• Full PvP survival gameplay",
                "• No land claiming available",
                "• Can raid and be raided"
            );
            
            // Place items in GUI
            gui.setItem(PEACEFUL_SLOT, peacefulItem);
            gui.setItem(NORMAL_SLOT, normalItem);
            
            // Open GUI for player
            player.openInventory(gui);
            
            plugin.getLogger().info("Opened mode selection GUI for player: " + player.getName());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening mode selection GUI for player: " + player.getName(), e);
            player.sendMessage(Component.text("Error opening mode selection. Please contact an administrator.", NamedTextColor.RED));
        }
    }
    
    /**
     * Sets a player's mode and persists it to the database
     * @param player The player whose mode to set
     * @param mode The mode to set
     */
    public void setPlayerMode(Player player, PlayerMode mode) {
        if (player == null || mode == null) {
            plugin.getLogger().warning("Attempted to set mode with null player or mode");
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        try {
            // Get or create player data
            PlayerData playerData = playerCache.get(playerUuid);
            if (playerData == null) {
                playerData = new PlayerData(playerUuid, LocalDateTime.now());
            }
            
            // Set the mode
            playerData.setMode(mode);
            
            // Update cache
            playerCache.put(playerUuid, playerData);
            
            // Save to database asynchronously
            playerDataDao.save(playerData).thenRun(() -> {
                plugin.getLogger().info("Successfully saved mode " + mode.getDisplayName() + " for player: " + player.getName());
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player mode for: " + player.getName(), throwable);
                return null;
            });
            
            // Send confirmation message
            String modeDisplayName = mode == PlayerMode.PEACEFUL ? "Peaceful" : "Normal";
            Component message = Component.text("You have selected ", NamedTextColor.GREEN)
                .append(Component.text(modeDisplayName, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" mode!", NamedTextColor.GREEN));
            player.sendMessage(message);
            
            // Close any open inventory
            player.closeInventory();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting player mode for: " + player.getName(), e);
            player.sendMessage(Component.text("Error setting your mode. Please try again or contact an administrator.", NamedTextColor.RED));
        }
    }
    
    /**
     * Changes a player's mode with cooldown check
     * @param player The player whose mode to change
     * @param newMode The new mode to set
     * @return true if the mode was changed successfully, false if on cooldown
     */
    public boolean changePlayerMode(Player player, PlayerMode newMode) {
        if (player == null || newMode == null) {
            plugin.getLogger().warning("Attempted to change mode with null player or mode");
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        try {
            // Get player data
            PlayerData playerData = playerCache.get(playerUuid);
            if (playerData == null) {
                // Load from database if not in cache
                Optional<PlayerData> result = playerDataDao.findById(playerUuid).get();
                if (result.isPresent()) {
                    playerData = result.get();
                    playerCache.put(playerUuid, playerData);
                } else {
                    player.sendMessage(Component.text("Error: Player data not found. Please contact an administrator.", NamedTextColor.RED));
                    return false;
                }
            }
            
            // Check if player can change mode (cooldown check)
            int cooldownHours = plugin.getConfigurationManager().getModeChangeCooldownHours();
            if (!playerData.canChangeMode(cooldownHours)) {
                String timeRemaining = playerData.getFormattedTimeUntilModeChange(cooldownHours);
                Component message = Component.text("You cannot change your mode yet! ", NamedTextColor.RED)
                    .append(Component.text("Time remaining: " + timeRemaining, NamedTextColor.YELLOW));
                player.sendMessage(message);
                return false;
            }
            
            // Check if trying to change to the same mode
            if (playerData.getMode() == newMode) {
                String currentMode = newMode == PlayerMode.PEACEFUL ? "Peaceful" : "Normal";
                Component message = Component.text("You are already in " + currentMode + " mode!", NamedTextColor.YELLOW);
                player.sendMessage(message);
                return false;
            }
            
            // Change the mode with timestamp
            PlayerMode oldMode = playerData.getMode();
            playerData.setModeWithTimestamp(newMode);
            
            // Update cache
            playerCache.put(playerUuid, playerData);
            
            // If switching from peaceful to normal, delete all claims and remove invitations
            if (oldMode == PlayerMode.PEACEFUL && newMode == PlayerMode.NORMAL) {
                plugin.getClaimManager().deleteAllPlayerClaims(player).thenAccept(claimCount -> {
                    if (claimCount > 0) {
                        // Send message about deleted claims
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Component claimMessage = Component.text("Deleted " + claimCount + " claims due to mode change.", NamedTextColor.YELLOW);
                                player.sendMessage(claimMessage);
                            }
                        }.runTask(plugin);
                    }
                });
                
                // Remove all invitations for this player
                plugin.getInvitationManager().removeAllPlayerInvitations(playerUuid).thenRun(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Component invitationMessage = Component.text("Removed access to all invited claims due to mode change.", NamedTextColor.YELLOW);
                            player.sendMessage(invitationMessage);
                        }
                    }.runTask(plugin);
                });
            }
            
            // Save to database asynchronously
            playerDataDao.save(playerData).thenRun(() -> {
                plugin.getLogger().info("Successfully changed mode from " + 
                    (oldMode != null ? oldMode.getDisplayName() : "none") + 
                    " to " + newMode.getDisplayName() + " for player: " + player.getName());
            }).exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player mode change for: " + player.getName(), throwable);
                return null;
            });
            
            // Clear revoked invitations cache if player changed to peaceful mode
            if (newMode == PlayerMode.PEACEFUL) {
                plugin.getProtectionManager().clearRevokedInvitations(player.getUniqueId());
            }
            
            // Send confirmation message
            String oldModeName = oldMode == PlayerMode.PEACEFUL ? "Peaceful" : (oldMode == PlayerMode.NORMAL ? "Normal" : "None");
            String newModeName = newMode == PlayerMode.PEACEFUL ? "Peaceful" : "Normal";
            Component message = Component.text("Mode changed from ", NamedTextColor.GREEN)
                .append(Component.text(oldModeName, NamedTextColor.YELLOW))
                .append(Component.text(" to ", NamedTextColor.GREEN))
                .append(Component.text(newModeName, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("! You can change again in 24 hours.", NamedTextColor.GRAY));
            player.sendMessage(message);
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error changing player mode for: " + player.getName(), e);
            player.sendMessage(Component.text("Error changing your mode. Please try again or contact an administrator.", NamedTextColor.RED));
            return false;
        }
    }
    
    /**
     * Gets a player's current mode
     * @param player The player whose mode to get
     * @return The player's mode, or null if not set
     */
    public PlayerMode getPlayerMode(Player player) {
        if (player == null) {
            return null;
        }
        
        return getPlayerMode(player.getUniqueId());
    }
    
    /**
     * Gets a player's current mode by UUID
     * @param playerUuid The player's UUID
     * @return The player's mode, or null if not set
     */
    public PlayerMode getPlayerMode(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        
        PlayerData playerData = playerCache.get(playerUuid);
        if (playerData != null) {
            return playerData.getMode();
        }
        
        // If not in cache, try to load from database synchronously for immediate access
        try {
            Optional<PlayerData> result = playerDataDao.findById(playerUuid).get();
            if (result.isPresent()) {
                playerData = result.get();
                playerCache.put(playerUuid, playerData);
                return playerData.getMode();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading player mode from database for UUID: " + playerUuid, e);
        }
        
        return null;
    }
    
    /**
     * Checks if a player can change their mode (cooldown check)
     * @param player The player to check
     * @return true if the player can change mode, false if on cooldown
     */
    public boolean canChangeMode(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        try {
            // Get player data
            PlayerData playerData = playerCache.get(playerUuid);
            if (playerData == null) {
                // Load from database if not in cache
                Optional<PlayerData> result = playerDataDao.findById(playerUuid).get();
                if (result.isPresent()) {
                    playerData = result.get();
                    playerCache.put(playerUuid, playerData);
                } else {
                    return false;
                }
            }
            
            // Get cooldown hours from configuration
            int cooldownHours = plugin.getConfigurationManager().getModeChangeCooldownHours();
            return playerData.canChangeMode(cooldownHours);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking if player can change mode: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Checks if a player is in peaceful mode
     * @param player The player to check
     * @return true if the player is in peaceful mode, false otherwise
     */
    public boolean isPeacefulPlayer(Player player) {
        return getPlayerMode(player) == PlayerMode.PEACEFUL;
    }
    
    /**
     * Checks if a player is in normal mode
     * @param player The player to check
     * @return true if the player is in normal mode, false otherwise
     */
    public boolean isNormalPlayer(Player player) {
        return getPlayerMode(player) == PlayerMode.NORMAL;
    }
    
    /**
     * Checks if a player has selected a mode
     * @param player The player to check
     * @return true if the player has selected a mode, false otherwise
     */
    public boolean hasSelectedMode(Player player) {
        if (player == null) {
            return false;
        }
        
        return hasSelectedMode(player.getUniqueId());
    }
    
    /**
     * Checks if a player has selected a mode by UUID
     * @param playerUuid The player's UUID
     * @return true if the player has selected a mode, false otherwise
     */
    public boolean hasSelectedMode(UUID playerUuid) {
        PlayerData playerData = playerCache.get(playerUuid);
        if (playerData != null) {
            return playerData.hasSelectedMode();
        }
        
        // Check database if not in cache
        try {
            Optional<PlayerData> result = playerDataDao.findById(playerUuid).get();
            if (result.isPresent()) {
                playerData = result.get();
                playerCache.put(playerUuid, playerData);
                return playerData.hasSelectedMode();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking mode selection status for UUID: " + playerUuid, e);
        }
        
        return false;
    }
    
    /**
     * Loads player data into cache when they join
     * @param player The player who joined
     */
    public void loadPlayerData(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Load player data asynchronously
        playerDataDao.findById(playerUuid).thenAccept(optionalPlayerData -> {
            if (optionalPlayerData.isPresent()) {
                PlayerData playerData = optionalPlayerData.get();
                playerCache.put(playerUuid, playerData);
                plugin.getLogger().info("Loaded player data for: " + player.getName());
            } else {
                // Create new player data for first-time players
                PlayerData newPlayerData = new PlayerData(playerUuid, LocalDateTime.now());
                playerCache.put(playerUuid, newPlayerData);
                plugin.getLogger().info("Created new player data for: " + player.getName());
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for: " + player.getName(), throwable);
            return null;
        });
    }
    
    /**
     * Removes player data from cache when they leave
     * @param player The player who left
     */
    public void unloadPlayerData(Player player) {
        if (player != null) {
            playerCache.remove(player.getUniqueId());
        }
    }
    
    /**
     * Handles mode selection from GUI clicks
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @return true if the click was handled, false otherwise
     */
    public boolean handleModeSelection(Player player, int slot) {
        if (player == null) {
            return false;
        }
        
        PlayerMode selectedMode = null;
        
        if (slot == PEACEFUL_SLOT) {
            selectedMode = PlayerMode.PEACEFUL;
        } else if (slot == NORMAL_SLOT) {
            selectedMode = PlayerMode.NORMAL;
        }
        
        if (selectedMode != null) {
            setPlayerMode(player, selectedMode);
            return true;
        }
        
        return false;
    }
    
    /**
     * Creates an item for the mode selection GUI
     * @param material The material for the item
     * @param name The display name
     * @param description The main description
     * @param lore Additional lore lines
     * @return The created ItemStack
     */
    private ItemStack createModeItem(Material material, String name, String description, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String displayName = name;
            if (displayName.equals("peaceful-mode-name") || displayName.equals("normal-mode-name")) {
                displayName = material == Material.EMERALD ? "Peaceful Mode" : "Normal Mode";
            }
            meta.displayName(Component.text(displayName, NamedTextColor.WHITE, TextDecoration.BOLD));
            
            // Set lore
            String desc = description;
            if (desc.equals("peaceful-mode-description")) {
                desc = "PvE gameplay with land claiming protection";
            } else if (desc.equals("normal-mode-description")) {
                desc = "Full PvP survival gameplay";
            }
            
            // Build lore list
            List<Component> loreList = new ArrayList<>();
            loreList.add(Component.text(desc, NamedTextColor.GRAY));
            loreList.add(Component.empty());
            
            for (String loreLine : lore) {
                loreList.add(Component.text(loreLine, NamedTextColor.YELLOW));
            }
            
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Gets the title used for the mode selection GUI
     * @return The GUI title
     */
    public String getModeSelectionTitle() {
        String title = plugin.getMessage("mode-selection-title");
        return title.equals("mode-selection-title") ? MODE_SELECTION_TITLE : title;
    }
    
    /**
     * Checks if a player can change their mode and shows cooldown information
     * @param player The player to check
     * @return true if the player can change mode, false if on cooldown
     */
    public boolean checkModeChangeCooldown(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        try {
            // Get player data
            PlayerData playerData = playerCache.get(playerUuid);
            if (playerData == null) {
                // Load from database if not in cache
                Optional<PlayerData> result = playerDataDao.findById(playerUuid).get();
                if (result.isPresent()) {
                    playerData = result.get();
                    playerCache.put(playerUuid, playerData);
                } else {
                    player.sendMessage(Component.text("Error: Player data not found. Please contact an administrator.", NamedTextColor.RED));
                    return false;
                }
            }
            
            // Get cooldown hours from configuration
            int cooldownHours = plugin.getConfigurationManager().getModeChangeCooldownHours();
            
            if (playerData.canChangeMode(cooldownHours)) {
                Component message = Component.text("You can change your mode now!", NamedTextColor.GREEN);
                player.sendMessage(message);
                return true;
            } else {
                String timeRemaining = playerData.getFormattedTimeUntilModeChange(cooldownHours);
                Component message = Component.text("You cannot change your mode yet! ", NamedTextColor.RED)
                    .append(Component.text("Time remaining: " + timeRemaining, NamedTextColor.YELLOW));
                player.sendMessage(message);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking mode change cooldown for: " + player.getName(), e);
            player.sendMessage(Component.text("Error checking cooldown. Please contact an administrator.", NamedTextColor.RED));
            return false;
        }
    }
}