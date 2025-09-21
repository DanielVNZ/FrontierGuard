package com.danielvnz.landclaimer.database.dao;

import com.danielvnz.landclaimer.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing purchased claims in the database.
 * Handles CRUD operations for player purchased claim counts.
 */
public class PurchasedClaimsDao {
    private static final Logger LOGGER = Logger.getLogger(PurchasedClaimsDao.class.getName());
    
    private final DatabaseManager databaseManager;
    
    public PurchasedClaimsDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Gets the number of purchased claims for a player
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing the purchased claim count
     */
    public CompletableFuture<Integer> getPurchasedClaims(UUID playerUuid) {
        return databaseManager.queryAsync(connection -> {
            String sql = "SELECT purchased_count FROM purchased_claims WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("purchased_count");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error getting purchased claims for player: " + playerUuid, e);
            }
            
            return 0; // Default to 0 if no record found
        });
    }
    
    /**
     * Sets the number of purchased claims for a player
     * @param playerUuid The player's UUID
     * @param purchasedCount The number of purchased claims
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> setPurchasedClaims(UUID playerUuid, int purchasedCount) {
        return databaseManager.executeAsync(connection -> {
            String sql = """
                INSERT OR REPLACE INTO purchased_claims (uuid, purchased_count, last_purchase)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setInt(2, purchasedCount);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error setting purchased claims for player: " + playerUuid, e);
                throw new RuntimeException("Failed to update purchased claims", e);
            }
        });
    }
    
    /**
     * Increments the purchased claims count for a player
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing the new purchased claim count
     */
    public CompletableFuture<Integer> incrementPurchasedClaims(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            // First, get current count
            return getPurchasedClaims(playerUuid).thenCompose(currentCount -> {
                int newCount = currentCount + 1;
                return setPurchasedClaims(playerUuid, newCount).thenApply(v -> newCount);
            }).join();
        });
    }
    
    /**
     * Deletes purchased claims record for a player
     * @param playerUuid The player's UUID
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> deletePurchasedClaims(UUID playerUuid) {
        return databaseManager.executeAsync(connection -> {
            String sql = "DELETE FROM purchased_claims WHERE uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error deleting purchased claims for player: " + playerUuid, e);
                throw new RuntimeException("Failed to delete purchased claims", e);
            }
        });
    }
}
