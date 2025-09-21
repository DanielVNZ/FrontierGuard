# 🛡️ FrontierGuard - The Ultimate Land Protection Plugin

> **Transform your Minecraft server with the most advanced dual-mode land claiming system!**

[![Version](https://img.shields.io/badge/Version-1.0.2-brightgreen)](https://github.com/DanielVNZ/FrontierGuard)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-blue)](https://minecraft.net)
[![Spigot](https://img.shields.io/badge/Spigot-Compatible-orange)](https://spigotmc.org)

---

## 🌟 **Why Choose FrontierGuard?**

**FrontierGuard** isn't just another land claiming plugin - it's a **complete gameplay transformation** that gives your players the freedom to choose their own adventure! Whether they want peaceful building or intense PvP combat, FrontierGuard delivers both experiences seamlessly.

### 🎯 **Perfect For:**
- **Survival Servers** wanting to offer both PvE and PvP experiences
- **Creative Communities** that need flexible protection systems  
- **RPG Servers** requiring territory control mechanics
- **Hybrid Servers** balancing casual and competitive players

---

## ⚡ **Game-Changing Features**

### 🏰 **Dual Player Modes - Revolutionary Gameplay Choice**
Give your players the **ultimate freedom** to choose their playstyle:

- **🕊️ Peaceful Mode**: Focus on building, exploration, and collaboration
  - **Chunk-based land claiming** with full protection
  - **Invitation system** to share builds with friends
  - **Economy integration** to purchase additional claims
  - **Complete grief protection** for a stress-free experience

- **⚔️ Normal Mode**: Full PvP survival experience
  - **Unrestricted combat** and raiding
  - **No claim limitations** - raid anywhere
  - **Traditional survival** gameplay mechanics
  - **Access to PvP areas** for organized battles

### 💰 **Advanced Economy Integration**
Transform land into a **valuable commodity**:
- **Scalable pricing**: First claim $1,000, second $2,000, third $3,000...
- **Vault integration** works with any economy plugin
- **Purchase additional claims** beyond the base limit
- **Permission bonuses** stack with purchased claims
- **No refunds policy** creates strategic decision-making

### 🎨 **Stunning Visual Effects**
**See your territory** like never before:
- **Color-coded boundaries**: Green for accessible, red for restricted
- **Terrain-following particles** that adapt to landscape
- **Player-specific visibility** - only you see your boundaries
- **Smart edge detection** - no overlapping particles on shared borders
- **5-second duration** with continuous particle streams

### 🏛️ **Intuitive GUI System**
**Professional-grade interface** that players love:
- **One-click mode switching** with confirmation dialogs
- **Real-time claim purchasing** with instant feedback
- **Invitation management** with permission controls
- **Live statistics** showing claims, limits, and pricing
- **Helpful tooltips** guiding players to commands

### ⚔️ **Dynamic PvP Areas**
Create **epic battle zones** with precision:
- **WorldEdit-style selection** for precise area creation
- **Named PvP zones** with custom boundaries
- **Universal PvP enforcement** regardless of player mode
- **Entry/exit notifications** with visual warnings
- **Admin management tools** for easy zone control

---

## 🚀 **Advanced Features That Set Us Apart**

### 🔐 **Sophisticated Permission System**
- **Granular controls** for every aspect of the plugin
- **Permission stacking** - bonuses ADD to purchased claims
- **Role-based limits** for VIP/Premium players
- **Admin bypass** for server management
- **Flexible inheritance** supporting any permission plugin

### 🛠️ **Comprehensive Admin Tools**
- **Player mode management** with force-change capabilities
- **Claim oversight** - view any player's claims
- **PvP area administration** with full CRUD operations
- **Configuration hot-reload** without server restart
- **Debug commands** for troubleshooting

### 🔄 **Smart Update System**
- **Automatic update checking** from SpigotMC
- **In-game notifications** for admins
- **Configurable check intervals** to reduce API calls
- **Direct download links** for easy updates

### 📊 **Robust Data Management**
- **SQLite database** with automatic schema creation
- **Asynchronous operations** for lag-free performance
- **Data integrity** with foreign key constraints
- **Automatic backup compatibility** with standard tools

---

## 💎 **Premium User Experience**

### 🎮 **Player-Friendly Design**
- **Intuitive commands** with helpful error messages
- **Tab completion** for all commands and arguments
- **Visual feedback** for every action
- **Contextual help** system guiding new users
- **Smooth transitions** between modes and states

### 🔊 **Rich Audio-Visual Feedback**
- **Success sounds** for claims and purchases
- **Warning sounds** for PvP area entries
- **Particle effects** for territorial visualization
- **Title notifications** for important events
- **Action bar updates** for real-time information

### 📱 **Modern UI Elements**
- **Component-based text** with hover effects
- **Color-coded information** for quick understanding
- **Progress indicators** for limits and usage
- **Interactive tooltips** with detailed explanations

---

## 🏆 **Why Servers Love FrontierGuard**

### 📈 **Increased Player Retention**
- **Flexible gameplay** keeps both builders and fighters engaged
- **Economic incentives** encourage long-term investment
- **Social features** promote community building
- **Protection systems** reduce frustration and grief

### ⚡ **Performance Optimized**
- **Async database operations** prevent server lag
- **Efficient chunk checking** with minimal overhead
- **Smart caching systems** for instant responses
- **Optimized particle rendering** for smooth visuals

### 🔧 **Easy Administration**
- **Comprehensive logging** for issue tracking
- **Flexible configuration** for any server type
- **Permission integration** with existing systems
- **Hot-reload capabilities** for live adjustments

---

## 📋 **Complete Command Reference**

### 🏠 **Land Management**
```
/fg claim          - Claim your current chunk
/fg unclaim        - Release your current chunk  
/fg claims         - View all your claims (X/Y format)
/fg claiminfo      - Inspect any chunk's ownership
/fg show           - Visualize nearby claim boundaries
```

### 👥 **Social Features**
```
/fg invite <player>     - Grant access to your claim
/fg uninvite <player>   - Revoke access from your claim
/fg invitations        - Manage all your invitations
```

### 🎮 **Player Systems**
```
/fg gui            - Open the main interface
/fg cooldown       - Check mode change timer
```

### ⚔️ **PvP Management**
```
/fg setpvpzone           - Get area selection tool
/fg createpvparea <name> - Create new PvP zone
/fg deletepvparea <name> - Remove PvP zone
/fg listpvpareas         - View all PvP areas
```

### 👑 **Administrative**
```
/fg reload                    - Reload configuration
/fg setmode <player> <mode>   - Change player modes
/fg forcemode <player> <mode> - Bypass cooldown restrictions
/fg adminclaims <player>      - Inspect player claims
/fg setclaimlimit <group> <#> - Configure claim limits
/fg update                    - Check for plugin updates
```

---

## 🔐 **Permission System**

### 🏷️ **Basic Permissions**
| Permission | Description | Default |
|------------|-------------|---------|
| `frontierguard.*` | Full plugin access | `false` |
| `frontierguard.use` | Basic command usage | `true` |
| `frontierguard.claim` | Claim chunks | `true` |
| `frontierguard.gui` | Access GUI system | `true` |
| `frontierguard.invite` | Manage invitations | `true` |

### 👑 **VIP/Premium Permissions**
| Permission | Effect | Recommended For |
|------------|--------|-----------------|
| `frontierguard.claimamount.10` | +10 bonus claims | VIP Rank |
| `frontierguard.claimamount.25` | +25 bonus claims | Premium Rank |
| `frontierguard.claimamount.50` | +50 bonus claims | Elite Rank |
| `frontierguard.claims.unlimited` | Unlimited claims | Staff/Donors |

### 🛡️ **Administrative Permissions**
| Permission | Access Level | Purpose |
|------------|-------------|---------|
| `frontierguard.admin` | Full admin access | Server management |
| `frontierguard.bypass` | Protection bypass | Building/testing |
| `frontierguard.admin.pvparea` | PvP zone control | Area management |

---

## ⚙️ **Easy Configuration**

### 📝 **config.yml**
```yaml
# Economic Settings
economy:
  base-claim-price: 1000    # Starting price for claims
  price-increment: 1000     # Price increase per claim

# Gameplay Settings  
mode-change:
  cooldown-hours: 24        # Prevent mode abuse

# Visual Settings
show:
  radius: 3                 # Boundary display radius

# Update System
update-check:
  enabled: true
  check-interval-hours: 6
  notify-on-join: true
```

---

## 🛠️ **Installation & Setup**

### 📦 **Quick Start (3 Steps)**
1. **Install Dependencies**: Vault + EssentialsX (or any economy plugin)
2. **Drop & Restart**: Place `FrontierGuard.jar` in plugins folder
3. **Configure**: Edit `config.yml` and set permissions

### 🔧 **Advanced Setup**
- **Permission Groups**: Configure VIP/Premium claim bonuses
- **PvP Areas**: Create designated battle zones
- **Economy**: Adjust claim pricing for your server economy
- **Messages**: Customize all player-facing text

---

## 🎯 **Perfect For These Server Types**

### 🏘️ **Survival Servers**
- **Protect builders** while maintaining PvP excitement
- **Economic gameplay** with land as valuable commodity
- **Community features** through invitation system

### 🏰 **Faction Servers**  
- **Territory control** with visual boundaries
- **Strategic PvP areas** for organized battles
- **Alliance systems** through claim invitations

### 🎨 **Creative Communities**
- **Build protection** for showcase areas
- **Collaborative building** with invitation permissions
- **Admin oversight** of all claimed territories

### ⚔️ **PvP Servers**
- **Safe zones** for new players to establish bases
- **Organized combat** in designated PvP areas
- **Economic warfare** through claim purchasing

---

## 🌟 **What Players Are Saying**

> *"Finally, a plugin that lets me build in peace without missing out on PvP action!"*

> *"The visual boundaries are gorgeous - I can actually see my territory!"*

> *"Love how permissions stack with purchased claims - great for VIP rewards!"*

> *"The GUI is so clean and intuitive - even new players understand it immediately."*

---

## 📊 **Technical Excellence**

### ⚡ **Performance**
- **Async database operations** - Zero lag impact
- **Efficient chunk checking** - Minimal server overhead  
- **Smart caching systems** - Instant response times
- **Optimized rendering** - Smooth visual effects

### 🔒 **Security**
- **SQL injection protection** with prepared statements
- **Permission validation** on every action
- **Data integrity** with foreign key constraints
- **Error handling** with graceful degradation

### 🔄 **Reliability**
- **Automatic database creation** and schema management
- **Connection pooling** for stability
- **Exception handling** with detailed logging
- **Hot-reload support** for configuration changes

---

## 🎁 **Bonus Features**

### 🔍 **Debug & Monitoring**
- **Economy testing** commands for troubleshooting
- **Comprehensive logging** for issue diagnosis
- **Performance metrics** for optimization
- **Admin oversight** tools for server management

### 🎨 **Customization**
- **Full message customization** with color code support
- **Configurable cooldowns** and limits
- **Flexible permission system** for any server structure
- **Visual effect controls** for performance tuning

### 🔄 **Future-Proof**
- **Modular architecture** for easy feature additions
- **API compatibility** with Minecraft updates
- **Plugin integration** support for expansions
- **Update system** for seamless upgrades

---

## 📋 **Mandatory Requirements**

### 🔧 **Server Requirements**
- **Minecraft Version**: 1.20+ (Paper/Spigot)
- **Java Version**: 21+ 
- **RAM**: Minimum 1GB (2GB+ recommended)
- **Storage**: 50MB+ free space

### 📦 **Required Dependencies**
- **[Vault](https://www.spigotmc.org/resources/vault.34315/)** - Economy & permission integration
- **Economy Plugin** - EssentialsX, CMI, or similar (tested with EssentialsX)

### 🔐 **Essential Permissions**
```yaml
# Minimum setup for basic functionality
default:
  - frontierguard.use
  - frontierguard.claim
  - frontierguard.gui
  - frontierguard.invite

# VIP example (adds 10 bonus claims)
vip:
  - frontierguard.claimamount.10

# Admin setup
admin:
  - frontierguard.admin
  - frontierguard.bypass
```

### ⚙️ **Critical Configuration**
```yaml
# Essential config.yml settings
database:
  type: sqlite
  file: landclaimer.db

mode-change:
  cooldown-hours: 24

show:
  radius: 3  # 1-10 chunks

update-check:
  enabled: true
```

---

## 🎉 **Get Started Today!**

### 📥 **Download**
- **SpigotMC**: [FrontierGuard Resource Page](https://www.spigotmc.org/resources/frontier-guard-beta.128966/)
- **GitHub**: [Latest Release](https://github.com/DanielVNZ/FrontierGuard)

### 💬 **Support**
- **Issues**: GitHub Issues page
- **Discord**: Coming soon!
- **SpigotMC**: Resource discussion page

### 🤝 **Community**
- **GitHub**: Star the repo and contribute!
- **Reviews**: Leave feedback on SpigotMC
- **Suggestions**: Open feature requests

---

## 🏆 **Join the FrontierGuard Revolution!**

**Stop settling for basic land claiming plugins.** Your players deserve a **premium experience** that respects their playstyle choices while maintaining server balance and performance.

**FrontierGuard** delivers enterprise-grade features with consumer-friendly usability. Whether you're running a small community server or a massive network, FrontierGuard scales to meet your needs.

### 🎯 **Ready to Transform Your Server?**

**[Download FrontierGuard Now](https://www.spigotmc.org/resources/frontier-guard-beta.128966/) and give your players the freedom they deserve!**

---

*FrontierGuard v1.0.2 - Protecting your world, empowering your players! 🛡️✨*

**Created with ❤️ by danielvnz**
