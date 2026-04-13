package model;

public class Item {
    private final ItemType type;
    private final int x;
    private final int y;
    private boolean collected;

    public Item(ItemType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.collected = false;
    }

    public void collect() {
        this.collected = true;
    }

    public boolean isCollected() {
        return collected;
    }

    public ItemType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
