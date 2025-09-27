package com.danielvnz.landclaimer.command;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.ClaimInvitationManager;
import com.danielvnz.landclaimer.manager.ClaimManager;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import com.danielvnz.landclaimer.model.ClaimData;
import com.danielvnz.landclaimer.model.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Command executor for LandClaimer plugin commands.
 * Handles claim, unclaim, claims, and claiminfo commands.
 */
public class LandClaimerCommandExecutor implements CommandExecutor, TabCompleter {
    
    private final LandClaimerPlugin plugin;
    private final ClaimManager claimManager;
    private final PlayerModeManager playerModeManager;
    private final ClaimInvitationManager invitationManager;
    
    public LandClaimerCommandExecutor(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
        this.playerModeManager = plugin.getPlayerModeManager();
        this.invitationManager = plugin.getInvitationManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Debug logging
        plugin.getLogger().info("Command received: label=" + label + ", args=" + Arrays.toString(args) + " from player=" + player.getName());
        
        if (args.length == 0) {
            // Open GUI when no arguments provided
            plugin.getGuiManager().openMainGui(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // Handle confirmation responses for mode changes
        if (subCommand.equalsIgnoreCase("y") || subCommand.equalsIgnoreCase("yes")) {
            plugin.getLogger().info("Processing Y/yes confirmation for player: " + player.getName());
            return plugin.getGuiManager().handleModeChangeConfirmation(player, true);
        } else if (subCommand.equalsIgnoreCase("n") || subCommand.equalsIgnoreCase("no")) {
            plugin.getLogger().info("Processing N/no confirmation for player: " + player.getName());
            return plugin.getGuiManager().handleModeChangeConfirmation(player, false);
        }
        
        switch (subCommand) {
            case "claim":
                return handleClaimCommand(player, args);
            case "unclaim":
                return handleUnclaimCommand(player, args);
            case "claims":
                return handleClaimsCommand(player, args);
            case "claiminfo":
            case "info":
                return handleClaimInfoCommand(player, args);
            case "show":
                return handleShowCommand(player, args);
            case "update":
                return handleUpdateCommand(player, args);
            case "testeconomy":
                return handleTestEconomyCommand(player, args);
            case "setpvpzone":
                return handleSetPvpZoneCommand(player, args);
            case "createpvparea":
                return handleCreatePvpAreaCommand(player, args);
            case "deletepvparea":
                return handleDeletePvpAreaCommand(player, args);
            case "listpvpareas":
                return handleListPvpAreasCommand(player, args);
            case "reload":
                return handleReloadCommand(player, args);
            case "setmode":
                return handleSetModeCommand(player, args);
            case "forcemode":
                return handleForceModeCommand(player, args);
            case "adminclaims":
                return handleAdminClaimsCommand(player, args);
            case "setclaimlimit":
                return handleSetClaimLimitCommand(player, args);
            case "gui":
                return handleGuiCommand(player, args);
            case "invite":
                return handleInviteCommand(player, args);
            case "uninvite":
                return handleUninviteCommand(player, args);
            case "invitations":
                return handleInvitationsCommand(player, args);
            case "cooldown":
                return handleCooldownCommand(player, args);
            case "setrep":
                return handleSetReputationCommand(player, args);
            case "addrep":
                return handleAddReputationCommand(player, args);
            case "rep":
                return handleReputationCommand(player, args);
            case "noob":
                return handleNoobCommand(player, args);
            case "testwg":
            case "testworldguard":
                return handleTestWorldGuardCommand(player, args);
            case "help":
                sendHelpMessage(player);
                return true;
            default:
                player.sendMessage(Component.text("Unknown command. Use /frontierguard help for available commands.", NamedTextColor.RED));
                return true;
        }
    }
    
    /**
     * Handles the claim command
     */
    private boolean handleClaimCommand(Player player, String[] args) {
        // Check if player has selected a mode
        if (!playerModeManager.hasSelectedMode(player)) {
            player.sendMessage(Component.text("You must select a game mode first! Use /frontierguard help for more information.", NamedTextColor.RED));
            return true;
        }
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text(plugin.getMessage("claim-failed-normal-mode"), NamedTextColor.RED));
            return true;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        
        // Attempt to claim the chunk
        if (claimManager.claimChunk(player, chunk)) {
            // Success message is sent by ClaimManager
            return true;
        } else {
            // Error message is sent by ClaimManager
            return true;
        }
    }
    
    /**
     * Handles the unclaim command
     */
    private boolean handleUnclaimCommand(Player player, String[] args) {
        // Check if player has selected a mode
        if (!playerModeManager.hasSelectedMode(player)) {
            player.sendMessage(Component.text("You must select a game mode first! Use /frontierguard help for more information.", NamedTextColor.RED));
            return true;
        }
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text("Only peaceful players can unclaim chunks!", NamedTextColor.RED));
            return true;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        
        // Attempt to unclaim the chunk
        if (claimManager.unclaimChunk(player, chunk)) {
            // Success message is sent by ClaimManager
            return true;
        } else {
            // Error message is sent by ClaimManager
            return true;
        }
    }
    
    /**
     * Handles the claims command (list player's claims)
     */
    private boolean handleClaimsCommand(Player player, String[] args) {
        // Check if player has selected a mode
        if (!playerModeManager.hasSelectedMode(player)) {
            player.sendMessage(Component.text("You must select a game mode first! Use /frontierguard help for more information.", NamedTextColor.RED));
            return true;
        }
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            player.sendMessage(Component.text("Only peaceful players can have claims!", NamedTextColor.RED));
            return true;
        }
        
