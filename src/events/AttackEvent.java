package events;

public class AttackEvent extends GameEvent {
    private final String attackerId;
    private final String targetId;
    private final int damage;
    private final boolean hit;

    public AttackEvent(String attackerId, String targetId, int damage, boolean hit) {
        super(attackerId + (hit ? " attacked " : " tried to attack ") + targetId);
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.damage = damage;
        this.hit = hit;
    }

    @Override
    public String getEventType() {
        return "ATTACK";
    }

    public String getAttackerId() {
        return attackerId;
    }

    public String getTargetId() {
        return targetId;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isHit() {
        return hit;
    }
}
