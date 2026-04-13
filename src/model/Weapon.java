package model;

public class Weapon {
    private final String name;
    private final int maxDamage;
    private final int minDamage;

    public Weapon(String name, int maxDamage, int minDamage) {
        this.name = name;
        this.maxDamage = maxDamage;
        this.minDamage = minDamage;
    }

    public int generateDamage() {
        return (int) (Math.random() * (maxDamage - minDamage + 1)) + minDamage;
    }

    public String getName() {
        return name;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public int getMinDamage() {
        return minDamage;
    }

    public int getAverageDamage() {
        return (maxDamage + minDamage) / 2;
    }
}
