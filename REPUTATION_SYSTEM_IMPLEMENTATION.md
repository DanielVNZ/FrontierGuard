# Reputation System Implementation Plan

## Overview
This document outlines the implementation of a reputation system for the FrontierGuard Minecraft plugin. The reputation system will track player behavior in PVP scenarios and provide visual feedback through the GUI.

## Requirements Summary
- **Reputation Scale**: -15 to +15 (starts at 0)
- **Player Scope**: Normal players only (not peaceful players)
- **Reputation Changes**:
  - -1 reputation: Normal player kills another normal player outside PVP zone
  - +1 reputation per hour of playing time (regeneration)
- **GUI Integration**: New icon in `/fg gui` showing current reputation
- **Admin Commands**: Commands to manually adjust player reputation

## Implementation Components

### 1. Database Layer
- **New Table**: `player_reputation`
  - `uuid` (VARCHAR, PRIMARY KEY)
  - `reputation` (INTEGER, DEFAULT 0)
  - `last_playtime_update` (TIMESTAMP)
  - `total_playtime_hours` (REAL)

- **New DAO**: `PlayerReputationDao`
  - Implements `BaseDao<PlayerReputation, UUID>`
  - Methods for reputation management and playtime tracking

### 2. Model Layer
- **New Class**: `PlayerReputation`
  - Fields: UUID, reputation value, last playtime update, total playtime
  - Validation methods for reputation bounds (-15 to +15)
  - Helper methods for reputation changes

### 3. Manager Layer
- **New Class**: `ReputationManager`
  - Handles reputation calculations and updates
  - Manages playtime tracking and reputation regeneration
  - Integrates with existing PlayerModeManager
  - Provides methods for PVP reputation changes

### 4. GUI Integration
- **Modified**: `GuiManager`
  - Add new reputation slot constant
  - Create reputation display item
  - Update GUI layout to include reputation icon
  - Handle reputation slot clicks (if needed)

### 5. Event Handling
- **Modified**: `PvpProtectionListener` or new listener
  - Track PVP kills outside PVP zones
  - Apply reputation penalties for normal players
  - Integrate with existing PVP protection system

### 6. Command Integration
- **Modified**: `LandClaimerCommandExecutor`
  - Add admin commands: `/fg setrep <player> <amount>`, `/fg addrep <player> <amount>`
  - Add player command: `/fg rep` (show own reputation)
  - Permission checks for admin commands

### 7. Playtime Tracking
- **New System**: Playtime tracking for reputation regeneration
- **Integration**: With existing player join/leave events
- **Scheduling**: Hourly reputation regeneration task

## Technical Details

### Database Schema
```sql
CREATE TABLE IF NOT EXISTS player_reputation (
    uuid VARCHAR(36) PRIMARY KEY,
    reputation INTEGER DEFAULT 0 CHECK (reputation >= -15 AND reputation <= 15),
    last_playtime_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_playtime_hours REAL DEFAULT 0.0
);
```

### Reputation Calculation Logic
1. **PVP Penalty**: When normal player kills another normal player outside PVP zone
   - Check if both players are normal mode
   - Check if location is outside PVP area
   - Apply -1 reputation penalty
   - Ensure reputation stays within bounds

2. **Playtime Regeneration**: Every hour of playtime
   - Track player session time
   - Add +1 reputation per hour
   - Cap at maximum reputation (+15)

### GUI Slot Assignment
- Current slots: 10, 12, 14, 16, 21, 22
- New reputation slot: 13 (between buy claims and claim info)
- Icon: Diamond with reputation value in lore

### Permission System
- `frontierguard.rep.admin` - Admin reputation commands
- `frontierguard.rep.view` - View reputation (default for all players)

## Implementation Order
1. Create PlayerReputation model class
2. Create PlayerReputationDao
3. Update DatabaseManager to create reputation table
4. Create ReputationManager
5. Update GuiManager to include reputation display
6. Add reputation tracking to PVP events
7. Add admin commands for reputation management
8. Implement playtime tracking system
9. Add reputation regeneration scheduler
10. Testing and validation

## Configuration Options
- Reputation bounds (min/max values)
- Regeneration rate (reputation per hour)
- PVP penalty amount
- GUI display options

## Testing Scenarios
1. Normal player kills another normal player outside PVP zone → -1 rep
2. Normal player kills another normal player in PVP zone → no change
3. Peaceful player actions → no reputation changes
4. Playtime accumulation → +1 rep per hour
5. Admin commands → manual reputation adjustments
6. GUI display → correct reputation shown
7. Reputation bounds → cannot exceed -15 to +15

## Future Enhancements
- Reputation-based rewards/penalties
- Reputation decay over time
- Reputation leaderboards
- Reputation-based permissions
- Integration with other plugin systems