        List<ClaimData> claims = claimManager.getPlayerClaims(player);
        
        if (claims.isEmpty()) {
            player.sendMessage(Component.text("You don't have any claims yet.", NamedTextColor.YELLOW));
            return true;
        }
        
        // Get claim limit for display
        int totalClaimLimit = 1; // Base limit
        try {
            // Get purchased claims from GUI manager
            int purchasedClaims = plugin.getGuiManager().getPurchasedClaims(player);
            totalClaimLimit += purchasedClaims;
            
            // Check for permission bonuses
            if (player.hasPermission("frontierguard.claims.unlimited")) {
                totalClaimLimit = Integer.MAX_VALUE;
            } else if (player.hasPermission("frontierguard.claimamount.*")) {
                totalClaimLimit = Integer.MAX_VALUE;
            } else {
                // Check for specific claim amount permissions and add them
                int permissionBonus = 0;
                for (int i = 1; i <= 1000; i++) {
                    if (player.hasPermission("frontierguard.claimamount." + i)) {
                        permissionBonus = Math.max(permissionBonus, i);
                    }
                }
                totalClaimLimit += permissionBonus;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting claim limit for claims command", e);
        }
        
        // Send header with claim count
        String limitText = (totalClaimLimit == Integer.MAX_VALUE) ? "∞" : String.valueOf(totalClaimLimit);
        player.sendMessage(Component.text("=== Your Claims (", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(claims.size(), NamedTextColor.WHITE, TextDecoration.BOLD))
            .append(Component.text("/", NamedTextColor.GRAY, TextDecoration.BOLD))
            .append(Component.text(limitText, NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text(") ===", NamedTextColor.GOLD, TextDecoration.BOLD)));
        
        // Send each claim
        for (ClaimData claim : claims) {
            // Calculate world coordinates of chunk center
            int worldX = claim.getChunkX() * 16 + 8; // Chunk center X
            int worldZ = claim.getChunkZ() * 16 + 8; // Chunk center Z
            
            Component claimInfo = Component.text("• ", NamedTextColor.GRAY)
                .append(Component.text(claim.getWorldName(), NamedTextColor.WHITE))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(worldX, NamedTextColor.YELLOW))
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(Component.text(worldZ, NamedTextColor.YELLOW))
                .append(Component.text(")", NamedTextColor.GRAY));
            
            player.sendMessage(claimInfo);
        }
        
        // Send summary
        int unclaimed = (totalClaimLimit == Integer.MAX_VALUE) ? Integer.MAX_VALUE : (totalClaimLimit - claims.size());
        String unclaimedText = (unclaimed == Integer.MAX_VALUE) ? "∞" : String.valueOf(unclaimed);
        
        player.sendMessage(Component.text("Unclaimed slots: ", NamedTextColor.YELLOW)
            .append(Component.text(unclaimedText, NamedTextColor.WHITE, TextDecoration.BOLD)));
        
        return true;
    }
    
