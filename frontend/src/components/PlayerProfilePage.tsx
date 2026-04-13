import type { Player } from "../types";

type PlayerProfilePageProps = {
  player: Player;
  mePlayer: Player | null;
  onBack: () => void;
};

function clampPercent(value: number): number {
  return Math.max(0, Math.min(100, value));
}

function distance(a: Player, b: Player): number {
  const dx = a.x - b.x;
  const dy = a.y - b.y;
  return Math.sqrt(dx * dx + dy * dy);
}

export function PlayerProfilePage({ player, mePlayer, onBack }: PlayerProfilePageProps) {
  const distanceFromMe = mePlayer && mePlayer.id !== player.id ? distance(mePlayer, player) : null;

  return (
    <main className="app player-page">
      <header className="topbar">
        <div className="brand">🧾 PLAYER PROFILE</div>
        <div className="pill">ID: {player.id}</div>
        <div className={`pill ${player.alive ? "pill-live" : "pill-wait"}`}>
          {player.alive ? "Alive" : "Eliminated"}
        </div>
        <button type="button" onClick={onBack}>
          ← Back to Match
        </button>
      </header>

      <section className="player-profile-grid">
        <section className="panel">
          <h3>Vitals</h3>
          <div className="hud-block">
            <div className="hud-row">
              <span>Health</span>
              <strong>{player.health}/100</strong>
            </div>
            <div className="bar bar-health">
              <div style={{ width: `${clampPercent(player.health)}%` }} />
            </div>
          </div>

          <div className="hud-block">
            <div className="hud-row">
              <span>Armor</span>
              <strong>{player.armor}/100</strong>
            </div>
            <div className="bar bar-armor">
              <div style={{ width: `${clampPercent(player.armor)}%` }} />
            </div>
          </div>
        </section>

        <section className="panel">
          <h3>Combat</h3>
          <ul className="stats">
            <li>Kills: {player.kills}</li>
            <li>Deaths: {player.deaths}</li>
            <li>Weapon: {player.weapon?.name ?? "None"}</li>
            <li>Weapon Avg Damage: {player.weapon?.damage ?? 0}</li>
          </ul>
        </section>

        <section className="panel">
          <h3>Position</h3>
          <ul className="stats">
            <li>X: {player.x}</li>
            <li>Y: {player.y}</li>
            <li>
              Distance from you: {distanceFromMe === null ? "self" : `${Math.round(distanceFromMe)}m`}
            </li>
          </ul>
        </section>
      </section>
    </main>
  );
}
