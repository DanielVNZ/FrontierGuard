package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Manages protection for claimed chunks.
 * Enforces protection rules based on player modes and locations.
 */
public class ProtectionManager {
    
    private final LandClaimerPlugin plugin;
    private final ClaimManager claimManager;
    private final ClaimInvitationManager invitationManager;
    private final PlayerModeManager playerModeManager;
    private final PvpAreaManager pvpAreaManager;
    
    // Cache to track players whose invitations should be immediately revoked
    private final Set<UUID> revokedInvitations = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    
    // Materials that are considered containers
    private static final Set<Material> CONTAINER_MATERIALS = Set.of(
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.ENDER_CHEST,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.BARREL,
        Material.DISPENSER,
        Material.DROPPER,
        Material.HOPPER,
        Material.SHULKER_BOX,
        Material.BLACK_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX
    );
    
    // Materials that are considered doors/gates
    private static final Set<Material> DOOR_MATERIALS = Set.of(
        Material.OAK_DOOR,
        Material.SPRUCE_DOOR,
        Material.BIRCH_DOOR,
        Material.JUNGLE_DOOR,
        Material.ACACIA_DOOR,
        Material.DARK_OAK_DOOR,
        Material.CRIMSON_DOOR,
        Material.WARPED_DOOR,
        Material.IRON_DOOR,
        Material.OAK_TRAPDOOR,
        Material.SPRUCE_TRAPDOOR,
        Material.BIRCH_TRAPDOOR,
        Material.JUNGLE_TRAPDOOR,
        Material.ACACIA_TRAPDOOR,
        Material.DARK_OAK_TRAPDOOR,
        Material.CRIMSON_TRAPDOOR,
        Material.WARPED_TRAPDOOR,
        Material.IRON_TRAPDOOR,
        Material.OAK_FENCE_GATE,
        Material.SPRUCE_FENCE_GATE,
        Material.BIRCH_FENCE_GATE,
        Material.JUNGLE_FENCE_GATE,
        Material.ACACIA_FENCE_GATE,
        Material.DARK_OAK_FENCE_GATE,
        Material.CRIMSON_FENCE_GATE,
        Material.WARPED_FENCE_GATE
    );
    
    public ProtectionManager(LandClaimerPlugin plugin, ClaimManager claimManager, PlayerModeManager playerModeManager, PvpAreaManager pvpAreaManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.invitationManager = plugin.getInvitationManager();
        this.playerModeManager = playerModeManager;
        this.pvpAreaManager = pvpAreaManager;
    }
    
