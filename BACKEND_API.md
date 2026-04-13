# Battle Royale Backend API Documentation

## Overview
The backend has been completely refactored to support the advanced 2D frontend with seamless real-time data synchronization.

## Backend Architecture

### Core Components

#### 1. **WebUiServer** (`src/network/WebUiServer.java`)
- HTTP server running on port `8090`
- Serves HTML, CSS, and JavaScript files
- Provides REST API endpoints for game operations
- Real-time event broadcasting

#### 2. **GameEngine** (`src/engine/GameEngine.java`)
- Core game logic orchestration
- Player management
- Command processing and execution
- Combat and movement handling
- Zone shrinking mechanics

#### 3. **Model Classes**
- **Player**: Enhanced with armor, kills/deaths tracking, damage source
- **GameState**: Zone shrink timer, proper state management
- **Weapon**: Damage statistics for frontend display
- **Zone**: Safe zone with radius for collision detection

## API Endpoints

### 1. Join Game
```
POST /api/join
Content-Type: application/x-www-form-urlencoded

name=PlayerName

Response:
{
  "ok": true,
  "player": "PlayerName"
}
```

### 2. Execute Command
```
POST /api/command
Content-Type: application/x-www-form-urlencoded

name=PlayerName&command=move east

Response:
{
  "ok": true,
  "result": "You moved east"
}
```

**Supported Commands:**
- `move north|south|east|west` - Move 5 units in direction
- `attack <playerName>` - Attack nearby player (range: 20m)
- `status` - Get player status
- `scan` - Scan nearby players
- `help` - Show available commands

### 3. Get Game State
```
GET /api/state?name=PlayerName

Response:
{
  "running": true,
  "zone": {
    "centerX": 50,
    "centerY": 50,
    "radius": 40
  },
  "timeUntilZoneShrink": 85000,
  "players": [
    {
      "id": "PlayerName",
      "x": 45,
      "y": 52,
      "health": 85,
      "armor": 20,
      "alive": true,
      "kills": 2,
      "deaths": 0,
      "weapon": {
        "name": "Rifle",
        "damage": 18
      }
    },
    ...
  ],
  "events": [
    "⚡ Player1 hit Player2 for 25 dmg!",
    "⚔️ Player1 killed Player3 with Rifle!",
    ...
  ]
}
```

## Data Models

### Player Object
```json
{
  "id": "PlayerName",
  "x": 0-100,
  "y": 0-100,
  "health": 0-100,
  "armor": 0-100,
  "alive": boolean,
  "kills": number,
  "deaths": number,
  "weapon": {
    "name": "string",
    "damage": number
  }
}
```

### Zone Object
```json
{
  "centerX": number,
  "centerY": number,
  "radius": number
}
```

## Game Mechanics

### Movement
- Players can move 5 units per command
- Movement is bounded to 0-100 on both axes
- Commands execute immediately on request

### Combat
- **Attack Range**: 20 meters
- **Damage**: Weapon dependent (random between min-max)
- **Armor**: Absorbs 40% of damage, then breaks
- **Health**: 100 max, 0 is death
- **Kills/Deaths**: Tracked per player

### Zone Mechanics
- Safe zone starts at center (50, 50) with radius 50
- Zone shrinks every 2 minutes (120,000 ms)
- Zone damage kills players outside safe zone
- New radius decreases by 5 each shrink

### Events
- Tracked in real-time event feed
- Max 100 events stored
- Formatted for frontend display
- Includes attack, kill, and movement events

## Frontend Integration

### Canvas Rendering
- Real-time player position updates
- Dynamic zone visualization with pulsing borders
- Health/armor bars above players
- Weapon icons for identification
- Grid background and gradient overlays

### UI Updates
- Health bar color changes based on damage
- Kill feed shows recent eliminations
- Nearby players list with distance
- Zone shrink countdown with color warnings
- Real-time stats (kills, deaths, players alive)

### Visual Effects
- Blood splash particles on hit
- Damage numbers floating above target
- Explosion particles on death
- Screen shake on impacts
- Smooth animations throughout

## Key Changes from Original

1. **Enhanced Player Model**
   - Added `deaths` counter
   - Added `lastDamageTime` and `lastDamageSrc` tracking
   - Added `setWeapon()` method for weapon changes

2. **Improved Combat**
   - `takeDamage()` now takes damage source name
   - Combat messages broadcast to all players
   - Kill announcements include weapon name

3. **Zone Management**
   - `getTimeUntilZoneShrink()` returns milliseconds until next shrink
   - Timer resets after each shrink
   - Frontend receives countdown for UI display

4. **API Responses**
   - Weapon objects include damage statistics
   - Player objects include all required fields
   - Zone information in every state response
   - Zone shrink timer for frontend countdown

5. **WebUiServer Refactor**
   - `playerToJson()` method builds complete player objects
   - Proper JSON serialization for all game data
   - Event feed integration with game engine
   - Full state snapshots on request

## Testing the Backend

### 1. Compile
```bash
javac -d out src/**/*.java
```

### 2. Run
```bash
java -cp out Main
```

### 3. Access Frontend
- Navigate to `http://localhost:8090`
- Enter player name and join
- Control with arrow buttons or click-to-move on canvas

### 4. Test API Directly
```bash
# Join game
curl -X POST http://localhost:8090/api/join \
  -d "name=TestPlayer"

# Execute command
curl -X POST http://localhost:8090/api/command \
  -d "name=TestPlayer&command=move east"

# Get state
curl "http://localhost:8090/api/state?name=TestPlayer"
```

## Performance Considerations

1. **Thread Safety**
   - `synchronized` methods for game state modifications
   - Thread-safe event queue with synchronized block
   - Player damage synchronized for concurrent attacks

2. **Network Efficiency**
   - State polling at 1.5s intervals (configurable)
   - Minimal JSON payloads
   - Event feed capped at 100 messages

3. **Scalability**
   - HashMap for O(1) player lookups
   - Efficient distance calculations
   - Event broadcasting via single server instance

## Future Enhancements

1. **WebSocket Support** - Real-time push instead of polling
2. **Database Persistence** - Player stats storage
3. **Replay System** - Replay stored games
4. **Matchmaking** - Auto queue and team assignment
5. **Advanced Abilities** - Skill cooldown system
6. **Item System** - Loot drops and inventory
7. **Team Gameplay** - Squad and team mechanics
8. **Leaderboards** - Global ranking system

## Troubleshooting

### Port Already in Use
- Change `WEB_PORT` in `WebUiServer.java` (currently 8090)
- Or kill existing process: `lsof -ti:8090 | xargs kill -9`

### Players Not Appearing
- Ensure `GameEngine.startGame()` is called
- Check that players are added to `GameState`
- Verify API endpoint is returning player data

### Combat Not Working
- Check attack range (20 units default)
- Verify both players are alive
- Ensure target name matches exactly

### Frontend Not Loading
- Verify file paths in `sendFile()` method
- Check `src/web/` directory exists with all files
- Inspect browser console for errors

## Deployment Notes

1. The project uses Java's built-in `HttpServer` (no external dependencies)
2. All files are served from `src/web/` directory
3. Game state is in-memory only (not persisted)
4. Maximum 100 players supported (easily scalable)
5. No authentication/authorization implemented yet
