package model;

public class Zone {
    private final int centerX;
    private final int centerY;
    private int radius;


    public Zone(int centerX, int centerY, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    public boolean isInside(int x, int y) {
        int dx = x - centerX;
        int dy = y - centerY;
        double distance = Math.sqrt(dx*dx + dy*dy);
        return distance <= radius;
    }

    public void shrinkZone(){
        radius-=5;
        if(radius<10){
            radius = 10;
        }
        System.out.println("⚠ Safe zone shrinking. New radius: "+radius);
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getRadius() {
        return radius;
    }

}
