package events;

public class RoundEvent extends GameEvent {
    private final int roundNumber;

    public RoundEvent(int roundNumber) {
        super("=== ROUND " + roundNumber + " ===");
        this.roundNumber = roundNumber;
    }

    @Override
    public String getEventType() {
        return "ROUND_START";
    }

    public int getRoundNumber() {
        return roundNumber;
    }
}
