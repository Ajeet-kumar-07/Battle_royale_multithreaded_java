import { useCallback, useEffect, useState } from "react";
import { fetchState } from "../api/client";
import type { GameState } from "../types";

export function useGameState(playerName: string | null) {
  const [state, setState] = useState<GameState | null>(null);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    if (!playerName) {
      return;
    }

    try {
      const nextState = await fetchState(playerName);
      setState(nextState);
      setError("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch game state");
    }
  }, [playerName]);

  useEffect(() => {
    if (!playerName) {
      return;
    }

    const initialTimer = window.setTimeout(() => {
      void refresh();
    }, 0);
    const timer = window.setInterval(() => {
      void refresh();
    }, 1500);

    return () => {
      window.clearTimeout(initialTimer);
      window.clearInterval(timer);
    };
  }, [playerName, refresh]);

  return { state, error, refresh };
}