    /**
     * Checks if a player can break a block
     * @param player The player attempting to break the block
     * @param block The block being broken
     * @return true if the player can break the block, false otherwise
     */
    public boolean canBreakBlock(Player player, Block block) {
        if (player == null || block == null) {
            return true; // Allow if null (shouldn't happen in normal circumstances)
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if the block is in a PVP area - no building allowed in PVP areas
        if (pvpAreaManager.isInPvpArea(block.getLocation())) {
            return false; // No building in PVP areas
        }
        
        // Check if the block is in a claimed chunk
        if (!claimManager.isLocationClaimed(block.getLocation())) {
            return true; // Not claimed, allow breaking
        }
        
        // Check if the player owns the chunk
        if (claimManager.isChunkOwnedBy(player, block.getLocation())) {
            return true; // Owner can break blocks in their own claim
        }
        
        // Check if the player is invited to this claim
        var claim = claimManager.getClaimInfo(block.getLocation().getChunk());
        if (claim != null) {
            try {
                // Check if player's invitations have been revoked
                if (revokedInvitations.contains(player.getUniqueId())) {
                    return false; // Invitations have been revoked
                }
                
                // Use synchronous check for invitation (we'll make this async later if needed)
                if (invitationManager.canPlayerBuild(player, claim).get()) {
                    // Additional check: invited players must still be peaceful
                    if (playerModeManager.isPeacefulPlayer(player)) {
                        return true; // Invited peaceful player can break blocks
                    } else {
                        // Player is no longer peaceful, immediately revoke invitations
                        plugin.getLogger().info("Player " + player.getName() + " is no longer peaceful, immediately revoking invitation access");
                        revokedInvitations.add(player.getUniqueId());
                        invitationManager.removeAllPlayerInvitations(player.getUniqueId());
                        return false;
                    }
                } else {
                    // Player is not invited to this claim, deny access
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking invitation for block break: " + e.getMessage());
                return false; // Deny access on error
            }
        }
        
        // Check if the player is in peaceful mode (they should be protected from breaking others' blocks)
        if (playerModeManager.isPeacefulPlayer(player)) {
            return false; // Peaceful players can't break blocks in others' claims
        }
        
        // Normal players can break blocks in claimed chunks (for raiding)
        return true;
    }
    
    /**
     * Checks if a player can place a block
     * @param player The player attempting to place the block
     * @param block The block being placed
     * @return true if the player can place the block, false otherwise
     */
    public boolean canPlaceBlock(Player player, Block block) {
        if (player == null || block == null) {
            return true; // Allow if null (shouldn't happen in normal circumstances)
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if the block is in a PVP area - no building allowed in PVP areas
        if (pvpAreaManager.isInPvpArea(block.getLocation())) {
            return false; // No building in PVP areas
        }
        
        // Check if the block is in a claimed chunk
        if (!claimManager.isLocationClaimed(block.getLocation())) {
            return true; // Not claimed, allow placing
        }
        
        // Check if the player owns the chunk
        if (claimManager.isChunkOwnedBy(player, block.getLocation())) {
            return true; // Owner can place blocks in their own claim
        }
        
        // Check if the player is invited to this claim
        var claim = claimManager.getClaimInfo(block.getLocation().getChunk());
        if (claim != null) {
            try {
                // Check if player's invitations have been revoked
                if (revokedInvitations.contains(player.getUniqueId())) {
                    return false; // Invitations have been revoked
                }
                
                // Use synchronous check for invitation (we'll make this async later if needed)
                if (invitationManager.canPlayerBuild(player, claim).get()) {
                    // Additional check: invited players must still be peaceful
                    if (playerModeManager.isPeacefulPlayer(player)) {
                        return true; // Invited peaceful player can place blocks
                    } else {
                        // Player is no longer peaceful, immediately revoke invitations
                        plugin.getLogger().info("Player " + player.getName() + " is no longer peaceful, immediately revoking invitation access");
                        revokedInvitations.add(player.getUniqueId());
                        invitationManager.removeAllPlayerInvitations(player.getUniqueId());
                        return false;
                    }
                } else {
                    // Player is not invited to this claim, deny access
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking invitation for block place: " + e.getMessage());
                return false; // Deny access on error
            }
        }
        
        // Check if the player is in peaceful mode (they should be protected from placing in others' claims)
        if (playerModeManager.isPeacefulPlayer(player)) {
            return false; // Peaceful players can't place blocks in others' claims
        }
        
        // Normal players can place blocks in claimed chunks (for raiding)
        return true;
    }
    
    /**
     * Checks if a player can access a container
     * @param player The player attempting to access the container
     * @param block The container block
     * @return true if the player can access the container, false otherwise
     */
    public boolean canAccessContainer(Player player, Block block) {
        if (player == null || block == null) {
            return true; // Allow if null (shouldn't happen in normal circumstances)
        }
        
        // Check if the block is actually a container
        if (!CONTAINER_MATERIALS.contains(block.getType())) {
            return true; // Not a container, allow access
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if the container is in a claimed chunk
        if (!claimManager.isLocationClaimed(block.getLocation())) {
            return true; // Not claimed, allow access
        }
        
        // Check if the player owns the chunk
        if (claimManager.isChunkOwnedBy(player, block.getLocation())) {
            return true; // Owner can access containers in their own claim
        }
        
        // Check if the player is invited to this claim
        var claim = claimManager.getClaimInfo(block.getLocation().getChunk());
        if (claim != null) {
            try {
                // Check if player's invitations have been revoked
                if (revokedInvitations.contains(player.getUniqueId())) {
                    return false; // Invitations have been revoked
                }
                
                // Use synchronous check for invitation (we'll make this async later if needed)
                if (invitationManager.canPlayerAccessContainers(player, claim).get()) {
                    // Additional check: invited players must still be peaceful
                    if (playerModeManager.isPeacefulPlayer(player)) {
                        return true; // Invited peaceful player can access containers
                    } else {
                        // Player is no longer peaceful, immediately revoke invitations
                        plugin.getLogger().info("Player " + player.getName() + " is no longer peaceful, immediately revoking invitation access");
                        revokedInvitations.add(player.getUniqueId());
                        invitationManager.removeAllPlayerInvitations(player.getUniqueId());
                        return false;
                    }
                } else {
                    // Player is not invited to this claim, deny access
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking invitation for container access: " + e.getMessage());
                return false; // Deny access on error
            }
        }
        
        // Check if the player is in peaceful mode (they should be protected from accessing others' containers)
        if (playerModeManager.isPeacefulPlayer(player)) {
            return false; // Peaceful players can't access containers in others' claims
        }
        
        // Normal players can access containers in claimed chunks (for raiding)
        return true;
    }
    
    /**
     * Checks if a player can interact with a door/gate
     * @param player The player attempting to interact with the door/gate
     * @param block The door/gate block
     * @return true if the player can interact with the door/gate, false otherwise
     */
    public boolean canInteractWithDoor(Player player, Block block) {
        if (player == null || block == null) {
            return true; // Allow if null (shouldn't happen in normal circumstances)
        }
        
        // Check if the block is actually a door/gate
        if (!DOOR_MATERIALS.contains(block.getType())) {
            return true; // Not a door/gate, allow interaction
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("frontierguard.bypass")) {
            return true;
        }
        
        // Check if the door/gate is in a claimed chunk
        if (!claimManager.isLocationClaimed(block.getLocation())) {
            return true; // Not claimed, allow interaction
        }
        
        // Check if the player owns the chunk
        if (claimManager.isChunkOwnedBy(player, block.getLocation())) {
            return true; // Owner can interact with doors/gates in their own claim
        }
        
        // Check if the player is in peaceful mode (they should be protected from interacting with others' doors/gates)
        if (playerModeManager.isPeacefulPlayer(player)) {
            return false; // Peaceful players can't interact with doors/gates in others' claims
        }
        
        // Normal players can interact with doors/gates in claimed chunks (for raiding)
        return true;
    }
    
    /**
     * Handles block break protection
     * @param event The BlockBreakEvent
     */
    public void handleBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!canBreakBlock(player, block)) {
            event.setCancelled(true);
            
            // Check if it's a PVP area
            if (pvpAreaManager.isInPvpArea(block.getLocation())) {
                // Simple chat message for PVP area - no popup
                String message = plugin.getMessage("pvp-area-no-break");
                if (message.equals("pvp-area-no-break")) {
                    message = "&c&lYou cannot break blocks in PvP areas!";
                }
                // Convert color codes to Adventure Component
                Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                player.sendMessage(messageComponent);
            } else {
                // Send protection message for claimed areas
                String message = plugin.getMessage("protection-block-break");
                if (message.equals("protection-block-break")) {
                    message = "You cannot break blocks in this protected area!";
                }
                player.sendMessage(Component.text(message, NamedTextColor.RED));
                plugin.getVisualFeedbackManager().showClaimFailure(player, "Protected area!");
            }
        }
    }
    
    /**
     * Handles block place protection
     * @param event The BlockPlaceEvent
     */
    public void handleBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!canPlaceBlock(player, block)) {
            event.setCancelled(true);
            
            // Check if it's a PVP area
            if (pvpAreaManager.isInPvpArea(block.getLocation())) {
                // Simple chat message for PVP area - no popup
                String message = plugin.getMessage("pvp-area-no-build");
                if (message.equals("pvp-area-no-build")) {
                    message = "&c&lYou cannot build in PvP areas!";
                }
                // Convert color codes to Adventure Component
                Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                player.sendMessage(messageComponent);
            } else {
                // Send protection message for claimed areas
                String message = plugin.getMessage("protection-block-place");
                if (message.equals("protection-block-place")) {
                    message = "You cannot place blocks in this protected area!";
                }
                player.sendMessage(Component.text(message, NamedTextColor.RED));
                plugin.getVisualFeedbackManager().showClaimFailure(player, "Protected area!");
            }
        }
    }
    
    /**
     * Handles container access protection
     * @param event The PlayerInteractEvent
     */
    public void handleContainerAccess(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null) {
            return;
        }
        
        if (!canAccessContainer(player, block)) {
            event.setCancelled(true);
            
            // Send protection message
            String message = plugin.getMessage("protection-container-access");
            if (message.equals("protection-container-access")) {
                message = "You cannot access containers in this protected area!";
            }
            
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            
            // Show visual feedback
            plugin.getVisualFeedbackManager().showClaimFailure(player, "Protected container!");
        }
    }
    
    /**
     * Handles door/gate interaction protection
     * @param event The PlayerInteractEvent
     */
    public void handleDoorInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null) {
            return;
        }
        
