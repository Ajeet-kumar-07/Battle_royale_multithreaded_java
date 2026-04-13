export type Weapon = {
  name: string;
  damage: number;
};

export type Player = {
  id: string;
  x: number;
  y: number;
  health: number;
  armor: number;
  alive: boolean;
  kills: number;
  deaths: number;
  weapon: Weapon | null;
};

export type Zone = {
  centerX: number;
  centerY: number;
  radius: number;
};

export type Item = {
  type: string;
  x: number;
  y: number;
};

export type GameState = {
  running: boolean;
  round: number;
  zone: Zone;
  timeUntilZoneShrink: number;
  players: Player[];
  items: Item[];
  events: string[];
};
