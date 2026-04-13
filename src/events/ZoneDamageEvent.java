package events;

public class ZoneDamageEvent extends GameEvent {
    private final String playerId;
    private final int damage;

    public ZoneDamageEvent(String playerId, int damage) {
        super(playerId + " took zone damage (-" + damage + " HP)");
        this.playerId = playerId;
        this.damage = damage;
    }

    @Override
    public String getEventType() {
        return "ZONE_DAMAGE";
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getDamage() {
        return damage;
    }
}
