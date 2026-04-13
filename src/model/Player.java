package model;

public class Player implements Runnable {
    private final String id;
    private int health;
    private boolean alive;
    private Weapon weapon;
    private int x;
    private int y;
    private int kills = 0;
    private int deaths = 0;
    private int armor = 0;
    private int lastRoundAlive = 0;
    private long lastDamageTime = 0;
    private String lastDamageSrc = "";

    public Player(String id, Weapon weapon, int x, int y) {
        this.id = id;
        this.health = 100;
        this.alive = true;
        this.weapon = weapon;
        this.x = x;
        this.y = y;
    }

    public int attack() {
        return weapon.generateDamage();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > 100) x = 100;
        if (y > 100) y = 100;
    }

    public synchronized void takeDamage(int damage, String sourceName) {
        if (!alive) return;

        health -= damage;
        this.lastDamageTime = System.currentTimeMillis();
        this.lastDamageSrc = sourceName;

        if (health <= 0) {
            health = 0;
            alive = false;
            deaths++;
            System.out.println(id + " has been eliminated by " + sourceName);
        }
    }

    public int calculateDamageAfterArmor(int damage) {
        if (armor > 0) {
            int absorbed = (int) (damage * 0.4);
            armor -= absorbed;
            if (armor < 0) {
                int overflow = Math.abs(armor);
                armor = 0;
                return damage - absorbed + overflow;
            }
            return damage - absorbed;
        }
        return damage;
    }

    public void addArmor(int amount) {
        this.armor = Math.min(100, this.armor + amount);
    }

    public void heal(int amount) {
        this.health = Math.min(100, this.health + amount);
    }

    public void addKill() {
        kills++;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getArmor() {
        return armor;
    }

    public void setLastRoundAlive(int round) {
        this.lastRoundAlive = round;
    }

    public int getLastRoundAlive() {
        return lastRoundAlive;
    }

    public boolean isAlive() {
        return alive;
    }

    public String getId() {
        return id;
    }

    public int getHealth() {
        return health;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public void moveTowards(int targetX, int targetY) {
        int dx = targetX - x;
        int dy = targetY - y;

        if (dx != 0) dx = dx / Math.abs(dx);
        if (dy != 0) dy = dy / Math.abs(dy);
        move(dx * 5, dy * 5);
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }

    public String getLastDamageSrc() {
        return lastDamageSrc;
    }

    @Override
    public void run() {
        while (alive) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(id + " is thinking");
        }
    }
}