    /**
     * Handles the claiminfo command
     */
    private boolean handleClaimInfoCommand(Player player, String[] args) {
        Chunk chunk = player.getLocation().getChunk();
        ClaimData claimData = claimManager.getClaimInfo(chunk);
        
        if (claimData == null) {
            player.sendMessage(Component.text("This chunk is not claimed.", NamedTextColor.YELLOW));
            return true;
        }
        
        // Get owner name
        String ownerName = "Unknown";
        try {
            UUID ownerUuid = claimData.getOwnerUuid();
            Player owner = plugin.getServer().getPlayer(ownerUuid);
            if (owner != null) {
                ownerName = owner.getName();
            } else {
                // Try to get from offline player
                ownerName = plugin.getServer().getOfflinePlayer(ownerUuid).getName();
                if (ownerName == null) {
                    ownerName = "Unknown Player";
                }
            }
        } catch (Exception e) {
            ownerName = "Unknown Player";
        }
        
        // Determine player's relationship to this claim
        boolean isOwner = claimData.getOwnerUuid().equals(player.getUniqueId());
        boolean isInvited = false;
        
        if (!isOwner) {
            // Check if player is invited to this claim
            try {
                isInvited = plugin.getInvitationManager().canPlayerBuild(player, claimData).get();
            } catch (Exception e) {
                // If there's an error checking invitation, assume not invited
                isInvited = false;
            }
        }
        
        // Send claim information with relationship context
        if (isOwner) {
            player.sendMessage(Component.text("=== Your Claim ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else if (isInvited) {
            player.sendMessage(Component.text("=== Invited Claim ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else {
            player.sendMessage(Component.text("=== Claim Information ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        }
        
        player.sendMessage(Component.text("Owner: ", NamedTextColor.GREEN)
            .append(Component.text(ownerName, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("World: ", NamedTextColor.GREEN)
            .append(Component.text(claimData.getWorldName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Chunk: ", NamedTextColor.GREEN)
            .append(Component.text("(" + claimData.getChunkX() + ", " + claimData.getChunkZ() + ")", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Claimed: ", NamedTextColor.GREEN)
            .append(Component.text(claimData.getClaimTime().toString(), NamedTextColor.WHITE)));
        
        // Add relationship-specific information
        if (isOwner) {
            player.sendMessage(Component.text("Status: ", NamedTextColor.GREEN)
                .append(Component.text("You own this claim", NamedTextColor.GREEN, TextDecoration.BOLD)));
        } else if (isInvited) {
            player.sendMessage(Component.text("Status: ", NamedTextColor.GREEN)
                .append(Component.text("You are invited to this claim", NamedTextColor.YELLOW, TextDecoration.BOLD)));
        } else {
            player.sendMessage(Component.text("Status: ", NamedTextColor.GREEN)
                .append(Component.text("You have no access to this claim", NamedTextColor.RED, TextDecoration.BOLD)));
        }
        
        return true;
    }
    
    /**
     * Handles the show command - displays boundaries of claims near the player
     */
    private boolean handleShowCommand(Player player, String[] args) {
        // Check if player has selected a mode
        if (!playerModeManager.hasSelectedMode(player)) {
            player.sendMessage(Component.text("You must select a game mode first! Use /frontierguard help for more information.", NamedTextColor.RED));
            return true;
        }
        
        Location playerLocation = player.getLocation();
        World world = playerLocation.getWorld();
        int playerChunkX = playerLocation.getChunk().getX();
        int playerChunkZ = playerLocation.getChunk().getZ();
        
        // Get radius from configuration
        int radius = plugin.getConfigurationManager().getShowRadius();
        
        int claimsFound = 0;
        
        // Search for claims in the configured radius
        for (int x = playerChunkX - radius; x <= playerChunkX + radius; x++) {
            for (int z = playerChunkZ - radius; z <= playerChunkZ + radius; z++) {
                Chunk chunk = world.getChunkAt(x, z);
                ClaimData claimData = claimManager.getClaimInfo(chunk);
                
                if (claimData != null) {
                    // Show boundary for this claim for 5 seconds
                    plugin.getVisualFeedbackManager().showChunkBoundaryTimed(chunk, player, 5);
                    claimsFound++;
                }
            }
        }
        
        if (claimsFound == 0) {
            player.sendMessage(Component.text("No claims found within " + radius + " chunks of your location.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Showing boundaries for " + claimsFound + " claim(s) within " + radius + " chunks for 5 seconds.", NamedTextColor.GREEN));
        }
        
        return true;
    }
    
    /**
     * Handles the setpvpzone command
     */
    private boolean handleSetPvpZoneCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.pvparea")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        // Give the player the selection tool
        plugin.getPvpAreaManager().giveSelectionTool(player);
        return true;
    }
    
    /**
     * Handles the createpvparea command
     */
    private boolean handleCreatePvpAreaCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.pvparea")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /frontierguard createpvparea <name>", NamedTextColor.RED));
            return true;
        }
        
        String name = args[1];
        
        // Validate name
        if (name.length() < 3 || name.length() > 20) {
            player.sendMessage(Component.text("PVP area name must be between 3 and 20 characters!", NamedTextColor.RED));
            return true;
        }
        
        // Create the PVP area from selection
        plugin.getPvpAreaManager().createFromSelection(player, name);
        return true;
    }
    
    /**
     * Handles the deletepvparea command
     */
    private boolean handleDeletePvpAreaCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.pvparea")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /frontierguard deletepvparea <name>", NamedTextColor.RED));
            return true;
        }
        
        String name = args[1];
        
        // Delete the PVP area
        boolean success = plugin.getPvpAreaManager().deletePvpArea(name);
        
        if (success) {
            player.sendMessage(Component.text("PVP area '", NamedTextColor.GREEN)
                .append(Component.text(name, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("' deleted successfully!", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("PVP area '", NamedTextColor.RED)
                .append(Component.text(name, NamedTextColor.YELLOW))
                .append(Component.text("' not found!", NamedTextColor.RED)));
        }
        
        return true;
    }
    
    /**
     * Handles the listpvpareas command
     */
    private boolean handleListPvpAreasCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.pvparea")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        var areas = plugin.getPvpAreaManager().listPvpAreas();
        
        if (areas.isEmpty()) {
            player.sendMessage(Component.text("No PVP areas have been created yet.", NamedTextColor.YELLOW));
            return true;
        }
        
        // Send header
        player.sendMessage(Component.text("=== PVP Areas ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        // Send each area
        for (var area : areas) {
            Component areaInfo = Component.text("• ", NamedTextColor.GRAY)
                .append(Component.text(area.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(area.getWorldName(), NamedTextColor.YELLOW))
                .append(Component.text(") - ", NamedTextColor.GRAY))
                .append(Component.text(area.getArea() + " blocks", NamedTextColor.GREEN));
            
            player.sendMessage(areaInfo);
        }
        
        // Send count
        player.sendMessage(Component.text("Total PVP areas: ", NamedTextColor.GREEN)
            .append(Component.text(areas.size(), NamedTextColor.WHITE, TextDecoration.BOLD)));
        
        return true;
    }
    
    /**
     * Handles the reload command
     */
    private boolean handleReloadCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.reload")) {
            plugin.getErrorHandler().handlePermissionError(player, "frontierguard.admin.reload");
            return true;
        }
        
        try {
            // Reload configuration
            boolean success = plugin.getConfigurationManager().reloadConfig();
            
            if (success) {
                plugin.getErrorHandler().sendSuccessMessage(player, "Configuration reloaded successfully!");
            } else {
                plugin.getErrorHandler().sendErrorMessage(player, "Failed to reload configuration! Check console for errors.");
            }
        } catch (Exception e) {
            plugin.getErrorHandler().handleConfigurationError(player, "reload configuration", e);
        }
        
        return true;
    }
    
    /**
     * Handles the setmode command
     */
    private boolean handleSetModeCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.setmode")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /frontierguard setmode <peaceful|normal>", NamedTextColor.RED));
            return true;
        }
        
        String modeString = args[1].toLowerCase();
        PlayerMode mode;
        
        switch (modeString) {
            case "peaceful":
                mode = PlayerMode.PEACEFUL;
                break;
            case "normal":
                mode = PlayerMode.NORMAL;
                break;
            default:
                player.sendMessage(Component.text("Invalid mode! Use 'peaceful' or 'normal'.", NamedTextColor.RED));
                return true;
        }
        
        // Set the player's mode
        playerModeManager.setPlayerMode(player, mode);
        player.sendMessage(Component.text("Your mode has been set to ", NamedTextColor.GREEN)
            .append(Component.text(mode.getDisplayName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text("!", NamedTextColor.GREEN)));
        
        return true;
    }
    
    /**
     * Handles the forcemode command
     */
    private boolean handleForceModeCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.forcemode")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /frontierguard forcemode <player> <peaceful|normal>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[1];
        String modeString = args[2].toLowerCase();
        
        // Find the target player
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player '", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.YELLOW))
                .append(Component.text("' not found!", NamedTextColor.RED)));
            return true;
        }
        
        PlayerMode mode;
        switch (modeString) {
            case "peaceful":
                mode = PlayerMode.PEACEFUL;
                break;
            case "normal":
                mode = PlayerMode.NORMAL;
                break;
            default:
                player.sendMessage(Component.text("Invalid mode! Use 'peaceful' or 'normal'.", NamedTextColor.RED));
                return true;
        }
        
        // Set the target player's mode
        playerModeManager.setPlayerMode(target, mode);
        
        // Send messages to both players
        player.sendMessage(Component.text("Set ", NamedTextColor.GREEN)
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text("'s mode to ", NamedTextColor.GREEN))
            .append(Component.text(mode.getDisplayName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text("!", NamedTextColor.GREEN)));
        
        target.sendMessage(Component.text("Your mode has been changed to ", NamedTextColor.GREEN)
            .append(Component.text(mode.getDisplayName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text(" by an administrator!", NamedTextColor.GREEN)));
        
        return true;
    }
    
    /**
     * Handles the adminclaims command
     */
    private boolean handleAdminClaimsCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.claims")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /frontierguard adminclaims <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[1];
        
        // Find the target player
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player '", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.YELLOW))
                .append(Component.text("' not found!", NamedTextColor.RED)));
            return true;
        }
        
