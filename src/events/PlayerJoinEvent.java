package events;

public class PlayerJoinEvent extends GameEvent {
    private final String playerId;
    private final int currentPlayers;
    private final int requiredPlayers;

    public PlayerJoinEvent(String playerId, int currentPlayers, int requiredPlayers) {
        super("🎮 " + playerId + " joined the lobby (" + currentPlayers + "/" + requiredPlayers + ")");
        this.playerId = playerId;
        this.currentPlayers = currentPlayers;
        this.requiredPlayers = requiredPlayers;
    }

    @Override
    public String getEventType() {
        return "PLAYER_JOIN";
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }
}
