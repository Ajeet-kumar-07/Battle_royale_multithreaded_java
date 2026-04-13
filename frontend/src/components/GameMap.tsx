import type { GameState } from "../types";

type GameMapProps = {
  state: GameState;
  me: string;
  latestChatByPlayer: Record<string, { message: string; at: number }>;
  nowMs: number;
};

const MAP_SIZE = 100;
const CANVAS_SIZE = 720;
const VIEW_PADDING = 10;
const CHAT_BUBBLE_HOLD_MS = 12000;
const CHAT_BUBBLE_FADE_MS = 6000;
const CHAT_BUBBLE_TTL_MS = CHAT_BUBBLE_HOLD_MS + CHAT_BUBBLE_FADE_MS;

function scale(value: number): number {
  return (value / MAP_SIZE) * CANVAS_SIZE;
}

export function GameMap({ state, me, latestChatByPlayer, nowMs }: GameMapProps) {
  const mePlayer = state.players.find((player) => player.id === me);

  return (
    <section className="panel map-panel">
      <h3>Map</h3>
      <svg
        viewBox={`${VIEW_PADDING} ${VIEW_PADDING} ${CANVAS_SIZE - VIEW_PADDING * 2} ${CANVAS_SIZE - VIEW_PADDING * 2}`}
        className="map"
      >
        <rect x={0} y={0} width={CANVAS_SIZE} height={CANVAS_SIZE} fill="#0f172a" />

        {Array.from({ length: 10 }).map((_, idx) => {
          const p = ((idx + 1) / 10) * CANVAS_SIZE;
          return (
            <g key={`grid-${idx}`}>
              <line x1={p} y1={0} x2={p} y2={CANVAS_SIZE} stroke="#1e293b" strokeWidth={1} />
              <line x1={0} y1={p} x2={CANVAS_SIZE} y2={p} stroke="#1e293b" strokeWidth={1} />
            </g>
          );
        })}

        <circle
          cx={scale(state.zone.centerX)}
          cy={scale(state.zone.centerY)}
          r={scale(state.zone.radius)}
          fill="rgba(34,197,94,0.08)"
          stroke="#22c55e"
          strokeWidth={2}
        />

        {state.items.map((item, index) => (
          <g key={`${item.type}-${item.x}-${item.y}-${index}`}>
            <rect
              x={scale(item.x) - 4}
              y={scale(item.y) - 4}
              width={8}
              height={8}
              fill="#f59e0b"
            />
          </g>
        ))}

        {state.players.map((player) => {
          const isMe = player.id === me;
          const fill = !player.alive ? "#6b7280" : isMe ? "#38bdf8" : "#ef4444";
          const barWidth = 26;
          const barHeight = 4;
          const healthRatio = Math.max(0, Math.min(100, player.health)) / 100;
          const px = scale(player.x);
          const py = scale(player.y);
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
              <circle cx={px} cy={py} r={7} fill={fill} />
              {chatText && visible ? (
                <g style={{ opacity: fade }}>
                  <rect x={px - 48} y={py - 38} width={96} height={14} rx={6} className="chat-bubble-bg" />
                  <text x={px} y={py - 28} textAnchor="middle" className="chat-bubble-text">
                    {chatText}
                  </text>
                </g>
              ) : null}
              <rect x={px - barWidth / 2} y={py - 15} width={barWidth} height={barHeight} fill="#0b1220" rx={2} />
              <rect
                x={px - barWidth / 2}
                y={py - 15}
                width={barWidth * healthRatio}
                height={barHeight}
                fill="#22c55e"
                rx={2}
              />
              <text x={px + 10} y={py - 8} className="map-label">
                {player.id}
              </text>
            </g>
          );
        })}
      </svg>

      {mePlayer ? (
        <div className="me-row">
          <span>HP {mePlayer.health}</span>
          <span>Armor {mePlayer.armor}</span>
          <span>Kills {mePlayer.kills}</span>
          <span>Weapon {mePlayer.weapon?.name ?? "None"}</span>
        </div>
      ) : null}
    </section>
  );
}
