# PlaceholderAPI Integration for FrontierGuard

## Overview
FrontierGuard now supports PlaceholderAPI placeholders for player reputation and player mode information. This allows other plugins to display FrontierGuard data in their interfaces.

## Available Placeholders

### Reputation Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%frontierguard_reputation%` | Player's reputation number | `5` |
| `%frontierguard_reputation_number%` | Same as above | `5` |
| `%frontierguard_reputation_status%` | Reputation status text | `Good` |
| `%frontierguard_reputation_color%` | Reputation color code | `§2` |
| `%frontierguard_reputation_full%` | Full reputation with color and status | `§25 (Good)` |

### Player Mode Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%frontierguard_mode%` | Player mode (lowercase) | `normal` |
| `%frontierguard_mode_display%` | Player mode (capitalized) | `Normal` |
| `%frontierguard_is_normal%` | True if player is in normal mode | `true` |
| `%frontierguard_is_peaceful%` | True if player is in peaceful mode | `false` |

### Playtime Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%frontierguard_playtime_hours%` | Total playtime in hours | `15.5` |
| `%frontierguard_playtime_formatted%` | Formatted playtime | `15h 30m` |

### Coordinate Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%frontierguard_coords%` | Live coordinates (only for -15 rep players) | `(100, 64, -200)` or empty |
| `%frontierguard_coordinates%` | Same as coords | `(100, 64, -200)` or empty |
| `%frontierguard_location%` | Same as coords | `(100, 64, -200)` or empty |

### Noob Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%frontierguard_noob%` | Shows "Noob" for new players or manually marked players | `Noob` or empty |

### Combined Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%frontierguard_mode_reputation%` | Mode with reputation info | `Normal - §25 (Good)` |

## Reputation Status Levels

| Reputation Range | Status | Color Code |
|------------------|--------|------------|
| 10-15 | Excellent | `§a` (Green) |
| 5-9 | Good | `§2` (Dark Green) |
| 0-4 | Neutral | `§e` (Yellow) |
| -5 to -1 | Poor | `§6` (Gold) |
| -10 to -6 | Bad | `§c` (Red) |
| -15 to -11 | Terrible | `§4` (Dark Red) |

## Usage Examples

### In Chat Plugins (like EssentialsX Chat)
```yaml
# In EssentialsX chat format
format: '&7[%frontierguard_mode_display%] &f%player_name%: %message%'
```

### In Scoreboard Plugins (like SidebarAPI)
```yaml
# In scoreboard configuration
lines:
  - "&6Mode: &f%frontierguard_mode_display%"
  - "&6Reputation: %frontierguard_reputation_full%"
  - "&6Playtime: &f%frontierguard_playtime_formatted%"
```

### In Tab List Plugins (like TabList)
```yaml
# In tab list format
header:
  - "&6Welcome &f%player_name%"
  - "&7Mode: &f%frontierguard_mode_display% &7| Rep: %frontierguard_reputation_full%"
  - "%frontierguard_coords%"
```

### In Action Bar Plugins
```yaml
# Action bar message
message: "&6Mode: &f%frontierguard_mode_display% &7| &6Rep: %frontierguard_reputation_full%"
```

### In Custom Commands
```java
// In plugin code
import me.clip.placeholderapi.PlaceholderAPI;

String message = PlaceholderAPI.setPlaceholders(player, 
    "Your reputation: %frontierguard_reputation_full%");
player.sendMessage(message);
```

## Installation Requirements

1. **PlaceholderAPI Plugin**: Must be installed on the server
2. **FrontierGuard Plugin**: Must be installed and enabled
3. **Other Plugins**: Any plugin that supports PlaceholderAPI can use these placeholders

## Special Coordinate Placeholder Behavior

The coordinate placeholders (`%frontierguard_coords%`, `%frontierguard_coordinates%`, `%frontierguard_location%`) have special behavior:

- **Only shows coordinates for players with -15 reputation**
- **Returns empty string for all other reputation levels**
- **Updates in real-time as the player moves**
- **Format: (x, y, z) where x, y, z are block coordinates**

### Example Behavior:
- Player with reputation 0: `%frontierguard_coords%` → (empty)
- Player with reputation -10: `%frontierguard_coords%` → (empty)
- Player with reputation -15: `%frontierguard_coords%` → `(100, 64, -200)`

This is useful for tracking players with the worst reputation in real-time through tab lists, scoreboards, or other displays.

## Special Noob Placeholder Behavior

The noob placeholder (`%frontierguard_noob%`) has special behavior:

- **Shows "Noob" for players within their first 30 minutes on the server**
- **Shows "Noob" for players manually marked by admins using `/fg noob <player>`**
- **Returns empty string for all other players**
- **Manual noob status lasts for 30 minutes from when it was applied**

### Example Behavior:
- New player (joined 10 minutes ago): `%frontierguard_noob%` → `Noob`
- Player joined 35 minutes ago: `%frontierguard_noob%` → (empty)
- Player manually marked as noob: `%frontierguard_noob%` → `Noob`
- Player after 30 minutes of manual marking: `%frontierguard_noob%` → (empty)

This is useful for identifying new players or players who need extra attention/guidance.

## Testing Placeholders

### Using PlaceholderAPI Commands
```
/papi parse me %frontierguard_reputation%
/papi parse me %frontierguard_mode_display%
/papi parse me %frontierguard_reputation_full%
/papi parse me %frontierguard_coords%
/papi parse me %frontierguard_noob%
```

### Using External Plugins
1. Install a plugin that supports PlaceholderAPI (like EssentialsX, SidebarAPI, etc.)
2. Configure the plugin to use FrontierGuard placeholders
3. Test with different player modes and reputation values

## Troubleshooting

### Placeholders Not Working
1. Check if PlaceholderAPI is installed: `/papi list`
2. Check if FrontierGuard expansion is registered: `/papi list frontierguard`
3. Check server logs for PlaceholderAPI registration messages
4. Ensure the player has a valid mode selected

### Placeholders Show as Literal Text
1. Verify PlaceholderAPI is properly installed
2. Check if the plugin using placeholders supports PlaceholderAPI
3. Ensure the placeholder syntax is correct (case-sensitive)

### Reputation Shows as 0
1. Check if the player is in Normal mode (Peaceful players don't have reputation)
2. Verify the reputation system is working with `/fg rep`
3. Check database for player reputation data

## Advanced Usage

### Conditional Display
Some plugins support conditional placeholders:
```yaml
# Only show reputation for normal players
format: '%frontierguard_is_normal%{&6Rep: %frontierguard_reputation_full%}{}'
```

### Color Coding
Use the color placeholder for custom formatting:
```yaml
# Custom reputation display
format: '%frontierguard_reputation_color%Reputation: %frontierguard_reputation%'
```

## Plugin Compatibility

### Tested Plugins
- ✅ EssentialsX (Chat, Tab, etc.)
- ✅ SidebarAPI
- ✅ TabList
- ✅ DeluxeMenus
- ✅ Any plugin supporting PlaceholderAPI

### Performance Notes
- Placeholders are cached for performance
- Reputation data is loaded asynchronously
- Minimal impact on server performance

## Support

If you encounter issues with PlaceholderAPI integration:
1. Check server logs for error messages
2. Verify PlaceholderAPI version compatibility
3. Test with `/papi parse` commands
4. Report issues with specific placeholder and error details
