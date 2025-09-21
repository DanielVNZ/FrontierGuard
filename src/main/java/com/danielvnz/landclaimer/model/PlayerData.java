package com.danielvnz.landclaimer.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents player data including their selected mode and state information.
 * Used for managing player state and tracking mode selection status.
 */
public class PlayerData {
    private final UUID uuid;
    private PlayerMode mode;
    private boolean hasSelectedMode;
    private final LocalDateTime firstJoin;
    private LocalDateTime lastModeChange;
    
    /**
     * Creates a new PlayerData instance for a player who hasn't selected a mode yet
     * @param uuid The player's UUID
     * @param firstJoin The time when the player first joined the server
     */
    public PlayerData(UUID uuid, LocalDateTime firstJoin) {
        this.uuid = Objects.requireNonNull(uuid, "Player UUID cannot be null");
        this.mode = null;
        this.hasSelectedMode = false;
        this.firstJoin = Objects.requireNonNull(firstJoin, "First join time cannot be null");
        this.lastModeChange = null;
    }
    
    /**
     * Creates a new PlayerData instance with a selected mode
     * @param uuid The player's UUID
     * @param mode The player's selected mode
     * @param firstJoin The time when the player first joined the server
     */
    public PlayerData(UUID uuid, PlayerMode mode, LocalDateTime firstJoin) {
        this.uuid = Objects.requireNonNull(uuid, "Player UUID cannot be null");
        this.mode = mode;
        this.hasSelectedMode = mode != null;
        this.firstJoin = Objects.requireNonNull(firstJoin, "First join time cannot be null");
        this.lastModeChange = null;
    }
    
    /**
     * Creates a new PlayerData instance for a new player (current time as first join)
     * @param uuid The player's UUID
     */
    public PlayerData(UUID uuid) {
        this(uuid, LocalDateTime.now());
    }
    
    /**
     * Creates a new PlayerData instance with all fields
     * @param uuid The player's UUID
     * @param mode The player's selected mode
     * @param firstJoin The time when the player first joined the server
     * @param lastModeChange The time when the player last changed their mode
     */
    public PlayerData(UUID uuid, PlayerMode mode, LocalDateTime firstJoin, LocalDateTime lastModeChange) {
        this.uuid = Objects.requireNonNull(uuid, "Player UUID cannot be null");
        this.mode = mode;
        this.hasSelectedMode = mode != null;
        this.firstJoin = Objects.requireNonNull(firstJoin, "First join time cannot be null");
        this.lastModeChange = lastModeChange;
    }
    
    /**
     * Gets the player's UUID
     * @return The player's UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * Gets the player's selected mode
     * @return The player's mode, or null if not selected
     */
    public PlayerMode getMode() {
        return mode;
    }
    
    /**
     * Sets the player's mode
     * @param mode The mode to set
     */
    public void setMode(PlayerMode mode) {
        this.mode = mode;
        this.hasSelectedMode = mode != null;
    }
    
    /**
     * Sets the player's mode and updates the last mode change timestamp
     * @param mode The mode to set
     */
    public void setModeWithTimestamp(PlayerMode mode) {
        this.mode = mode;
        this.hasSelectedMode = mode != null;
        this.lastModeChange = LocalDateTime.now();
    }
    
    /**
     * Checks if the player has selected a mode
     * @return true if the player has selected a mode, false otherwise
     */
    public boolean hasSelectedMode() {
        return hasSelectedMode;
    }
    
    /**
     * Gets the time when the player first joined the server
     * @return The first join time
     */
    public LocalDateTime getFirstJoin() {
        return firstJoin;
    }
    
    /**
     * Gets the time when the player last changed their mode
     * @return The last mode change time, or null if never changed
     */
    public LocalDateTime getLastModeChange() {
        return lastModeChange;
    }
    
    /**
     * Sets the time when the player last changed their mode
     * @param lastModeChange The last mode change time
     */
    public void setLastModeChange(LocalDateTime lastModeChange) {
        this.lastModeChange = lastModeChange;
    }
    
