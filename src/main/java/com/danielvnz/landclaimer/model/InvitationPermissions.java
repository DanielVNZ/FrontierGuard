package com.danielvnz.landclaimer.model;

import java.util.UUID;

/**
 * Represents the permissions for a player invited to a claim.
 * Contains granular permission settings for different actions.
 */
public class InvitationPermissions {
    
    private final long claimId;
    private final UUID invitedUuid;
    private final UUID invitedByUuid;
    private final boolean canBuild;
    private final boolean canAccessContainers;
    private final boolean canManageInvitations;
    private final long invitationTime;
    
    public InvitationPermissions(long claimId, UUID invitedUuid, UUID invitedByUuid, 
                               boolean canBuild, boolean canAccessContainers, 
                               boolean canManageInvitations, long invitationTime) {
        this.claimId = claimId;
        this.invitedUuid = invitedUuid;
        this.invitedByUuid = invitedByUuid;
        this.canBuild = canBuild;
        this.canAccessContainers = canAccessContainers;
        this.canManageInvitations = canManageInvitations;
        this.invitationTime = invitationTime;
    }
    
    public long getClaimId() {
        return claimId;
    }
    
    public UUID getInvitedUuid() {
        return invitedUuid;
    }
    
    public UUID getInvitedByUuid() {
        return invitedByUuid;
    }
    
    public boolean canBuild() {
        return canBuild;
    }
    
    public boolean canAccessContainers() {
        return canAccessContainers;
    }
    
    public boolean canManageInvitations() {
        return canManageInvitations;
    }
    
    public long getInvitationTime() {
        return invitationTime;
    }
    
    /**
     * Creates a new InvitationPermissions with updated build permission
     */
    public InvitationPermissions withCanBuild(boolean canBuild) {
        return new InvitationPermissions(claimId, invitedUuid, invitedByUuid, 
                                       canBuild, canAccessContainers, canManageInvitations, invitationTime);
    }
    
    /**
     * Creates a new InvitationPermissions with updated container access permission
     */
    public InvitationPermissions withCanAccessContainers(boolean canAccessContainers) {
        return new InvitationPermissions(claimId, invitedUuid, invitedByUuid, 
                                       canBuild, canAccessContainers, canManageInvitations, invitationTime);
    }
    
    /**
     * Creates a new InvitationPermissions with updated invitation management permission
     */
    public InvitationPermissions withCanManageInvitations(boolean canManageInvitations) {
        return new InvitationPermissions(claimId, invitedUuid, invitedByUuid, 
                                       canBuild, canAccessContainers, canManageInvitations, invitationTime);
    }
}
