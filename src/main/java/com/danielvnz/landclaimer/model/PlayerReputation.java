package com.danielvnz.landclaimer.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a player's reputation data including current reputation value,
 * playtime tracking, and last update timestamps.
 * Reputation ranges from -15 to +15 and regenerates over time.
 */
public class PlayerReputation {
    
    public static final int MIN_REPUTATION = -15;
    public static final int MAX_REPUTATION = 15;
    public static final int DEFAULT_REPUTATION = 0;
    
    private final java.util.UUID uuid;
    private int reputation;
    private LocalDateTime lastPlaytimeUpdate;
    private double totalPlaytimeHours;
    
    /**
     * Creates a new PlayerReputation instance with default values
     * @param uuid The player's UUID
     */
    public PlayerReputation(java.util.UUID uuid) {
        this(uuid, DEFAULT_REPUTATION, LocalDateTime.now(), 0.0);
    }
    
    /**
     * Creates a new PlayerReputation instance with specified values
     * @param uuid The player's UUID
     * @param reputation The reputation value
     * @param lastPlaytimeUpdate The last playtime update timestamp
     * @param totalPlaytimeHours The total playtime in hours
     */
    public PlayerReputation(java.util.UUID uuid, int reputation, LocalDateTime lastPlaytimeUpdate, double totalPlaytimeHours) {
        this.uuid = Objects.requireNonNull(uuid, "Player UUID cannot be null");
        this.reputation = clampReputation(reputation);
        this.lastPlaytimeUpdate = Objects.requireNonNull(lastPlaytimeUpdate, "Last playtime update cannot be null");
        this.totalPlaytimeHours = Math.max(0.0, totalPlaytimeHours);
    }
    
    /**
     * Gets the player's UUID
     * @return The player's UUID
     */
    public java.util.UUID getUuid() {
        return uuid;
    }
    
    /**
     * Gets the current reputation value
     * @return The reputation value (between MIN_REPUTATION and MAX_REPUTATION)
     */
    public int getReputation() {
        return reputation;
    }
    
    /**
     * Sets the reputation value, clamping it to valid bounds
     * @param reputation The reputation value to set
     */
    public void setReputation(int reputation) {
        this.reputation = clampReputation(reputation);
    }
    
    /**
     * Adds to the reputation value, clamping the result to valid bounds
     * @param amount The amount to add (can be negative)
     * @return The actual amount added (may be less if hitting bounds)
     */
    public int addReputation(int amount) {
        int oldReputation = this.reputation;
        this.reputation = clampReputation(this.reputation + amount);
        return this.reputation - oldReputation;
    }
    
    /**
     * Gets the last playtime update timestamp
     * @return The last playtime update timestamp
     */
    public LocalDateTime getLastPlaytimeUpdate() {
        return lastPlaytimeUpdate;
    }
    
    /**
     * Sets the last playtime update timestamp
     * @param lastPlaytimeUpdate The timestamp to set
     */
    public void setLastPlaytimeUpdate(LocalDateTime lastPlaytimeUpdate) {
        this.lastPlaytimeUpdate = Objects.requireNonNull(lastPlaytimeUpdate, "Last playtime update cannot be null");
    }
    
    /**
     * Gets the total playtime in hours
     * @return The total playtime in hours
     */
    public double getTotalPlaytimeHours() {
        return totalPlaytimeHours;
    }
    
    /**
     * Sets the total playtime in hours
     * @param totalPlaytimeHours The total playtime in hours
     */
    public void setTotalPlaytimeHours(double totalPlaytimeHours) {
        this.totalPlaytimeHours = Math.max(0.0, totalPlaytimeHours);
    }
    
    /**
     * Adds playtime and potentially increases reputation
     * @param additionalHours The additional playtime in hours
     * @return The amount of reputation gained from this playtime
     */
    public int addPlaytime(double additionalHours) {
        if (additionalHours <= 0) {
            return 0;
        }
        
        this.totalPlaytimeHours += additionalHours;
        
        // Calculate reputation gain (1 reputation per hour)
        int reputationGain = (int) Math.floor(additionalHours);
        if (reputationGain > 0) {
            return addReputation(reputationGain);
        }
        
        return 0;
    }
    
    /**
     * Checks if the reputation is at the minimum value
     * @return true if reputation is at minimum, false otherwise
     */
    public boolean isAtMinReputation() {
        return reputation == MIN_REPUTATION;
    }
    
    /**
     * Checks if the reputation is at the maximum value
     * @return true if reputation is at maximum, false otherwise
     */
    public boolean isAtMaxReputation() {
        return reputation == MAX_REPUTATION;
    }
    
    /**
     * Gets a string representation of the reputation status
     * @return A descriptive string of the reputation level
     */
    public String getReputationStatus() {
        if (reputation >= 10) {
            return "Excellent";
        } else if (reputation >= 5) {
            return "Good";
        } else if (reputation >= 0) {
            return "Neutral";
        } else if (reputation >= -5) {
            return "Poor";
        } else if (reputation >= -10) {
            return "Bad";
        } else {
            return "Terrible";
        }
    }
    
    /**
     * Gets the color associated with the reputation level
     * @return A color code for display purposes
     */
    public String getReputationColor() {
        if (reputation >= 10) {
            return "§a"; // Green
        } else if (reputation >= 5) {
            return "§2"; // Dark Green
        } else if (reputation >= 0) {
            return "§e"; // Yellow
        } else if (reputation >= -5) {
            return "§6"; // Gold
        } else if (reputation >= -10) {
            return "§c"; // Red
        } else {
            return "§4"; // Dark Red
        }
    }
    
    /**
     * Clamps a reputation value to the valid bounds
     * @param reputation The reputation value to clamp
     * @return The clamped reputation value
     */
    private static int clampReputation(int reputation) {
        return Math.max(MIN_REPUTATION, Math.min(MAX_REPUTATION, reputation));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlayerReputation that = (PlayerReputation) obj;
        return Objects.equals(uuid, that.uuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
    
    @Override
    public String toString() {
        return String.format("PlayerReputation{uuid=%s, reputation=%d, totalPlaytime=%.2fh, status=%s}", 
                           uuid, reputation, totalPlaytimeHours, getReputationStatus());
    }
}
