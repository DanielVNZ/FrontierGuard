package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.dao.ClaimInvitationDao;
import com.danielvnz.landclaimer.model.ClaimData;
import com.danielvnz.landclaimer.model.InvitationPermissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages claim invitations for allowing players to build on others' claims.
 * Handles inviting, uninviting, and checking permissions for claim access.
 */
public class ClaimInvitationManager {
    
    private final LandClaimerPlugin plugin;
    private final ClaimInvitationDao invitationDao;
    
    public ClaimInvitationManager(LandClaimerPlugin plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.invitationDao = createInvitationDao(plugin.getDatabaseManager());
    }
    
    /**
     * Creates the ClaimInvitationDao instance. Can be overridden for testing.
     * @param databaseManager The database manager
     * @return The ClaimInvitationDao instance
     */
    protected ClaimInvitationDao createInvitationDao(com.danielvnz.landclaimer.database.DatabaseManager databaseManager) {
        return new ClaimInvitationDao(databaseManager);
    }
    
    /**
     * Invites a player to a claim
     * @param inviter The player sending the invitation
     * @param invitedPlayer The player being invited
     * @param claim The claim to invite to
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> invitePlayer(Player inviter, Player invitedPlayer, ClaimData claim) {
        // Check if the invited player is in peaceful mode
        if (!plugin.getPlayerModeManager().isPeacefulPlayer(invitedPlayer)) {
            inviter.sendMessage(Component.text("You can only invite peaceful players to your claim!", NamedTextColor.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        // Get all claims owned by the inviter
        List<ClaimData> allClaims = plugin.getClaimManager().getPlayerClaims(inviter);
        if (allClaims.isEmpty()) {
            inviter.sendMessage(Component.text("You don't have any claims to invite players to!", NamedTextColor.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        // Create a list of futures for inviting to all claims
        List<CompletableFuture<Void>> inviteFutures = new ArrayList<>();
        
        for (ClaimData inviterClaim : allClaims) {
            inviteFutures.add(invitationDao.invitePlayer(
                inviterClaim.getId(), 
                invitedPlayer.getUniqueId(), 
                inviter.getUniqueId(),
                true, // canBuild
                true, // canAccessContainers
                false // canManageInvitations (default to false)
            ));
        }
        
        // Wait for all invitations to complete
        return CompletableFuture.allOf(inviteFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                // Send confirmation messages
                inviter.sendMessage(Component.text("You have invited " + invitedPlayer.getName() + " to all " + allClaims.size() + " of your claims!", NamedTextColor.GREEN));
                invitedPlayer.sendMessage(Component.text(inviter.getName() + " has invited you to all their claims!", NamedTextColor.GREEN));
                invitedPlayer.sendMessage(Component.text("You can now build and edit blocks in all their claims.", NamedTextColor.YELLOW));
            })
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Error inviting player to claims", throwable);
                inviter.sendMessage(Component.text("Error inviting player. Please try again.", NamedTextColor.RED));
                return null;
            });
    }
    
    /**
     * Uninvites a player from all claims owned by the uninviter
     * @param uninviter The player removing the invitation
     * @param uninvitedPlayer The player being uninvited
     * @param claim The claim parameter (can be null, not used anymore)
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> uninvitePlayer(Player uninviter, Player uninvitedPlayer, ClaimData claim) {
        // Get all claims owned by the uninviter
        List<ClaimData> allClaims = plugin.getClaimManager().getPlayerClaims(uninviter);
        if (allClaims.isEmpty()) {
            uninviter.sendMessage(Component.text("You don't have any claims!", NamedTextColor.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        // Create a list of futures for uninviting from all claims
        List<CompletableFuture<Void>> uninviteFutures = new ArrayList<>();
        
        for (ClaimData uninviterClaim : allClaims) {
            uninviteFutures.add(invitationDao.uninvitePlayer(uninviterClaim.getId(), uninvitedPlayer.getUniqueId()));
        }
        
        // Wait for all uninvitations to complete
        return CompletableFuture.allOf(uninviteFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                // Send confirmation messages
                uninviter.sendMessage(Component.text("You have uninvited " + uninvitedPlayer.getName() + " from all " + allClaims.size() + " of your claims.", NamedTextColor.GREEN));
                uninvitedPlayer.sendMessage(Component.text(uninviter.getName() + " has uninvited you from all their claims.", NamedTextColor.RED));
            })
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Error uninviting player from claims", throwable);
                uninviter.sendMessage(Component.text("Error uninviting player. Please try again.", NamedTextColor.RED));
                return null;
            });
    }
    
    /**
     * Checks if a player can build in a claim
     * @param player The player to check
     * @param claim The claim to check
     * @return CompletableFuture containing true if the player can build, false otherwise
     */
    public CompletableFuture<Boolean> canPlayerBuild(Player player, ClaimData claim) {
        // Owner can always build
        if (claim.getOwnerUuid().equals(player.getUniqueId())) {
            return CompletableFuture.completedFuture(true);
        }
        
        // Check if player is invited to ANY claim owned by the same landowner and has build permission
        return invitationDao.getInvitationPermissionsByOwner(claim.getOwnerUuid(), player.getUniqueId())
            .thenApply(permissions -> permissions != null && permissions.canBuild());
    }
    
