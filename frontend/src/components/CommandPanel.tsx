import { useState } from "react";

type CommandPanelProps = {
  disabled: boolean;
  onCommand: (command: string) => Promise<void>;
  lastResult: string;
};

const QUICK_COMMANDS = [
  { label: "⬆ North", value: "move north" },
  { label: "⬇ South", value: "move south" },
  { label: "⬅ West", value: "move west" },
  { label: "➡ East", value: "move east" },
  { label: "🔍 Scan", value: "scan" },
  { label: "📊 Status", value: "status" },
  { label: "🎒 Pickup", value: "pickup" },
  { label: "❓ Help", value: "help" },
];

export function CommandPanel({ disabled, onCommand, lastResult }: CommandPanelProps) {
  const [rawCommand, setRawCommand] = useState("");

  return (
    <section className="panel">
      <h3>Commands</h3>
      <div className="quick-grid">
        {QUICK_COMMANDS.map((command) => (
          <button
            key={command.value}
            disabled={disabled}
            onClick={async () => {
              await onCommand(command.value);
            }}
          >
            {command.label}
          </button>
        ))}
      </div>

      <form
        className="command-form"
        onSubmit={async (event) => {
          event.preventDefault();
          const next = rawCommand.trim();
          if (!next) {
            return;
          }
          await onCommand(next);
          setRawCommand("");
        }}
      >
        <input
          value={rawCommand}
          disabled={disabled}
          placeholder="attack Player2"
          onChange={(event) => {
            setRawCommand(event.target.value);
          }}
        />
        <button type="submit" disabled={disabled || !rawCommand.trim()}>
          Send
        </button>
      </form>

      <div className="result">{lastResult || "Ready."}</div>

      <div className="shortcut-hint">
        Shortcuts: Arrow / WASD = move, <strong>Q</strong> scan, <strong>E</strong> status,
        <strong> P</strong> pickup, <strong>H</strong> help, <strong>F</strong> attack nearest. Chat: <strong>say hi</strong>.
      </div>
    </section>
  );
}
