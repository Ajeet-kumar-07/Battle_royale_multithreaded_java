import { useState } from "react";

type JoinFormProps = {
  onJoin: (name: string) => Promise<void>;
  busy: boolean;
  error: string;
};

export function JoinForm({ onJoin, busy, error }: JoinFormProps) {
  const [name, setName] = useState("");

  return (
    <div className="join-card">
      <h1>⚔️ Battle Royale</h1>
      <p className="muted">Join the HTTP match server on port 8090.</p>
      <div className="tips">
        <span>🎯 Move: 5-unit cardinal commands</span>
        <span>⚡ Attack range: 20m</span>
        <span>☣️ Zone damage applies on command ticks</span>
      </div>
      <form
        onSubmit={async (event) => {
          event.preventDefault();
          if (!name.trim()) {
            return;
          }
          await onJoin(name.trim());
        }}
      >
        <input
          value={name}
          maxLength={20}
          onChange={(event) => {
            setName(event.target.value);
          }}
          placeholder="Player name"
          aria-label="Player name"
        />
        <button type="submit" disabled={busy || !name.trim()}>
          {busy ? "Joining..." : "Join Match"}
        </button>
      </form>
      {error ? <div className="error">{error}</div> : null}
    </div>
  );
}
