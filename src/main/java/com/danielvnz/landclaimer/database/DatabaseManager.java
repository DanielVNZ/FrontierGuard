package com.danielvnz.landclaimer.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connections and schema creation for the LandClaimer plugin.
 * Handles SQLite database initialization, connection pooling, and error handling.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    
    private final String databasePath;
    private final ExecutorService executor;
    private Connection connection;
    private boolean initialized = false;
    
    /**
     * Creates a new DatabaseManager instance
     * @param dataFolder The plugin's data folder where the database will be stored
     */
    public DatabaseManager(File dataFolder) {
        this.databasePath = new File(dataFolder, "landclaimer.db").getAbsolutePath();
        this.executor = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Creates a DatabaseManager for testing with a specific database URL
     * @param databaseUrl The JDBC URL for the database connection
     */
    public DatabaseManager(String databaseUrl) {
        this.databasePath = databaseUrl;
        this.executor = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Initializes the database connection and creates tables if they don't exist
     * @return CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create connection
                String url = databasePath.startsWith("jdbc:") ? databasePath : "jdbc:sqlite:" + databasePath;
                connection = DriverManager.getConnection(url);
                
                // Enable foreign keys for SQLite
                if (url.contains("sqlite")) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("PRAGMA foreign_keys = ON");
                    }
                }
                
                // Create tables
                createTables();
                initialized = true;
                
                LOGGER.info("Database initialized successfully at: " + databasePath);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        }, executor);
    }
    
    /**
     * Creates the database tables if they don't exist
     * @throws SQLException if table creation fails
     */
    private void createTables() throws SQLException {
        boolean isH2 = databasePath.contains("h2:");
        createPlayerModesTable();
        createLandClaimsTable(isH2);
        createPvpAreasTable(isH2);
        createPurchasedClaimsTable();
        createClaimInvitationsTable();
        createPlayerReputationTable();
        createNoobStatusTable();
    }
    
    /**
     * Creates the player_modes table
     * @throws SQLException if table creation fails
     */
    private void createPlayerModesTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_modes (
                uuid VARCHAR(36) PRIMARY KEY,
                mode VARCHAR(10),
                first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_mode_change TIMESTAMP
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created player_modes table");
        }
    }
    
    /**
     * Creates the land_claims table
     * @param isH2 Whether this is an H2 database (uses different syntax)
     * @throws SQLException if table creation fails
     */
    private void createLandClaimsTable(boolean isH2) throws SQLException {
        String autoIncrement = isH2 ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String sql = """
            CREATE TABLE IF NOT EXISTS land_claims (
                id INTEGER PRIMARY KEY %s,
                owner_uuid VARCHAR(36) NOT NULL,
                world_name VARCHAR(50) NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                claim_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(world_name, chunk_x, chunk_z)
            )
            """.formatted(autoIncrement);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created land_claims table");
        }
    }
    
    /**
     * Creates the pvp_areas table
     * @param isH2 Whether this is an H2 database (uses different syntax)
     * @throws SQLException if table creation fails
     */
    private void createPvpAreasTable(boolean isH2) throws SQLException {
        String autoIncrement = isH2 ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String sql = """
            CREATE TABLE IF NOT EXISTS pvp_areas (
                id INTEGER PRIMARY KEY %s,
                name VARCHAR(50) UNIQUE NOT NULL,
                world_name VARCHAR(50) NOT NULL,
                min_x INTEGER NOT NULL,
                min_y INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                max_y INTEGER NOT NULL,
                max_z INTEGER NOT NULL
            )
            """.formatted(autoIncrement);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created pvp_areas table");
        }
    }
    
    /**
     * Creates the purchased_claims table
     * @throws SQLException if table creation fails
     */
    private void createPurchasedClaimsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS purchased_claims (
                uuid VARCHAR(36) PRIMARY KEY,
                purchased_count INTEGER NOT NULL DEFAULT 0,
                last_purchase TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created purchased_claims table");
        }
    }
    
    /**
     * Creates the claim_invitations table
     * @throws SQLException if table creation fails
     */
    private void createClaimInvitationsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS claim_invitations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                claim_id INTEGER NOT NULL,
                invited_uuid VARCHAR(36) NOT NULL,
                invited_by_uuid VARCHAR(36) NOT NULL,
                can_build BOOLEAN NOT NULL DEFAULT 1,
                can_access_containers BOOLEAN NOT NULL DEFAULT 1,
                can_manage_invitations BOOLEAN NOT NULL DEFAULT 0,
                invitation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(claim_id, invited_uuid),
                FOREIGN KEY (claim_id) REFERENCES land_claims(id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created claim_invitations table");
        }
    }
    
    /**
     * Creates the player_reputation table
     * @throws SQLException if table creation fails
     */
    private void createPlayerReputationTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_reputation (
                uuid VARCHAR(36) PRIMARY KEY,
                reputation INTEGER DEFAULT 0 CHECK (reputation >= -15 AND reputation <= 15),
                last_playtime_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_playtime_hours REAL DEFAULT 0.0
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created player_reputation table");
        }
    }
    
    /**
     * Creates the noob_status table
     * @throws SQLException if table creation fails
     */
    private void createNoobStatusTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS noob_status (
                uuid VARCHAR(36) PRIMARY KEY,
                expiration_time TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.fine("Created noob_status table");
        }
    }
    
    /**
     * Gets a database connection
     * @return The database connection
     * @throws SQLException if connection is not available
     */
    public Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database not initialized");
        }
        if (connection == null || connection.isClosed()) {
            // Reconnect if connection was closed
            String url = databasePath.startsWith("jdbc:") ? databasePath : "jdbc:sqlite:" + databasePath;
            connection = DriverManager.getConnection(url);
            
            // Enable foreign keys for SQLite
            if (url.contains("sqlite")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
            }
        }
        return connection;
    }
    
    /**
     * Executes a database operation asynchronously
     * @param operation The operation to execute
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> executeAsync(DatabaseOperation operation) {
        return CompletableFuture.runAsync(() -> {
            try {
                Connection conn = getConnection();
                operation.execute(conn);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database operation failed", e);
                throw new RuntimeException("Database operation failed", e);
            }
        }, executor);
    }
    
    /**
     * Executes a database query asynchronously
     * @param <T> The return type of the query
     * @param query The query to execute
     * @return CompletableFuture containing the query result
     */
    public <T> CompletableFuture<T> queryAsync(DatabaseQuery<T> query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = getConnection();
                return query.execute(conn);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database query failed", e);
                throw new RuntimeException("Database query failed", e);
            }
        }, executor);
    }
    
    /**
     * Checks if the database is initialized and ready for use
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Closes the database connection and shuts down the executor
     */
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing database connection", e);
        }
        
        executor.shutdown();
        initialized = false;
    }
    
    /**
     * Functional interface for database operations that don't return a value
     */
    @FunctionalInterface
    public interface DatabaseOperation {
        void execute(Connection connection) throws SQLException;
    }
    
    /**
     * Functional interface for database queries that return a value
     * @param <T> The return type of the query
     */
    @FunctionalInterface
    public interface DatabaseQuery<T> {
        T execute(Connection connection) throws SQLException;
    }
}