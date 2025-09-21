package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.database.dao.PvpAreaDao;
import com.danielvnz.landclaimer.model.PvpArea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages PVP areas where all players can engage in combat regardless of mode.
 * Handles creation, deletion, and boundary checking for PVP zones.
 */
public class PvpAreaManager {
    
    private final LandClaimerPlugin plugin;
    private final PvpAreaDao pvpAreaDao;
    
    // Cache for PVP areas to avoid database queries
    private final ConcurrentHashMap<String, PvpArea> pvpAreaCache;
    
    // Selection tool material
    private static final Material SELECTION_TOOL = Material.STICK;
    private static final String SELECTION_TOOL_NAME = "PVP Area Selection Tool";
    private static final String SELECTION_TOOL_LORE = "Right-click to select corners for PVP area";
    
    public PvpAreaManager(LandClaimerPlugin plugin) {
        this.plugin = plugin;
        this.pvpAreaDao = createPvpAreaDao(plugin.getDatabaseManager());
        this.pvpAreaCache = new ConcurrentHashMap<>();
        
        // Load existing PVP areas into cache
        loadPvpAreas();
    }
    
    /**
     * Creates the PvpAreaDao instance. Can be overridden for testing.
     * @param databaseManager The database manager
     * @return The PvpAreaDao instance
     */
    protected PvpAreaDao createPvpAreaDao(DatabaseManager databaseManager) {
        return new PvpAreaDao(databaseManager);
    }
    
    /**
     * Loads all PVP areas from database into cache
     */
    private void loadPvpAreas() {
        try {
            List<PvpArea> areas = pvpAreaDao.findAll().get();
            for (PvpArea area : areas) {
                pvpAreaCache.put(area.getName(), area);
            }
            plugin.getLogger().info("Loaded " + areas.size() + " PVP areas into cache");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load PVP areas", e);
        }
    }
    
