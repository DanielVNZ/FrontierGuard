package com.danielvnz.landclaimer.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a land claim made by a peaceful player.
 * Contains chunk coordinates, owner information, and claim metadata.
 */
public class ClaimData {
    private final Long id;
    private final UUID ownerUuid;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final LocalDateTime claimTime;
    
    /**
     * Creates a new ClaimData instance (for database retrieval)
     * @param id The database ID of the claim
     * @param ownerUuid The UUID of the player who owns this claim
     * @param worldName The name of the world where the claim is located
     * @param chunkX The X coordinate of the claimed chunk
     * @param chunkZ The Z coordinate of the claimed chunk
     * @param claimTime The time when the claim was made
     */
    public ClaimData(Long id, UUID ownerUuid, String worldName, int chunkX, int chunkZ, LocalDateTime claimTime) {
        this.id = id;
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "Owner UUID cannot be null");
        this.worldName = Objects.requireNonNull(worldName, "World name cannot be null");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimTime = Objects.requireNonNull(claimTime, "Claim time cannot be null");
    }
    
    /**
     * Creates a new ClaimData instance (for new claims, ID will be set by database)
     * @param ownerUuid The UUID of the player who owns this claim
     * @param worldName The name of the world where the claim is located
     * @param chunkX The X coordinate of the claimed chunk
     * @param chunkZ The Z coordinate of the claimed chunk
     * @param claimTime The time when the claim was made
     */
    public ClaimData(UUID ownerUuid, String worldName, int chunkX, int chunkZ, LocalDateTime claimTime) {
        this(null, ownerUuid, worldName, chunkX, chunkZ, claimTime);
    }
    
    /**
     * Creates a new ClaimData instance with the current time (for new claims)
     * @param ownerUuid The UUID of the player who owns this claim
     * @param worldName The name of the world where the claim is located
     * @param chunkX The X coordinate of the claimed chunk
     * @param chunkZ The Z coordinate of the claimed chunk
     */
    public ClaimData(UUID ownerUuid, String worldName, int chunkX, int chunkZ) {
        this(null, ownerUuid, worldName, chunkX, chunkZ, LocalDateTime.now());
    }
    
    /**
     * Gets the database ID of the claim
     * @return The claim ID, or null if not yet saved to database
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Gets the UUID of the claim owner
     * @return The owner's UUID
     */
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    /**
     * Gets the world name where the claim is located
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Gets the X coordinate of the claimed chunk
     * @return The chunk X coordinate
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the Z coordinate of the claimed chunk
     * @return The chunk Z coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Gets the time when the claim was made
     * @return The claim time
     */
    public LocalDateTime getClaimTime() {
        return claimTime;
    }
    
    /**
     * Creates a unique key for this claim based on world and chunk coordinates
     * @return A string key in format "world:x:z"
     */
    public String getChunkKey() {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ClaimData claimData = (ClaimData) obj;
        return chunkX == claimData.chunkX &&
               chunkZ == claimData.chunkZ &&
               Objects.equals(worldName, claimData.worldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(worldName, chunkX, chunkZ);
    }
    
    @Override
    public String toString() {
        return "ClaimData{" +
                "id=" + id +
                ", ownerUuid=" + ownerUuid +
                ", worldName='" + worldName + '\'' +
                ", chunkX=" + chunkX +
                ", chunkZ=" + chunkZ +
                ", claimTime=" + claimTime +
                '}';
    }
}