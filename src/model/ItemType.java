package model;

public enum ItemType {
    MEDKIT(30, "MEDKIT"),
    ARMOR(50, "ARMOR"),
    SNIPER(40, "SNIPER"),
    SHIELD(25, "SHIELD");

    private final int value;
    private final String displayName;

    ItemType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }
}
