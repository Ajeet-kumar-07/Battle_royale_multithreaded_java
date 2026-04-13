package events;

public abstract class GameEvent {
    private final long timestamp;
    private final String message;

    public GameEvent(String message) {
        this.timestamp = System.currentTimeMillis();
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public abstract String getEventType();
}
