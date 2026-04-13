# 🎮 Battle Royale - Quick Start Guide

## System Requirements
- Java 11 or higher
- Modern web browser (Chrome, Firefox, Edge, Safari)
- Terminal/Command prompt

## Installation & Setup

### 1. Navigate to Project Directory
```bash
cd d:\java_projects\new_battle
```

### 2. Compile Java Source Files
```bash
javac -d out src/**/*.java
```

Or on Windows PowerShell:
```powershell
javac -d out src\**\*.java
```

### 3. Run the Server
```bash
java -cp out Main
```

You should see output like:
```
🚀 Initializing Battle Royale Server...
✅ Server components initialized
🌐 Web UI available at http://localhost:8090
🎮 Game server started on port 8080
```

### 4. Open in Browser
Navigate to: `http://localhost:8090`

You should see the Battle Royale login screen with the logo "⚔️ BATTLE ROYALE"

## How to Play

### Joining the Game
1. Enter your player name (max 20 characters)
2. Click "Join Match"
3. Wait for game to start

### Movement
**Choose one method:**
- Click arrow buttons (⬆️ ⬅️ ⬇️ ➡️) at bottom
- Click anywhere on the map canvas to move in that direction
- Use keyboard (after clicking canvas)

Each move advances you 5 units in that direction.

### Combat
**Attack nearby players:**
- Right-click on enemy player on the map
- Or type: `attack PlayerName` in command box
- Attack range: 20 units max
- Damage varies by weapon (10-30 typical)

### Status & Info
- Click "Status" button to see your stats
- Click "Scan" button to find nearby players (25m range)
- Click "Help" for command reference

### UI Elements

#### Left Sidebar
- **Health Bar** (Green) - Your hit points (0-100)
- **Armor Bar** (Blue) - Damage absorption (0-100)
- **Weapon Info** - Current weapon and damage
- **Ability Slots** - Future ability system (1-4 keys)

#### Center (Main Canvas)
- **Blue Dot** - You (player)
- **Red Dots** - Enemies
- **Green Circle** - Safe zone
- **Red Border** - Danger zone (outside safe = damage)
- **Green Name Tags** - Player names and health
- **Weapon Icons** - 🔫 = gun, ⚔️ = sword, etc.

#### Right Sidebar
- **Kill Feed** - Recent eliminations (⚡ attacks, ⚔️ kills)
- **Stats** - Your kills and deaths
- **Nearby Players** - Players within scanner range

#### Top Header
- **Your Name** - Player identification
- **Match Status** - 🔴 Running or ⏳ Waiting
- **Players Alive** - X/100 format
- **Zone Timer** - Seconds until safe zone shrinks

#### Bottom Command Bar
- **Arrow Buttons** - Quick movement
- **Scan/Status/Help** - Game info buttons
- **Command Input** - Type custom commands

## Game Mechanics

### Survival
- **Health**: Start with 100 HP
- **Damage**: Each attack deals 10-30 damage
- **Armor**: Absorbs 40% of damage (max 100)
- **Death**: Health reaches 0 → eliminated

### Zone Dynamics
- **Safe Zone**: Green pulsing circle
- **Shrinking**: Zone shrinks every 2 minutes
- **Damage**: Outside zone = 5 HP/second
- **Final Zone**: Very small at match end

### Weapons
- **Rifle**: 20 avg damage, 10-30 range
- **Pistol**: 15 avg damage, 8-22 range
- **Shotgun**: 25 avg damage, 15-35 range
- **Sniper**: 28 avg damage, 18-38 range
- **Sword**: 20 avg damage, 10-30 range

(Weapon assigned randomly on spawn)

### Scoring
- **Kills**: +1 for eliminating an enemy
- **Survival**: More time alive = better
- **Ranking**: Based on kills and alive time

## Example Game Session

