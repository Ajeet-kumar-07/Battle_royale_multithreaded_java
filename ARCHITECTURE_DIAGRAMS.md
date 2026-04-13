# System Architecture & Data Flow Diagrams

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     GAME CLIENT (Browser)                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  HTML5 Canvas (2D Game Rendering)                    │   │
│  │  ├─ Player dots with health bars                     │   │
│  │  ├─ Zone visualization (pulsing circle)              │   │
│  │  ├─ Weapon icons and labels                          │   │
│  │  └─ Particle effects (blood, explosions)             │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Game UI (HUD, Kill Feed, Stats)                     │   │
│  │  ├─ Health/Armor bars (colored)                      │   │
│  │  ├─ Weapon info display                              │   │
│  │  ├─ Kill feed with recent eliminations               │   │
│  │  ├─ Nearby players list                              │   │
│  │  ├─ Zone shrink countdown                            │   │
│  │  └─ Player stats (kills, deaths)                     │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Input Controls & Command Execution                  │   │
│  │  ├─ Movement buttons (arrow keys)                    │   │
│  │  ├─ Canvas click detection                           │   │
│  │  ├─ Attack commands (right-click)                    │   │
│  │  └─ Scan/Status commands                             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↑↓ HTTP REST API
                  (polling every 1.5 seconds)
┌─────────────────────────────────────────────────────────────┐
│                    GAME SERVER (Java)                        │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  WebUiServer (Port 8090)                             │   │
│  │  ├─ POST /api/join       → Add player               │   │
│  │  ├─ POST /api/command    → Execute action           │   │
│  │  ├─ GET  /api/state      → Get game state           │   │
│  │  ├─ GET  /               → Serve HTML               │   │
│  │  ├─ GET  /app.js         → Serve JavaScript         │   │
│  │  └─ GET  /styles.css     → Serve stylesheet         │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  GameEngine (Core Game Logic)                        │   │
│  │  ├─ executeCommand()    → Parse & execute           │   │
│  │  ├─ addPlayerIfAbsent() → Create new players        │   │
│  │  ├─ movePlayer()        → Update position           │   │
│  │  ├─ attackPlayer()      → Calculate damage          │   │
│  │  └─ startGame()         → Initialize match          │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  GameState (Data Container)                          │   │
│  │  ├─ players HashMap<String, Player>                 │   │
│  │  ├─ safeZone: Zone                                   │   │
│  │  ├─ items: List<Item>                                │   │
│  │  ├─ currentRound: int                                │   │
│  │  └─ gameRunning: boolean                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ↓                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Game Engines (Specialized Systems)                   │  │
│  │                                                        │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │  │
│  │  │CombatEngine  │  │MovementEngine│  │ZoneEngine  │  │  │
│  │  │              │  │              │  │            │  │  │
│  │  │- Calculate   │  │- Validate    │  │- Shrink    │  │  │
│  │  │  damage      │  │  positions   │  │  zone      │  │  │
│  │  │- Apply armor │  │- Boundary    │  │- Apply     │  │  │
│  │  │  reduction   │  │  checking    │  │  damage    │  │  │
│  │  │- Track kills │  │- Update      │  │- Track     │  │  │
│  │  │              │  │  coordinates │  │  shrinks   │  │  │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │  │
│  └────────────────────────────────────────────────────────┘  │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  EventBus (Event Publishing System)                  │   │
│  │  ├─ publish(event)     → Broadcast to listeners      │   │
│  │  ├─ registerListener() → Register event handler      │   │
│  │  └─ Events:                                          │   │
│  │     • AttackEvent   (hit/miss)                       │   │
│  │     • DeathEvent    (elimination)                    │   │
│  │     • ZoneDamageEvent                                │   │
│  │     • PlayerJoinEvent                                │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 2. API Request/Response Flow

```
BROWSER REQUEST:
┌─────────────────────────────────────────┐
│ POST /api/command                       │
│ Content-Type: form-data                 │
│                                         │
│ name=Player1&command=attack Player2    │
└─────────────────────────────────────────┘
                    ↓
         WebUiServer.handleCommand()
                    ↓
┌─────────────────────────────────────────┐
│ 1. Parse request body                   │
│ 2. Get player by name                   │
│ 3. Validate player exists               │
│ 4. Execute: gameEngine.executeCommand() │
│ 5. Get result string                    │
└─────────────────────────────────────────┘
                    ↓
SERVER RESPONSE:
┌─────────────────────────────────────────┐
│ HTTP 200 OK                             │
│ Content-Type: application/json          │
│                                         │
│ {                                       │
│   "ok": true,                           │
│   "result": "You attacked Player2 ..." │
│ }                                       │
└─────────────────────────────────────────┘
                    ↓
         Browser.handleCommandResponse()
                    ↓
         Trigger visual effects:
         - Blood splash particles
         - Damage number floating
         - Screen shake
```

## 3. Game State API Response Structure