    /**
     * Creates a new PVP area
     * @param name The name of the PVP area
     * @param corner1 The first corner location
     * @param corner2 The second corner location
     * @return true if the area was created successfully, false otherwise
     */
    public boolean createPvpArea(String name, Location corner1, Location corner2) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (corner1 == null || corner2 == null) {
            return false;
        }
        
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            return false; // Corners must be in the same world
        }
        
        // Check if area with this name already exists
        if (pvpAreaCache.containsKey(name)) {
            return false;
        }
        
        // Create PVP area from bedrock to build height
        World world = corner1.getWorld();
        int minY = world.getMinHeight(); // Bedrock level
        int maxY = world.getMaxHeight() - 1; // Build height
        
        PvpArea pvpArea = new PvpArea(
            name,
            world.getName(),
            Math.min(corner1.getBlockX(), corner2.getBlockX()),
            minY,
            Math.min(corner1.getBlockZ(), corner2.getBlockZ()),
            Math.max(corner1.getBlockX(), corner2.getBlockX()),
            maxY,
            Math.max(corner1.getBlockZ(), corner2.getBlockZ())
        );
        
        try {
            // Save to database
            pvpAreaDao.save(pvpArea).get();
            
            // Add to cache
            pvpAreaCache.put(name, pvpArea);
            
            plugin.getLogger().info("Created PVP area: " + name + " in world " + world.getName());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create PVP area: " + name, e);
            return false;
        }
    }
    
    /**
     * Deletes a PVP area
     * @param name The name of the PVP area to delete
     * @return true if the area was deleted successfully, false otherwise
     */
    public boolean deletePvpArea(String name) {
        if (name == null || !pvpAreaCache.containsKey(name)) {
            return false;
        }
        
        try {
            pvpAreaDao.deleteByName(name).get();
            
            // Remove from cache
            pvpAreaCache.remove(name);
            
            plugin.getLogger().info("Deleted PVP area: " + name);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete PVP area: " + name, e);
            return false;
        }
    }
    
    /**
     * Checks if a location is in any PVP area
     * @param location The location to check
     * @return true if the location is in a PVP area, false otherwise
     */
    public boolean isInPvpArea(Location location) {
        if (location == null) {
            return false;
        }
        
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        for (PvpArea area : pvpAreaCache.values()) {
            if (area.getWorldName().equals(worldName) && area.contains(x, y, z)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the PVP area at a specific location
     * @param location The location to check
     * @return The PVP area if found, null otherwise
     */
    public PvpArea getPvpArea(Location location) {
        if (location == null) {
            return null;
        }
        
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        for (PvpArea area : pvpAreaCache.values()) {
            if (area.getWorldName().equals(worldName) && area.contains(x, y, z)) {
                return area;
            }
        }
        
        return null;
    }
    
    /**
     * Gets all PVP areas
     * @return List of all PVP areas
     */
    public List<PvpArea> listPvpAreas() {
        return List.copyOf(pvpAreaCache.values());
    }
    
    /**
     * Gets a PVP area by name
     * @param name The name of the PVP area
     * @return The PVP area if found, null otherwise
     */
    public PvpArea getPvpArea(String name) {
        return pvpAreaCache.get(name);
    }
    
    /**
     * Gives a player the PVP area selection tool
     * @param player The player to give the tool to
     */
    public void giveSelectionTool(Player player) {
        if (player == null) {
            return;
        }
        
        ItemStack tool = new ItemStack(SELECTION_TOOL);
        ItemMeta meta = tool.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(SELECTION_TOOL_NAME, NamedTextColor.GOLD, TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text(SELECTION_TOOL_LORE, NamedTextColor.GRAY),
                Component.text("Select two corners to define PVP area", NamedTextColor.YELLOW),
                Component.text("Area will extend from bedrock to build height", NamedTextColor.YELLOW)
            ));
            tool.setItemMeta(meta);
        }
        
        player.getInventory().addItem(tool);
        player.sendMessage(Component.text("You have been given the PVP area selection tool!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Right-click on two corners to define your PVP area.", NamedTextColor.YELLOW));
    }
    
    /**
     * Handles selection tool interaction
     * @param player The player using the tool
     * @param location The location they clicked
     * @return true if the interaction was handled, false otherwise
     */
    public boolean handleSelectionTool(Player player, Location location) {
        if (player == null || location == null) {
            return false;
        }
        
        // Check if player has admin permission
        if (!player.hasPermission("frontierguard.admin.pvparea")) {
            return false;
        }
        
        // Get or create selection data for player
        SelectionData selectionData = getOrCreateSelectionData(player);
        
        if (selectionData.corner1 == null) {
            // First corner
            selectionData.corner1 = location.clone();
            player.sendMessage(Component.text("First corner selected at ", NamedTextColor.GREEN)
                .append(Component.text("(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")", NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GREEN)));
            player.sendMessage(Component.text("Right-click on the second corner.", NamedTextColor.YELLOW));
            return true;
        } else {
            // Second corner
            selectionData.corner2 = location.clone();
            
            // Validate selection
            if (!selectionData.corner1.getWorld().equals(selectionData.corner2.getWorld())) {
                player.sendMessage(Component.text("Both corners must be in the same world!", NamedTextColor.RED));
                clearSelectionData(player);
                return true;
            }
            
            // Show selection preview
            showSelectionPreview(player, selectionData);
            
            player.sendMessage(Component.text("Second corner selected at ", NamedTextColor.GREEN)
                .append(Component.text("(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")", NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GREEN)));
            player.sendMessage(Component.text("Use /landclaimer createpvparea <name> to create the PVP area.", NamedTextColor.YELLOW));
            
            return true;
        }
    }
    
    /**
     * Creates a PVP area from the player's current selection
     * @param player The player creating the area
     * @param name The name for the PVP area
     * @return true if the area was created successfully, false otherwise
     */
    public boolean createFromSelection(Player player, String name) {
        if (player == null || name == null) {
            return false;
        }
        
        SelectionData selectionData = getSelectionData(player);
        if (selectionData == null || selectionData.corner1 == null || selectionData.corner2 == null) {
            player.sendMessage(Component.text("You must select two corners first!", NamedTextColor.RED));
            return false;
        }
        
        boolean success = createPvpArea(name, selectionData.corner1, selectionData.corner2);
        
        if (success) {
            player.sendMessage(Component.text("PVP area '", NamedTextColor.GREEN)
                .append(Component.text(name, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("' created successfully!", NamedTextColor.GREEN)));
            
            // Show area info
            PvpArea area = getPvpArea(name);
            if (area != null) {
                player.sendMessage(Component.text("Area: ", NamedTextColor.GRAY)
                    .append(Component.text(area.getArea() + " blocks", NamedTextColor.WHITE))
                    .append(Component.text(" | Volume: ", NamedTextColor.GRAY))
                    .append(Component.text(area.getVolume() + " blocks", NamedTextColor.WHITE)));
            }
        } else {
            player.sendMessage(Component.text("Failed to create PVP area. Name might already exist.", NamedTextColor.RED));
        }
        
        // Clear selection
        clearSelectionData(player);
        return success;
    }
    
    /**
     * Shows a preview of the current selection
     * @param player The player to show the preview to
     * @param selectionData The selection data
     */
    private void showSelectionPreview(Player player, SelectionData selectionData) {
        if (selectionData.corner1 == null || selectionData.corner2 == null) {
            return;
        }
        
        Location corner1 = selectionData.corner1;
        Location corner2 = selectionData.corner2;
        
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        
        long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        long volume = area * (corner1.getWorld().getMaxHeight() - corner1.getWorld().getMinHeight());
        
        player.sendMessage(Component.text("=== Selection Preview ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Corner 1: ", NamedTextColor.GRAY)
            .append(Component.text("(" + corner1.getBlockX() + ", " + corner1.getBlockY() + ", " + corner1.getBlockZ() + ")", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Corner 2: ", NamedTextColor.GRAY)
            .append(Component.text("(" + corner2.getBlockX() + ", " + corner2.getBlockY() + ", " + corner2.getBlockZ() + ")", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Area: ", NamedTextColor.GRAY)
            .append(Component.text(area + " blocks", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Volume: ", NamedTextColor.GRAY)
            .append(Component.text(volume + " blocks", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Y Range: ", NamedTextColor.GRAY)
            .append(Component.text(corner1.getWorld().getMinHeight() + " to " + (corner1.getWorld().getMaxHeight() - 1), NamedTextColor.WHITE)));
    }
    
    /**
     * Clears the selection data for a player
     * @param player The player to clear selection for
     */
    public void clearSelectionData(Player player) {
        if (player != null) {
            selectionDataMap.remove(player.getUniqueId());
        }
    }
    
    // Selection data storage
    private static class SelectionData {
        Location corner1;
        Location corner2;
    }
    
    private final ConcurrentHashMap<UUID, SelectionData> selectionDataMap = new ConcurrentHashMap<>();
    
    private SelectionData getOrCreateSelectionData(Player player) {
        return selectionDataMap.computeIfAbsent(player.getUniqueId(), k -> new SelectionData());
    }
    
    private SelectionData getSelectionData(Player player) {
        return selectionDataMap.get(player.getUniqueId());
    }
}
