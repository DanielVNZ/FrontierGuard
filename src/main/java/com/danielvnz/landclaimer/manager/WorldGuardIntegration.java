package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.logging.Level;

/**
 * WorldGuard integration for FrontierGuard. Supports WorldGuard 7+ only.
 * Uses reflection to avoid ClassNotFoundException when WorldEdit/WorldGuard is not installed.
 */
public class WorldGuardIntegration {

    private final LandClaimerPlugin plugin;
    private final boolean worldGuardAvailable;

    public WorldGuardIntegration(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.worldGuardAvailable = isWorldGuardPresent();
        
        if (!worldGuardAvailable) {
            plugin.getLogger().info("WorldGuard not found. Claims will not be region-checked.");
        } else {
            plugin.getLogger().info("WorldGuard 7+ detected. Claims will respect protected regions.");
        }
    }

    private boolean isWorldGuardPresent() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class.forName("com.sk89q.worldedit.math.BlockVector3");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isWorldGuardAvailable() {
        return worldGuardAvailable;
    }

    /**
     * Checks if any WorldGuard region intersects the given chunk.
     *
     * @param chunk The chunk to check.
     * @return true if the chunk is inside at least one WorldGuard region.
     */
    public boolean isChunkProtected(Chunk chunk) {
        if (!worldGuardAvailable || chunk == null) {
            plugin.getLogger().fine("WorldGuard not available or chunk is null - returning false");
            return false;
        }

        try {
            // Use WorldGuard's proper API to check if any regions intersect the chunk box
            World world = chunk.getWorld();
            int startX = chunk.getX() << 4;
            int startZ = chunk.getZ() << 4;
            int endX = startX + 15;
            int endZ = startZ + 15;
            
            // Get WorldGuard instance and container
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardInstance.getClass().getMethod("getPlatform").invoke(worldGuardInstance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = container.getClass().getMethod("createQuery").invoke(container);
            
            // Check multiple points within the chunk to see if any are in a region
            // Use a more comprehensive sampling approach
            int[][] samplePoints = {
                {startX, startZ},           // Corner 1
                {endX, startZ},             // Corner 2  
                {startX, endZ},             // Corner 3
                {endX, endZ},               // Corner 4
                {(startX + endX) / 2, (startZ + endZ) / 2}, // Center
                {startX + 4, startZ + 4},   // Quarter points
                {endX - 4, startZ + 4},
                {startX + 4, endZ - 4},
                {endX - 4, endZ - 4}
            };
            
            // Check multiple Y levels, including the region's Y range (84-96)
            int[] yLevels = {0, 32, 64, 80, 90, 100, 128, 160, 192, 224, 255};
            
            for (int[] pt : samplePoints) {
                for (int y : yLevels) {
                    // Create Bukkit location
                    org.bukkit.Location bukkitLoc = new org.bukkit.Location(world, pt[0], y, pt[1]);
                    
                    // Use reflection to convert Bukkit location to WorldEdit location
                    Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                    Object worldEditLocation = bukkitAdapterClass.getMethod("adapt", org.bukkit.Location.class).invoke(null, bukkitLoc);
                    
                    // Query for regions at this location
                    Object set = query.getClass().getMethod("getApplicableRegions", worldEditLocation.getClass()).invoke(query, worldEditLocation);
                    Object regionsObj = set.getClass().getMethod("getRegions").invoke(set);
            
                    // Cast to standard Java Set to avoid reflection issues with Guava's RegularImmutableSet
                    @SuppressWarnings("unchecked")
                    java.util.Set<Object> regions = (java.util.Set<Object>) regionsObj;
                    
                    boolean isEmpty = regions.isEmpty();
                    
                    if (!isEmpty) {
                        // List all regions found
                        for (Object region : regions) {
                            String regionId = (String) region.getClass().getMethod("getId").invoke(region);
                            plugin.getLogger().info("Found WorldGuard region '" + regionId + "' protecting chunk (" + chunk.getX() + "," + chunk.getZ() + ")");
                        }
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking WorldGuard protection for chunk " + chunk, e);
            return false;
        }
    }

    /**
     * Gets the name of the first WorldGuard region that intersects the given chunk.
     *
     * @param chunk The chunk to check.
     * @return The name of the first region found, or null if no regions intersect.
     */
    public String getProtectedRegionName(Chunk chunk) {
        if (!worldGuardAvailable || chunk == null) {
            return null;
        }

        try {
            World world = chunk.getWorld();
            
            // Check center of chunk at a Y level that should be in the region (90)
            int startX = chunk.getX() << 4;
            int startZ = chunk.getZ() << 4;
            int centerX = startX + 8;
            int centerZ = startZ + 8;
            int y = 90; // Use a Y level that should be in the region (84-96)

            // Create Bukkit location
            org.bukkit.Location bukkitLoc = new org.bukkit.Location(world, centerX, y, centerZ);
            
            // Use reflection to convert Bukkit location to WorldEdit location
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object worldEditLocation = bukkitAdapterClass.getMethod("adapt", org.bukkit.Location.class).invoke(null, bukkitLoc);
            
            // Get WorldGuard instance and query
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardInstance.getClass().getMethod("getPlatform").invoke(worldGuardInstance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = container.getClass().getMethod("createQuery").invoke(container);
            
            // Query for regions at this location
            Object set = query.getClass().getMethod("getApplicableRegions", worldEditLocation.getClass()).invoke(query, worldEditLocation);
            Object regionsObj = set.getClass().getMethod("getRegions").invoke(set);
            
            // Cast to standard Java Set to avoid reflection issues with Guava's RegularImmutableSet
            @SuppressWarnings("unchecked")
            java.util.Set<Object> regions = (java.util.Set<Object>) regionsObj;
            
            if (!regions.isEmpty()) {
                Object firstRegion = regions.iterator().next();
                return (String) firstRegion.getClass().getMethod("getId").invoke(firstRegion);
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting WorldGuard region name for chunk " + chunk, e);
            return null;
        }
    }
}