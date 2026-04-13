# Copilot Instructions for `new_battle`

## Big picture (read first)
- This is a Java backend battle-royale simulation with two server interfaces:
  - HTTP UI/API on `8090` in `src/network/WebUiServer.java`
  - TCP text server on `8080` in `src/network/GameServer.java` + `ClientHandler.java`
- `Main` starts both servers and wires one shared `GameEngine` instance.
- Core state lives in `GameState` (`players`, `safeZone`, `items`, `round`, `stats`).

## Game loop model (project-specific)
- The game currently runs in **command-driven mode** (`COMMAND_DRIVEN_MODE = true` in `GameEngine`).
- World progression is triggered by player commands, not by a periodic tick:
  - `move`, `attack`, `pickup` call `processCommandDrivenUpdate()`.
  - Zone damage + zone shrink checks happen inside that method.
- Preserve this behavior unless explicitly changing game design.

## Architecture and data flow
- `WebUiServer` exposes `/api/join`, `/api/command`, `/api/state`.
- `/api/state` builds JSON manually; keep fields stable: `running`, `round`, `zone`, `timeUntilZoneShrink`, `players`, `items`, `events`.
- Events are published via `GameEventBus`; `WebUiServer` stores a capped feed (`MAX_EVENTS = 100`).
- Combat path: `GameEngine.attackPlayer()` -> armor reduction via `Player.calculateDamageAfterArmor()` -> `Player.takeDamage()` -> `AttackEvent`/`DeathEvent`.

## Frontend integration reality
- `WebUiServer` serves static assets from `frontend/dist` (preferred), fallback `src/web` (legacy).
- In this workspace, `frontend/src` is effectively empty; playable UI is expected from built artifacts in `frontend/dist`.
- If you change frontend behavior, treat `frontend` as a Vite React app (`npm run build`) so backend serves updated `dist` files.

## Developer workflows
- Java compile/run (PowerShell-friendly):
  - Compile: `javac -d out src\**\*.java`
  - Run: `java -cp out Main`
- Frontend (optional local dev): in `frontend/` use `npm run dev`, `npm run build`, `npm run lint`.
- API smoke test: `POST /api/join`, `POST /api/command`, `GET /api/state` on `http://localhost:8090`.

## Conventions to follow when editing
- Keep command responses human-readable and consistent with existing strings/emojis (used directly by clients/event feed).
- Keep `synchronized` boundaries on `GameEngine` command/state methods and on `GameEventBus`/`eventFeed` interactions.
- Preserve map bounds and movement granularity (`0..100`, 5-unit moves) unless requirement says otherwise.
- Keep player JSON contract in `playerToJson()` backward-compatible for UI polling clients.
- Prefer adding features by extending `engine/`, `events/`, and `model/` classes rather than embedding logic in network handlers.
