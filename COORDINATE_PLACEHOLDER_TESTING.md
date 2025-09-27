# Coordinate Placeholder Testing Guide

## New Placeholder: Live Coordinates for -15 Reputation Players

### Available Placeholders
- `%frontierguard_coords%`
- `%frontierguard_coordinates%` 
- `%frontierguard_location%`

All three placeholders work identically.

## Testing Scenarios

### 1. Basic Functionality Test

**Setup:**
1. Set a player's reputation to -15: `/fg setrep PlayerName -15`
2. Add placeholder to tab list or scoreboard: `%frontierguard_coords%`

**Expected Result:**
- Player should see their live coordinates in format: `(100, 64, -200)`
- Coordinates should update in real-time as player moves

### 2. Reputation Threshold Test

**Test Cases:**
- Player with reputation -14: `%frontierguard_coords%` → (empty)
- Player with reputation -15: `%frontierguard_coords%` → `(x, y, z)`
- Player with reputation -16: `%frontierguard_coords%` → (empty, clamped to -15)

**Commands to Test:**
```
/fg setrep PlayerName -14
/papi parse PlayerName %frontierguard_coords%

/fg setrep PlayerName -15  
/papi parse PlayerName %frontierguard_coords%

/fg setrep PlayerName -16
/papi parse PlayerName %frontierguard_coords%
```

### 3. Real-Time Update Test

**Setup:**
1. Set player reputation to -15
2. Add placeholder to tab list header
3. Have player move around the world

**Expected Result:**
- Coordinates should update immediately as player moves
- Format should remain consistent: `(x, y, z)`
- No lag or delays in updates

### 4. Integration Test with Tab List

**Configuration Example (TabList plugin):**
```yaml
header:
  - "&6Welcome &f%player_name%"
  - "&7Mode: &f%frontierguard_mode_display%"
  - "&7Rep: %frontierguard_reputation_full%"
  - "%frontierguard_coords%"
```

**Expected Behavior:**
- Players with -15 reputation: See coordinates line
- All other players: Empty line (or no coordinates line)

### 5. Integration Test with Scoreboard

**Configuration Example (SidebarAPI):**
```yaml
lines:
  - "&6Player: &f%player_name%"
  - "&6Mode: &f%frontierguard_mode_display%"
  - "&6Reputation: %frontierguard_reputation_full%"
  - "%frontierguard_coords%"
  - "&6World: &f%world_name%"
```

**Expected Behavior:**
- Coordinates line appears only for -15 reputation players
- Updates in real-time as player moves

### 6. Edge Case Testing

**Test Cases:**
1. **Player in different worlds:** Coordinates should show current world coords
2. **Player teleporting:** Coordinates should update immediately
3. **Player logging out/in:** Coordinates should resume correctly
4. **Reputation changes:** Coordinates should appear/disappear immediately

**Commands:**
```
/tp PlayerName 1000 100 1000
/fg setrep PlayerName -15
/fg setrep PlayerName -14
```

### 7. Performance Test

**Setup:**
1. Set 10+ players to -15 reputation
2. Add coordinate placeholder to tab list
3. Have all players move simultaneously

**Expected Result:**
- No noticeable lag or performance impact
- All coordinates update smoothly
- Server remains stable

## Troubleshooting

### Coordinates Not Showing
1. Check player reputation: `/fg rep PlayerName`
2. Verify reputation is exactly -15
3. Test placeholder: `/papi parse PlayerName %frontierguard_coords%`
4. Check if PlaceholderAPI is working: `/papi list frontierguard`

### Coordinates Not Updating
1. Verify the plugin using placeholders supports real-time updates
2. Check if the plugin caches placeholders (some plugins cache for performance)
3. Test with `/papi parse` command to see if placeholder itself updates

### Empty Coordinates
1. Check if player has valid location
2. Verify player is not in a null world
3. Test with different players

## Example Use Cases

### 1. Admin Tracking
```yaml
# In admin scoreboard
lines:
  - "&cWanted Players:"
  - "%frontierguard_coords%"
```

### 2. Public Shaming
```yaml
# In public tab list
header:
  - "&cWanted: %player_name%"
  - "%frontierguard_coords%"
```

### 3. Moderation Tool
```yaml
# In staff scoreboard
lines:
  - "&6Player: &f%player_name%"
  - "&6Reputation: %frontierguard_reputation_full%"
  - "%frontierguard_coords%"
```

## Notes

- Coordinates are block coordinates (integers)
- Updates are real-time and efficient
- Only works for players with exactly -15 reputation
- Returns empty string for all other reputation levels
- Compatible with all PlaceholderAPI-supported plugins
