package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.model.PvpArea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for PvpArea entities.
 * Handles database operations for PVP areas.
 */
public class PvpAreaDao {
    
    private final DatabaseManager databaseManager;
    
    public PvpAreaDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Creates the pvp_areas table if it doesn't exist
     */
    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS pvp_areas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(50) UNIQUE NOT NULL,
                world_name VARCHAR(50) NOT NULL,
                min_x INTEGER NOT NULL,
                min_y INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                max_y INTEGER NOT NULL,
                max_z INTEGER NOT NULL
            )
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
    
    /**
     * Saves a PVP area to the database
     * @param pvpArea The PVP area to save
     * @return CompletableFuture that completes when the save operation is done
     */
    public CompletableFuture<Void> save(PvpArea pvpArea) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO pvp_areas 
                (name, world_name, min_x, min_y, min_z, max_x, max_y, max_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, pvpArea.getName());
                stmt.setString(2, pvpArea.getWorldName());
                stmt.setInt(3, pvpArea.getMinX());
                stmt.setInt(4, pvpArea.getMinY());
                stmt.setInt(5, pvpArea.getMinZ());
                stmt.setInt(6, pvpArea.getMaxX());
                stmt.setInt(7, pvpArea.getMaxY());
                stmt.setInt(8, pvpArea.getMaxZ());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save PVP area: " + pvpArea.getName(), e);
            }
        });
    }
    
    /**
     * Finds a PVP area by name
     * @param name The name of the PVP area
     * @return CompletableFuture containing the PVP area if found, empty otherwise
     */
    public CompletableFuture<Optional<PvpArea>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, name, world_name, min_x, min_y, min_z, max_x, max_y, max_z
                FROM pvp_areas
                WHERE name = ?
                """;
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, name);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToPvpArea(rs));
                    } else {
                        return Optional.empty();
                    }
                }
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find PVP area by name: " + name, e);
            }
        });
    }
    
    /**
     * Finds all PVP areas
     * @return CompletableFuture containing a list of all PVP areas
     */
    public CompletableFuture<List<PvpArea>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, name, world_name, min_x, min_y, min_z, max_x, max_y, max_z
                FROM pvp_areas
                ORDER BY name
                """;
            
            List<PvpArea> areas = new ArrayList<>();
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    areas.add(mapResultSetToPvpArea(rs));
                }
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find all PVP areas", e);
            }
            
            return areas;
        });
    }
    
    /**
     * Deletes a PVP area by name
     * @param name The name of the PVP area to delete
     * @return CompletableFuture that completes when the delete operation is done
     */
    public CompletableFuture<Void> deleteByName(String name) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM pvp_areas WHERE name = ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, name);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new RuntimeException("PVP area not found: " + name);
                }
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete PVP area: " + name, e);
            }
        });
    }
    
    /**
     * Deletes a PVP area by ID
     * @param id The ID of the PVP area to delete
     * @return CompletableFuture that completes when the delete operation is done
     */
    public CompletableFuture<Void> deleteById(int id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM pvp_areas WHERE id = ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, id);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new RuntimeException("PVP area not found with ID: " + id);
                }
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete PVP area with ID: " + id, e);
            }
        });
    }
    
    /**
     * Checks if a PVP area exists by name
     * @param name The name of the PVP area
     * @return CompletableFuture containing true if the area exists, false otherwise
     */
    public CompletableFuture<Boolean> existsByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM pvp_areas WHERE name = ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, name);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check if PVP area exists: " + name, e);
            }
        });
    }
    
    /**
     * Counts the total number of PVP areas
     * @return CompletableFuture containing the count of PVP areas
     */
    public CompletableFuture<Integer> count() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM pvp_areas";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to count PVP areas", e);
            }
        });
    }
    
    /**
     * Maps a ResultSet to a PvpArea object
     * @param rs The ResultSet to map
     * @return The mapped PvpArea object
     * @throws SQLException If there's an error reading from the ResultSet
     */
    private PvpArea mapResultSetToPvpArea(ResultSet rs) throws SQLException {
        return new PvpArea(
            rs.getString("name"),
            rs.getString("world_name"),
            rs.getInt("min_x"),
            rs.getInt("min_y"),
            rs.getInt("min_z"),
            rs.getInt("max_x"),
            rs.getInt("max_y"),
            rs.getInt("max_z")
        );
    }
}