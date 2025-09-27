package com.danielvnz.landclaimer.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a player's noob status in the database.
 * Tracks when a player was manually marked as noob and when it expires.
 */
public class NoobStatus {
    
    private final UUID playerUuid;
    private final LocalDateTime expirationTime;
    private final LocalDateTime createdAt;
    
    /**
     * Creates a new NoobStatus instance
     * @param playerUuid The UUID of the player
     * @param expirationTime When the noob status expires
     * @param createdAt When the noob status was created
     */
    public NoobStatus(UUID playerUuid, LocalDateTime expirationTime, LocalDateTime createdAt) {
        this.playerUuid = playerUuid;
        this.expirationTime = expirationTime;
        this.createdAt = createdAt;
    }
    
    /**
     * Creates a new NoobStatus instance with current time as created time
     * @param playerUuid The UUID of the player
     * @param expirationTime When the noob status expires
     */
    public NoobStatus(UUID playerUuid, LocalDateTime expirationTime) {
        this(playerUuid, expirationTime, LocalDateTime.now());
    }
    
    /**
     * Creates a new NoobStatus that expires 30 minutes from now
     * @param playerUuid The UUID of the player
     */
    public static NoobStatus createForThirtyMinutes(UUID playerUuid) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusMinutes(30);
        return new NoobStatus(playerUuid, expiration, now);
    }
    
    /**
     * Gets the player's UUID
     * @return The player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Gets the expiration time
     * @return The expiration time
     */
    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }
    
    /**
     * Gets the creation time
     * @return The creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Checks if this noob status has expired
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }
    
    /**
     * Gets the remaining time in minutes
     * @return Minutes remaining, or 0 if expired
     */
    public long getRemainingMinutes() {
        if (isExpired()) {
            return 0;
        }
        
        return java.time.Duration.between(LocalDateTime.now(), expirationTime).toMinutes();
    }
    
    /**
     * Gets the remaining time in a human-readable format
     * @return Remaining time as a string
     */
    public String getRemainingTimeFormatted() {
        long minutes = getRemainingMinutes();
        if (minutes <= 0) {
            return "Expired";
        }
        
        if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        String result = hours + " hour" + (hours == 1 ? "" : "s");
        if (remainingMinutes > 0) {
            result += " " + remainingMinutes + " minute" + (remainingMinutes == 1 ? "" : "s");
        }
        
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NoobStatus that = (NoobStatus) obj;
        return playerUuid.equals(that.playerUuid);
    }
    
    @Override
    public int hashCode() {
        return playerUuid.hashCode();
    }
    
    @Override
    public String toString() {
        return "NoobStatus{" +
                "playerUuid=" + playerUuid +
                ", expirationTime=" + expirationTime +
                ", createdAt=" + createdAt +
                ", expired=" + isExpired() +
                '}';
    }
}