```
1. Open browser to http://localhost:8090
   → See login screen

2. Enter name: "ShadowNinja" and join
   → Appear on map at random location
   → Blue dot shows your position
   → See other players as red dots

3. Move east 5 times
   → See position update: (40, 50) → (65, 50)
   → Health bar shows 100/100

4. Nearby enemy appears
   → Right-click on their dot
   → Attack message shows in events
   → Hit for 22 damage
   → Enemy health drops to 78

5. Enemy counter-attacks
   → You take 18 damage
   → Health drops to 82
   → See "-18" damage number float above you
   → Blue health bar shrinks

6. Fight continues
   → Final attack kills enemy
   → Kill feed shows "ShadowNinja 🔫 EnemyName"
   → Your kill count: 1

7. Zone starts shrinking
   → Green circle gets smaller
   → Red border approaches
   → Timer counts down from 120s

8. Move toward safe zone
   → Stay within green circle
   → Avoid red damage zone

9. Final 1v1 scenario
   → Only 2 players left
   → Fight in shrinking zone
   → Winner gets "VICTORY ROYALE"
```

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| ↑ | Move North |
| ↓ | Move South |
| ← | Move West |
| → | Move East |
| 1-4 | Activate abilities (future) |
| Enter | Send command |

## Troubleshooting

### Game Won't Load
**Problem**: Blank page or "Cannot GET /"
- Solution: Wait 5 seconds for server startup
- Check terminal output for errors
- Try refreshing page (Ctrl+F5)

### Can't Join
**Problem**: "Join failed" message
- Solution: Check player name (max 20 chars)
- Try a different name
- Check server is running in terminal

### No Other Players Visible
**Problem**: Only see your own dot
- Solution: Open another browser tab/window
- Join with different name
- Refresh page to see other players

### Combat Not Working
**Problem**: "Target is out of range"
- Solution: Move closer to enemy (within 20 units)
- Use Scan button to find nearby players
- Try right-click on enemy dot directly

### Lag/Stuttering
**Problem**: Jerky movement, delayed updates
- Solution: Close other browser tabs
- Clear browser cache (Ctrl+Shift+Del)
- Use Chrome for best performance
- Check internet connection

### Server Crashes
**Problem**: Java crash or "Connection refused"
- Solution: Restart server (`java -cp out Main`)
- Check for error messages in terminal
- Verify port 8090 is not in use
- Try different port if needed

## Performance Tips

1. **Browser**: Use Chrome for best performance
2. **Fullscreen**: Click fullscreen for better visibility
3. **Graphics**: Close other demanding programs
4. **Connection**: Play on same local network for low latency
5. **Resolution**: Works best on screens 1920x1080 or larger

## Advanced Commands

Type these in the command input box:

```
move north        → Move up 5 units
move south        → Move down 5 units
move east         → Move right 5 units
move west         → Move left 5 units
attack Player123  → Attack specific player
scan              → Show nearby players
status            → Show your stats
help              → Show all commands
```

## Network Details

- **Web Server**: http://localhost:8090
- **Game Server**: localhost:8080 (TCP)
- **Update Rate**: 1.5 seconds (polling)
- **Max Players**: 100

## File Structure

```
d:\java_projects\new_battle\
├── src/
│   ├── Main.java
│   ├── engine/
│   │   ├── GameEngine.java
│   │   ├── CombatEngine.java
│   │   ├── MovementEngine.java
│   │   └── ZoneEngine.java
│   ├── model/
│   │   ├── Player.java
│   │   ├── GameState.java
│   │   ├── Weapon.java
│   │   └── Zone.java
│   ├── network/
│   │   ├── GameServer.java
│   │   ├── GameClient.java
│   │   ├── ClientHandler.java
│   │   └── WebUiServer.java
│   ├── events/
│   │   ├── GameEvent.java
│   │   ├── GameEventBus.java
│   │   ├── AttackEvent.java
│   │   ├── DeathEvent.java
│   │   └── ...
│   └── web/
│       ├── index.html
│       ├── app.js
│       └── styles.css
├── out/          (Compiled classes)
├── BACKEND_API.md
└── INTEGRATION_CHECKLIST.md
```

## Getting Help

1. **Check Console**: Press F12 in browser, look for errors
2. **Check Terminal**: Look at server output for logs
3. **Check Files**: See BACKEND_API.md for API details
4. **Check Integration**: See INTEGRATION_CHECKLIST.md for status

## Credits

- Backend: Java, HttpServer API
- Frontend: HTML5 Canvas, JavaScript ES6+
- Physics: Distance calculations, collision detection
- Combat: Damage formulas, armor system

## License

This is a battle royale game built for demonstration purposes.

---

**Ready to battle? Join at http://localhost:8090!** ⚔️
