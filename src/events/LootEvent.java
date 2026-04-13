package events;

public class LootEvent extends GameEvent {
    private final String playerId;
    private final String itemType;

    public LootEvent(String playerId, String itemType) {
        super("📦 " + playerId + " found " + itemType);
        this.playerId = playerId;
        this.itemType = itemType;
    }

    @Override
    public String getEventType() {
        return "LOOT";
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getItemType() {
        return itemType;
    }
}
