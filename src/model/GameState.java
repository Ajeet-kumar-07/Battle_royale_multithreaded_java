package model;

import java.util.*;

public class GameState {
    private final Map<String, Player> players;
    private Zone safeZone;
    private int currentRound;
    private MatchStats stats;
    private List<Item> items;
    private boolean gameRunning;
    private long lastZoneShrinkTime;
    private static final long ZONE_SHRINK_INTERVAL = 120000; // 2 minutes

    public GameState() {
        this.players = new HashMap<>();
        this.safeZone = new Zone(50, 50, 50);
        this.currentRound = 0;
        this.stats = new MatchStats();
        this.items = new ArrayList<>();
        this.gameRunning = false;
        this.lastZoneShrinkTime = System.currentTimeMillis();
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public Collection<Player> getAllPlayers() {
        return players.values();
    }

    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player player : players.values()) {
            if (player.isAlive()) {
                alive.add(player);
            }
        }
        return alive;
    }

    public void incrementRound() {
        currentRound++;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public long getTimeUntilZoneShrink() {
        long elapsed = System.currentTimeMillis() - lastZoneShrinkTime;
        long remaining = ZONE_SHRINK_INTERVAL - elapsed;
        return Math.max(0, remaining);
    }

    public void resetZoneShrinkTimer() {
        this.lastZoneShrinkTime = System.currentTimeMillis();
    }

    public static long getZoneShrinkInterval() {
        return ZONE_SHRINK_INTERVAL;
    }

    // Getters and Setters
    public Map<String, Player> getPlayers() { return players; }
    public Zone getSafeZone() { return safeZone; }
    public void setSafeZone(Zone zone) { this.safeZone = zone; }
    public int getCurrentRound() { return currentRound; }
    public MatchStats getStats() { return stats; }
    public boolean isGameRunning() { return gameRunning; }
    public void setGameRunning(boolean running) { this.gameRunning = running; }
}
