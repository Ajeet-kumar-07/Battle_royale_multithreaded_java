import { useEffect, useMemo, useRef, useState } from "react";
import type { GameState } from "../types";

type IsometricMapProps = {
  state: GameState;
  me: string;
  latestChatByPlayer: Record<string, { message: string; at: number }>;
  nowMs: number;
  onCommand: (command: string) => Promise<void>;
  canInteract?: boolean;
};

const CANVAS_SIZE = 720;
const VIEW_PADDING = 56;
const HALF_W = 10;
const HALF_H = 5;
const PLAYER_LERP = 0.2;
const ZONE_LERP = 0.12;
const CHAT_BUBBLE_HOLD_MS = 12000;
const CHAT_BUBBLE_FADE_MS = 6000;
const CHAT_BUBBLE_TTL_MS = CHAT_BUBBLE_HOLD_MS + CHAT_BUBBLE_FADE_MS;

type Point = { x: number; y: number };
type ZoneView = { centerX: number; centerY: number; radius: number };

function project(x: number, y: number) {
  return {
    x: (x - y) * HALF_W,
    y: (x + y) * HALF_H,
  };
}

function toScreen(point: { x: number; y: number }) {
  return {
    x: CANVAS_SIZE / 2 + point.x,
    y: 110 + point.y,
  };
}

function fromScreen(screenX: number, screenY: number) {
  const px = screenX - CANVAS_SIZE / 2;
  const py = screenY - 110;
  const x = (px / HALF_W + py / HALF_H) / 2;
  const y = (py / HALF_H - px / HALF_W) / 2;
  return { x, y };
}

function zoneRadii(radius: number) {
  return {
    rx: radius * HALF_W,
    ry: radius * HALF_H,
  };
}

function lerp(current: number, target: number, alpha: number) {
  return current + (target - current) * alpha;
}

function closeEnough(a: number, b: number) {
  return Math.abs(a - b) < 0.02;
}

