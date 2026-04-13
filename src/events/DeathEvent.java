package events;

public class DeathEvent extends GameEvent {
    private final String playerId;
    private final String killerId;

    public DeathEvent(String playerId, String killerId) {
        super("💀 " + playerId + " was eliminated by " + killerId);
        this.playerId = playerId;
        this.killerId = killerId;
    }

    @Override
    public String getEventType() {
        return "DEATH";
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getKillerId() {
        return killerId;
    }
}
