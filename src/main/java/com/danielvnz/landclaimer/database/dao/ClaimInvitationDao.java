package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.model.InvitationPermissions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing claim invitations in the database.
 * Handles CRUD operations for claim invitation system.
 */
public class ClaimInvitationDao {
    private static final Logger LOGGER = Logger.getLogger(ClaimInvitationDao.class.getName());
    
    private final DatabaseManager databaseManager;
    
    public ClaimInvitationDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Invites a player to a claim with default permissions
     * @param claimId The ID of the claim
     * @param invitedUuid The UUID of the player being invited
     * @param invitedByUuid The UUID of the player sending the invitation
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> invitePlayer(long claimId, UUID invitedUuid, UUID invitedByUuid) {
        return invitePlayer(claimId, invitedUuid, invitedByUuid, true, true, false);
    }
    
    /**
     * Invites a player to a claim with specific permissions
     * @param claimId The ID of the claim
     * @param invitedUuid The UUID of the player being invited
     * @param invitedByUuid The UUID of the player sending the invitation
     * @param canBuild Whether the player can build
     * @param canAccessContainers Whether the player can access containers
     * @param canManageInvitations Whether the player can manage invitations
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> invitePlayer(long claimId, UUID invitedUuid, UUID invitedByUuid,
                                              boolean canBuild, boolean canAccessContainers, boolean canManageInvitations) {
        return databaseManager.executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO claim_invitations (claim_id, invited_uuid, invited_by_uuid, 
                                                         can_build, can_access_containers, can_manage_invitations, invitation_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                stmt.setString(2, invitedUuid.toString());
                stmt.setString(3, invitedByUuid.toString());
                stmt.setBoolean(4, canBuild);
                stmt.setBoolean(5, canAccessContainers);
                stmt.setBoolean(6, canManageInvitations);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error inviting player to claim: " + claimId, e);
                throw new RuntimeException("Failed to invite player", e);
            }
        });
    }
    
    /**
     * Removes a player's invitation from a claim
     * @param claimId The ID of the claim
     * @param invitedUuid The UUID of the player to uninvite
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> uninvitePlayer(long claimId, UUID invitedUuid) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM claim_invitations WHERE claim_id = ? AND invited_uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                stmt.setString(2, invitedUuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error uninviting player from claim: " + claimId, e);
                throw new RuntimeException("Failed to uninvite player", e);
            }
        });
    }
    
    /**
     * Checks if a player is invited to a claim
     * @param claimId The ID of the claim
     * @param playerUuid The UUID of the player to check
     * @return CompletableFuture containing true if invited, false otherwise
     */
    public CompletableFuture<Boolean> isPlayerInvited(long claimId, UUID playerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT 1 FROM claim_invitations WHERE claim_id = ? AND invited_uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                stmt.setString(2, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error checking invitation for claim: " + claimId, e);
                return false;
            }
        });
    }
    
    /**
     * Gets the invitation permissions for a player in a claim
     * @param claimId The ID of the claim
     * @param playerUuid The UUID of the player
     * @return CompletableFuture containing the invitation permissions, or null if not invited
     */
    public CompletableFuture<InvitationPermissions> getInvitationPermissions(long claimId, UUID playerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = """
                SELECT claim_id, invited_uuid, invited_by_uuid, can_build, can_access_containers, 
                       can_manage_invitations, invitation_time
                FROM claim_invitations 
                WHERE claim_id = ? AND invited_uuid = ?
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                stmt.setString(2, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new InvitationPermissions(
                            rs.getLong("claim_id"),
                            UUID.fromString(rs.getString("invited_uuid")),
                            UUID.fromString(rs.getString("invited_by_uuid")),
                            rs.getBoolean("can_build"),
                            rs.getBoolean("can_access_containers"),
                            rs.getBoolean("can_manage_invitations"),
                            rs.getLong("invitation_time")
                        );
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error getting invitation permissions for claim: " + claimId, e);
            }
            return null;
        });
    }
    
    /**
     * Updates the invitation permissions for a player
     * @param permissions The new invitation permissions
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> updateInvitationPermissions(InvitationPermissions permissions) {
        return databaseManager.executeAsync(connection -> {
            String sql = """
                UPDATE claim_invitations 
                SET can_build = ?, can_access_containers = ?, can_manage_invitations = ?
                WHERE claim_id = ? AND invited_uuid = ?
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setBoolean(1, permissions.canBuild());
                stmt.setBoolean(2, permissions.canAccessContainers());
                stmt.setBoolean(3, permissions.canManageInvitations());
                stmt.setLong(4, permissions.getClaimId());
                stmt.setString(5, permissions.getInvitedUuid().toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error updating invitation permissions for claim: " + permissions.getClaimId(), e);
                throw new RuntimeException("Failed to update invitation permissions", e);
            }
        });
    }
    
    /**
     * Gets all invited players for a claim
     * @param claimId The ID of the claim
     * @return CompletableFuture containing list of invited player UUIDs
     */
    public CompletableFuture<List<UUID>> getInvitedPlayers(long claimId) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT invited_uuid FROM claim_invitations WHERE claim_id = ?";
            List<UUID> invitedPlayers = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        invitedPlayers.add(UUID.fromString(rs.getString("invited_uuid")));
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error getting invited players for claim: " + claimId, e);
            }
            
            return invitedPlayers;
        });
    }
    
    /**
     * Gets all invitation permissions for a claim
     * @param claimId The ID of the claim
     * @return CompletableFuture containing list of invitation permissions
     */
    public CompletableFuture<List<InvitationPermissions>> getAllInvitationPermissions(long claimId) {
        return databaseManager.queryAsync(connection -> {
            String sql = """
                SELECT claim_id, invited_uuid, invited_by_uuid, can_build, can_access_containers, 
                       can_manage_invitations, invitation_time
                FROM claim_invitations 
                WHERE claim_id = ?
                ORDER BY invitation_time ASC
                """;
            List<InvitationPermissions> permissions = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        permissions.add(new InvitationPermissions(
                            rs.getLong("claim_id"),
                            UUID.fromString(rs.getString("invited_uuid")),
                            UUID.fromString(rs.getString("invited_by_uuid")),
                            rs.getBoolean("can_build"),
                            rs.getBoolean("can_access_containers"),
                            rs.getBoolean("can_manage_invitations"),
                            rs.getLong("invitation_time")
                        ));
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error getting all invitation permissions for claim: " + claimId, e);
            }
            
            return permissions;
        });
    }
    
    /**
     * Gets all claims a player is invited to
     * @param playerUuid The UUID of the player
     * @return CompletableFuture containing list of claim IDs
     */
    public CompletableFuture<List<Long>> getPlayerInvitations(UUID playerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT claim_id FROM claim_invitations WHERE invited_uuid = ?";
            List<Long> claimIds = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        claimIds.add(rs.getLong("claim_id"));
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error getting player invitations for: " + playerUuid, e);
            }
            
            return claimIds;
        });
    }
    
    /**
     * Removes all invitations for a claim (when claim is deleted)
     * @param claimId The ID of the claim
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> removeAllInvitations(long claimId) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM claim_invitations WHERE claim_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, claimId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error removing all invitations for claim: " + claimId, e);
                throw new RuntimeException("Failed to remove invitations", e);
            }
        });
    }
}