    /**
     * Checks if a player can access containers in a claim
     * @param player The player to check
     * @param claim The claim to check
     * @return CompletableFuture containing true if the player can access containers, false otherwise
     */
    public CompletableFuture<Boolean> canPlayerAccessContainers(Player player, ClaimData claim) {
        // Owner can always access containers
        if (claim.getOwnerUuid().equals(player.getUniqueId())) {
            return CompletableFuture.completedFuture(true);
        }
        
        // Check if player is invited to ANY claim owned by the same landowner and has container access permission
        return invitationDao.getInvitationPermissionsByOwner(claim.getOwnerUuid(), player.getUniqueId())
            .thenApply(permissions -> permissions != null && permissions.canAccessContainers());
    }
    
    /**
     * Checks if a player can manage invitations for a claim
     * @param player The player to check
     * @param claim The claim to check
     * @return CompletableFuture containing true if the player can manage invitations, false otherwise
     */
    public CompletableFuture<Boolean> canPlayerManageInvitations(Player player, ClaimData claim) {
        // Owner can always manage invitations
        if (claim.getOwnerUuid().equals(player.getUniqueId())) {
            return CompletableFuture.completedFuture(true);
        }
        
        // Check if player is invited and has invitation management permission
        return invitationDao.getInvitationPermissions(claim.getId(), player.getUniqueId())
            .thenApply(permissions -> permissions != null && permissions.canManageInvitations());
    }
    
    /**
     * Gets all invited players for a claim
     * @param claim The claim to get invitations for
     * @return CompletableFuture containing list of invited player names
     */
    public CompletableFuture<List<String>> getInvitedPlayerNames(ClaimData claim) {
        return invitationDao.getInvitedPlayers(claim.getId())
            .thenApply(invitedUuids -> {
                return invitedUuids.stream()
                    .map(this::getPlayerName)
                    .toList();
            });
    }
    
    /**
     * Gets all invitation permissions for a claim
     * @param claim The claim to get permissions for
     * @return CompletableFuture containing list of invitation permissions
     */
    public CompletableFuture<List<InvitationPermissions>> getAllInvitationPermissions(ClaimData claim) {
        return invitationDao.getAllInvitationPermissions(claim.getId());
    }
    
    /**
     * Updates the permissions for an invited player
     * @param claim The claim
     * @param playerUuid The UUID of the player to update
     * @param canBuild Whether the player can build
     * @param canAccessContainers Whether the player can access containers
     * @param canManageInvitations Whether the player can manage invitations
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> updatePlayerPermissions(ClaimData claim, UUID playerUuid, 
                                                          boolean canBuild, boolean canAccessContainers, 
                                                          boolean canManageInvitations) {
        // Get the claim owner to update permissions across all their claims
        UUID claimOwnerUuid = claim.getOwnerUuid();
        
        // Get all claims owned by the claim owner
        List<ClaimData> allClaims = plugin.getClaimManager().getPlayerClaims(claimOwnerUuid);
        if (allClaims.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.completedFuture(null).thenCompose(v -> {
            // Create a list of futures for updating permissions across all claims
            List<CompletableFuture<Void>> updateFutures = new ArrayList<>();
            
            for (ClaimData ownerClaim : allClaims) {
                updateFutures.add(invitationDao.getInvitationPermissions(ownerClaim.getId(), playerUuid)
                    .thenCompose(permissions -> {
                        if (permissions == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        InvitationPermissions updatedPermissions = permissions
                            .withCanBuild(canBuild)
                            .withCanAccessContainers(canAccessContainers)
                            .withCanManageInvitations(canManageInvitations);
                        
                        return invitationDao.updateInvitationPermissions(updatedPermissions);
                    }));
            }
            
            // Wait for all permission updates to complete
            return CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0]));
        });
    }
    
    /**
     * Gets the player name from UUID
     * @param uuid The player's UUID
     * @return The player's name, or "Unknown Player" if not found
     */
    private String getPlayerName(UUID uuid) {
        // First try to get online player
        Player onlinePlayer = org.bukkit.Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        // Try to get offline player
        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        
        // Fallback to UUID string if name not available
        return "Unknown Player";
    }
    
    /**
     * Removes all invitations for a claim (called when claim is deleted)
     * @param claim The claim being deleted
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> removeAllInvitations(ClaimData claim) {
        return invitationDao.removeAllInvitations(claim.getId());
    }
    
    /**
     * Removes all invitations for a player (called when player changes from peaceful to normal mode)
     * @param playerUuid The UUID of the player
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> removeAllPlayerInvitations(UUID playerUuid) {
        return invitationDao.getPlayerInvitations(playerUuid)
            .thenCompose(claimIds -> {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (Long claimId : claimIds) {
                    futures.add(invitationDao.uninvitePlayer(claimId, playerUuid));
                }
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            });
    }
}
