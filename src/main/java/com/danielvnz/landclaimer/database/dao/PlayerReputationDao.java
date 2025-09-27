package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.model.PlayerReputation;

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
 * Data Access Object for PlayerReputation entities.
 * Handles database operations for player reputation tracking.
 */
public class PlayerReputationDao implements BaseDao<PlayerReputation, UUID> {
    
    private final DatabaseManager databaseManager;
    
    public PlayerReputationDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Creates the player_reputation table if it doesn't exist
     */
    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_reputation (
                uuid VARCHAR(36) PRIMARY KEY,
                reputation INTEGER DEFAULT 0 CHECK (reputation >= -15 AND reputation <= 15),
                last_playtime_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_playtime_hours REAL DEFAULT 0.0
            )
            """;
        
        try (var connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
    
    @Override
    public CompletableFuture<Void> save(PlayerReputation playerReputation) {
        return databaseManager.executeAsync(connection -> {
            String sql = "INSERT OR REPLACE INTO player_reputation (uuid, reputation, last_playtime_update, total_playtime_hours) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerReputation.getUuid().toString());
                stmt.setInt(2, playerReputation.getReputation());
                stmt.setTimestamp(3, Timestamp.valueOf(playerReputation.getLastPlaytimeUpdate()));
                stmt.setDouble(4, playerReputation.getTotalPlaytimeHours());
                
                stmt.executeUpdate();
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<PlayerReputation>> findById(UUID uuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, reputation, last_playtime_update, total_playtime_hours FROM player_reputation WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToPlayerReputation(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<List<PlayerReputation>> findAll() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, reputation, last_playtime_update, total_playtime_hours FROM player_reputation ORDER BY reputation DESC";
            List<PlayerReputation> reputations = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    reputations.add(mapResultSetToPlayerReputation(rs));
                }
                
                return reputations;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> update(PlayerReputation playerReputation) {
        return save(playerReputation); // INSERT OR REPLACE handles both insert and update
    }
    
    @Override
    public CompletableFuture<Void> deleteById(UUID uuid) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM player_reputation WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> existsById(UUID uuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT 1 FROM player_reputation WHERE uuid = ? LIMIT 1";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> count() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT COUNT(*) FROM player_reputation";
            
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
     * Gets the top players by reputation
     * @param limit The maximum number of players to return
     * @return CompletableFuture containing a list of top players by reputation
     */
    public CompletableFuture<List<PlayerReputation>> getTopPlayers(int limit) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, reputation, last_playtime_update, total_playtime_hours FROM player_reputation ORDER BY reputation DESC LIMIT ?";
            List<PlayerReputation> reputations = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reputations.add(mapResultSetToPlayerReputation(rs));
                    }
                    
                    return reputations;
                }
            }
        });
    }
    
    /**
     * Gets the bottom players by reputation (most negative)
     * @param limit The maximum number of players to return
     * @return CompletableFuture containing a list of bottom players by reputation
     */
    public CompletableFuture<List<PlayerReputation>> getBottomPlayers(int limit) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, reputation, last_playtime_update, total_playtime_hours FROM player_reputation ORDER BY reputation ASC LIMIT ?";
            List<PlayerReputation> reputations = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reputations.add(mapResultSetToPlayerReputation(rs));
                    }
                    
                    return reputations;
                }
            }
        });
    }
    
    /**
     * Updates only the reputation value for a player
     * @param uuid The player's UUID
     * @param reputation The new reputation value
     * @return CompletableFuture that completes when the update is done
     */
    public CompletableFuture<Void> updateReputation(UUID uuid, int reputation) {
        return databaseManager.executeAsync(connection -> {
            String sql = "UPDATE player_reputation SET reputation = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, reputation);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Updates the playtime for a player
     * @param uuid The player's UUID
     * @param additionalHours The additional playtime in hours
     * @param lastUpdate The timestamp of the last update
     * @return CompletableFuture that completes when the update is done
     */
    public CompletableFuture<Void> updatePlaytime(UUID uuid, double additionalHours, LocalDateTime lastUpdate) {
        return databaseManager.executeAsync(connection -> {
            String sql = "UPDATE player_reputation SET total_playtime_hours = total_playtime_hours + ?, last_playtime_update = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setDouble(1, additionalHours);
                stmt.setTimestamp(2, Timestamp.valueOf(lastUpdate));
                stmt.setString(3, uuid.toString());
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Maps a ResultSet to a PlayerReputation object
     * @param rs The ResultSet containing the data
     * @return A PlayerReputation object
     * @throws SQLException If there's an error reading the data
     */
    private PlayerReputation mapResultSetToPlayerReputation(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        int reputation = rs.getInt("reputation");
        LocalDateTime lastPlaytimeUpdate = rs.getTimestamp("last_playtime_update").toLocalDateTime();
        double totalPlaytimeHours = rs.getDouble("total_playtime_hours");
        
        return new PlayerReputation(uuid, reputation, lastPlaytimeUpdate, totalPlaytimeHours);
    }
}
