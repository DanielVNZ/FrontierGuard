package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.dao.PurchasedClaimsDao;
import com.danielvnz.landclaimer.model.InvitationPermissions;
import com.danielvnz.landclaimer.model.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages GUI interfaces for the FrontierGuard plugin.
 * Handles main menu, mode selection, and claim purchasing.
 */
public class GuiManager {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    private final ClaimManager claimManager;
    private final ClaimInvitationManager invitationManager;
    private final Economy economy;
    private final PurchasedClaimsDao purchasedClaimsDao;
    
    // GUI slot constants
    private static final int MODE_CHANGE_SLOT = 10;
    private static final int BUY_CLAIMS_SLOT = 12;
    private static final int CLAIM_INFO_SLOT = 14;
    private static final int MANAGE_INVITATIONS_SLOT = 16;
    private static final int CLOSE_SLOT = 22;
    
    // Map to store pending mode changes for confirmation
    private final java.util.Map<UUID, PlayerMode> pendingModeChanges = new java.util.HashMap<>();
    
    public GuiManager(LandClaimerPlugin plugin, PlayerModeManager playerModeManager, ClaimManager claimManager, Economy economy) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
        this.claimManager = claimManager;
        this.invitationManager = plugin.getInvitationManager();
        this.economy = economy;
        this.purchasedClaimsDao = createPurchasedClaimsDao(plugin.getDatabaseManager());
    }
    
    /**
     * Creates the PurchasedClaimsDao instance. Can be overridden for testing.
     * @param databaseManager The database manager
     * @return The PurchasedClaimsDao instance
     */
    protected PurchasedClaimsDao createPurchasedClaimsDao(com.danielvnz.landclaimer.database.DatabaseManager databaseManager) {
        return new PurchasedClaimsDao(databaseManager);
    }
    
    /**
     * Opens the main GUI for a player
     * @param player The player to open the GUI for
     */
    public void openMainGui(Player player) {
        if (player == null) {
            plugin.getLogger().warning("Attempted to open GUI for null player");
            return;
        }
        
        try {
            Inventory gui = Bukkit.createInventory(null, 27, Component.text("FrontierGuard Menu"));
            
            // Mode change item
            ItemStack modeItem = createModeChangeItem(player);
            gui.setItem(MODE_CHANGE_SLOT, modeItem);
            
            // Buy claims item
            ItemStack buyClaimsItem = createBuyClaimsItem(player);
            gui.setItem(BUY_CLAIMS_SLOT, buyClaimsItem);
            
            // Claim info item
            ItemStack claimInfoItem = createClaimInfoItem(player);
            gui.setItem(CLAIM_INFO_SLOT, claimInfoItem);
            
            // Manage invitations item (only for peaceful players in their own claims)
            if (playerModeManager.isPeacefulPlayer(player)) {
                var claim = claimManager.getClaimInfo(player.getLocation().getChunk());
                if (claim != null && claim.getOwnerUuid().equals(player.getUniqueId())) {
                    ItemStack manageInvitationsItem = createManageInvitationsItem(player);
                    gui.setItem(MANAGE_INVITATIONS_SLOT, manageInvitationsItem);
                }
            }
            
            // Close item
            ItemStack closeItem = createCloseItem();
            gui.setItem(CLOSE_SLOT, closeItem);
            
            player.openInventory(gui);
            plugin.getLogger().info("Opened main GUI for player: " + player.getName());
            
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error opening main GUI for player: " + player.getName(), e);
            player.sendMessage(Component.text("Error opening GUI. Please contact an administrator.", NamedTextColor.RED));
        }
    }
    
    /**
     * Handles GUI click events
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @return true if the click was handled, false otherwise
     */
    public boolean handleGuiClick(Player player, int slot) {
        if (player == null) {
            return false;
        }
        
        plugin.getLogger().info("handleGuiClick called for player: " + player.getName() + ", slot: " + slot);
        
        switch (slot) {
            case MODE_CHANGE_SLOT:
                plugin.getLogger().info("Handling mode change click");
                return handleModeChangeClick(player);
            case BUY_CLAIMS_SLOT:
                plugin.getLogger().info("Handling buy claims click");
                return handleBuyClaimsClick(player);
            case CLAIM_INFO_SLOT:
                plugin.getLogger().info("Handling claim info click");
                return handleClaimInfoClick(player);
            case MANAGE_INVITATIONS_SLOT:
                plugin.getLogger().info("Handling manage invitations click - opening invitation management GUI");
                openInvitationManagementGui(player);
                return true;
            case CLOSE_SLOT:
                plugin.getLogger().info("Handling close click");
                player.closeInventory();
                return true;
            default:
                plugin.getLogger().info("Unknown slot clicked: " + slot);
                return false;
        }
    }
    
    /**
     * Handles mode change confirmation response
     * @param player The player confirming
     * @param confirmed Whether they confirmed the change
     * @return true if handled
     */
    public boolean handleModeChangeConfirmation(Player player, boolean confirmed) {
        UUID playerUuid = player.getUniqueId();
        
        if (!pendingModeChanges.containsKey(playerUuid)) {
            player.sendMessage(Component.text("You don't have a pending mode change to confirm.", NamedTextColor.RED));
            return false;
        }
        
        PlayerMode pendingMode = pendingModeChanges.remove(playerUuid);
        
        if (confirmed) {
            // Show confirmation message
            String confirmMessage = plugin.getMessage("mode-change-confirmed");
            if (confirmMessage.equals("mode-change-confirmed")) {
                confirmMessage = "&a&lMode change confirmed! Changing to &6{0}&a mode...";
            }
            confirmMessage = confirmMessage.replace("{0}", pendingMode.name().toLowerCase());
            
            net.kyori.adventure.text.Component confirmComponent = 
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(confirmMessage);
            player.sendMessage(confirmComponent);
            
            // Actually change the mode
            playerModeManager.changePlayerMode(player, pendingMode);
        } else {
            // Show cancellation message
            String cancelMessage = plugin.getMessage("mode-change-cancelled");
            if (cancelMessage.equals("mode-change-cancelled")) {
                cancelMessage = "&c&lMode change cancelled. You remain in &6{0}&c mode.";
            }
            cancelMessage = cancelMessage.replace("{0}", playerModeManager.getPlayerMode(player).name().toLowerCase());
            
            net.kyori.adventure.text.Component cancelComponent = 
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(cancelMessage);
            player.sendMessage(cancelComponent);
        }
        
        return true;
    }
    
    /**
     * Handles mode change click
     */
    private boolean handleModeChangeClick(Player player) {
        // Check permission first
        if (!player.hasPermission("frontierguard.changemode")) {
            player.sendMessage(Component.text("You don't have permission to change your mode!", NamedTextColor.RED));
            return true;
        }
        
        PlayerMode currentMode = playerModeManager.getPlayerMode(player);
        PlayerMode newMode = (currentMode == PlayerMode.PEACEFUL) ? PlayerMode.NORMAL : PlayerMode.PEACEFUL;
        
        // Check if player can change mode
        if (!playerModeManager.canChangeMode(player)) {
            playerModeManager.checkModeChangeCooldown(player);
            return true;
        }
        
        // Store pending mode change and show confirmation
        pendingModeChanges.put(player.getUniqueId(), newMode);
        showModeChangeConfirmation(player, newMode);
        return true;
    }
    
    /**
     * Handles buy claims click
     */
    private boolean handleBuyClaimsClick(Player player) {
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text("Only peaceful players can buy claims!", NamedTextColor.RED));
            return true;
        }
        
        if (economy == null) {
            player.sendMessage(Component.text("Economy system not available! Please contact an administrator.", NamedTextColor.RED));
            return true;
        }
        
        // Calculate current claim count and next purchase price
        CompletableFuture.supplyAsync(() -> {
            try {
                return claimManager.getPlayerClaims(player).size();
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Error getting player claims for GUI", e);
                return 0;
            }
        }).thenAccept(claimedCount -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Use total claim limit (including purchased claims) for pricing
                int totalClaimLimit = getMaxClaims(player);
                int nextPrice = calculateClaimPrice(totalClaimLimit);
                double balance = economy.getBalance(player);
                
                if (balance >= nextPrice) {
                    // Process the purchase
                    economy.withdrawPlayer(player, nextPrice);
                    
                    // Increase player's purchased claims and wait for completion
                    purchasedClaimsDao.incrementPurchasedClaims(player.getUniqueId()).thenAccept(newPurchasedCount -> {
                        // Refresh the GUI to show updated information
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // Get the updated claim limit after purchase
                            int newLimit = 1 + newPurchasedCount; // Base limit + purchased claims
                            openMainGui(player);
                            player.sendMessage(Component.text("Successfully purchased 1 additional claim! New limit: " + newLimit, NamedTextColor.GREEN));
                        });
                    }).exceptionally(throwable -> {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error purchasing claims for player: " + player.getName(), throwable);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(Component.text("Error purchasing claim! Please contact an administrator.", NamedTextColor.RED));
                        });
                        return null;
                    });
                } else {
                    player.sendMessage(Component.text("You don't have enough money! You need $" + nextPrice + " but only have $" + String.format("%.2f", balance), NamedTextColor.RED));
                }
            });
        });
        
        return true;
    }
    
    /**
     * Handles claim info click
     */
    private boolean handleClaimInfoClick(Player player) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return claimManager.getPlayerClaims(player).size();
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Error getting player claims for info", e);
                return 0;
            }
        }).thenAccept(claimedCount -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Refresh the GUI to show updated information
                openMainGui(player);
            });
        });
        
        return true;
    }
    
    /**
     * Shows mode change confirmation message
     */
    private void showModeChangeConfirmation(Player player, PlayerMode newMode) {
        int cooldownHours = plugin.getConfigurationManager().getModeChangeCooldownHours();
        PlayerMode currentMode = playerModeManager.getPlayerMode(player);
        boolean willDeleteClaims = currentMode == PlayerMode.PEACEFUL && newMode == PlayerMode.NORMAL;
        
        String messageKey = willDeleteClaims ? "mode-change-confirm-with-claims" : "mode-change-confirm";
        String message = plugin.getMessage(messageKey);
        if (message.equals(messageKey)) {
            if (willDeleteClaims) {
                message = "&e&lAre you sure you want to change to &6{0}&e mode? &cYou won't be able to change for &6{1}&c hours.\n&c&lWARNING: &cAll your claims will be deleted!\n&a&lCONFIRMATION: &aType &6&l/fg Y &ato continue or &6&l/fg N &ato cancel.";
            } else {
                message = "&e&lAre you sure you want to change to &6{0}&e mode? &cYou won't be able to change for &6{1}&c hours.\n&a&lCONFIRMATION: &aType &6&l/fg Y &ato continue or &6&l/fg N &ato cancel.";
            }
        }
        
        message = message.replace("{0}", newMode.name().toLowerCase());
        message = message.replace("{1}", String.valueOf(cooldownHours));
        
        net.kyori.adventure.text.Component messageComponent = 
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        player.sendMessage(messageComponent);
    }
    
    /**
     * Calculates the price for the next claim
     */
    private int calculateClaimPrice(int currentClaimCount) {
        return 1000 + (currentClaimCount * 1000); // $1000 base + $1000 per existing claim
    }
    
    /**
     * Gets the maximum claims a player can have
     */
    private int getMaxClaims(Player player) {
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
        
        // Base limit (1) + purchased claims
        int baseLimit = 1;
        int purchasedClaims = getPurchasedClaims(player);
        return baseLimit + purchasedClaims;
    }
    
    /**
     * Gets the maximum claims a player can have asynchronously
     */
    private CompletableFuture<Integer> getMaxClaimsAsync(Player player) {
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
        
        // Base limit (1) + purchased claims
        int baseLimit = 1;
        return getPurchasedClaimsAsync(player).thenApply(purchasedClaims -> baseLimit + purchasedClaims);
    }
    
    /**
     * Creates the mode change item
     */
    private ItemStack createModeChangeItem(Player player) {
        PlayerMode currentMode = playerModeManager.getPlayerMode(player);
        PlayerMode otherMode = (currentMode == PlayerMode.PEACEFUL) ? PlayerMode.NORMAL : PlayerMode.PEACEFUL;
        
        Material material = (currentMode == PlayerMode.PEACEFUL) ? Material.DIAMOND_SWORD : Material.EMERALD;
        String name = "Change to " + otherMode.name().toLowerCase() + " mode";
        
        List<String> lore = new ArrayList<>();
        lore.add("Click to change your game mode");
        lore.add("");
        lore.add("&7Current Mode:");
        lore.add("&f" + currentMode.name().toLowerCase().substring(0, 1).toUpperCase() + currentMode.name().toLowerCase().substring(1));
        lore.add("");
        lore.add("&7Switch To:");
        lore.add("&f" + otherMode.name().toLowerCase().substring(0, 1).toUpperCase() + otherMode.name().toLowerCase().substring(1));
        lore.add("");
        
        // Check if player can change mode
        if (playerModeManager.canChangeMode(player)) {
            lore.add("&a✓ Ready to change mode");
        } else {
            lore.add("&c✗ Mode change on cooldown");
            // Could add remaining time here if needed
        }
        lore.add("");
        lore.add("&7Cooldown: &f24 hours");
        
        // Add warning for peaceful to normal transition
        if (currentMode == PlayerMode.PEACEFUL && otherMode == PlayerMode.NORMAL) {
            lore.add("");
            lore.add("&c⚠ Warning: All claims will be deleted!");
        }
        
        return createGuiItem(material, name, lore);
    }
    
    /**
     * Creates the buy claims item
     */
    private ItemStack createBuyClaimsItem(Player player) {
        List<String> lore = new ArrayList<>();
        
        if (economy == null) {
            lore.add("Economy system not available");
            lore.add("Contact an administrator");
            return createGuiItem(Material.BARRIER, "Buy Claims (Disabled)", lore);
        }
        
        // Get current claim count synchronously for GUI display
        int claimedCount = 0;
        try {
            claimedCount = claimManager.getPlayerClaims(player).size();
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Error getting player claims for GUI item", e);
        }
        
        // Use total claim limit for pricing (not just claimed chunks)
        int totalClaimLimit = getMaxClaims(player);
        int nextPrice = calculateClaimPrice(totalClaimLimit);
        
        lore.add("Click to buy an additional claim");
        lore.add("");
        lore.add("&7Current Status:");
        lore.add("&aClaimed: &f" + claimedCount + "&7/&b" + totalClaimLimit);
        lore.add("&eUnclaimed: &f" + (totalClaimLimit - claimedCount));
        lore.add("");
        lore.add("&7Next Purchase:");
        lore.add("&6Price: &a$" + nextPrice);
        
        // Check if player has enough money
        if (economy != null) {
            double balance = economy.getBalance(player);
            if (balance >= nextPrice) {
                lore.add("&a✓ You can afford this");
            } else {
                lore.add("&c✗ Insufficient funds");
                lore.add("&7You have: &f$" + String.format("%.2f", balance));
            }
        }
        lore.add("");
        lore.add("&cNote: Claims cannot be sold back");
        
        return createGuiItem(Material.GOLD_INGOT, "Buy Claims", lore);
    }
    
    /**
     * Creates the claim info item
     */
    private ItemStack createClaimInfoItem(Player player) {
        List<String> lore = new ArrayList<>();
        
        // Get current claim information
        int claimedCount = 0;
        try {
            claimedCount = claimManager.getPlayerClaims(player).size();
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Error getting player claims for info item", e);
        }
        
        int totalClaimLimit = getMaxClaims(player);
        int unclaimedCount = totalClaimLimit - claimedCount;
        int nextPrice = calculateClaimPrice(totalClaimLimit);
        int purchasedClaims = getPurchasedClaims(player);
        
        lore.add("Click to refresh claim information");
        lore.add("");
        lore.add("&7Claim Status:");
        lore.add("&aClaimed: &f" + claimedCount + "&7/&b" + totalClaimLimit);
        lore.add("&eUnclaimed: &f" + unclaimedCount);
        lore.add("");
        lore.add("&7Details:");
        lore.add("&7Base Limit: &f1");
        lore.add("&7Purchased: &f" + purchasedClaims);
        lore.add("&7Total Limit: &b" + totalClaimLimit);
        lore.add("");
        lore.add("&7Next Purchase Price: &a$" + nextPrice);
        
        return createGuiItem(Material.MAP, "Claim Information", lore);
    }
    
    /**
     * Creates the close item
     */
    private ItemStack createCloseItem() {
        List<String> lore = new ArrayList<>();
        lore.add("Click to close the FrontierGuard menu");
        lore.add("");
        lore.add("&7All changes are saved automatically");
        
        return createGuiItem(Material.BARRIER, "Close Menu", lore);
    }
    
    /**
     * Creates a GUI item with the specified properties
     */
    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD));
            
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                // Parse color codes in lore
                Component loreComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(line);
                loreComponents.add(loreComponent);
            }
            meta.lore(loreComponents);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates the manage invitations item for the main GUI
     */
    private ItemStack createManageInvitationsItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("Manage Invitations", NamedTextColor.GOLD, TextDecoration.BOLD));
            
            // Set the player's own head texture
            try {
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(player);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not set player head texture for manage invitations: " + e.getMessage());
            }
            
            // Set initial lore
            List<Component> initialLoreComponents = new ArrayList<>();
            initialLoreComponents.add(Component.text("Click to manage who can", NamedTextColor.GRAY));
            initialLoreComponents.add(Component.text("build and access your claim", NamedTextColor.GRAY));
            initialLoreComponents.add(Component.text(""));
            initialLoreComponents.add(Component.text("Loading invitations...", NamedTextColor.YELLOW));
            initialLoreComponents.add(Component.text(""));
            initialLoreComponents.add(Component.text("• View invited players", NamedTextColor.WHITE));
            initialLoreComponents.add(Component.text("• Set build permissions", NamedTextColor.WHITE));
            initialLoreComponents.add(Component.text("• Set container access", NamedTextColor.WHITE));
            initialLoreComponents.add(Component.text("• Manage invitation rights", NamedTextColor.WHITE));
            
            meta.lore(initialLoreComponents);
            item.setItemMeta(meta);
            
            var claim = claimManager.getClaimInfo(player.getLocation().getChunk());
            if (claim != null) {
                // Get invitation count asynchronously and update the item
                invitationManager.getAllInvitationPermissions(claim).thenAccept(permissions -> {
                    int invitationCount = permissions.size();
                    
                    // Update the item in the player's inventory if they still have the GUI open
                    if (player.getOpenInventory() != null && 
                        player.getOpenInventory().getTitle().contains("FrontierGuard Menu")) {
                        
                        ItemStack updatedItem = new ItemStack(Material.PLAYER_HEAD);
                        ItemMeta updatedMeta = updatedItem.getItemMeta();
                        
                        if (updatedMeta != null) {
                            updatedMeta.displayName(Component.text("Manage Invitations", NamedTextColor.GOLD, TextDecoration.BOLD));
                            
                            // Set the player's own head texture
                            try {
                                if (updatedMeta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                                    skullMeta.setOwningPlayer(player);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Could not set player head texture for manage invitations: " + e.getMessage());
                            }
                            
                            List<Component> updatedLoreComponents = new ArrayList<>();
                            updatedLoreComponents.add(Component.text("Click to manage who can", NamedTextColor.GRAY));
                            updatedLoreComponents.add(Component.text("build and access your claim", NamedTextColor.GRAY));
                            updatedLoreComponents.add(Component.text(""));
                            updatedLoreComponents.add(Component.text("Current Invitations: " + invitationCount, NamedTextColor.YELLOW));
                            updatedLoreComponents.add(Component.text(""));
                            updatedLoreComponents.add(Component.text("• View invited players", NamedTextColor.WHITE));
                            updatedLoreComponents.add(Component.text("• Set build permissions", NamedTextColor.WHITE));
                            updatedLoreComponents.add(Component.text("• Set container access", NamedTextColor.WHITE));
                            updatedLoreComponents.add(Component.text("• Manage invitation rights", NamedTextColor.WHITE));
                            
                            updatedMeta.lore(updatedLoreComponents);
                            updatedItem.setItemMeta(updatedMeta);
                            
                            // Update the item in the GUI
                            player.getOpenInventory().getTopInventory().setItem(MANAGE_INVITATIONS_SLOT, updatedItem);
                        }
                    }
                });
            }
        }
        
        return item;
    }
    
    /**
     * Opens the invitation management GUI for a player
     */
    public void openInvitationManagementGui(Player player) {
        if (player == null) {
            plugin.getLogger().warning("Attempted to open invitation GUI for null player");
            return;
        }
        
        plugin.getLogger().info("Opening invitation management GUI for player: " + player.getName());
        
        var claim = claimManager.getClaimInfo(player.getLocation().getChunk());
        if (claim == null || !claim.getOwnerUuid().equals(player.getUniqueId())) {
            plugin.getLogger().info("Player " + player.getName() + " is not in their own claim");
            player.sendMessage(Component.text("You must be in your own claim to manage invitations!", NamedTextColor.RED));
            return;
        }
        
        if (!playerModeManager.isPeacefulPlayer(player)) {
            plugin.getLogger().info("Player " + player.getName() + " is not peaceful");
            player.sendMessage(Component.text("Only peaceful players can manage invitations!", NamedTextColor.RED));
            return;
        }
        
        plugin.getLogger().info("Player " + player.getName() + " passed all checks, opening GUI");
        
        // Create GUI immediately with loading state
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Manage Invitations"));
        
        // Add back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Back to Main Menu", NamedTextColor.YELLOW));
            List<Component> backLore = new ArrayList<>();
            backLore.add(Component.text("Click to return to main menu", NamedTextColor.GRAY));
            backMeta.lore(backLore);
            backItem.setItemMeta(backMeta);
        }
        gui.setItem(0, backItem);
        
        // Add loading item
        ItemStack loadingItem = new ItemStack(Material.CLOCK);
        ItemMeta loadingMeta = loadingItem.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.displayName(Component.text("Loading Invitations...", NamedTextColor.YELLOW));
            List<Component> loadingLore = new ArrayList<>();
            loadingLore.add(Component.text("Please wait while we load", NamedTextColor.GRAY));
            loadingLore.add(Component.text("your invited players...", NamedTextColor.GRAY));
            loadingMeta.lore(loadingLore);
            loadingItem.setItemMeta(loadingMeta);
        }
        gui.setItem(13, loadingItem);
        
        // Fill empty slots with barrier blocks
        for (int i = 1; i < 27; i++) {
            if (i != 13) { // Don't overwrite loading item
                ItemStack barrier = new ItemStack(Material.BARRIER);
                ItemMeta barrierMeta = barrier.getItemMeta();
                if (barrierMeta != null) {
                    barrierMeta.displayName(Component.text("", NamedTextColor.WHITE));
                    barrier.setItemMeta(barrierMeta);
                }
                gui.setItem(i, barrier);
            }
        }
        
        player.openInventory(gui);
        plugin.getLogger().info("Opened invitation management GUI for " + player.getName() + " with loading state");
        
        // Get all invitation permissions asynchronously and update GUI
        invitationManager.getAllInvitationPermissions(claim).thenAccept(permissions -> {
            plugin.getLogger().info("Found " + permissions.size() + " invited players for " + player.getName());
            
            // Update GUI on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory() != null && 
                    player.getOpenInventory().getTitle().contains("Manage Invitations")) {
                    
                    // Clear the GUI
                    Inventory updatedGui = player.getOpenInventory().getTopInventory();
                    updatedGui.clear();
                    
                    // Add back button
                    updatedGui.setItem(0, backItem);
                    
                    // Add each invited player
                    for (int i = 0; i < permissions.size(); i++) {
                        InvitationPermissions perm = permissions.get(i);
                        ItemStack playerItem = createPlayerPermissionItem(perm);
                        updatedGui.setItem(i + 9, playerItem); // Start from slot 9
                        plugin.getLogger().info("Added player " + getPlayerName(perm.getInvitedUuid()) + " to slot " + (i + 9));
                    }
                    
                    // Fill remaining slots with barrier blocks
                    for (int i = permissions.size() + 9; i < 27; i++) {
                        ItemStack barrier = new ItemStack(Material.BARRIER);
                        ItemMeta barrierMeta = barrier.getItemMeta();
                        if (barrierMeta != null) {
                            barrierMeta.displayName(Component.text("", NamedTextColor.WHITE));
                            barrier.setItemMeta(barrierMeta);
                        }
                        updatedGui.setItem(i, barrier);
                    }
                    
                    plugin.getLogger().info("Updated invitation management GUI for " + player.getName() + " with " + permissions.size() + " players");
                }
            });
        });
    }
    
    /**
     * Refreshes the invitation management GUI without reopening it
     */
    private void refreshInvitationManagementGui(Player player) {
        if (player.getOpenInventory() == null || 
            !player.getOpenInventory().getTitle().contains("Manage Invitations")) {
            plugin.getLogger().info("Player " + player.getName() + " doesn't have invitation GUI open, opening new one");
            openInvitationManagementGui(player);
            return;
        }
        
        var claim = claimManager.getClaimInfo(player.getLocation().getChunk());
        if (claim == null || !claim.getOwnerUuid().equals(player.getUniqueId())) {
            plugin.getLogger().info("Player " + player.getName() + " is not in their own claim, closing GUI");
            player.closeInventory();
            return;
        }
        
        // Get updated permissions and refresh the GUI
        invitationManager.getAllInvitationPermissions(claim).thenAccept(permissions -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory() != null && 
                    player.getOpenInventory().getTitle().contains("Manage Invitations")) {
                    
                    Inventory gui = player.getOpenInventory().getTopInventory();
                    gui.clear();
                    
                    // Add back button
                    ItemStack backItem = new ItemStack(Material.ARROW);
                    ItemMeta backMeta = backItem.getItemMeta();
                    if (backMeta != null) {
                        backMeta.displayName(Component.text("Back to Main Menu", NamedTextColor.YELLOW));
                        List<Component> backLore = new ArrayList<>();
                        backLore.add(Component.text("Click to return to main menu", NamedTextColor.GRAY));
                        backMeta.lore(backLore);
                        backItem.setItemMeta(backMeta);
                    }
                    gui.setItem(0, backItem);
                    
                    // Add each invited player
                    for (int i = 0; i < permissions.size(); i++) {
                        InvitationPermissions perm = permissions.get(i);
                        ItemStack playerItem = createPlayerPermissionItem(perm);
                        gui.setItem(i + 9, playerItem);
                        plugin.getLogger().info("Refreshed player " + getPlayerName(perm.getInvitedUuid()) + " in slot " + (i + 9));
                    }
                    
                    // Fill remaining slots with barrier blocks
                    for (int i = permissions.size() + 9; i < 27; i++) {
                        ItemStack barrier = new ItemStack(Material.BARRIER);
                        ItemMeta barrierMeta = barrier.getItemMeta();
                        if (barrierMeta != null) {
                            barrierMeta.displayName(Component.text("", NamedTextColor.WHITE));
                            barrier.setItemMeta(barrierMeta);
                        }
                        gui.setItem(i, barrier);
                    }
                    
                    plugin.getLogger().info("Refreshed invitation management GUI for " + player.getName() + " with " + permissions.size() + " players");
                }
            });
        });
    }
    
    /**
     * Creates an item representing a player's permissions
     */
    private ItemStack createPlayerPermissionItem(InvitationPermissions permissions) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String playerName = getPlayerName(permissions.getInvitedUuid());
            meta.displayName(Component.text(playerName, NamedTextColor.GOLD, TextDecoration.BOLD));
            
            // Set the player head texture if possible
            try {
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(permissions.getInvitedUuid()));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not set player head texture for " + playerName + ": " + e.getMessage());
            }
            
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("Click to toggle permissions", NamedTextColor.GRAY));
            loreComponents.add(Component.text(""));
            loreComponents.add(Component.text("Build: " + (permissions.canBuild() ? "✓" : "✗"), 
                permissions.canBuild() ? NamedTextColor.GREEN : NamedTextColor.RED));
            loreComponents.add(Component.text("Containers: " + (permissions.canAccessContainers() ? "✓" : "✗"), 
                permissions.canAccessContainers() ? NamedTextColor.GREEN : NamedTextColor.RED));
            loreComponents.add(Component.text("Manage Invites: " + (permissions.canManageInvitations() ? "✓" : "✗"), 
                permissions.canManageInvitations() ? NamedTextColor.GREEN : NamedTextColor.RED));
            loreComponents.add(Component.text(""));
            loreComponents.add(Component.text("Right-click to remove", NamedTextColor.RED));
            
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Gets the player name from UUID
     */
    private String getPlayerName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        
        return "Unknown Player";
    }
    
    /**
     * Gets the number of claims a player has purchased
     */
    public int getPurchasedClaims(Player player) {
        try {
            return purchasedClaimsDao.getPurchasedClaims(player.getUniqueId()).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting purchased claims for player: " + player.getName(), e);
            return 0;
        }
    }
    
    /**
     * Gets the number of claims a player has purchased asynchronously
     */
    public CompletableFuture<Integer> getPurchasedClaimsAsync(Player player) {
        return purchasedClaimsDao.getPurchasedClaims(player.getUniqueId())
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Error getting purchased claims for player: " + player.getName(), throwable);
                return 0;
            });
    }
    
    /**
     * Handles clicks in the invitation management GUI
     */
    public boolean handleInvitationManagementClick(Player player, int slot, boolean isRightClick) {
        plugin.getLogger().info("Invitation GUI click: slot=" + slot + ", rightClick=" + isRightClick + ", player=" + player.getName());
        
        if (slot == 0) {
            // Back button
            plugin.getLogger().info("Back button clicked");
            openMainGui(player);
            return true;
        }
        
        if (slot < 9) {
            plugin.getLogger().info("Invalid slot: " + slot + " (must be >= 9 for player slots)");
            return false; // Invalid slot
        }
        
        // Check if the clicked item is a barrier (empty slot)
        if (player.getOpenInventory() != null) {
            ItemStack clickedItem = player.getOpenInventory().getTopInventory().getItem(slot);
            if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                plugin.getLogger().info("Clicked on barrier block at slot " + slot + " - ignoring");
                return false;
            }
        }
        
        var claim = claimManager.getClaimInfo(player.getLocation().getChunk());
        if (claim == null || !claim.getOwnerUuid().equals(player.getUniqueId())) {
            plugin.getLogger().info("Player " + player.getName() + " is not in their own claim");
            player.sendMessage(Component.text("You must be in your own claim to manage invitations!", NamedTextColor.RED));
            return false;
        }
        
        // Get the invitation permissions for this slot
        invitationManager.getAllInvitationPermissions(claim).thenAccept(permissions -> {
            int playerIndex = slot - 9;
            plugin.getLogger().info("Player index: " + playerIndex + ", permissions size: " + permissions.size());
            
            if (playerIndex >= 0 && playerIndex < permissions.size()) {
                InvitationPermissions perm = permissions.get(playerIndex);
                String playerName = getPlayerName(perm.getInvitedUuid());
                plugin.getLogger().info("Processing permissions for player: " + playerName);
                
                if (isRightClick) {
                    // Remove invitation
                    plugin.getLogger().info("Removing invitation for player: " + playerName);
                    invitationManager.uninvitePlayer(player, Bukkit.getPlayer(perm.getInvitedUuid()), claim)
                        .thenRun(() -> {
                            plugin.getLogger().info("Successfully removed invitation for " + playerName);
                            // Refresh the GUI without reopening
                            refreshInvitationManagementGui(player);
                        });
                } else {
                    // Toggle permissions - cycle through permission levels
                    plugin.getLogger().info("Toggling permissions for player: " + playerName);
                    boolean newCanBuild;
                    boolean newCanAccessContainers;
                    boolean newCanManageInvitations;
                    
                    // Cycle through permission levels
                    if (!perm.canBuild()) {
                        // Level 0 -> Level 1: Enable build only
                        newCanBuild = true;
                        newCanAccessContainers = false;
                        newCanManageInvitations = false;
                    } else if (perm.canBuild() && !perm.canAccessContainers()) {
                        // Level 1 -> Level 2: Enable build and containers
                        newCanBuild = true;
                        newCanAccessContainers = true;
                        newCanManageInvitations = false;
                    } else if (perm.canBuild() && perm.canAccessContainers() && !perm.canManageInvitations()) {
                        // Level 2 -> Level 3: Enable full permissions
                        newCanBuild = true;
                        newCanAccessContainers = true;
                        newCanManageInvitations = true;
                    } else {
                        // Level 3 -> Level 0: Disable everything
                        newCanBuild = false;
                        newCanAccessContainers = false;
                        newCanManageInvitations = false;
                    }
                    
                    plugin.getLogger().info("New permissions - Build: " + newCanBuild + ", Containers: " + newCanAccessContainers + ", Manage: " + newCanManageInvitations);
                    
                    invitationManager.updatePlayerPermissions(claim, perm.getInvitedUuid(), 
                        newCanBuild, newCanAccessContainers, newCanManageInvitations)
                        .thenRun(() -> {
                            plugin.getLogger().info("Successfully updated permissions for " + playerName);
                            player.sendMessage(Component.text("Updated permissions for " + playerName, NamedTextColor.GREEN));
                            // Refresh the GUI without reopening
                            refreshInvitationManagementGui(player);
                        });
                }
            } else {
                plugin.getLogger().warning("Invalid player index: " + playerIndex + " (permissions size: " + permissions.size() + ")");
            }
        });
        
        return true;
    }
    
    /**
     * Refreshes a specific slot in the main GUI
     * @param player The player viewing the GUI
     * @param slot The slot to refresh
     */
    public void refreshMainGuiSlot(Player player, int slot) {
        if (player.getOpenInventory() == null || 
            !player.getOpenInventory().getTitle().contains("FrontierGuard Menu")) {
            return;
        }
        
        org.bukkit.inventory.Inventory inventory = player.getOpenInventory().getTopInventory();
        
        switch (slot) {
            case MODE_CHANGE_SLOT:
                inventory.setItem(slot, createModeChangeItem(player));
                break;
            case BUY_CLAIMS_SLOT:
                inventory.setItem(slot, createBuyClaimsItem(player));
                break;
            case CLAIM_INFO_SLOT:
                inventory.setItem(slot, createClaimInfoItem(player));
                break;
            case MANAGE_INVITATIONS_SLOT:
                if (playerModeManager.isPeacefulPlayer(player)) {
                    var claim = claimManager.getClaimInfo(player.getLocation().getChunk());
                    if (claim != null && claim.getOwnerUuid().equals(player.getUniqueId())) {
                        inventory.setItem(slot, createManageInvitationsItem(player));
                    }
                }
                break;
            case CLOSE_SLOT:
                inventory.setItem(slot, createCloseItem());
                break;
        }
    }
    
}