        // Get the target player's claims
        var claims = plugin.getClaimManager().getPlayerClaims(target);
        
        if (claims.isEmpty()) {
            player.sendMessage(Component.text(target.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" has no claims.", NamedTextColor.GRAY)));
            return true;
        }
        
        // Send claims list
        player.sendMessage(Component.text("=== ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(target.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text("'s Claims ===", NamedTextColor.GOLD, TextDecoration.BOLD)));
        
        for (var claim : claims) {
            // Calculate world coordinates of chunk center
            int worldX = claim.getChunkX() * 16 + 8; // Chunk center X
            int worldZ = claim.getChunkZ() * 16 + 8; // Chunk center Z
            
            Component claimInfo = Component.text("• ", NamedTextColor.GRAY)
                .append(Component.text(claim.getWorldName(), NamedTextColor.YELLOW))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(worldX + ", " + worldZ, NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.GRAY));
            
            player.sendMessage(claimInfo);
        }
        
        player.sendMessage(Component.text("Total claims: ", NamedTextColor.GREEN)
            .append(Component.text(claims.size(), NamedTextColor.WHITE, TextDecoration.BOLD)));
        
        return true;
    }
    
    /**
     * Handles the setclaimlimit command
     */
    private boolean handleSetClaimLimitCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.setclaimlimit")) {
            plugin.getErrorHandler().handlePermissionError(player, "frontierguard.admin.setclaimlimit");
            return true;
        }
        
        if (args.length < 3) {
            plugin.getErrorHandler().sendErrorMessage(player, "Usage: /frontierguard setclaimlimit <group> <limit>");
            return true;
        }
        
        String group = args[1];
        
        // Validate group name
        if (!plugin.getErrorHandler().validateInput(player, group, "Group name", 1, 20)) {
            return true;
        }
        
        // Validate limit
        Integer limit = plugin.getErrorHandler().validateNumericInput(player, args[2], "Claim limit", 0, 1000);
        if (limit == null) {
            return true;
        }
        
        try {
            // Set the claim limit
            plugin.getConfigurationManager().setClaimLimit(group, limit);
            plugin.getErrorHandler().sendSuccessMessage(player, 
                "Set claim limit for group '" + group + "' to " + limit + "!");
        } catch (Exception e) {
            plugin.getErrorHandler().handleConfigurationError(player, "set claim limit", e);
        }
        
        return true;
    }
    
    /**
     * Handles the GUI command to open the main menu
     */
    private boolean handleGuiCommand(Player player, String[] args) {
        plugin.getGuiManager().openMainGui(player);
        return true;
    }
    
    /**
     * Handles the cooldown command to check mode change cooldown status
     */
    private boolean handleCooldownCommand(Player player, String[] args) {
        playerModeManager.checkModeChangeCooldown(player);
        return true;
    }
    
    /**
     * Sends help message to the player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== FrontierGuard Commands ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/frontierguard claim", NamedTextColor.YELLOW)
            .append(Component.text(" - Claim the chunk you're standing in (peaceful mode only)", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard unclaim", NamedTextColor.YELLOW)
            .append(Component.text(" - Unclaim the chunk you're standing in", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard claims", NamedTextColor.YELLOW)
            .append(Component.text(" - List all your claims", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard claiminfo", NamedTextColor.YELLOW)
            .append(Component.text(" - Show information about the current chunk (works for any claim)", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard show", NamedTextColor.YELLOW)
            .append(Component.text(" - Show claim boundaries near you for 5 seconds", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard invite <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Invite a player to your claim", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard invite <landowner> <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Invite a player to landowner's claim", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard uninvite <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Remove a player's access to your claim", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard invitations", NamedTextColor.YELLOW)
            .append(Component.text(" - View invited players for your claim", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard cooldown", NamedTextColor.YELLOW)
            .append(Component.text(" - Check your mode change cooldown status", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard rep [player]", NamedTextColor.YELLOW)
            .append(Component.text(" - View reputation (yours or another player's)", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/frontierguard help", NamedTextColor.YELLOW)
            .append(Component.text(" - Show this help message", NamedTextColor.WHITE)));
        
        // Show admin commands if player has any admin permission
        boolean hasAdminPermission = player.hasPermission("frontierguard.admin.pvparea") ||
                                   player.hasPermission("frontierguard.admin.reload") ||
                                   player.hasPermission("frontierguard.admin.setmode") ||
                                   player.hasPermission("frontierguard.admin.forcemode") ||
                                   player.hasPermission("frontierguard.admin.claims") ||
                                   player.hasPermission("frontierguard.admin.setclaimlimit") ||
                                   player.hasPermission("frontierguard.rep.admin");
        
        if (hasAdminPermission) {
            player.sendMessage(Component.text("", NamedTextColor.GRAY));
            player.sendMessage(Component.text("=== Admin Commands ===", NamedTextColor.RED, TextDecoration.BOLD));
            
            if (player.hasPermission("frontierguard.admin.pvparea")) {
                player.sendMessage(Component.text("/frontierguard setpvpzone", NamedTextColor.YELLOW)
                    .append(Component.text(" - Get PVP area selection tool", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("/frontierguard createpvparea <name>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Create PVP area from selection", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("/frontierguard deletepvparea <name>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Delete a PVP area", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("/frontierguard listpvpareas", NamedTextColor.YELLOW)
                    .append(Component.text(" - List all PVP areas", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.admin.reload")) {
                player.sendMessage(Component.text("/frontierguard reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload plugin configuration", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.admin.setmode")) {
                player.sendMessage(Component.text("/frontierguard setmode <peaceful|normal>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Set your own mode", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.admin.forcemode")) {
                player.sendMessage(Component.text("/frontierguard forcemode <player> <peaceful|normal>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Force set another player's mode", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.admin.claims")) {
                player.sendMessage(Component.text("/frontierguard adminclaims <player>", NamedTextColor.YELLOW)
                    .append(Component.text(" - View another player's claims", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.admin.setclaimlimit")) {
                player.sendMessage(Component.text("/frontierguard setclaimlimit <group> <limit>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Set claim limit for permission group", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.admin")) {
                player.sendMessage(Component.text("/frontierguard update", NamedTextColor.YELLOW)
                    .append(Component.text(" - Check for plugin updates", NamedTextColor.WHITE)));
            }
            
            if (player.hasPermission("frontierguard.rep.admin")) {
                player.sendMessage(Component.text("/frontierguard setrep <player> <reputation>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Set a player's reputation (-15 to 15)", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("/frontierguard addrep <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Add/subtract reputation from a player", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("/frontierguard noob <player>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Mark a player as noob for 30 minutes", NamedTextColor.WHITE)));
            }
        }
        
        // Show mode information
        PlayerMode mode = playerModeManager.getPlayerMode(player);
        if (mode == null) {
            player.sendMessage(Component.text("", NamedTextColor.GRAY));
            player.sendMessage(Component.text("You haven't selected a game mode yet!", NamedTextColor.RED));
            player.sendMessage(Component.text("Join the server to select between Peaceful and Normal mode.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Current mode: ", NamedTextColor.GREEN)
                .append(Component.text(mode.getDisplayName(), NamedTextColor.WHITE, TextDecoration.BOLD)));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("claim", "unclaim", "claims", "claiminfo", "info", "gui", "invite", "uninvite", "invitations", "cooldown", "rep", "help"));
            
            // Add admin commands if player has permission
            if (sender.hasPermission("frontierguard.admin.pvparea")) {
                subCommands.addAll(Arrays.asList("setpvpzone", "createpvparea", "deletepvparea", "listpvpareas"));
            }
            if (sender.hasPermission("frontierguard.admin.reload")) {
                subCommands.add("reload");
            }
            if (sender.hasPermission("frontierguard.admin.setmode")) {
                subCommands.add("setmode");
            }
            if (sender.hasPermission("frontierguard.admin.forcemode")) {
                subCommands.add("forcemode");
            }
            if (sender.hasPermission("frontierguard.admin.claims")) {
                subCommands.add("adminclaims");
            }
            if (sender.hasPermission("frontierguard.admin.setclaimlimit")) {
                subCommands.add("setclaimlimit");
            }
            if (sender.hasPermission("frontierguard.rep.admin")) {
                subCommands.addAll(Arrays.asList("setrep", "addrep", "noob"));
            }
            
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
            
            return completions;
        }
        
        // Tab completion for deletepvparea command
        if (args.length == 2 && args[0].equalsIgnoreCase("deletepvparea")) {
            if (sender.hasPermission("frontierguard.admin.pvparea")) {
                return plugin.getPvpAreaManager().listPvpAreas().stream()
                    .map(area -> area.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }
        
        // GUI command doesn't need tab completion (no additional args)
        
        // Tab completion for setmode command
        if (args.length == 2 && args[0].equalsIgnoreCase("setmode")) {
            if (sender.hasPermission("frontierguard.admin.setmode")) {
                return Arrays.asList("peaceful", "normal").stream()
                    .filter(mode -> mode.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }
        
        // Tab completion for forcemode command
        if (args.length == 2 && args[0].equalsIgnoreCase("forcemode")) {
            if (sender.hasPermission("frontierguard.admin.forcemode")) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("forcemode")) {
            if (sender.hasPermission("frontierguard.admin.forcemode")) {
                return Arrays.asList("peaceful", "normal").stream()
                    .filter(mode -> mode.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
            }
        }
        
        // Tab completion for adminclaims command
        if (args.length == 2 && args[0].equalsIgnoreCase("adminclaims")) {
            if (sender.hasPermission("frontierguard.admin.claims")) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }
        
        // Tab completion for setclaimlimit command
        if (args.length == 2 && args[0].equalsIgnoreCase("setclaimlimit")) {
            if (sender.hasPermission("frontierguard.admin.setclaimlimit")) {
                return plugin.getConfigurationManager().getClaimLimitGroups().stream()
                    .filter(group -> group.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Handles the invite command
     */
    private boolean handleInviteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /fg invite <player> OR /fg invite <landowner> <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetPlayerName;
        ClaimData claim;
        
        if (args.length == 2) {
            // /fg invite <player> - invite to current claim
            targetPlayerName = args[1];
            
            // Check if player is in a claim
            Chunk chunk = player.getLocation().getChunk();
            claim = claimManager.getClaimInfo(chunk);
            
            if (claim == null) {
                player.sendMessage(Component.text("You must be standing in a claim to invite someone!", NamedTextColor.RED));
                return true;
            }
        } else if (args.length == 3) {
            // /fg invite <landowner> <player> - invite to landowner's claim
            String landownerName = args[1];
            targetPlayerName = args[2];
            
            // Find the landowner's claim
            Player landowner = plugin.getServer().getPlayer(landownerName);
            if (landowner == null) {
                player.sendMessage(Component.text("Landowner '" + landownerName + "' not found or not online.", NamedTextColor.RED));
                return true;
            }
            
            // Get the landowner's claim at their current location
            Chunk landownerChunk = landowner.getLocation().getChunk();
            claim = claimManager.getClaimInfo(landownerChunk);
            
            if (claim == null) {
                player.sendMessage(Component.text("Landowner '" + landownerName + "' is not standing in a claim!", NamedTextColor.RED));
                return true;
            }
            
            if (!claim.getOwnerUuid().equals(landowner.getUniqueId())) {
                player.sendMessage(Component.text("Landowner '" + landownerName + "' does not own the claim they're standing in!", NamedTextColor.RED));
                return true;
            }
        } else {
            player.sendMessage(Component.text("Usage: /fg invite <player> OR /fg invite <landowner> <player>", NamedTextColor.RED));
            return true;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player '" + targetPlayerName + "' not found or not online.", NamedTextColor.RED));
            return true;
        }
        
        if (targetPlayer.equals(player)) {
            player.sendMessage(Component.text("You cannot invite yourself!", NamedTextColor.RED));
            return true;
        }
        
        // Check if player owns the claim or has manage invitations permission
        if (!claim.getOwnerUuid().equals(player.getUniqueId())) {
            // Check if player has manage invitations permission for this claim
            invitationManager.canPlayerManageInvitations(player, claim).thenAccept(canManage -> {
                if (!canManage) {
                    player.sendMessage(Component.text("You don't have permission to invite players to this claim!", NamedTextColor.RED));
                    return;
                }
                
                // Invite the player
                invitationManager.invitePlayer(player, targetPlayer, claim);
            });
            return true;
        }
        
        // Invite the player
        invitationManager.invitePlayer(player, targetPlayer, claim);
        return true;
    }
    
    /**
     * Handles the uninvite command
     */
    private boolean handleUninviteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /fg uninvite <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetPlayerName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player '" + targetPlayerName + "' not found or not online.", NamedTextColor.RED));
            return true;
        }
        
        // Check if player has any claims (uninvite works across all claims)
        List<ClaimData> playerClaims = claimManager.getPlayerClaims(player);
        if (playerClaims.isEmpty()) {
            player.sendMessage(Component.text("You don't have any claims to uninvite players from!", NamedTextColor.RED));
            return true;
        }
        
        // Uninvite the player from all claims (pass null as claim since it's not needed)
        invitationManager.uninvitePlayer(player, targetPlayer, null);
        return true;
    }
    
    /**
     * Handles the invitations command
     */
    private boolean handleInvitationsCommand(Player player, String[] args) {
        // Check if player is in a claim
        Chunk chunk = player.getLocation().getChunk();
        ClaimData claim = claimManager.getClaimInfo(chunk);
        
        if (claim == null) {
            player.sendMessage(Component.text("You must be standing in a claim to view invitations!", NamedTextColor.RED));
            return true;
        }
        
        // Check if player owns the claim
        if (!claim.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You can only view invitations for your own claims!", NamedTextColor.RED));
            return true;
        }
        
        // Get and display invited players
        invitationManager.getInvitedPlayerNames(claim).thenAccept(invitedNames -> {
            if (invitedNames.isEmpty()) {
                player.sendMessage(Component.text("No players are invited to this claim.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Invited players:", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (String name : invitedNames) {
                    player.sendMessage(Component.text("• " + name, NamedTextColor.WHITE));
                }
            }
        });
        
        return true;
    }
    
    /**
     * Handles the update command
     */
    private boolean handleUpdateCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        // Check for updates
        player.sendMessage(Component.text("Checking for updates...", NamedTextColor.YELLOW));
        
        // Perform the update check asynchronously and provide feedback
        plugin.getUpdateChecker().checkForUpdates(true); // Force check for manual command
        
        // Wait a moment for the async check to complete, then show results
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                String updateInfo = plugin.getUpdateChecker().getDetailedUpdateInfo();
                
                // Split the message into lines and send each one
                String[] lines = updateInfo.split("\n");
                for (String line : lines) {
                    NamedTextColor color = line.contains("Update available") ? NamedTextColor.GREEN : 
                                         line.contains("up to date") ? NamedTextColor.GREEN :
                                         line.contains("Unable to check") ? NamedTextColor.RED : NamedTextColor.WHITE;
                    player.sendMessage(Component.text(line, color));
                }
                
                // If update is available, show additional info
                if (plugin.getUpdateChecker().isUpdateAvailable()) {
                    player.sendMessage(Component.text(""));
                    player.sendMessage(Component.text("Click the link above to download the latest version!", NamedTextColor.GOLD, TextDecoration.BOLD));
                }
            }
        }.runTaskLater(plugin, 20L); // Wait 1 second for the async check to complete
        
        return true;
    }
    
    /**
     * Handles the test economy command (debug)
     */
    private boolean handleTestEconomyCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        player.sendMessage(Component.text("=== Economy Debug Info ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        // Test Vault connection
        if (plugin.getEconomy() == null) {
            player.sendMessage(Component.text("❌ Economy: NOT CONNECTED", NamedTextColor.RED));
            player.sendMessage(Component.text("Vault plugin not found or economy provider not available", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("✅ Economy: CONNECTED", NamedTextColor.GREEN));
            
            // Test economy functions
            double balance = plugin.getEconomy().getBalance(player);
            player.sendMessage(Component.text("Your balance: $" + String.format("%.2f", balance), NamedTextColor.YELLOW));
            
            // Test economy provider
            String provider = plugin.getEconomy().getName();
            player.sendMessage(Component.text("Economy provider: " + provider, NamedTextColor.YELLOW));
        }
        
        // Test database
        try {
            int purchasedClaims = plugin.getGuiManager().getPurchasedClaims(player);
            player.sendMessage(Component.text("Purchased claims: " + purchasedClaims, NamedTextColor.YELLOW));
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Database error: " + e.getMessage(), NamedTextColor.RED));
        }
        
        // Test claim limits
        int maxClaims = plugin.getConfigurationManager().getClaimLimit("default");
        player.sendMessage(Component.text("Default claim limit: " + maxClaims, NamedTextColor.YELLOW));
        
        return true;
    }
    
    /**
     * Handles the test WorldGuard command (debug)
     */
    private boolean handleTestWorldGuardCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        player.sendMessage(Component.text("=== WorldGuard Debug Info ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        Location location = player.getLocation();
        Chunk chunk = location.getChunk();
        
        // Test WorldGuard integration
        if (!claimManager.getWorldGuardIntegration().isWorldGuardAvailable()) {
            player.sendMessage(Component.text("❌ WorldGuard: NOT AVAILABLE", NamedTextColor.RED));
            player.sendMessage(Component.text("WorldGuard plugin not found or integration failed", NamedTextColor.GRAY));
            return true;
        }
        
        player.sendMessage(Component.text("✅ WorldGuard: AVAILABLE", NamedTextColor.GREEN));
        
        // Test location info
        player.sendMessage(Component.text("Location: " + location.toString(), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Chunk: (" + chunk.getX() + ", " + chunk.getZ() + ")", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("World: " + location.getWorld().getName(), NamedTextColor.YELLOW));
        
        // Test region detection
        try {
            boolean isProtected = claimManager.getWorldGuardIntegration().isChunkProtected(chunk);
            if (isProtected) {
                player.sendMessage(Component.text("✅ Location is PROTECTED by WorldGuard", NamedTextColor.RED));
                
                String regionName = claimManager.getWorldGuardIntegration().getProtectedRegionName(chunk);
                if (regionName != null) {
                    player.sendMessage(Component.text("Region name: " + regionName, NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("Region name: Unknown", NamedTextColor.GRAY));
                }
            } else {
                player.sendMessage(Component.text("❌ Location is NOT PROTECTED by WorldGuard", NamedTextColor.GREEN));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Error checking location protection: " + e.getMessage(), NamedTextColor.RED));
        }
        
        // Test chunk protection
        try {
            boolean chunkProtected = claimManager.getWorldGuardIntegration().isChunkProtected(chunk);
            if (chunkProtected) {
                player.sendMessage(Component.text("✅ Chunk is PROTECTED by WorldGuard", NamedTextColor.RED));
                
                String regionName = claimManager.getWorldGuardIntegration().getProtectedRegionName(chunk);
                if (regionName != null) {
                    player.sendMessage(Component.text("Chunk region name: " + regionName, NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("Chunk region name: Unknown", NamedTextColor.GRAY));
                }
            } else {
                player.sendMessage(Component.text("❌ Chunk is NOT PROTECTED by WorldGuard", NamedTextColor.GREEN));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Error checking chunk protection: " + e.getMessage(), NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Handles the setrep command for setting a player's reputation
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if handled
     */
    private boolean handleSetReputationCommand(Player player, String[] args) {
        if (!player.hasPermission("frontierguard.rep.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /fg setrep <player> <reputation>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage(Component.text("Player '" + targetName + "' not found!", NamedTextColor.RED));
            return true;
        }
        
        try {
            int reputation = Integer.parseInt(args[2]);
            
            if (reputation < -15 || reputation > 15) {
                player.sendMessage(Component.text("Reputation must be between -15 and 15!", NamedTextColor.RED));
                return true;
            }
            
            var reputationManager = plugin.getReputationManager();
            if (reputationManager != null) {
                reputationManager.setPlayerReputation(target, reputation).thenAccept(actualReputation -> {
                    player.sendMessage(Component.text(
                        String.format("Set %s's reputation to %d", target.getName(), actualReputation),
                        NamedTextColor.GREEN));
                    
                    target.sendMessage(Component.text(
                        String.format("Your reputation has been set to %d by an admin", actualReputation),
                        NamedTextColor.YELLOW));
                });
            } else {
                player.sendMessage(Component.text("Reputation system is not available!", NamedTextColor.RED));
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid reputation value! Must be a number.", NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Handles the addrep command for adding to a player's reputation
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if handled
     */
    private boolean handleAddReputationCommand(Player player, String[] args) {
        if (!player.hasPermission("frontierguard.rep.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /fg addrep <player> <amount>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage(Component.text("Player '" + targetName + "' not found!", NamedTextColor.RED));
            return true;
        }
        
        try {
            int amount = Integer.parseInt(args[2]);
            
            var reputationManager = plugin.getReputationManager();
            if (reputationManager != null) {
                reputationManager.addPlayerReputation(target, amount).thenAccept(actualChange -> {
                    if (actualChange != 0) {
                        player.sendMessage(Component.text(
                            String.format("%s %d reputation to %s", 
                                        amount >= 0 ? "Added" : "Removed", Math.abs(actualChange), target.getName()),
                            NamedTextColor.GREEN));
                        
                        target.sendMessage(Component.text(
                            String.format("Your reputation has been %s by %d by an admin", 
                                        amount >= 0 ? "increased" : "decreased", Math.abs(actualChange)),
                            NamedTextColor.YELLOW));
                    } else {
                        player.sendMessage(Component.text(
                            String.format("Could not change %s's reputation (may be at limit)", target.getName()),
                            NamedTextColor.YELLOW));
                    }
                });
            } else {
                player.sendMessage(Component.text("Reputation system is not available!", NamedTextColor.RED));
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount value! Must be a number.", NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Handles the rep command for viewing reputation
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if handled
     */
    private boolean handleReputationCommand(Player player, String[] args) {
        final Player target;
        
        // Check if player is asking for another player's reputation
        if (args.length >= 2) {
            if (!player.hasPermission("frontierguard.rep.view.others")) {
                player.sendMessage(Component.text("You don't have permission to view other players' reputation!", NamedTextColor.RED));
                return true;
            }
            
            String targetName = args[1];
            Player foundPlayer = Bukkit.getPlayer(targetName);
            
            if (foundPlayer == null) {
                player.sendMessage(Component.text("Player '" + targetName + "' not found!", NamedTextColor.RED));
                return true;
            }
            
            target = foundPlayer;
        } else {
            target = player;
        }
        
        var reputationManager = plugin.getReputationManager();
        if (reputationManager != null) {
            reputationManager.getPlayerReputation(target).thenAccept(reputation -> {
                if (reputation != null) {
                    String message = String.format("%s's reputation: %s%d (%s)", 
                                                 target.getName(), 
                                                 reputation.getReputationColor(), 
                                                 reputation.getReputation(),
                                                 reputation.getReputationStatus());
                    
                    player.sendMessage(Component.text(message, NamedTextColor.WHITE));
                } else {
                    player.sendMessage(Component.text(
                        String.format("%s's reputation: §e0 (Neutral)", target.getName()),
                        NamedTextColor.WHITE));
                }
            });
        } else {
            player.sendMessage(Component.text("Reputation system is not available!", NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Handles the noob command for marking players as noob
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if handled
     */
    private boolean handleNoobCommand(Player player, String[] args) {
        if (!player.hasPermission("frontierguard.rep.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /fg noob <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage(Component.text("Player '" + targetName + "' not found!", NamedTextColor.RED));
            return true;
        }
        
        var noobManager = plugin.getNoobManager();
        if (noobManager == null) {
            player.sendMessage(Component.text("Noob system is not available!", NamedTextColor.RED));
            return true;
        }
        
        // Check if player is already marked as noob
        if (noobManager.isPlayerNoob(target)) {
            long remainingTime = noobManager.getRemainingNoobTime(target);
            player.sendMessage(Component.text(
                String.format("%s is already marked as noob for %d more minutes", target.getName(), remainingTime),
                NamedTextColor.YELLOW));
            return true;
        }
        
        // Mark player as noob
        if (noobManager.markPlayerAsNoob(target)) {
            player.sendMessage(Component.text(
                String.format("Marked %s as noob for 30 minutes", target.getName()),
                NamedTextColor.GREEN));
            
            target.sendMessage(Component.text(
                "You have been marked as a noob for 30 minutes by an admin",
                NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text(
                "Failed to mark player as noob",
                NamedTextColor.RED));
        }
        
        return true;
    }
    
    
}
