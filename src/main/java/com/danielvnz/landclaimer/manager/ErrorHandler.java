package com.danielvnz.landclaimer.manager;

import com.danielvnz.landclaimer.LandClaimerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Centralized error handling and user feedback system.
 * Provides consistent error messages and logging across the plugin.
 */
public class ErrorHandler {
    
    private final LandClaimerPlugin plugin;
    
    public ErrorHandler(LandClaimerPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles database errors with appropriate logging and user feedback
     * @param player The player to send error message to (can be null)
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    public void handleDatabaseError(Player player, String operation, Throwable error) {
        // Log the error
        plugin.getLogger().log(Level.SEVERE, "Database error during " + operation, error);
        
        // Send user-friendly message to player if available
        if (player != null) {
            sendErrorMessage(player, "A database error occurred. Please try again later.");
        }
        
        // Log additional context
        if (error instanceof SQLException) {
            SQLException sqlError = (SQLException) error;
            plugin.getLogger().severe("SQL Error Code: " + sqlError.getErrorCode());
            plugin.getLogger().severe("SQL State: " + sqlError.getSQLState());
        }
    }
    
    /**
     * Handles configuration errors
     * @param player The player to send error message to (can be null)
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    public void handleConfigurationError(Player player, String operation, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "Configuration error during " + operation, error);
        
        if (player != null) {
            sendErrorMessage(player, "A configuration error occurred. Please contact an administrator.");
        }
    }
    
    /**
     * Handles permission errors
     * @param player The player who lacks permission
     * @param permission The permission that was required
     */
    public void handlePermissionError(Player player, String permission) {
        plugin.getLogger().info("Player " + player.getName() + " attempted to use command without permission: " + permission);
        
        sendErrorMessage(player, "You don't have permission to use this command!");
    }
    
    /**
     * Handles validation errors
     * @param player The player to send error message to
     * @param message The validation error message
     */
    public void handleValidationError(Player player, String message) {
        plugin.getLogger().info("Validation error for player " + player.getName() + ": " + message);
        sendErrorMessage(player, message);
    }
    
    /**
     * Handles general plugin errors
     * @param player The player to send error message to (can be null)
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    public void handlePluginError(Player player, String operation, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "Plugin error during " + operation, error);
        
        if (player != null) {
            sendErrorMessage(player, "An unexpected error occurred. Please try again later.");
        }
    }
    
    /**
     * Handles async operation errors
     * @param operation The operation that failed
     * @param error The exception that occurred
     */
    public void handleAsyncError(String operation, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "Async operation error during " + operation, error);
        
        // Check if it's a CompletionException and unwrap it
        if (error instanceof CompletionException && error.getCause() != null) {
            plugin.getLogger().severe("Root cause: " + error.getCause().getMessage());
        }
    }
    
    /**
     * Sends a standardized error message to a player
     * @param player The player to send the message to
     * @param message The error message
     */
    public void sendErrorMessage(Player player, String message) {
        if (player != null && player.isOnline()) {
            Component errorComponent = Component.text("❌ ", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.RED));
            player.sendMessage(errorComponent);
        }
    }
    
    /**
     * Sends a standardized success message to a player
     * @param player The player to send the message to
     * @param message The success message
     */
    public void sendSuccessMessage(Player player, String message) {
        if (player != null && player.isOnline()) {
            Component successComponent = Component.text("✅ ", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.GREEN));
            player.sendMessage(successComponent);
        }
    }
    
    /**
     * Sends a standardized warning message to a player
     * @param player The player to send the message to
     * @param message The warning message
     */
    public void sendWarningMessage(Player player, String message) {
        if (player != null && player.isOnline()) {
            Component warningComponent = Component.text("⚠️ ", NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.YELLOW));
            player.sendMessage(warningComponent);
        }
    }
    
    /**
     * Sends a standardized info message to a player
     * @param player The player to send the message to
     * @param message The info message
     */
    public void sendInfoMessage(Player player, String message) {
        if (player != null && player.isOnline()) {
            Component infoComponent = Component.text("ℹ️ ", NamedTextColor.AQUA)
                .append(Component.text(message, NamedTextColor.WHITE));
            player.sendMessage(infoComponent);
        }
    }
    
    /**
     * Validates input parameters and throws appropriate errors
     * @param player The player making the request
     * @param parameter The parameter value
     * @param parameterName The name of the parameter
     * @param minLength Minimum length (0 to skip)
     * @param maxLength Maximum length (0 to skip)
     * @return true if validation passes
     * @throws IllegalArgumentException if validation fails
     */
    public boolean validateInput(Player player, String parameter, String parameterName, int minLength, int maxLength) {
        if (parameter == null || parameter.trim().isEmpty()) {
            handleValidationError(player, parameterName + " cannot be empty!");
            return false;
        }
        
        String trimmed = parameter.trim();
        
        if (minLength > 0 && trimmed.length() < minLength) {
            handleValidationError(player, parameterName + " must be at least " + minLength + " characters long!");
            return false;
        }
        
        if (maxLength > 0 && trimmed.length() > maxLength) {
            handleValidationError(player, parameterName + " must be no more than " + maxLength + " characters long!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates numeric input
     * @param player The player making the request
     * @param input The input string
     * @param parameterName The name of the parameter
     * @param minValue Minimum value (null to skip)
     * @param maxValue Maximum value (null to skip)
     * @return The parsed integer, or null if validation fails
     */
    public Integer validateNumericInput(Player player, String input, String parameterName, Integer minValue, Integer maxValue) {
        if (input == null || input.trim().isEmpty()) {
            handleValidationError(player, parameterName + " cannot be empty!");
            return null;
        }
        
        try {
            int value = Integer.parseInt(input.trim());
            
            if (minValue != null && value < minValue) {
                handleValidationError(player, parameterName + " must be at least " + minValue + "!");
                return null;
            }
            
            if (maxValue != null && value > maxValue) {
                handleValidationError(player, parameterName + " must be no more than " + maxValue + "!");
                return null;
            }
            
            return value;
            
        } catch (NumberFormatException e) {
            handleValidationError(player, parameterName + " must be a valid number!");
            return null;
        }
    }
    
    /**
     * Logs plugin statistics for debugging
     */
    public void logPluginStats() {
        plugin.getLogger().info("=== Plugin Statistics ===");
        plugin.getLogger().info("Online players: " + plugin.getServer().getOnlinePlayers().size());
        plugin.getLogger().info("Plugin initialized: " + plugin.isInitialized());
        
        if (plugin.getConfigurationManager() != null) {
            plugin.getLogger().info(plugin.getConfigurationManager().getConfigStats());
        }
    }
}