export function IsometricMap({ state, me, latestChatByPlayer, nowMs, onCommand, canInteract = true }: IsometricMapProps) {
  const [renderPositions, setRenderPositions] = useState<Record<string, Point>>(() => {
    const initial: Record<string, Point> = {};
    for (const player of state.players) {
      initial[player.id] = { x: player.x, y: player.y };
    }
    return initial;
  });
  const [zoneView, setZoneView] = useState<ZoneView>({
    centerX: state.zone.centerX,
    centerY: state.zone.centerY,
    radius: state.zone.radius,
  });

  const playerTargetsRef = useRef<Record<string, Point>>({});
  const zoneTargetRef = useRef<ZoneView>({
    centerX: state.zone.centerX,
    centerY: state.zone.centerY,
    radius: state.zone.radius,
  });

  useEffect(() => {
    const nextTargets: Record<string, Point> = {};
    for (const player of state.players) {
      nextTargets[player.id] = { x: player.x, y: player.y };
    }
    playerTargetsRef.current = nextTargets;
  }, [state.players]);

  useEffect(() => {
    zoneTargetRef.current = {
      centerX: state.zone.centerX,
      centerY: state.zone.centerY,
      radius: state.zone.radius,
    };
  }, [state.zone.centerX, state.zone.centerY, state.zone.radius]);

  useEffect(() => {
    let raf = 0;

    const tick = () => {
      const targets = playerTargetsRef.current;

      setRenderPositions((previous) => {
        const next: Record<string, Point> = { ...previous };
        let changed = false;

        for (const [id, target] of Object.entries(targets)) {
          const current = next[id] ?? target;
          const nx = lerp(current.x, target.x, PLAYER_LERP);
          const ny = lerp(current.y, target.y, PLAYER_LERP);
          const fx = closeEnough(nx, target.x) ? target.x : nx;
          const fy = closeEnough(ny, target.y) ? target.y : ny;

          if (!closeEnough(current.x, fx) || !closeEnough(current.y, fy)) {
            changed = true;
          }
          next[id] = { x: fx, y: fy };
        }

        for (const id of Object.keys(next)) {
          if (!targets[id]) {
            delete next[id];
            changed = true;
          }
        }

        return changed ? next : previous;
      });

      setZoneView((current) => {
        const target = zoneTargetRef.current;
        const centerX = lerp(current.centerX, target.centerX, ZONE_LERP);
        const centerY = lerp(current.centerY, target.centerY, ZONE_LERP);
        const radius = lerp(current.radius, target.radius, ZONE_LERP);

        const next: ZoneView = {
          centerX: closeEnough(centerX, target.centerX) ? target.centerX : centerX,
          centerY: closeEnough(centerY, target.centerY) ? target.centerY : centerY,
          radius: closeEnough(radius, target.radius) ? target.radius : radius,
        };

        if (
          next.centerX === current.centerX &&
          next.centerY === current.centerY &&
          next.radius === current.radius
        ) {
          return current;
        }
        return next;
      });

      raf = window.requestAnimationFrame(tick);
    };

    raf = window.requestAnimationFrame(tick);
    return () => {
      window.cancelAnimationFrame(raf);
    };
  }, []);

  const mePlayer = state.players.find((player) => player.id === me);
  const center = toScreen(project(zoneView.centerX, zoneView.centerY));
  const { rx, ry } = zoneRadii(zoneView.radius);

  const drawOrder = useMemo(() => {
    return [...state.players].sort((a, b) => {
      const pa = renderPositions[a.id] ?? { x: a.x, y: a.y };
      const pb = renderPositions[b.id] ?? { x: b.x, y: b.y };
      return pa.x + pa.y - (pb.x + pb.y);
    });
  }, [renderPositions, state.players]);

  const svgRef = useRef<SVGSVGElement | null>(null);

  const toSvgPoint = (clientX: number, clientY: number) => {
    const svg = svgRef.current;
    if (!svg) return null;
    const rect = svg.getBoundingClientRect();
    const visibleSize = CANVAS_SIZE - VIEW_PADDING * 2;
    const x = VIEW_PADDING + ((clientX - rect.left) / rect.width) * visibleSize;
    const y = VIEW_PADDING + ((clientY - rect.top) / rect.height) * visibleSize;
    return { x, y };
  };

  const resolveMoveCommand = (targetWorldX: number, targetWorldY: number) => {
    if (!mePlayer) return null;
    const dx = targetWorldX - mePlayer.x;
    const dy = targetWorldY - mePlayer.y;
    if (Math.abs(dx) < 1 && Math.abs(dy) < 1) return null;

    if (Math.abs(dx) > Math.abs(dy)) {
      return dx > 0 ? "move east" : "move west";
    }
    return dy > 0 ? "move south" : "move north";
  };

  const resolveAttackCommand = (sx: number, sy: number) => {
    const ATTACK_PICK_RADIUS = 16;
    let best: { id: string; dist: number } | null = null;

    for (const player of state.players) {
      if (player.id === me || !player.alive) continue;
      const rp = renderPositions[player.id] ?? { x: player.x, y: player.y };
      const p = toScreen(project(rp.x, rp.y));
      const dist = Math.hypot(sx - p.x, sy - p.y);
      if (dist <= ATTACK_PICK_RADIUS && (!best || dist < best.dist)) {
        best = { id: player.id, dist };
      }
    }

    return best ? `attack ${best.id}` : null;
  };

  return (
    <section className="panel map-panel">
      <h3>Map (Isometric)</h3>
      <svg
        ref={svgRef}
        viewBox={`${VIEW_PADDING} ${VIEW_PADDING} ${CANVAS_SIZE - VIEW_PADDING * 2} ${CANVAS_SIZE - VIEW_PADDING * 2}`}
        className="map isometric-map"
        onClick={(event) => {
          if (!canInteract) return;
          const point = toSvgPoint(event.clientX, event.clientY);
          if (!point) return;
          const world = fromScreen(point.x, point.y);
          const command = resolveMoveCommand(world.x, world.y);
          if (command) {
            void onCommand(command);
          }
        }}
        onContextMenu={(event) => {
          event.preventDefault();
          if (!canInteract) return;
          const point = toSvgPoint(event.clientX, event.clientY);
          if (!point) return;
          const command = resolveAttackCommand(point.x, point.y);
          if (command) {
            void onCommand(command);
          }
        }}
      >
        <rect x={0} y={0} width={CANVAS_SIZE} height={CANVAS_SIZE} fill="#0f172a" />

        {[0, 25, 50, 75, 100].map((line) => {
          const startA = toScreen(project(0, line));
          const endA = toScreen(project(100, line));
          const startB = toScreen(project(line, 0));
          const endB = toScreen(project(line, 100));
          return (
            <g key={`iso-grid-${line}`}>
              <line x1={startA.x} y1={startA.y} x2={endA.x} y2={endA.y} stroke="#1e293b" strokeWidth={1} />
              <line x1={startB.x} y1={startB.y} x2={endB.x} y2={endB.y} stroke="#1e293b" strokeWidth={1} />
            </g>
          );
        })}

        <ellipse
          className="iso-zone"
          cx={center.x}
          cy={center.y}
          rx={rx}
          ry={ry}
          fill="rgba(34,197,94,0.09)"
          stroke="#22c55e"
          strokeWidth={2}
        />

        {state.items.map((item, index) => {
          const p = toScreen(project(item.x, item.y));
          return (
            <rect
              key={`${item.type}-${item.x}-${item.y}-${index}`}
              x={p.x - 4}
              y={p.y - 10}
              width={8}
              height={8}
              fill="#f59e0b"
              transform={`rotate(45 ${p.x} ${p.y - 6})`}
            />
          );
        })}

        {drawOrder.map((player) => {
          const rp = renderPositions[player.id] ?? { x: player.x, y: player.y };
          const p = toScreen(project(rp.x, rp.y));
          const isMe = player.id === me;
          const fill = !player.alive ? "#6b7280" : isMe ? "#38bdf8" : "#ef4444";
          const healthRatio = Math.max(0, Math.min(100, player.health)) / 100;
          const chat = latestChatByPlayer[player.id];
          const ageMs = chat ? nowMs - chat.at : Number.POSITIVE_INFINITY;
          const visible = ageMs <= CHAT_BUBBLE_TTL_MS;
          const fade =
            ageMs <= CHAT_BUBBLE_HOLD_MS
              ? 1
              : visible
                ? Math.max(0.2, 1 - (ageMs - CHAT_BUBBLE_HOLD_MS) / CHAT_BUBBLE_FADE_MS)
                : 0;
          const chatText = chat && chat.message.length > 28 ? `${chat.message.slice(0, 28)}…` : chat?.message;
          return (
            <g key={player.id}>
              <ellipse cx={p.x} cy={p.y + 7} rx={8} ry={4} fill="rgba(2,6,23,0.7)" />
              <circle cx={p.x} cy={p.y} r={7} fill={fill} />
              {chatText && visible ? (
                <g style={{ opacity: fade }}>
                  <rect x={p.x - 48} y={p.y - 40} width={96} height={14} rx={6} className="chat-bubble-bg" />
                  <text x={p.x} y={p.y - 30} textAnchor="middle" className="chat-bubble-text">
                    {chatText}
                  </text>
                </g>
              ) : null}
              <rect x={p.x - 13} y={p.y - 16} width={26} height={4} fill="#0b1220" rx={2} />
              <rect x={p.x - 13} y={p.y - 16} width={26 * healthRatio} height={4} fill="#22c55e" rx={2} />
              <text x={p.x + 9} y={p.y - 9} className="map-label">
                {player.id}
              </text>
            </g>
          );
        })}
      </svg>

      {mePlayer ? (
        <div className="me-row">
          <span>Projection: 2:1 isometric</span>
          <span>HP {mePlayer.health}</span>
          <span>Armor {mePlayer.armor}</span>
          <span>Weapon {mePlayer.weapon?.name ?? "None"}</span>
        </div>
      ) : null}

      <div className="muted">Tip: gameplay still uses backend world coordinates (0-100); this is visual projection only.</div>
      <div className="muted">Left click: move toward tile • Right click player: attack target</div>
    </section>
  );
}
