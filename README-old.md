# FrontierGuard - Land Claiming Plugin

A comprehensive Minecraft plugin for land claiming with PvP mechanics, player modes, and advanced protection systems.

## 📋 Dependencies

**Required Dependencies:**
- **Vault** - Economy integration and permission system
- **EssentialsX** (or other economy plugin) - Economy functionality for claim purchasing
  - *Note: Only tested with EssentialsX + Vault combination*

**Server Requirements:**
- **Paper/Spigot 1.20+** - Server software
- **Java 21+** - Runtime environment

## 📋 Table of Contents

- [Dependencies](#dependencies)
- [Features](#features)
- [Installation](#installation)
- [Player Modes](#player-modes)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [GUI System](#gui-system)
- [PvP Areas](#pvp-areas)
- [Visual Effects](#visual-effects)
- [Database](#database)
- [API Version](#api-version)

## ✨ Features

- **Dual Player Modes**: Peaceful (PvE with land claiming) and Normal (Full PvP)
- **Land Claiming System**: Claim chunks to protect your builds
- **Player Invitations**: Invite others to your claims with customizable permissions
- **PvP Areas**: Designated zones where PvP is always enabled
- **Visual Boundary System**: See claim boundaries with color-coded particles
- **Mode Change Cooldown**: Prevent frequent mode switching (24-hour cooldown)
- **GUI Interface**: User-friendly menu system
- **Admin Tools**: Comprehensive admin commands for server management
- **SQLite Database**: Reliable data storage
- **Customizable Messages**: Full message customization support

## 🚀 Installation

1. **Install Dependencies First:**
   - Install **Vault** plugin
   - Install **EssentialsX** (or another economy plugin)
   - Restart server to load dependencies

2. **Install FrontierGuard:**
   - Download the latest `FrontierGuard-1.0.0.jar` file
   - Place it in your server's `plugins` folder
   - Restart your server

3. **Configure:**
   - Configure the plugin using the generated `config.yml` file
   - Set up permissions using your permission plugin (LuckPerms, etc.)
   - Test economy integration with `/fg gui` (claim purchasing)

## 🎮 Player Modes

### Peaceful Mode
- **PvE Gameplay**: No PvP combat
- **Land Claiming**: Can claim chunks for protection
- **Build Protection**: Protected from griefing and raiding
- **Invitation System**: Can invite others to claims

### Normal Mode
- **Full PvP**: Complete PvP survival experience
- **No Claiming**: Cannot claim or own land
- **Raiding**: Can break blocks and access containers in others' claims
- **No Protection**: Vulnerable to PvP and griefing

## 📝 Commands

### Basic Commands

| Command | Description | Permission | Mode Required |
|---------|-------------|------------|---------------|
| `/fg claim` | Claim the chunk you're standing in | `frontierguard.claim` | Peaceful |
| `/fg unclaim` | Unclaim the chunk you're standing in | `frontierguard.unclaim` | Any |
| `/fg claims` | List all your claims | `frontierguard.claims` | Peaceful |
| `/fg claiminfo` | Show information about the current chunk | `frontierguard.claiminfo` | Any |
| `/fg show` | Show claim boundaries near you for 5 seconds | `frontierguard.claiminfo` | Any |
| `/fg gui` | Open the GUI menu | `frontierguard.gui` | Any |

### Invitation Commands

| Command | Description | Permission | Mode Required |
|---------|-------------|------------|---------------|
| `/fg invite <player>` | Invite a player to your claim | `frontierguard.invite` | Peaceful |
| `/fg invite <landowner> <player>` | Invite a player to landowner's claim | `frontierguard.invite` | Peaceful |
| `/fg uninvite <player>` | Remove a player's access to your claim | `frontierguard.uninvite` | Peaceful |
| `/fg invitations` | View invited players for your claim | `frontierguard.invitations` | Peaceful |

### Mode Commands

| Command | Description | Permission | Mode Required |
|---------|-------------|------------|---------------|
| `/fg setmode <peaceful\|normal>` | Set your own mode | `frontierguard.admin.setmode` | Any |
| `/fg forcemode <player> <peaceful\|normal>` | Force set another player's mode | `frontierguard.admin.forcemode` | Any |
| `/fg cooldown` | Check your mode change cooldown | `frontierguard.cooldown` | Any |

### Admin Commands

| Command | Description | Permission | Mode Required |
|---------|-------------|------------|---------------|
| `/fg adminclaims <player>` | View another player's claims | `frontierguard.admin.claims` | Any |
| `/fg setclaimlimit <group> <limit>` | Set claim limit for permission group | `frontierguard.admin.setclaimlimit` | Any |
| `/fg reload` | Reload plugin configuration | `frontierguard.admin.reload` | Any |

### PvP Area Commands

| Command | Description | Permission | Mode Required |
|---------|-------------|------------|---------------|
| `/fg setpvpzone` | Get PvP area selection tool | `frontierguard.admin.pvparea` | Any |
| `/fg createpvparea <name>` | Create PvP area from selection | `frontierguard.admin.pvparea` | Any |
| `/fg deletepvparea <name>` | Delete a PvP area | `frontierguard.admin.pvparea` | Any |
| `/fg listpvpareas` | List all PvP areas | `frontierguard.admin.pvparea` | Any |

## 🔐 Permissions

### Basic Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `frontierguard.*` | Access to all FrontierGuard commands | `false` |
| `frontierguard.use` | Use the main frontierguard command | `true` |
| `frontierguard.claim` | Claim chunks | `true` |
| `frontierguard.unclaim` | Unclaim chunks | `true` |
| `frontierguard.claims` | List claims | `true` |
| `frontierguard.claiminfo` | Check claim information | `true` |
| `frontierguard.gui` | Open GUI menu | `true` |
| `frontierguard.invite` | Invite players to claims | `true` |
| `frontierguard.uninvite` | Uninvite players from claims | `true` |
| `frontierguard.invitations` | View claim invitations | `true` |
| `frontierguard.cooldown` | Check mode change cooldown | `true` |
| `frontierguard.changemode` | Change player mode via GUI | `true` |

### Admin Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `frontierguard.admin` | Access to admin commands | `op` |
| `frontierguard.admin.pvparea` | Manage PvP areas | `op` |
| `frontierguard.admin.reload` | Reload plugin configuration | `op` |
| `frontierguard.admin.setmode` | Set player modes | `op` |
| `frontierguard.admin.forcemode` | Force player mode changes | `op` |
| `frontierguard.admin.claims` | View all player claims | `op` |
| `frontierguard.admin.setclaimlimit` | Set claim limits | `op` |
| `frontierguard.bypass` | Bypass all protection systems | `op` |

### Claim Limit Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `frontierguard.claims.unlimited` | Unlimited claims | `false` |
| `frontierguard.claims.vip` | VIP claim limit group (25 claims) | `false` |
| `frontierguard.claims.premium` | Premium claim limit group (50 claims) | `false` |
| `frontierguard.claimamount.*` | Unlimited claims (overrides all limits) | `false` |
| `frontierguard.claimamount.10` | Up to 10 claims | `false` |
| `frontierguard.claimamount.20` | Up to 20 claims | `false` |
| `frontierguard.claimamount.50` | Up to 50 claims | `false` |
| `frontierguard.claimamount.100` | Up to 100 claims | `false` |

## ⚙️ Configuration

### Main Configuration (`config.yml`)

```yaml
# Claim limits per permission group
claim-limits:
  default: 1      # Default claim limit for all players
  vip: 1         # Claim limit for VIP players
  premium: 1     # Claim limit for premium players

# Database configuration
database:
  type: sqlite              # Database type (sqlite only)
  file: landclaimer.db     # Database file name

# Mode change cooldown
mode-change:
  cooldown-hours: 24       # Hours between mode changes

# Show command configuration
show:
  radius: 3               # Radius in chunks for /fg show (1-10)

# Customizable messages
messages:
  # All plugin messages can be customized here
  # Use Minecraft color codes with & symbol
```

### Message Customization

All plugin messages can be customized in the `messages` section of `config.yml`. Use Minecraft color codes:

- `&0` = Black, `&1` = Dark Blue, `&2` = Dark Green, `&3` = Dark Aqua
- `&4` = Dark Red, `&5` = Dark Purple, `&6` = Gold, `&7` = Gray
- `&8` = Dark Gray, `&9` = Blue, `&a` = Green, `&b` = Aqua
- `&c` = Red, `&d` = Light Purple, `&e` = Yellow, `&f` = White
- `&k` = Obfuscated, `&l` = Bold, `&m` = Strikethrough, `&n` = Underline, `&o` = Italic, `&r` = Reset

## 🎨 GUI System

The plugin features a comprehensive GUI system accessible via `/fg gui`:

- **Mode Selection**: Choose between Peaceful and Normal modes
- **Claim Management**: View and manage your claims
- **Invitation System**: Manage player invitations
- **Admin Tools**: Access admin functions (if you have permissions)

## ⚔️ PvP Areas

PvP Areas are special zones where PvP is always enabled, regardless of player mode:

- **Creation**: Use `/fg setpvpzone` to get selection tools, then `/fg createpvparea <name>`
- **Management**: List, delete, and manage PvP areas via admin commands
- **Protection**: No building or block breaking allowed in PvP areas
- **Visual Feedback**: Players receive warnings when entering/exiting PvP areas

## 🌈 Visual Effects

### Claim Boundaries (`/fg show`)

- **Green Particles**: Claims you have access to (own or invited)
- **Red Particles**: Claims you don't have access to
- **Terrain Following**: Particles follow the terrain height
- **Player-Specific**: Only visible to the command user
- **Duration**: Shows for 5 seconds
- **Smart Edges**: Doesn't show particles on shared edges between same-owner chunks

### Other Visual Effects

- **Claim Success**: Green particles and success sound when claiming
- **Unclaim Success**: Smoke particles when unclaiming
- **PvP Area Warnings**: Visual and audio warnings when entering PvP areas
- **Mode Change**: Title notifications for mode changes

## 💾 Database

The plugin uses SQLite for data storage:

- **Automatic Setup**: Database is created automatically
- **Location**: Stored in the plugin's data folder
- **Data Stored**: Claims, invitations, player modes, PvP areas
- **Backup**: Regular backups recommended

## 🔧 API Version

- **Minecraft Version**: 1.20+
- **Java Version**: 17+
- **Bukkit API**: 1.20

## 📞 Support

For support, bug reports, or feature requests, please visit:
- **GitHub**: https://github.com/danielvnz/landclaimer
- **Author**: danielvnz

## 📄 License

This plugin is provided as-is. Please respect the terms of use and don't redistribute without permission.

---

**FrontierGuard v1.0.0** - Protecting your world, one chunk at a time! 🛡️
