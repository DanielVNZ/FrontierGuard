package com.danielvnz.landclaimer.model;

import java.util.Objects;

/**
 * Represents a designated PVP area where all players can engage in combat
 * regardless of their player mode. Contains region boundary data.
 */
public class PvpArea {
    private final String name;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    
    /**
     * Creates a new PvpArea instance
     * @param name The unique name of the PVP area
     * @param worldName The name of the world where the area is located
     * @param minX The minimum X coordinate of the area
     * @param minY The minimum Y coordinate of the area
     * @param minZ The minimum Z coordinate of the area
     * @param maxX The maximum X coordinate of the area
     * @param maxY The maximum Y coordinate of the area
     * @param maxZ The maximum Z coordinate of the area
     */
    public PvpArea(String name, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.name = Objects.requireNonNull(name, "PVP area name cannot be null");
        this.worldName = Objects.requireNonNull(worldName, "World name cannot be null");
        
        // Ensure min coordinates are actually smaller than max coordinates
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxZ = Math.max(minZ, maxZ);
    }
    
    /**
     * Gets the name of the PVP area
     * @return The area name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the world name where the area is located
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Gets the minimum X coordinate
     * @return The minimum X coordinate
     */
    public int getMinX() {
        return minX;
    }
    
    /**
     * Gets the minimum Y coordinate
     * @return The minimum Y coordinate
     */
    public int getMinY() {
        return minY;
    }
    
    /**
     * Gets the minimum Z coordinate
     * @return The minimum Z coordinate
     */
    public int getMinZ() {
        return minZ;
    }
    
    /**
     * Gets the maximum X coordinate
     * @return The maximum X coordinate
     */
    public int getMaxX() {
        return maxX;
    }
    
    /**
     * Gets the maximum Y coordinate
     * @return The maximum Y coordinate
     */
    public int getMaxY() {
        return maxY;
    }
    
    /**
     * Gets the maximum Z coordinate
     * @return The maximum Z coordinate
     */
    public int getMaxZ() {
        return maxZ;
    }
    
    /**
     * Checks if the given coordinates are within this PVP area
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @param z The Z coordinate to check
     * @return true if the coordinates are within the area, false otherwise
     */
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * Checks if the given coordinates are within this PVP area (ignoring Y coordinate)
     * @param x The X coordinate to check
     * @param z The Z coordinate to check
     * @return true if the coordinates are within the area horizontally, false otherwise
     */
    public boolean containsHorizontal(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
    
    /**
     * Gets the volume of this PVP area in blocks
     * @return The volume in blocks
     */
    public long getVolume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
    
    /**
     * Gets the area of this PVP area in blocks (horizontal area)
     * @return The horizontal area in blocks
     */
    public long getArea() {
        return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PvpArea pvpArea = (PvpArea) obj;
        return Objects.equals(name, pvpArea.name) &&
               Objects.equals(worldName, pvpArea.worldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, worldName);
    }
    
    @Override
    public String toString() {
        return "PvpArea{" +
                "name='" + name + '\'' +
                ", worldName='" + worldName + '\'' +
                ", minX=" + minX +
                ", minY=" + minY +
                ", minZ=" + minZ +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", maxZ=" + maxZ +
                '}';
    }
}