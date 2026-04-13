import type { GameState } from "../types";

const FORM_HEADERS = {
  "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
};

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

export async function joinGame(name: string): Promise<{ ok: boolean; player: string }> {
  const form = new URLSearchParams({ name });
  const response = await fetch("/api/join", {
    method: "POST",
    headers: FORM_HEADERS,
    body: form.toString(),
  });
  return parseJson<{ ok: boolean; player: string }>(response);
}

export async function executeCommand(
  name: string,
  command: string,
): Promise<{ ok: boolean; result: string }> {
  const form = new URLSearchParams({ name, command });
  const response = await fetch("/api/command", {
    method: "POST",
    headers: FORM_HEADERS,
    body: form.toString(),
  });
  return parseJson<{ ok: boolean; result: string }>(response);
}

export async function fetchState(name: string): Promise<GameState> {
  const response = await fetch(`/api/state?name=${encodeURIComponent(name)}`);
  return parseJson<GameState>(response);
}
