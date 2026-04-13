package events;

public class AchievementEvent extends GameEvent {
    public AchievementEvent(String message) {
        super("🏅 " + message);
    }

    @Override
    public String getEventType() {
        return "ACHIEVEMENT";
    }
}
