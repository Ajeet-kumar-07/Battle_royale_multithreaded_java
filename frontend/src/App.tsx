import { useCallback, useEffect, useMemo, useState } from "react";
import { executeCommand, joinGame } from "./api/client";
import { CommandPanel } from "./components/CommandPanel";
import { GameMap } from "./components/GameMap";
import { IsometricMap } from "./components/IsometricMap";
import { JoinForm } from "./components/JoinForm";
import { PlayerProfilePage } from "./components/PlayerProfilePage";
import { useGameState } from "./hooks/useGameState";

function formatSeconds(ms: number): string {
  return `${Math.max(0, Math.floor(ms / 1000))}s`;
}

function playerIdFromPath(pathname: string): string | null {
  const match = pathname.match(/^\/player\/([^/]+)$/);
  if (!match) return null;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return null;
  }
}

function clampPercent(value: number): number {
  return Math.max(0, Math.min(100, value));
}

function eventTone(eventText: string): "kill" | "hit" | "zone" | "achievement" | "neutral" {
  if (eventText.includes("💬")) return "neutral";
  if (eventText.includes("🏅")) return "achievement";
  if (eventText.includes("⚔️") || eventText.toLowerCase().includes("winner")) return "kill";
  if (eventText.includes("⚡")) return "hit";
  if (eventText.includes("⚠️") || eventText.toLowerCase().includes("zone")) return "zone";
  return "neutral";
}

function getZonePhase(radius: number) {
  if (radius >= 40) return { label: "EARLY", damage: 5 };
  if (radius >= 30) return { label: "MID", damage: 10 };
  if (radius >= 20) return { label: "LATE", damage: 15 };
  return { label: "FINAL", damage: 20 };
}

function distance(x1: number, y1: number, x2: number, y2: number): number {
  const dx = x1 - x2;
  const dy = y1 - y2;
  return Math.sqrt(dx * dx + dy * dy);
}

function parseChatEvent(eventText: string): { playerId: string; message: string } | null {
  const bubbleAt = eventText.indexOf("💬");
  if (bubbleAt < 0) return null;

  const payload = eventText.slice(bubbleAt + 2).trim();
  const sep = payload.indexOf(":");
  if (sep <= 0 || sep >= payload.length - 1) return null;

  const playerId = payload.slice(0, sep).trim();
  const message = payload.slice(sep + 1).trim();
  if (!playerId || !message) return null;

  return { playerId, message };
}

type ChatBubble = {
  message: string;
  at: number;
};