```
Browser: GET /api/state?name=Player1
                    ↓
         WebUiServer.handleState()
                    ↓
         Build complete game state:
                    ↓
{
  "running": true,                    ← Match status
  
  "zone": {                           ← Zone data for canvas
    "centerX": 50,
    "centerY": 50,
    "radius": 40
  },
  
  "timeUntilZoneShrink": 85000,       ← Countdown in ms
  
  "players": [                        ← All players data
    {
      "id": "Player1",                ← Player identity
      "x": 45,                        ← Position for canvas
      "y": 52,
      "health": 85,                   ← HUD display
      "armor": 20,
      "alive": true,                  ← Alive status
      "kills": 2,                     ← Stats display
      "deaths": 0,
      "weapon": {                     ← Weapon info
        "name": "Rifle",
        "damage": 18
      }
    },
    { ... more players ... }
  ],
  
  "events": [                         ← Kill feed
    "⚡ Player1 hit Player2 for 25 dmg!",
    "⚔️ Player1 killed Player3 with Rifle!",
    ...
  ]
}
                    ↓
         Browser receives JSON
                    ↓
         Frontend processes:
         • renderer.setGameState(data, playerName)
         • ui.updateHealth(myPlayer.health, 100)
         • ui.updateStats(kills, deaths, alive, total)
         • ui.updateZoneTimer(timeUntilZoneShrink)
         • ui.addKillFeedEntry(...) for new events
                    ↓
         Canvas renders:
         • Draw zone with radius
         • Draw all players at (x, y)
         • Draw health bars above players
         • Animate zone pulsing
                    ↓
         UI updates:
         • Health bar width and color
         • Armor bar visibility
         • Weapon display with emoji
         • Kill feed scrolls
         • Player count updates
         • Zone timer countdown
```

## 4. Combat Sequence Diagram

```
Player A (Attacker)          GameEngine          Player B (Target)
        │                         │                      │
        │  POST /api/command      │                      │
        │  attack Player2         │                      │
        ├────────────────────────>│                      │
        │                         │                      │
        │                    Calculate distance          │
        │                    Dist = 15 (valid)           │
        │                         │                      │
        │                    Generate damage             │
        │                    Damage = 22                 │
        │                         │                      │
        │                    Calculate armor reduction   │
        │                    Armor absorbs 8 dmg         │
        │                    Actual = 14                 │
        │                         │                      │
        │                         │  takeDamage(14, A)   │
        │                         │──────────────────────>│
        │                         │                  Health -= 14
        │                         │                  Health = 86
        │                         │<──────────────────────┤
        │                         │                      │
        │                    Publish AttackEvent         │
        │                         │                      │
        │                    Broadcast to clients        │
        │                    "⚡ A hit B for 14 dmg"    │
        │                         │                      │
        │   Response: "Hit!"       │                      │
        │<────────────────────────┤                      │
        │                         │                      │
        │   GET /api/state        │                      │
        ├────────────────────────>│                      │
        │                         │                      │
        │   Returns updated       │                      │
        │   game state with       │                      │
        │   B.health = 86         │                      │
        │<────────────────────────┤                      │
        │                         │                      │
        │   Browser renders       │                      │
        │   • Health bar update   │                      │
        │   • Particle effects    │                      │
        │   • Damage number       │                      │
        │   • Event in feed       │                      │
        │                         │                      │


If target dies (health <= 0):

        │                         │                      │
        │  Attack deals lethal    │                      │
        │  damage                 │                      │
        │                         │  takeDamage(100, A)  │
        │                         │──────────────────────>│
        │                         │                  Health = 0
        │                         │                  alive = false
        │                         │<──────────────────────┤
        │                         │                      │
        │                    A.kills++                   │
        │                         │                      │
        │                    Publish DeathEvent          │
        │                         │                      │
        │                    Broadcast:                  │
        │                    "⚔️ A killed B with Rifle!"│
        │                         │                      │
        │   GET /api/state        │                      │
        ├────────────────────────>│                      │
        │                         │                      │
        │   Returns state with:   │                      │
        │   • B.alive = false     │                      │
        │   • A.kills = 3         │                      │
        │   • Players alive -= 1  │                      │
        │<────────────────────────┤                      │
        │                         │                      │
        │   Browser renders       │                      │
        │   • B disappears        │                      │
        │   • Kill feed update    │                      │
        │   • Explosion effect    │                      │
        │   • Screen shake        │                      │
        │                         │                      │
```

## 5. Zone Shrinking Cycle

```
Game Start (t=0)
│
├─ Zone Center: (50, 50)
├─ Zone Radius: 50
├─ Safety: All players safe
├─ Timer: 120 seconds until first shrink
│
├─ WAIT 120 seconds
│
├─ Zone Shrink #1 (t=120s)
│
├─ Zone Center: (50, 50)
├─ Zone Radius: 45 (decreased by 5)
├─ Players outside: Take 5 HP damage
├─ Events: "⚠ Zone is shrinking!"
├─ New Timer: 120 seconds until next shrink
│
├─ WAIT 120 seconds
│
├─ Zone Shrink #2 (t=240s)
│
├─ Zone Center: (50, 50)
├─ Zone Radius: 40
├─ Danger: Smaller safe area
├─ More players forced to move
│
├─ (Continues until radius = 10)
│
└─ Zone Shrink #N (Final)
   │
   ├─ Zone Radius: 10 (minimum)
   ├─ All survivors: In tiny zone
   ├─ Last man standing: Victory!
   │
```

