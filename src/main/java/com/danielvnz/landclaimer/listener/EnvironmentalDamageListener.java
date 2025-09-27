package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.PlayerModeManager;
import com.danielvnz.landclaimer.manager.PvpAreaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for environmental damage protection.
 * Protects peaceful players from fire, lava, and other environmental damage
 * caused by other players outside of PVP areas.
 */
public class EnvironmentalDamageListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final PlayerModeManager playerModeManager;
    private final PvpAreaManager pvpAreaManager;
    
    // Track recently player-placed fire/lava blocks (location -> player UUID)
    private final ConcurrentHashMap<Location, java.util.UUID> playerPlacedFire = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Location, java.util.UUID> playerPlacedLava = new ConcurrentHashMap<>();
    
    // Damage causes that should be protected for peaceful players
    private static final Set<EntityDamageEvent.DamageCause> PROTECTED_DAMAGE_CAUSES = Set.of(
        EntityDamageEvent.DamageCause.FIRE,
        EntityDamageEvent.DamageCause.FIRE_TICK,
        EntityDamageEvent.DamageCause.LAVA,
        EntityDamageEvent.DamageCause.HOT_FLOOR,
        EntityDamageEvent.DamageCause.CONTACT,
        EntityDamageEvent.DamageCause.CRAMMING,
        EntityDamageEvent.DamageCause.DROWNING,
        EntityDamageEvent.DamageCause.FALLING_BLOCK,
        EntityDamageEvent.DamageCause.MAGIC,
        EntityDamageEvent.DamageCause.WITHER
    );
    
    public EnvironmentalDamageListener(LandClaimerPlugin plugin, PlayerModeManager playerModeManager, PvpAreaManager pvpAreaManager) {
        this.plugin = plugin;
        this.playerModeManager = playerModeManager;
        this.pvpAreaManager = pvpAreaManager;
        
        // Start cleanup task for old tracking data
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupOldTracking, 20L, 20L * 60L); // Every minute
    }
    
    /**
     * Handles block place events to track player-placed fire and lava
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        boolean isPeaceful = playerModeManager.isPeacefulPlayer(player);

        // TNT rules - peaceful players can place TNT but not near other players
        if (block.getType() == Material.TNT && isPeaceful) {
            // Check for nearby players
            boolean hasNearbyPlayers = player.getWorld().getNearbyEntities(location, 5, 5, 5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .anyMatch(p -> !p.equals(player));

            if (hasNearbyPlayers) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot place TNT within 5 blocks of another player!", NamedTextColor.RED));
                return;
            }
        }

        // Fire/Lava/TNT placement rules
        if (block.getType() == Material.FIRE || block.getType() == Material.LAVA || block.getType() == Material.TNT) {
            // Check for nearby players
            boolean hasNearbyPlayers = player.getWorld().getNearbyEntities(location, 5, 5, 5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .anyMatch(p -> isPeaceful ? !p.equals(player) : playerModeManager.isPeacefulPlayer(p));

            if (hasNearbyPlayers) {
                event.setCancelled(true);
                if (isPeaceful) {
                    player.sendMessage(Component.text("You cannot place fire, lava, or TNT within 5 blocks of another player!", NamedTextColor.RED));
                } else {
                    if (block.getType() == Material.FIRE) {
                        player.sendMessage(Component.text("You cannot place fire within 5 blocks of a peaceful player!", NamedTextColor.RED));
                    } else if (block.getType() == Material.LAVA) {
                        player.sendMessage(Component.text("You cannot place lava within 5 blocks of a peaceful player!", NamedTextColor.RED));
                    } else if (block.getType() == Material.TNT) {
                        player.sendMessage(Component.text("You cannot place TNT within 5 blocks of a peaceful player!", NamedTextColor.RED));
                    }
                }
                return;
            }

            // Track fire/lava placement
            if (block.getType() == Material.FIRE) {
                playerPlacedFire.put(location, player.getUniqueId());
                plugin.getLogger().info("Tracked player-placed fire at " + location + " by " + player.getName() +
                        " (mode: " + (isPeaceful ? "peaceful" : "normal") + ")");
            } else if (block.getType() == Material.LAVA) {
                playerPlacedLava.put(location, player.getUniqueId());
                plugin.getLogger().info("Tracked player-placed lava at " + location + " by " + player.getName() +
                        " (mode: " + (isPeaceful ? "peaceful" : "normal") + ")");
            }
            // TNT doesn't need tracking since it's handled by explosion protection
        }
    }
    
    /**
     * Handles lava bucket placement events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        
        // Only handle lava bucket placement
        if (event.getBucket() != Material.LAVA_BUCKET) {
            return;
        }
        
        boolean isPeaceful = playerModeManager.isPeacefulPlayer(player);
        
        // Get the location where the lava would be placed
        Location targetLocation = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        
        // Check for nearby players
        boolean hasNearbyPlayers = player.getWorld().getNearbyEntities(targetLocation, 5, 5, 5).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .anyMatch(p -> isPeaceful ? !p.equals(player) : playerModeManager.isPeacefulPlayer(p));
        
        if (hasNearbyPlayers) {
            event.setCancelled(true);
            if (isPeaceful) {
                player.sendMessage(Component.text("You cannot place lava within 5 blocks of another player!", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("You cannot place lava within 5 blocks of a peaceful player!", NamedTextColor.RED));
            }
            return;
        }
        
        // Track lava placement
        playerPlacedLava.put(targetLocation, player.getUniqueId());
        plugin.getLogger().info("Tracked player-placed lava (bucket) at " + targetLocation + " by " + player.getName() +
                " (mode: " + (isPeaceful ? "peaceful" : "normal") + ")");
    }
    
    /**
     * Cleans up old tracking data
     */
    private void cleanupOldTracking() {
        // For now, we'll keep all tracking data since we're using UUIDs instead of timestamps
        // In the future, we could implement a more sophisticated cleanup system
        // that tracks both UUID and timestamp
    }
    
    /**
     * Handles environmental damage events for peaceful player protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        // Only handle if the event isn't already cancelled
        if (event.isCancelled()) {
            return;
        }
        
        // Only handle player damage
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if player is in peaceful mode
        if (!playerModeManager.isPeacefulPlayer(player)) {
            return; // Only protect peaceful players
        }
        
        // Check if we're in a PVP area
        if (pvpAreaManager.isInPvpArea(player.getLocation())) {
            return; // Allow all damage in PVP areas
        }
        
        // Check if this is a protected damage cause
        if (!PROTECTED_DAMAGE_CAUSES.contains(event.getCause())) {
            return; // Only protect specific damage causes
        }
        
        // Debug logging
        plugin.getLogger().info("Environmental damage detected: " + event.getCause() + " for peaceful player " + player.getName());
        
        // Check if the damage source is player-related
        if (isPlayerCausedDamage(event, player)) {
            // Cancel the damage and notify the player
            event.setCancelled(true);
            sendProtectionMessage(player, event.getCause());
            plugin.getLogger().info("Protected peaceful player " + player.getName() + " from " + event.getCause());
        } else {
            plugin.getLogger().info("Damage not player-caused, allowing damage to " + player.getName());
        }
    }
    
    /**
     * Checks if the damage is caused by another player's actions
     */
    private boolean isPlayerCausedDamage(EntityDamageEvent event, Player victim) {
        Location location = victim.getLocation();
        
        switch (event.getCause()) {
            case FIRE:
            case FIRE_TICK:
                // Check if there's fire nearby that could be player-placed
                return isPlayerPlacedFire(location);
                
            case LAVA:
            case HOT_FLOOR:
                // Check if there's lava nearby that could be player-placed
                return isPlayerPlacedLava(location);
                
            case CONTACT:
                // Check if it's from cactus or other blocks that could be player-placed
                return isPlayerPlacedContact(location);
                
            case FALLING_BLOCK:
                // Check if it's from a falling block that could be player-placed
                return isPlayerPlacedFallingBlock(location);
                
            case MAGIC:
            case WITHER:
                // These are usually from potions or effects, likely player-caused
                return true;
                
            case POISON:
                // Poison can be from mobs (bees, cave spiders) or players (potions)
                // We need to be more conservative and only block if we can prove it's player-caused
                // For now, allow all poison damage to peaceful players (mob poison should work)
                return false;
                
            case CRAMMING:
            case DROWNING:
                // These are usually natural, but could be player-caused in some cases
                return false; // Allow natural cramming/drowning
                
            default:
                return false;
        }
    }
    
    /**
     * Checks if there's player-placed fire nearby
     */
    private boolean isPlayerPlacedFire(Location location) {
        // Check a 3x3x3 area around the player for fire
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location checkLocation = location.clone().add(x, y, z);
                    Block block = checkLocation.getBlock();
                    if (block.getType() == Material.FIRE) {
                        // Check if this fire was placed by a player
                        if (playerPlacedFire.containsKey(checkLocation)) {
                            plugin.getLogger().info("Found player-placed fire at " + checkLocation);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if there's player-placed lava nearby
     */
    private boolean isPlayerPlacedLava(Location location) {
        // Check a 3x3x3 area around the player for lava
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location checkLocation = location.clone().add(x, y, z);
                    Block block = checkLocation.getBlock();
                    if (block.getType() == Material.LAVA) {
                        // Check if this lava was placed by a player
                        if (playerPlacedLava.containsKey(checkLocation)) {
                            plugin.getLogger().info("Found player-placed lava at " + checkLocation);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if there's player-placed contact damage nearby
     */
    private boolean isPlayerPlacedContact(Location location) {
        // Check the block the player is standing on
        Block block = location.getBlock();
        if (block.getType() == Material.CACTUS || block.getType() == Material.SWEET_BERRY_BUSH) {
            // Check if this block is in a claimed area (likely player-placed)
            return plugin.getClaimManager().isLocationClaimed(block.getLocation());
        }
        return false;
    }
    
    /**
     * Checks if there's a player-placed falling block nearby
     */
    private boolean isPlayerPlacedFallingBlock(Location location) {
        // Check blocks above the player
        for (int y = 1; y <= 10; y++) {
            Block block = location.clone().add(0, y, 0).getBlock();
            if (block.getType() != Material.AIR) {
                // Check if this block is in a claimed area (likely player-placed)
                return plugin.getClaimManager().isLocationClaimed(block.getLocation());
            }
        }
        return false;
    }
    
    /**
     * Sends a protection message to the player
     */
    private void sendProtectionMessage(Player player, EntityDamageEvent.DamageCause cause) {
        String message = getProtectionMessage(cause);
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }
    
    /**
     * Gets the appropriate protection message for the damage cause
     */
    private String getProtectionMessage(EntityDamageEvent.DamageCause cause) {
        switch (cause) {
            case FIRE:
            case FIRE_TICK:
                return "You are protected from fire damage as a peaceful player!";
            case LAVA:
            case HOT_FLOOR:
                return "You are protected from lava damage as a peaceful player!";
            case CONTACT:
                return "You are protected from contact damage as a peaceful player!";
            case FALLING_BLOCK:
                return "You are protected from falling block damage as a peaceful player!";
            case MAGIC:
                return "You are protected from magic damage as a peaceful player!";
            case WITHER:
                return "You are protected from wither damage as a peaceful player!";
            default:
                return "You are protected from environmental damage as a peaceful player!";
        }
    }
}
