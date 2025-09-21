package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;
import com.danielvnz.landclaimer.model.PlayerData;
import com.danielvnz.landclaimer.model.PlayerMode;

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
 * Data Access Object for PlayerData entities.
 * Handles database operations for player mode and state information.
 */
public class PlayerDataDao implements BaseDao<PlayerData, UUID> {
    
    private final DatabaseManager databaseManager;
    
    public PlayerDataDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    @Override
    public CompletableFuture<Void> save(PlayerData playerData) {
        return databaseManager.executeAsync(connection -> {
            // Use INSERT OR REPLACE for SQLite compatibility
            String sql = "INSERT OR REPLACE INTO player_modes (uuid, mode, first_join, last_mode_change) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, playerData.getMode() != null ? playerData.getMode().getDisplayName() : null);
                stmt.setTimestamp(3, Timestamp.valueOf(playerData.getFirstJoin()));
                stmt.setTimestamp(4, playerData.getLastModeChange() != null ? 
                    Timestamp.valueOf(playerData.getLastModeChange()) : null);
                
                stmt.executeUpdate();
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<PlayerData>> findById(UUID uuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, mode, first_join, last_mode_change FROM player_modes WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToPlayerData(rs));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<List<PlayerData>> findAll() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, mode, first_join, last_mode_change FROM player_modes";
            List<PlayerData> players = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    players.add(mapResultSetToPlayerData(rs));
                }
            }
            
            return players;
        });
    }
    
    @Override
    public CompletableFuture<Void> update(PlayerData playerData) {
        return databaseManager.executeAsync(connection -> {
            String sql = "UPDATE player_modes SET mode = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerData.getMode() != null ? playerData.getMode().getDisplayName() : null);
                stmt.setString(2, playerData.getUuid().toString());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("No player found with UUID: " + playerData.getUuid());
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteById(UUID uuid) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM player_modes WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> existsById(UUID uuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT 1 FROM player_modes WHERE uuid = ?";
            
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
            String sql = "SELECT COUNT(*) FROM player_modes";
            
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
     * Finds all players with a specific mode
     * @param mode The player mode to search for
     * @return CompletableFuture containing a list of players with the specified mode
     */
    public CompletableFuture<List<PlayerData>> findByMode(PlayerMode mode) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, mode, first_join, last_mode_change FROM player_modes WHERE mode = ?";
            List<PlayerData> players = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, mode.getDisplayName());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        players.add(mapResultSetToPlayerData(rs));
                    }
                }
            }
            
            return players;
        });
    }
    
    /**
     * Finds all players who haven't selected a mode yet
     * @return CompletableFuture containing a list of players without a mode
     */
    public CompletableFuture<List<PlayerData>> findPlayersWithoutMode() {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT uuid, mode, first_join, last_mode_change FROM player_modes WHERE mode IS NULL";
            List<PlayerData> players = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    players.add(mapResultSetToPlayerData(rs));
                }
            }
            
            return players;
        });
    }
    
    /**
     * Updates only the player's mode
     * @param uuid The player's UUID
     * @param mode The new mode to set
     * @return CompletableFuture that completes when the update is done
     */
    public CompletableFuture<Void> updatePlayerMode(UUID uuid, PlayerMode mode) {
        return databaseManager.executeAsync(connection -> {
            String sql = "UPDATE player_modes SET mode = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, mode != null ? mode.getDisplayName() : null);
                stmt.setString(2, uuid.toString());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("No player found with UUID: " + uuid);
                }
            }
        });
    }
    
    /**
     * Maps a ResultSet row to a PlayerData object
     * @param rs The ResultSet positioned at a valid row
     * @return The mapped PlayerData object
     * @throws SQLException if there's an error reading from the ResultSet
     */
    private PlayerData mapResultSetToPlayerData(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String modeStr = rs.getString("mode");
        PlayerMode mode = modeStr != null ? PlayerMode.fromDisplayName(modeStr) : null;
        LocalDateTime firstJoin = rs.getTimestamp("first_join").toLocalDateTime();
        Timestamp lastModeChangeTimestamp = rs.getTimestamp("last_mode_change");
        LocalDateTime lastModeChange = lastModeChangeTimestamp != null ? 
            lastModeChangeTimestamp.toLocalDateTime() : null;
        
        return new PlayerData(uuid, mode, firstJoin, lastModeChange);
    }
}