    /**
     * Checks if the player is in peaceful mode
     * @return true if the player is in peaceful mode, false otherwise
     */
    public boolean isPeaceful() {
        return mode == PlayerMode.PEACEFUL;
    }
    
    /**
     * Checks if the player is in normal mode
     * @return true if the player is in normal mode, false otherwise
     */
    public boolean isNormal() {
        return mode == PlayerMode.NORMAL;
    }
    
    /**
     * Checks if the player can claim land based on their mode
     * @return true if the player can claim land, false otherwise
     */
    public boolean canClaimLand() {
        return mode != null && mode.canClaimLand();
    }
    
    /**
     * Checks if the player has PVP protection based on their mode
     * @return true if the player has PVP protection, false otherwise
     */
    public boolean hasPvpProtection() {
        return mode != null && mode.hasPvpProtection();
    }
    
    /**
     * Checks if this is a new player (hasn't selected a mode yet)
     * @return true if the player is new and needs to select a mode, false otherwise
     */
    public boolean isNewPlayer() {
        return !hasSelectedMode;
    }
    
    /**
     * Checks if the player can change their mode (cooldown check)
     * @param cooldownHours The cooldown period in hours
     * @return true if the player can change mode, false if on cooldown
     */
    public boolean canChangeMode(int cooldownHours) {
        if (lastModeChange == null) {
            return true; // First time selecting a mode, no cooldown
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownEnd = lastModeChange.plusHours(cooldownHours);
        return now.isAfter(cooldownEnd) || now.isEqual(cooldownEnd);
    }
    
    /**
     * Checks if the player can change their mode (cooldown has expired) - uses default 24 hours
     * @return true if the player can change their mode, false if still on cooldown
     */
    public boolean canChangeMode() {
        return canChangeMode(24); // Default to 24 hours for backward compatibility
    }
    
    /**
     * Gets the time remaining until the player can change their mode again
     * @param cooldownHours The cooldown period in hours
     * @return The time remaining, or null if no cooldown
     */
    public java.time.Duration getTimeUntilModeChange(int cooldownHours) {
        if (lastModeChange == null) {
            return null; // No cooldown
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownEnd = lastModeChange.plusHours(cooldownHours);
        
        if (now.isAfter(cooldownEnd) || now.isEqual(cooldownEnd)) {
            return null; // Cooldown has expired
        }
        
        return java.time.Duration.between(now, cooldownEnd);
    }
    
    /**
     * Gets the time remaining until the player can change their mode again - uses default 24 hours
     * @return The time remaining, or null if no cooldown
     */
    public java.time.Duration getTimeUntilModeChange() {
        return getTimeUntilModeChange(24); // Default to 24 hours for backward compatibility
    }
    
    /**
     * Gets a formatted string showing the time remaining until mode change is allowed
     * @param cooldownHours The cooldown period in hours
     * @return A formatted time string, or null if no cooldown
     */
    public String getFormattedTimeUntilModeChange(int cooldownHours) {
        java.time.Duration timeRemaining = getTimeUntilModeChange(cooldownHours);
        if (timeRemaining == null) {
            return null;
        }
        
        long hours = timeRemaining.toHours();
        long minutes = timeRemaining.toMinutesPart();
        long seconds = timeRemaining.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
    
    /**
     * Gets a formatted string showing the time remaining until mode change is allowed - uses default 24 hours
     * @return A formatted time string, or null if no cooldown
     */
    public String getFormattedTimeUntilModeChange() {
        return getFormattedTimeUntilModeChange(24); // Default to 24 hours for backward compatibility
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PlayerData that = (PlayerData) obj;
        return Objects.equals(uuid, that.uuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", mode=" + mode +
                ", hasSelectedMode=" + hasSelectedMode +
                ", firstJoin=" + firstJoin +
                ", lastModeChange=" + lastModeChange +
                '}';
    }
}