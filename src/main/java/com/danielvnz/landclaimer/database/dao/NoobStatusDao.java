package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.model.NoobStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Data Access Object for NoobStatus entities.
 * Handles database operations for player noob status tracking.
 */
public class NoobStatusDao {
    
    private static final Logger LOGGER = Logger.getLogger(NoobStatusDao.class.getName());
    
    private final DatabaseManager databaseManager;
    
    public NoobStatusDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Saves a noob status to the database
     * @param noobStatus The noob status to save
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> save(NoobStatus noobStatus) {
        return databaseManager.executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO noob_status (uuid, expiration_time, created_at)
                VALUES (?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, noobStatus.getPlayerUuid().toString());
                stmt.setTimestamp(2, Timestamp.valueOf(noobStatus.getExpirationTime()));
                stmt.setTimestamp(3, Timestamp.valueOf(noobStatus.getCreatedAt()));
                
                stmt.executeUpdate();
                LOGGER.fine("Saved noob status for player: " + noobStatus.getPlayerUuid());
            }
        });
    }
    
    /**
     * Finds a noob status by player UUID
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing the NoobStatus or null if not found
     */
    public CompletableFuture<NoobStatus> findByPlayerUuid(UUID playerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, expiration_time, created_at FROM noob_status WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        LocalDateTime expirationTime = rs.getTimestamp("expiration_time").toLocalDateTime();
                        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                        
                        return new NoobStatus(uuid, expirationTime, createdAt);
                    }
                }
            }
            
            return null;
        });
    }
    
    /**
     * Deletes a noob status by player UUID
     * @param playerUuid The player's UUID
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Boolean> deleteByPlayerUuid(UUID playerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "DELETE FROM noob_status WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                
                int rowsAffected = stmt.executeUpdate();
                boolean deleted = rowsAffected > 0;
                
                if (deleted) {
                    LOGGER.fine("Deleted noob status for player: " + playerUuid);
                }
                
                return deleted;
            }
        });
    }
    
    /**
     * Finds all active (non-expired) noob statuses
     * @return CompletableFuture containing a list of active NoobStatus objects
     */
    public CompletableFuture<List<NoobStatus>> findAllActive() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, expiration_time, created_at FROM noob_status WHERE expiration_time > ?";
            
            List<NoobStatus> activeStatuses = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        LocalDateTime expirationTime = rs.getTimestamp("expiration_time").toLocalDateTime();
                        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                        
                        activeStatuses.add(new NoobStatus(uuid, expirationTime, createdAt));
                    }
                }
            }
            
            return activeStatuses;
        });
    }
    
    /**
     * Deletes all expired noob statuses
     * @return CompletableFuture containing the number of deleted records
     */
    public CompletableFuture<Integer> deleteExpired() {
        return databaseManager.queryAsync(connection -> {
            String sql = "DELETE FROM noob_status WHERE expiration_time <= ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                
                int deletedCount = stmt.executeUpdate();
                
                if (deletedCount > 0) {
                    LOGGER.info("Deleted " + deletedCount + " expired noob statuses");
                }
                
                return deletedCount;
            }
        });
    }
    
    /**
     * Counts all noob statuses in the database
     * @return CompletableFuture containing the total count
     */
    public CompletableFuture<Integer> countAll() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT COUNT(*) FROM noob_status";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
            return 0;
        });
    }
    
    /**
     * Counts active (non-expired) noob statuses
     * @return CompletableFuture containing the active count
     */
    public CompletableFuture<Integer> countActive() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT COUNT(*) FROM noob_status WHERE expiration_time > ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
            return 0;
        });
    }
    
    /**
     * Clears all noob statuses from the database
     * @return CompletableFuture containing the number of deleted records
     */
    public CompletableFuture<Integer> deleteAll() {
        return databaseManager.queryAsync(connection -> {
            String sql = "DELETE FROM noob_status";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int deletedCount = stmt.executeUpdate();
                
                if (deletedCount > 0) {
                    LOGGER.info("Deleted all " + deletedCount + " noob statuses");
                }
                
                return deletedCount;
            }
        });
    }
}