export default function App() {
  const [playerName, setPlayerName] = useState<string | null>(null);
  const [joinBusy, setJoinBusy] = useState(false);
  const [joinError, setJoinError] = useState("");
  const [lastResult, setLastResult] = useState("");
  const [commandBusy, setCommandBusy] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [isometricView, setIsometricView] = useState(true);
  const [selectedPlayerId, setSelectedPlayerId] = useState<string | null>(() =>
    playerIdFromPath(window.location.pathname),
  );

  const { state, error: stateError, refresh } = useGameState(playerName);
  const [latestChatByPlayer, setLatestChatByPlayer] = useState<Record<string, ChatBubble>>({});

  const me = useMemo(() => {
    if (!state || !playerName) {
      return null;
    }
    return state.players.find((player) => player.id === playerName) ?? null;
  }, [state, playerName]);

  const selectedPlayer = useMemo(() => {
    if (!state || !selectedPlayerId) {
      return null;
    }
    return state.players.find((player) => player.id === selectedPlayerId) ?? null;
  }, [selectedPlayerId, state]);

  const nearestEnemyId = useMemo(() => {
    if (!state || !playerName) {
      return null;
    }

    const mePlayer = state.players.find((player) => player.id === playerName);
    if (!mePlayer || !mePlayer.alive) {
      return null;
    }

    let best: { id: string; dist: number } | null = null;
    for (const player of state.players) {
      if (player.id === playerName || !player.alive) continue;
      const dist = distance(mePlayer.x, mePlayer.y, player.x, player.y);
      if (!best || dist < best.dist) {
        best = { id: player.id, dist };
      }
    }

    return best?.id ?? null;
  }, [playerName, state]);

  const zonePhase = useMemo(() => {
    return state ? getZonePhase(state.zone.radius) : null;
  }, [state]);

  const achievementEvents = useMemo(() => {
    if (!state) return [];
    return state.events.filter((event) => event.includes("🏅")).slice(-5).reverse();
  }, [state]);

  const chatEvents = useMemo(() => {
    if (!state) return [];
    return state.events.filter((event) => event.startsWith("💬 ")).slice(-25).reverse();
  }, [state]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 300);

    return () => {
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (!state) return;

    const newestMessages: Record<string, string> = {};
    for (const event of state.events) {
      const parsed = parseChatEvent(event);
      if (!parsed) continue;
      newestMessages[parsed.playerId] = parsed.message;
    }

    setLatestChatByPlayer((previous) => {
      const next: Record<string, ChatBubble> = { ...previous };
      const now = Date.now();
      const KEEP_LOCAL_CHAT_MS = 30000;

      for (const [playerId, bubble] of Object.entries(next)) {
        if (now - bubble.at > KEEP_LOCAL_CHAT_MS) {
          delete next[playerId];
        }
      }

      for (const [playerId, message] of Object.entries(newestMessages)) {
        const prev = previous[playerId];
        if (prev && prev.message === message) {
          next[playerId] = prev;
        } else {
          next[playerId] = { message, at: Date.now() };
        }
      }

      return next;
    });
  }, [state]);

  const executePlayerCommand = useCallback(
    async (command: string) => {
      if (!playerName || commandBusy) {
        return;
      }

      const trimmed = command.trim();
      const lower = trimmed.toLowerCase();
      const isChatCommand = lower.startsWith("say ") || lower.startsWith("chat ");
      if (lower.startsWith("say ") || lower.startsWith("chat ")) {
        const firstSpace = trimmed.indexOf(" ");
        const msg = firstSpace > -1 ? trimmed.slice(firstSpace + 1).trim() : "";
        if (msg) {
          setLatestChatByPlayer((previous) => ({
            ...previous,
            [playerName]: { message: msg, at: Date.now() },
          }));
        }
      }

      setCommandBusy(true);
      try {
        const result = await executeCommand(playerName, command);
        setLastResult(result.result);
        if (!isChatCommand) {
          await refresh();
        }
      } catch (err) {
        setLastResult(err instanceof Error ? err.message : "Command failed");
      } finally {
        setCommandBusy(false);
      }
    },
    [commandBusy, playerName, refresh],
  );

  useEffect(() => {
    if (!playerName) {
      return;
    }

    const keyToCommand: Record<string, string> = {
      ArrowUp: "move north",
      ArrowDown: "move south",
      ArrowLeft: "move west",
      ArrowRight: "move east",
      w: "move north",
      s: "move south",
      a: "move west",
      d: "move east",
      q: "scan",
      e: "status",
      h: "help",
      p: "pickup",
    };

    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      const tag = target?.tagName.toLowerCase();
      const isTypingTarget =
        tag === "input" || tag === "textarea" || target?.isContentEditable === true;

      if (isTypingTarget) {
        return;
      }

      const command = keyToCommand[event.key] ?? keyToCommand[event.key.toLowerCase()];
      if (!command && event.key.toLowerCase() === "f" && nearestEnemyId) {
        event.preventDefault();
        void executePlayerCommand(`attack ${nearestEnemyId}`);
        return;
      }

      if (!command) {
        return;
      }

      event.preventDefault();
      void executePlayerCommand(command);
    };

    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [executePlayerCommand, nearestEnemyId, playerName]);

  useEffect(() => {
    const onPopState = () => {
      setSelectedPlayerId(playerIdFromPath(window.location.pathname));
    };

    window.addEventListener("popstate", onPopState);
    return () => {
      window.removeEventListener("popstate", onPopState);
    };
  }, []);

  useEffect(() => {
    const nextPath = selectedPlayerId ? `/player/${encodeURIComponent(selectedPlayerId)}` : "/";
    if (window.location.pathname !== nextPath) {
      window.history.pushState(null, "", nextPath);
    }
  }, [selectedPlayerId]);

  if (!playerName) {
    return (
      <main className="app app-center">
        <JoinForm
          busy={joinBusy}
          error={joinError}
          onJoin={async (nextName) => {
            setJoinBusy(true);
            setJoinError("");
            try {
              await joinGame(nextName);
              setPlayerName(nextName);
            } catch (err) {
              setJoinError(err instanceof Error ? err.message : "Join failed");
            } finally {
              setJoinBusy(false);
            }
          }}
        />
      </main>
    );
  }

  if (selectedPlayer) {
    return (
      <PlayerProfilePage
        player={selectedPlayer}
        mePlayer={me}
        onBack={() => {
          setSelectedPlayerId(null);
        }}
      />
    );
  }

  return (
    <main className="app">
      <header className="topbar">
        <div className="brand">⚔️ BATTLE ROYALE</div>
        <div className="pill">Player: {playerName}</div>
        <div className="pill">Round: {state?.round ?? 0}</div>
        <div className={`pill ${state?.running ? "pill-live" : "pill-wait"}`}>
          {state?.running ? "🔴 Running" : "⏳ Waiting"}
        </div>
        <div className="pill">Zone: {state ? formatSeconds(state.timeUntilZoneShrink) : "--"}</div>
        <div className="pill">Phase: {zonePhase ? `${zonePhase.label} (-${zonePhase.damage}/tick)` : "--"}</div>
        <button
          type="button"
          onClick={() => {
            setIsometricView((prev) => !prev);
          }}
        >
          {isometricView ? "Top-down View" : "Isometric View"}
        </button>
        <button
          type="button"
          onClick={() => {
            setSelectedPlayerId(playerName);
          }}
        >
          Profile
        </button>
        <button
          onClick={() => {
            setPlayerName(null);
            setLastResult("");
            setSelectedPlayerId(null);
          }}
        >
          Leave
        </button>
      </header>

      <section className="layout">
        <div className="layout-col layout-left">
          <CommandPanel
            disabled={commandBusy}
            lastResult={lastResult}
            onCommand={executePlayerCommand}
          />

          <section className="panel">
            <h3>State</h3>
            {stateError ? <div className="error">{stateError}</div> : null}
            {me ? (
              <>
                <div className="hud-block">
                  <div className="hud-row">
                    <span>Health</span>
                    <strong>{me.health}/100</strong>
                  </div>
                  <div className="bar bar-health">
                    <div style={{ width: `${clampPercent(me.health)}%` }} />
                  </div>
                </div>

                <div className="hud-block">
                  <div className="hud-row">
                    <span>Armor</span>
                    <strong>{me.armor}/100</strong>
                  </div>
                  <div className="bar bar-armor">
                    <div style={{ width: `${clampPercent(me.armor)}%` }} />
                  </div>
                </div>

                <ul className="stats">
                  <li>Alive: {String(me.alive)}</li>
                  <li>Kills: {me.kills}</li>
                  <li>Deaths: {me.deaths}</li>
                  <li>Pos: ({me.x}, {me.y})</li>
                  <li>Weapon: {me.weapon?.name ?? "None"}</li>
                  <li>Zone Phase: {zonePhase ? `${zonePhase.label} (-${zonePhase.damage}/tick)` : "--"}</li>
                </ul>
              </>
            ) : (
              <div>Waiting for player data...</div>
            )}
          </section>

          <section className="panel">
            <h3>Achievements</h3>
            {achievementEvents.length === 0 ? (
              <div className="muted">No achievements unlocked yet.</div>
            ) : (
              <div className="achievement-list">
                {achievementEvents.map((event, index) => (
                  <div className="achievement-row" key={`${event}-${index}`}>
                    {event}
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        <div className="layout-col layout-center">
          {state ? (
            isometricView ? (
              <IsometricMap
                state={state}
                me={playerName}
                latestChatByPlayer={latestChatByPlayer}
                nowMs={nowMs}
                onCommand={executePlayerCommand}
                canInteract={!commandBusy}
              />
            ) : (
              <GameMap
                state={state}
                me={playerName}
                latestChatByPlayer={latestChatByPlayer}
                nowMs={nowMs}
              />
            )
          ) : (
            <section className="panel">Loading map...</section>
          )}
        </div>

        <div className="layout-col layout-right">
          <section className="panel players-panel">
            <h3>Players</h3>
            <div className="players-list">
              {(state?.players ?? []).map((player) => (
                <div key={player.id} className={`player-row ${player.alive ? "alive" : "dead"}`}>
                  <button
                    type="button"
                    className="player-button"
                    onClick={() => {
                      setSelectedPlayerId(player.id);
                    }}
                  >
                    <strong>{player.id}</strong>
                    <span>{player.alive ? "alive" : "dead"}</span>
                    <span>HP {player.health}</span>
                    <span>AR {player.armor}</span>
                    <span>K {player.kills}</span>
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="panel events-panel">
            <h3>Chat</h3>
            <div className="chat-list">
              {chatEvents.length === 0 ? (
                <div className="muted">No chat yet.</div>
              ) : (
                chatEvents.map((message, index) => (
                  <div key={`${message}-${index}`} className="chat-row">
                    {message}
                  </div>
                ))
              )}
            </div>

            <form
              className="chat-form"
              onSubmit={async (event) => {
                event.preventDefault();
                const text = chatMessage.trim();
                if (!text) return;
                await executePlayerCommand(`say ${text}`);
                setChatMessage("");
              }}
            >
              <input
                value={chatMessage}
                maxLength={140}
                placeholder="Type message..."
                onChange={(event) => {
                  setChatMessage(event.target.value);
                }}
              />
              <button type="submit" disabled={commandBusy || !chatMessage.trim()}>
                Chat
              </button>
            </form>

            <h3>Event Feed</h3>
            <div className="events">
              {(state?.events ?? []).slice().reverse().map((event, index) => (
                <div key={`${event}-${index}`} className={`event-row ${eventTone(event)}`}>
                  {event}
                </div>
              ))}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
