package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.model.ClaimData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.Duration;

/**
 * Manages visual feedback for claim operations.
 * Provides particle effects, sounds, and visual indicators for claiming/unclaiming.
 */
public class VisualFeedbackManager {
    
    private final LandClaimerPlugin plugin;
    
    public VisualFeedbackManager(LandClaimerPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Shows visual feedback for a successful claim
     * @param player The player who claimed the chunk
     * @param chunk The chunk that was claimed
     */
    public void showClaimSuccess(Player player, Chunk chunk) {
        if (player == null || chunk == null) {
            return;
        }
        
        // Play success sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        // Show particle effect at chunk center
        showClaimParticles(chunk, Particle.HAPPY_VILLAGER, NamedTextColor.GREEN);
        
        // Send title message using Adventure API
        Component title = Component.text("✓ CLAIMED", NamedTextColor.GREEN);
        Component subtitle = Component.text("Chunk protected!", NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(2000), // stay
            Duration.ofMillis(500)   // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
    }
    
    /**
     * Shows visual feedback for a successful unclaim
     * @param player The player who unclaimed the chunk
     * @param chunk The chunk that was unclaimed
     */
    public void showUnclaimSuccess(Player player, Chunk chunk) {
        if (player == null || chunk == null) {
            return;
        }
        
        // Play unclaim sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f);
        
        // Show particle effect at chunk center
        showClaimParticles(chunk, Particle.SMOKE, NamedTextColor.YELLOW);
        
        // Send title message
        Component title = Component.text("✗ UNCLAIMED", NamedTextColor.RED);
        Component subtitle = Component.text("Chunk no longer protected", NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(2000), // stay
            Duration.ofMillis(500)   // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
    }
    
    /**
     * Shows visual feedback for a failed claim attempt
     * @param player The player who attempted to claim
     * @param reason The reason for failure
     */
    public void showClaimFailure(Player player, String reason) {
        if (player == null) {
            return;
        }
        
        // Play failure sound
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        
        // Send title message
        Component title = Component.text("✗ CLAIM FAILED", NamedTextColor.RED);
        Component subtitle = Component.text(reason, NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(2000), // stay
            Duration.ofMillis(500)   // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
    }
    
    /**
     * Shows visual feedback for PvP denial
     * @param player The player who attempted to attack
     * @param reason The reason for PvP denial
     */
    public void showPvpDenied(Player player, String reason) {
        if (player == null) {
            return;
        }
        
        // Play failure sound
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        
        // Send title message
        Component title = Component.text("✗ PVP DENIED", NamedTextColor.RED);
        Component subtitle = Component.text(reason, NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(2000), // stay
            Duration.ofMillis(500)   // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
    }
    
    /**
     * Shows particle effects at the center of a chunk
     * @param chunk The chunk to show particles in
     * @param particle The particle type to show
     * @param color The color for colored particles
     */
    private void showClaimParticles(Chunk chunk, Particle particle, NamedTextColor color) {
        World world = chunk.getWorld();
        int centerX = chunk.getX() * 16 + 8;
        int centerZ = chunk.getZ() * 16 + 8;
        
        // Get a good Y coordinate (above ground level)
        int y = world.getHighestBlockYAt(centerX, centerZ) + 2;
        
        // Show particles in a circle pattern
        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 20;
            final double radius = 3.0;
            
            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }
                
                double angle = (2 * Math.PI * step) / maxSteps;
                double x = centerX + radius * Math.cos(angle);
                double z = centerZ + radius * Math.sin(angle);
                
                Location particleLocation = new Location(world, x, y, z);
                
                // Show different particles based on type
                if (particle == Particle.HAPPY_VILLAGER) {
                    world.spawnParticle(Particle.HAPPY_VILLAGER, particleLocation, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.END_ROD, particleLocation, 1, 0.1, 0.1, 0.1, 0.1);
                } else if (particle == Particle.SMOKE) {
                    world.spawnParticle(Particle.SMOKE, particleLocation, 2, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.CLOUD, particleLocation, 1, 0.1, 0.1, 0.1, 0.05);
                }
                
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Shows a boundary effect around a claimed chunk
     * @param chunk The chunk to show boundaries for
     * @param player The player to show the effect to
     */
    public void showChunkBoundary(Chunk chunk, Player player) {
        if (chunk == null || player == null) {
            return;
        }
        
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Calculate chunk boundaries
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;
        
        // Get a good Y coordinate
        int y = world.getHighestBlockYAt(minX + 8, minZ + 8) + 1;
        
        // Show particles along the chunk boundaries
        new BukkitRunnable() {
            int step = 0;
            final int totalSteps = 64; // 16 blocks per side * 4 sides
            
            @Override
            public void run() {
                if (step >= totalSteps) {
                    cancel();
                    return;
                }
                
                Location particleLocation;
                
                if (step < 16) {
                    // Top edge
                    particleLocation = new Location(world, minX + step, y, minZ);
                } else if (step < 32) {
                    // Right edge
                    particleLocation = new Location(world, maxX, y, minZ + (step - 16));
                } else if (step < 48) {
                    // Bottom edge
                    particleLocation = new Location(world, maxX - (step - 32), y, maxZ);
                } else {
                    // Left edge
                    particleLocation = new Location(world, minX, y, maxZ - (step - 48));
                }
                
                // Only show particles if player is within render distance
                if (player.getLocation().distance(particleLocation) <= 64) {
                    world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, 
                        new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f));
                }
                
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Shows claim boundaries for a specified duration and then clears them
     * @param chunk The chunk to show boundaries for
     * @param player The player to show the effect to
     * @param durationSeconds How long to show the boundaries (in seconds)
     */
    public void showChunkBoundaryTimed(Chunk chunk, Player player, int durationSeconds) {
        if (chunk == null || player == null) {
            return;
        }
        
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Calculate chunk boundaries
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;
        
        // Get claim data for this chunk and neighboring chunks
        ClaimData currentClaim = plugin.getClaimManager().getClaimInfo(chunk);
        if (currentClaim == null) {
            return; // No claim data, don't show particles
        }
        
        // Determine particle color based on player access
        boolean hasAccess = false;
        boolean isPeaceful = plugin.getPlayerModeManager().isPeacefulPlayer(player);
        
        if (isPeaceful) {
            // For peaceful players, check if they have access to this claim
            if (currentClaim.getOwnerUuid().equals(player.getUniqueId())) {
                hasAccess = true; // Player owns the claim
            } else {
                // Check if player is invited to this claim
                try {
                    hasAccess = plugin.getInvitationManager().canPlayerBuild(player, currentClaim).get();
                } catch (Exception e) {
                    hasAccess = false; // Default to no access on error
                }
            }
        } else {
            // Normal players have no access to any claims
            hasAccess = false;
        }
        
        // Choose particle color: green for access, red for no access
        Color particleColor = hasAccess ? Color.fromRGB(0, 255, 0) : Color.fromRGB(255, 0, 0);
        
        // Check neighboring chunks to avoid showing particles on shared edges
        ClaimData northClaim = plugin.getClaimManager().getClaimInfo(world.getChunkAt(chunkX, chunkZ - 1));
        ClaimData southClaim = plugin.getClaimManager().getClaimInfo(world.getChunkAt(chunkX, chunkZ + 1));
        ClaimData eastClaim = plugin.getClaimManager().getClaimInfo(world.getChunkAt(chunkX + 1, chunkZ));
        ClaimData westClaim = plugin.getClaimManager().getClaimInfo(world.getChunkAt(chunkX - 1, chunkZ));
        
        // Show particles along the chunk boundaries for the specified duration
        new BukkitRunnable() {
            final int maxTicks = durationSeconds * 20; // Convert seconds to ticks
            int currentTick = 0;
            
            @Override
            public void run() {
                if (currentTick >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Top edge (North) - only show if neighboring chunk is different owner or unclaimed
                if (northClaim == null || !northClaim.getOwnerUuid().equals(currentClaim.getOwnerUuid())) {
                    for (int x = minX; x <= maxX; x++) {
                        // Get terrain height for this specific location
                        int y = world.getHighestBlockYAt(x, minZ) + 1;
                        Location particleLocation = new Location(world, x, y, minZ);
                        if (player.getLocation().distance(particleLocation) <= 64) {
                            // Show multiple particles for thickness - only to the specific player
                            player.spawnParticle(Particle.DUST, particleLocation, 3, 0.2, 0.2, 0.2, 0, 
                                new Particle.DustOptions(particleColor, 2.0f));
                            player.spawnParticle(Particle.END_ROD, particleLocation, 1, 0, 0, 0, 0.1);
                        }
                    }
                }
                
                // Right edge (East) - only show if neighboring chunk is different owner or unclaimed
                if (eastClaim == null || !eastClaim.getOwnerUuid().equals(currentClaim.getOwnerUuid())) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // Get terrain height for this specific location
                        int y = world.getHighestBlockYAt(maxX, z) + 1;
                        Location particleLocation = new Location(world, maxX, y, z);
                        if (player.getLocation().distance(particleLocation) <= 64) {
                            player.spawnParticle(Particle.DUST, particleLocation, 3, 0.2, 0.2, 0.2, 0, 
                                new Particle.DustOptions(particleColor, 2.0f));
                            player.spawnParticle(Particle.END_ROD, particleLocation, 1, 0, 0, 0, 0.1);
                        }
                    }
                }
                
                // Bottom edge (South) - only show if neighboring chunk is different owner or unclaimed
                if (southClaim == null || !southClaim.getOwnerUuid().equals(currentClaim.getOwnerUuid())) {
                    for (int x = maxX; x >= minX; x--) {
                        // Get terrain height for this specific location
                        int y = world.getHighestBlockYAt(x, maxZ) + 1;
                        Location particleLocation = new Location(world, x, y, maxZ);
                        if (player.getLocation().distance(particleLocation) <= 64) {
                            player.spawnParticle(Particle.DUST, particleLocation, 3, 0.2, 0.2, 0.2, 0, 
                                new Particle.DustOptions(particleColor, 2.0f));
                            player.spawnParticle(Particle.END_ROD, particleLocation, 1, 0, 0, 0, 0.1);
                        }
                    }
                }
                
                // Left edge (West) - only show if neighboring chunk is different owner or unclaimed
                if (westClaim == null || !westClaim.getOwnerUuid().equals(currentClaim.getOwnerUuid())) {
                    for (int z = maxZ; z >= minZ; z--) {
                        // Get terrain height for this specific location
                        int y = world.getHighestBlockYAt(minX, z) + 1;
                        Location particleLocation = new Location(world, minX, y, z);
                        if (player.getLocation().distance(particleLocation) <= 64) {
                            player.spawnParticle(Particle.DUST, particleLocation, 3, 0.2, 0.2, 0.2, 0, 
                                new Particle.DustOptions(particleColor, 2.0f));
                            player.spawnParticle(Particle.END_ROD, particleLocation, 1, 0, 0, 0, 0.1);
                        }
                    }
                }
                
                currentTick++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks (10 times per second) for better performance
    }
    
    /**
     * Shows a warning effect for entering a PVP area
     * @param player The player entering the PVP area
     */
    public void showPvpAreaWarning(Player player) {
        if (player == null) {
            return;
        }
        
        // Play warning sound
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        
        // Show warning particles around player
        Location playerLoc = player.getLocation();
        for (int i = 0; i < 10; i++) {
            double angle = (2 * Math.PI * i) / 10;
            double x = playerLoc.getX() + 2 * Math.cos(angle);
            double z = playerLoc.getZ() + 2 * Math.sin(angle);
            Location particleLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY() + 1, z);
            
            playerLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // Send warning title
        Component title = Component.text("[WARNING] PVP AREA", NamedTextColor.YELLOW);
        Component subtitle = Component.text("You can be attacked here!", NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(1000), // fade in
            Duration.ofMillis(3000), // stay
            Duration.ofMillis(1000)  // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
    }
    
    /**
     * Shows a safe effect for leaving a PVP area
     * @param player The player leaving the PVP area
     */
    public void showPvpAreaExit(Player player) {
        if (player == null) {
            return;
        }
        
        // Play safe sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        // Show safe particles around player
        Location playerLoc = player.getLocation();
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i) / 8;
            double x = playerLoc.getX() + 1.5 * Math.cos(angle);
            double z = playerLoc.getZ() + 1.5 * Math.sin(angle);
            Location particleLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY() + 1, z);
            
            playerLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // Send safe title
        Component title = Component.text("✓ SAFE ZONE", NamedTextColor.GREEN);
        Component subtitle = Component.text("Protection restored", NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(2000), // stay
            Duration.ofMillis(500)   // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
    }
    
    /**
     * Shows a PVP area warning for peaceful players
     * @param player The player to show the warning to
     * @param areaName The name of the PVP area
     */
    public void showPvpAreaWarning(Player player, String areaName) {
        if (player == null) return;
        
        // Show title message
        Component title = Component.text("[WARNING] PVP AREA", NamedTextColor.YELLOW);
        Component subtitle = Component.text("Entering " + areaName, NamedTextColor.WHITE);
        Times times = Times.times(
            Duration.ofMillis(500),  // fade in
            Duration.ofMillis(3000), // stay
            Duration.ofMillis(500)   // fade out
        );
        Title fullTitle = Title.title(title, subtitle, times);
        player.showTitle(fullTitle);
        
        // Play warning sound
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.8f);
        
        // Show warning particle effect
        player.spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }
}
