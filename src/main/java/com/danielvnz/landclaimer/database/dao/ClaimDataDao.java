package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.model.ClaimData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for ClaimData entities.
 * Handles database operations for land claims made by peaceful players.
 */
public class ClaimDataDao implements BaseDao<ClaimData, Long> {
    
    private final DatabaseManager databaseManager;
    
    public ClaimDataDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    @Override
    public CompletableFuture<Void> save(ClaimData claimData) {
        return databaseManager.executeAsync(connection -> {
            String sql = "INSERT INTO land_claims (owner_uuid, world_name, chunk_x, chunk_z, claim_time) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, claimData.getOwnerUuid().toString());
                stmt.setString(2, claimData.getWorldName());
                stmt.setInt(3, claimData.getChunkX());
                stmt.setInt(4, claimData.getChunkZ());
                stmt.setTimestamp(5, Timestamp.valueOf(claimData.getClaimTime()));
                
                stmt.executeUpdate();
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<ClaimData>> findById(Long id) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT id, owner_uuid, world_name, chunk_x, chunk_z, claim_time FROM land_claims WHERE id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToClaimData(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<List<ClaimData>> findAll() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT id, owner_uuid, world_name, chunk_x, chunk_z, claim_time FROM land_claims";
            List<ClaimData> claims = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    claims.add(mapResultSetToClaimData(rs));
                }
            }
            
            return claims;
        });
    }
    
    @Override
    public CompletableFuture<Void> update(ClaimData claimData) {
        // Claims are typically immutable once created, but we can update the owner if needed
        return databaseManager.executeAsync(connection -> {
            String sql = "UPDATE land_claims SET owner_uuid = ? WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, claimData.getOwnerUuid().toString());
                stmt.setString(2, claimData.getWorldName());
                stmt.setInt(3, claimData.getChunkX());
                stmt.setInt(4, claimData.getChunkZ());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("No claim found at coordinates: " + claimData.getChunkKey());
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM land_claims WHERE id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> existsById(Long id) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT 1 FROM land_claims WHERE id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> count() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT COUNT(*) FROM land_claims";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        });
    }
    
    /**
     * Finds a claim by world name and chunk coordinates
     * @param worldName The world name
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @return CompletableFuture containing an Optional with the claim if found
     */
    public CompletableFuture<Optional<ClaimData>> findByChunk(String worldName, int chunkX, int chunkZ) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT id, owner_uuid, world_name, chunk_x, chunk_z, claim_time FROM land_claims WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, worldName);
                stmt.setInt(2, chunkX);
                stmt.setInt(3, chunkZ);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToClaimData(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    /**
     * Finds all claims owned by a specific player
     * @param ownerUuid The owner's UUID
     * @return CompletableFuture containing a list of claims owned by the player
     */
    public CompletableFuture<List<ClaimData>> findByOwner(UUID ownerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT id, owner_uuid, world_name, chunk_x, chunk_z, claim_time FROM land_claims WHERE owner_uuid = ?";
            List<ClaimData> claims = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, ownerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        claims.add(mapResultSetToClaimData(rs));
                    }
                }
            }
            
            return claims;
        });
    }
    
    /**
     * Finds all claims in a specific world
     * @param worldName The world name
     * @return CompletableFuture containing a list of claims in the world
     */
    public CompletableFuture<List<ClaimData>> findByWorld(String worldName) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT id, owner_uuid, world_name, chunk_x, chunk_z, claim_time FROM land_claims WHERE world_name = ?";
            List<ClaimData> claims = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, worldName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        claims.add(mapResultSetToClaimData(rs));
                    }
                }
            }
            
            return claims;
        });
    }
    
    /**
     * Counts the number of claims owned by a specific player
     * @param ownerUuid The owner's UUID
     * @return CompletableFuture containing the count of claims
     */
    public CompletableFuture<Long> countByOwner(UUID ownerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT COUNT(*) FROM land_claims WHERE owner_uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, ownerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    return 0L;
                }
            }
        });
    }
    
    /**
     * Deletes a claim by world name and chunk coordinates
     * @param worldName The world name
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @return CompletableFuture that completes when the delete operation is done
     */
    public CompletableFuture<Void> deleteByChunk(String worldName, int chunkX, int chunkZ) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM land_claims WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, worldName);
                stmt.setInt(2, chunkX);
                stmt.setInt(3, chunkZ);
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Deletes all claims owned by a specific player
     * @param ownerUuid The owner's UUID
     * @return CompletableFuture that completes when the delete operation is done
     */
    public CompletableFuture<Void> deleteByOwner(UUID ownerUuid) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM land_claims WHERE owner_uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, ownerUuid.toString());
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Checks if a chunk is already claimed
     * @param worldName The world name
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @return CompletableFuture containing true if the chunk is claimed, false otherwise
     */
    public CompletableFuture<Boolean> isChunkClaimed(String worldName, int chunkX, int chunkZ) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT 1 FROM land_claims WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, worldName);
                stmt.setInt(2, chunkX);
                stmt.setInt(3, chunkZ);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
    
    /**
     * Maps a ResultSet row to a ClaimData object
     * @param rs The ResultSet positioned at a valid row
     * @return The mapped ClaimData object
     * @throws SQLException if there's an error reading from the ResultSet
     */
    private ClaimData mapResultSetToClaimData(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String worldName = rs.getString("world_name");
        int chunkX = rs.getInt("chunk_x");
        int chunkZ = rs.getInt("chunk_z");
        LocalDateTime claimTime = rs.getTimestamp("claim_time").toLocalDateTime();
        
        return new ClaimData(id, ownerUuid, worldName, chunkX, chunkZ, claimTime);
    }
}