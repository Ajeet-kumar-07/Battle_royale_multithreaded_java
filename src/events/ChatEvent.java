package events;

public class ChatEvent extends GameEvent {
    private final String playerId;
    private final String message;

    public ChatEvent(String playerId, String message) {
        super("💬 " + playerId + ": " + message);
        this.playerId = playerId;
        this.message = message;
    }

    @Override
    public String getEventType() {
        return "CHAT";
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getMessageText() {
        return message;
    }
}