        if (!canInteractWithDoor(player, block)) {
            event.setCancelled(true);
            
            // Send protection message
            String message = plugin.getMessage("protection-door-access");
            if (message.equals("protection-door-access")) {
                message = "You cannot interact with doors in this protected area!";
            }
            
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            
            // Show visual feedback
            plugin.getVisualFeedbackManager().showClaimFailure(player, "Protected door!");
        }
    }
    
    /**
     * Checks if a location is protected
     * @param location The location to check
     * @return true if the location is protected, false otherwise
     */
    public boolean isLocationProtected(Location location) {
        if (location == null) {
            return false;
        }
        
        return claimManager.isLocationClaimed(location);
    }
    
    /**
     * Gets the protection status for a location
     * @param location The location to check
     * @return A string describing the protection status
     */
    public String getProtectionStatus(Location location) {
        if (location == null) {
            return "Not protected";
        }
        
        if (!claimManager.isLocationClaimed(location)) {
            return "Not protected";
        }
        
        // Get claim info
        var claimData = claimManager.getClaimInfo(location.getChunk());
        if (claimData != null) {
            return "Protected by " + claimData.getOwnerUuid().toString();
        }
        
        return "Protected";
    }
    
    /**
     * Clears the revoked invitations cache for a player
     * @param playerUuid The UUID of the player to clear
     */
    public void clearRevokedInvitations(UUID playerUuid) {
        revokedInvitations.remove(playerUuid);
        plugin.getLogger().info("Cleared revoked invitations cache for player: " + playerUuid);
    }
}