## 6. Data Model Relationships

```
┌─────────────────────────────────────────────────────────┐
│                      GameState                          │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  players: Map<String, Player>                    │  │
│  │                                                  │  │
│  │  ├─ "Player1" ─────────┐                         │  │
│  │  │                      │                         │  │
│  │  ├─ "Player2" ──────┐   │                         │  │
│  │  │                  │   │                         │  │
│  │  └─ "Player3" ──┐   │   │                         │  │
│  │                 │   │   │                         │  │
│  └─────────────────┼───┼───┼──────────────────────────┘  │
│                    │   │   │                              │
│  ┌─────────────────┴─┐ │   │                              │
│  │     Player       │ │   │                              │
│  │                  │ │   │                              │
│  │ id: String       │ │   │                              │
│  │ health: int      │ │   │                              │
│  │ armor: int       │ │   │                              │
│  │ x, y: int        │ │   │                              │
│  │ alive: boolean   │ │   │                              │
│  │ kills: int       │ │   │                              │
│  │ deaths: int      │ │   │                              │
│  │ weapon ──────────┼─┼───┼──┐                           │
│  │                  │ │   │  │                           │
│  └──────────────────┘ │   │  │                           │
│                       │   │  │                           │
│  ┌────────────────────┴─┐ │  │                           │
│  │     Player          │ │  │                           │
│  │                     │ │  │                           │
│  │ (Same structure)   │ │  │                           │
│  └────────────────────┘ │  │                           │
│                         │  │                           │
│  ┌─────────────────────┬┴┐ │                           │
│  │     Player        │ │  │                           │
│  │                   │ │  │                           │
│  │ (Same structure) │ │  │                           │
│  └───────────────────┘ │  │                           │
│                        │  │                           │
│  ┌────────────────────┬┘  │                           │
│  │    Weapon          │   │                           │
│  │                    │   │                           │
│  │ name: String       │   │                           │
│  │ minDamage: int     │   │                           │
│  │ maxDamage: int     │   │                           │
│  │ generateDamage()   │   │                           │
│  └────────────────────┘   │                           │
│                           │                           │
│  ┌───────────────────────┬┘                           │
│  │      Weapon           │                            │
│  │  (Another player's    │                            │
│  │   equipped weapon)    │                            │
│  └───────────────────────┘                            │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  safeZone: Zone                                  │  │
│  │                                                  │  │
│  │  centerX: int                                    │  │
│  │  centerY: int                                    │  │
│  │  radius: int                                     │  │
│  │  shrinkZone()                                    │  │
│  │  isInside(x, y): boolean                         │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  items: List<Item>                                     │
│  currentRound: int                                     │
│  gameRunning: boolean                                  │
│  lastZoneShrinkTime: long                              │
└─────────────────────────────────────────────────────────┘
```

## 7. Frontend-Backend Communication Timeline

```
Timeline:

t=0s    Player joins
        │
        ├─> POST /api/join (name=PlayerName)
        │   └─> Backend: Create Player, add to GameState
        │       Frontend: Transition to game screen
        │

t=1.5s  First poll
        │
        ├─> GET /api/state?name=PlayerName
        │   └─> Backend: Build complete state JSON
        │       Frontend: Render canvas, update UI
        │

t=3s    Player executes command
        │
        ├─> POST /api/command (command=move east)
        │   └─> Backend: Execute move, update position
        │       Frontend: Clear input
        │

t=4.5s  Another poll (automated)
        │
        ├─> GET /api/state?name=PlayerName
        │   └─> Backend: Return updated positions
        │       Frontend: Render with new positions
        │

t=6s    Another command
        │
        ├─> POST /api/command (command=attack Enemy)
        │   └─> Backend: Calculate damage, apply
        │       Frontend: Show attack effects
        │

t=7.5s  Poll (automated)
        │
        ├─> GET /api/state?name=PlayerName
        │   └─> Backend: Return state with health reduction
        │       Frontend: Update health bars, show kill feed
        │

...continues...
        │
        ├─ Every 1.5 seconds: GET /api/state
        ├─ On user input: POST /api/command
        └─ Game ends when 1 player alive

```

---

These diagrams show:
1. **System Architecture** - How all components connect
2. **API Communication** - Request/response flow
3. **Game State** - Complete state structure
4. **Combat System** - Detailed attack sequence
5. **Zone Mechanics** - Shrinking cycle
6. **Data Models** - Class relationships
7. **Timeline** - Real-world interaction sequence

This provides a complete visual understanding of how the frontend and backend work together!
