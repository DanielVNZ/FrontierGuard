package com.danielvnz.landclaimer.model;

/**
 * Enum representing the different gameplay modes available to players.
 * Players can choose between peaceful (PVE with land claiming) and normal (PVPVE) modes.
 */
public enum PlayerMode {
    /**
     * Peaceful mode - Players can claim land and are protected from PVP except in designated areas
     */
    PEACEFUL("peaceful"),
    
    /**
     * Normal mode - Standard survival multiplayer with full PVP and no land claiming
     */
    NORMAL("normal");
    
    private final String displayName;
    
    PlayerMode(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the display name of the player mode
     * @return The display name as a string
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets a PlayerMode from its display name
     * @param displayName The display name to look up
     * @return The corresponding PlayerMode, or null if not found
     */
    public static PlayerMode fromDisplayName(String displayName) {
        for (PlayerMode mode : values()) {
            if (mode.displayName.equalsIgnoreCase(displayName)) {
                return mode;
            }
        }
        return null;
    }
    
    /**
     * Checks if this mode allows land claiming
     * @return true if this mode can claim land, false otherwise
     */
    public boolean canClaimLand() {
        return this == PEACEFUL;
    }
    
    /**
     * Checks if this mode has PVP protection outside of PVP areas
     * @return true if this mode is protected from PVP, false otherwise
     */
    public boolean hasPvpProtection() {
        return this == PEACEFUL;
    }
}