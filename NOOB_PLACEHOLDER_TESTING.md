# Noob Placeholder Testing Guide

This guide provides quick steps to test the new `%frontierguard_noob%` placeholder functionality.

## Placeholder Behavior

The `%frontierguard_noob%` placeholder:
- Returns "Noob" for players within their first 30 minutes on the server
- Returns "Noob" for players manually marked by admins using `/fg noob <player>`
- Returns empty string for all other players
- Manual noob status expires after 30 minutes

## PVP Protection for Noob Players

Players with "Noob" status (both automatic and manual) receive special PVP protection:
- **Noob players cannot attack other players** outside of PVP zones
- **Noob players cannot be attacked by other players** outside of PVP zones
- **PVP is still allowed in designated PVP areas** for all players
- **Only applies to Normal mode players** (Peaceful players already have PVP protection)
- **Protection works for both new players and manually marked noobs**

## Database Persistence

Manual noob status is now stored in the database:
- **Survives server restarts** - Noob status continues counting down even after server restart
- **Accurate time tracking** - Uses real-world time, not server uptime
- **Automatic cleanup** - Expired noob statuses are automatically removed from the database
- **Performance optimized** - Uses in-memory cache for quick lookups with database backup

## Testing Steps

### 1. Test with New Players

**Automatic Noob Status for New Players:**
```bash
# Test the placeholder for a new player (joined < 30 minutes ago)
/papi parse <newplayer> %frontierguard_noob%
# Expected: "Noob"

# Test the placeholder for an existing player (joined > 30 minutes ago)
/papi parse <existingplayer> %frontierguard_noob%
# Expected: "" (empty)
```

### 2. Test Manual Noob Commands

**Mark a Player as Noob:**
```bash
# Mark a player as noob (requires frontierguard.rep.admin permission)
/fg noob <playername>
# Expected: Success message and player notified

# Test the placeholder for the marked player
/papi parse <playername> %frontierguard_noob%
# Expected: "Noob"
```

**Try to Mark Already Noob Player:**
```bash
# Try to mark the same player again
/fg noob <playername>
# Expected: Message saying player is already marked with remaining time
```

### 3. Test Permission Requirements

**Without Admin Permission:**
```bash
# Try to use noob command without frontierguard.rep.admin permission
/fg noob <playername>
# Expected: "You don't have permission to use this command!"
```

**View Help:**
```bash
# Check if noob command appears in help (with permission)
/fg help
# Expected: "/frontierguard noob <player> - Mark a player as noob for 30 minutes" in admin section
```

### 4. Test Time Expiration

**Check Remaining Time:**
```bash
# Mark a player as noob
/fg noob <playername>

# Wait a few minutes and check again
/fg noob <playername>
# Expected: Message showing remaining time

# Test placeholder during waiting period
/papi parse <playername> %frontierguard_noob%
# Expected: "Noob"

# After 30 minutes, test again
/papi parse <playername> %frontierguard_noob%
# Expected: "" (empty)
```

### 5. Test PVP Protection

**Noob Player Cannot Attack:**
```bash
# Mark a player as noob
/fg noob <playername>

# Have the noob player try to attack another player (outside PVP zone)
# Expected: "You cannot attack other players while you have the noob status!"
```

**Cannot Attack Noob Player:**
```bash
# Mark a player as noob
/fg noob <playername>

# Have another player try to attack the noob player (outside PVP zone)
# Expected: "You cannot attack players with noob status outside of PVP areas!"
```

**PVP Allowed in PVP Zones:**
```bash
# Mark a player as noob
/fg noob <playername>

# Have noob player attack another player inside a PVP zone
# Expected: PVP should work normally (no protection message)
```

**New Player Protection:**
```bash
# Test with a new player (joined < 30 minutes ago)
# Have new player try to attack another player (outside PVP zone)
# Expected: "You cannot attack other players while you have the noob status!"

# Have another player try to attack the new player (outside PVP zone)
# Expected: "You cannot attack players with noob status outside of PVP areas!"
```

### 6. Test Edge Cases

**Invalid Player:**
```bash
# Try to mark non-existent player
/fg noob NonExistentPlayer
# Expected: "Player 'NonExistentPlayer' not found!"
```

**Missing Arguments:**
```bash
# Use command without player name
/fg noob
# Expected: "Usage: /fg noob <player>"
```

**Tab Completion:**
```bash
# Type command and press Tab
/fg no<TAB>
# Expected: Completes to "noob" if you have permission
```

## Integration Testing

### Test in Tab Lists
Configure your tab list plugin to show the noob status:
```yaml
# Example for tab list plugins
header: 
  - "&7Players Online"
player-format: "%frontierguard_noob% &f%player_name%"
```

### Test in Chat Prefixes
Configure your chat plugin to show noob status:
```yaml
# Example for chat plugins
format: "%frontierguard_noob% &f%player_name%: %message%"
```

### Test in Scoreboards
Configure your scoreboard plugin:
```yaml
# Example scoreboard entry
- "&eStatus: %frontierguard_noob%"
```

## Expected Results Summary

| Scenario | Placeholder Result | Command Result | PVP Protection |
|----------|-------------------|----------------|----------------|
| New player (< 30 min) | `Noob` | N/A | Protected from PVP |
| Existing player (> 30 min) | `` (empty) | N/A | Normal PVP rules |
| Manually marked player | `Noob` | Success message | Protected from PVP |
| Player after 30 min manual mark | `` (empty) | N/A | Normal PVP rules |
| Admin marks already marked player | `Noob` | "Already marked" message | Protected from PVP |
| Non-admin tries command | `` (empty) | Permission error | N/A |
| Invalid player name | `` (empty) | Player not found error | N/A |
| Noob player in PVP zone | `Noob` | N/A | PVP allowed |
| Noob player outside PVP zone | `Noob` | N/A | PVP blocked |

## Troubleshooting

**Placeholder returns empty for new players:**
- Check if PlaceholderAPI is installed and registered
- Verify the player actually joined within the last 30 minutes
- Check server logs for any errors

**Command not working:**
- Verify user has `frontierguard.rep.admin` permission
- Check if FrontierGuard plugin is loaded properly
- Ensure command syntax is correct: `/fg noob <playername>`

**Noob status not expiring:**
- Manual noob status is now stored in the database and persists through server restarts
- Check system time is correct
- Verify there are no errors in server logs
- Check database connection is working properly

**PVP protection not working:**
- Verify the player is actually a noob (check placeholder)
- Ensure the player is in Normal mode (Peaceful players already have PVP protection)
- Check if the attack is happening outside a PVP zone
- Verify PvpProtectionManager is working correctly
- Check server logs for any errors in PVP protection system
