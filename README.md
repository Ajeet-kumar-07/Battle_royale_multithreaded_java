# ⚔️ Battle Royale (Multithreaded Java + React)

A multiplayer battle-royale simulation with:
- **Java backend game engine**
- **HTTP Web UI/API server** on port **8090**
- **TCP text game server** on port **8080**
- **React + Vite frontend** for map/HUD gameplay

---

## Features

- Real-time-ish multiplayer updates via state polling
- 2D map with players, safe zone, and event feed
- Combat with armor reduction, kills/deaths tracking
- Zone shrink + zone damage mechanics
- Loot pickup (`medkit`, `armor`, `shield`, `sniper`)
- Global game events via `GameEventBus`
- Two interfaces:
	- Browser gameplay (`/api/*` on 8090)
	- Terminal TCP client commands (8080)

---

## Tech Stack

- **Backend:** Java (JDK 11+), built-in `HttpServer`, socket server
- **Frontend:** React 19, TypeScript, Vite 7
- **Build tools:** `javac`, npm scripts (`dev`, `build`, `lint`, `preview`)

---

## Project Structure

```text
new_battle/
├─ src/
│  ├─ Main.java
│  ├─ engine/        # core gameplay systems
│  ├─ events/        # event types + event bus
│  ├─ model/         # game state, players, items, zone
│  ├─ network/       # HTTP + TCP servers/handlers
│  └─ util/          # loot/map/weapon helpers
├─ frontend/
│  ├─ src/           # React app
│  └─ dist/          # production build served by backend
└─ *.md              # docs
```

---

## How It Runs

`Main` starts both servers using one shared `GameEngine` instance:

- **Web UI server:** `http://localhost:8090`
- **TCP game server:** `localhost:8080`

Important engine behavior:
- The game uses **command-driven updates**.
- World progression (zone checks/damage updates) is triggered when players execute gameplay commands like `move`, `attack`, `pickup`.

---

## Quick Start (Backend + Web UI)

From project root:

### 1) Compile Java

**PowerShell (Windows):**

```powershell
javac -d out src\**\*.java
```

### 2) Run servers

```powershell
java -cp out Main
```

You should see startup logs for ports **8090** and **8080**.

### 3) Open game in browser

Go to:

```text
http://localhost:8090
```

---

## Frontend Development

From `frontend/`:

```powershell
npm install
npm run dev
```

For production build (served by backend from `frontend/dist`):

```powershell
npm run build
```

If `frontend/dist` is missing, backend falls back to legacy static files under `src/web` (if present).

---

## Core API Endpoints

Base URL: `http://localhost:8090`

### `POST /api/join`
Join or create player.

Form fields:
- `name` (string)

### `POST /api/command`
Execute command for a player.

Form fields:
- `name` (string)
- `command` (string)

Supported commands:
- `move north|south|east|west`
- `attack <playerName>`
- `pickup`
- `scan`
- `status`
- `say <message>` / `chat <message>`
- `help`

### `GET /api/state`
Returns full game snapshot JSON with:
- `running`, `round`, `zone`, `timeUntilZoneShrink`
- `players`
- `items`
- `events`

---

## TCP Server (Optional)

Connect to `localhost:8080` using any TCP client.

Flow:
1. Enter player name when prompted.
2. Use commands (`move`, `attack`, `scan`, `status`, `say`, `help`, `quit`).

Note: TCP lobby auto-start logic uses a minimum player threshold, while web `/api/join` can trigger immediate game start when needed.

---

## Gameplay Rules (Current Defaults)

- Coordinate bounds: `0..100` on X/Y
- Move step: `5` units
- Attack range: `20`
- Nearby scan range: `25`
- Zone shrink interval: `120s`
- Zone damage is applied during command-driven updates

---

## Troubleshooting

- **Port already in use**
	- Free port `8090` or `8080`, then restart.

- **Frontend not loading from backend**
	- Build frontend: `cd frontend && npm run build`

- **Push/pull Git issues**
	- Rebase with remote before pushing:
		- `git pull --rebase origin main`
		- `git push -u origin main`

---

## Useful Docs in This Repo

- `QUICKSTART.md`
- `BACKEND_API.md`
- `ARCHITECTURE_DIAGRAMS.md`
- `INTEGRATION_CHECKLIST.md`

---
