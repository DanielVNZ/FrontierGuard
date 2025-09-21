package com.danielvnz.landclaimer.listener;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.manager.ClaimInvitationManager;
import com.danielvnz.landclaimer.manager.ClaimManager;
import com.danielvnz.landclaimer.manager.PvpAreaManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

/**
 * Handles explosion protection for claimed chunks and PVP areas.
 * Prevents explosions from affecting protected areas.
 */
public class ExplosionProtectionListener implements Listener {
    
    private final LandClaimerPlugin plugin;
    private final ClaimManager claimManager;
    private final ClaimInvitationManager invitationManager;
    private final PvpAreaManager pvpAreaManager;
    
    public ExplosionProtectionListener(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
        this.invitationManager = plugin.getInvitationManager();
        this.pvpAreaManager = plugin.getPvpAreaManager();
    }
    
    /**
     * Handles explosion events to protect claimed chunks and PVP areas
     * @param event The EntityExplodeEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        List<Block> blocks = event.blockList();
        
        // Check if the explosion is from TNT
        if (!(entity instanceof TNTPrimed)) {
            return; // Only protect against TNT explosions for now
        }
        
        // Get the source location of the explosion
        Location explosionSource = entity.getLocation();
        
        // Check if the explosion source is in a PVP area
        boolean sourceInPvpArea = pvpAreaManager.isInPvpArea(explosionSource);
        
        // Remove blocks that should be protected
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLocation = block.getLocation();
            
            // Check if this block is in a PVP area
            if (pvpAreaManager.isInPvpArea(blockLocation)) {
                // If the explosion source is NOT in a PVP area, protect the PVP area
                if (!sourceInPvpArea) {
                    iterator.remove(); // Remove from explosion list (protect the block)
                    continue;
                }
            }
            
            // Check if this block is in a claimed chunk
            if (claimManager.isLocationClaimed(blockLocation)) {
                // Get the owner of the claimed chunk
                var claimData = claimManager.getClaimInfo(blockLocation.getChunk());
                if (claimData != null) {
                    // Check if the explosion source is in the same claim
                    boolean sourceInSameClaim = claimManager.isLocationClaimed(explosionSource) &&
                            claimManager.getClaimInfo(explosionSource.getChunk()) != null &&
                            claimManager.getClaimInfo(explosionSource.getChunk()).getOwnerUuid().equals(claimData.getOwnerUuid());
                    
                    // Check if the explosion source is from an invited player
                    boolean sourceIsInvited = false;
                    if (entity instanceof TNTPrimed) {
                        TNTPrimed tnt = (TNTPrimed) entity;
                        Entity source = tnt.getSource();
                        if (source instanceof Player) {
                            Player player = (Player) source;
                            try {
                                sourceIsInvited = invitationManager.canPlayerBuild(player, claimData).get();
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error checking invitation for explosion: " + e.getMessage());
                            }
                        }
                    }
                    
                    // If the explosion source is not in the same claim and not from an invited player, protect the block
                    if (!sourceInSameClaim && !sourceIsInvited) {
                        iterator.remove(); // Remove from explosion list (protect the block)
                    }
                }
            }
        }
        
        // If all blocks were protected, cancel the explosion entirely
        if (blocks.isEmpty()) {
            event.setCancelled(true);
            
            // Notify nearby players if the explosion was from TNT
            if (entity instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) entity;
                Entity source = tnt.getSource();
                if (source instanceof Player) {
                    Player player = (Player) source;
                    String message = plugin.getMessage("explosion-blocked");
                    if (message.equals("explosion-blocked")) {
                        message = "&c&lExplosion blocked! You cannot damage protected areas.";
                    }
                    Component messageComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                    player.sendMessage(messageComponent);
                }
            }
        }
    }
}
