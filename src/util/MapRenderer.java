package util;
import model.Player;
import model.Zone;

import java.util.Collection;



public class MapRenderer {
    private  static  final int MAP_SIZE = 20;

    public static String renderToString(Collection<Player> players, Zone zone) {
        StringBuilder mapOutput = new StringBuilder();
        char[][] grid = new char[MAP_SIZE][MAP_SIZE];
        
        for(int i = 0 ; i<MAP_SIZE ; i++){
            for(int j = 0 ; j<MAP_SIZE ; j++){
                grid[i][j] = '.';
            }
        }

        for(Player player : players){
            if(!player.isAlive())continue;
            int x = player.getX()/5;
            int y = player.getY()/5;

            if(x >= 0 && x<MAP_SIZE && y>= 0 && y<MAP_SIZE){
                grid[y][x] = getPlayerMarker(player);
            }
        }

        int centerX = zone.getCenterX()/5;
        int centerY = zone.getCenterY()/5;
        int radius = zone.getRadius()/5;
        for(int i = 0 ; i<MAP_SIZE ; i++){
            for(int j = 0 ; j<MAP_SIZE ; j++){
                int dx = j - centerX;
                int dy = i - centerY;
                double dist = Math.sqrt(dx*dx+dy*dy);
                if(Math.abs(dist-radius)<1){
                    if(grid[i][j]=='.'){
                        grid[i][j] = 'Z';
                    }
                }
            }
        }
        
        mapOutput.append("\n=== MAP ===\n");
        for(int i=0;i<MAP_SIZE;i++){
            for(int j=0;j<MAP_SIZE;j++){
                mapOutput.append(grid[i][j]).append(" ");
            }
            mapOutput.append("\n");
        }
        
        mapOutput.append("\n");
        mapOutput.append("Players\n");
        for(Player player : players){
            if(!player.isAlive())continue;
            mapOutput.append(getPlayerMarker(player)).append(" ")
                .append(player.getId()).append(" ")
                    .append(HealthBar(player.getHealth()))
                    .append(" ")
                    .append(player.getHealth())
                    .append("| kills ")
                    .append(player.getKills())
                    .append("\n");
        }
        
        return mapOutput.toString();
    }

    private static String HealthBar(int health){
        int bars = health /10;
        StringBuilder bar = new StringBuilder();

        for(int i = 0 ; i< bars ; i++){
            bar.append("█");
        }
        for(int i = bars ; i<10 ; i++){
            bar.append("░");
        }
        return bar.toString();
    }


    public static void render(Collection<Player>players,Zone zone){
        char[][] grid = new char[MAP_SIZE][MAP_SIZE];
        for(int i = 0 ; i<MAP_SIZE ; i++){
            for(int j = 0 ; j<MAP_SIZE ; j++){
                grid[i][j] = '.';
            }
        }

        for(Player player : players){
            if(!player.isAlive())continue;
            int x = player.getX()/5;
            int y = player.getY()/5;

            if(x >= 0 && x<MAP_SIZE && y>= 0 && y<MAP_SIZE){
                grid[y][x] = getPlayerMarker(player);
            }
        }

        int centerX = zone.getCenterX()/5;
        int centerY = zone.getCenterY()/5;
        int radius = zone.getRadius()/5;
        for(int i = 0 ; i<MAP_SIZE ; i++){
            for(int j = 0 ; j<MAP_SIZE ; j++){
                int dx = j - centerX;
                int dy = i - centerY;
                double dist = Math.sqrt(dx*dx+dy*dy);
                if(Math.abs(dist-radius)<1){
                    if(grid[i][j]=='.'){
                        grid[i][j] = 'z';
                    }
                }
            }
        }
        System.out.println("\n=== MAP ===");

        for(int i=0;i<MAP_SIZE;i++){
            for(int j=0;j<MAP_SIZE;j++){
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }

        System.out.println();

        System.out.println("Players");
        for(Player player : players){
            if(!player.isAlive())continue;
            System.out.println(
                    getPlayerMarker(player)+" "
                    + player.getId() + " "
                    + HealthBar(player.getHealth())
                    + " "
                    + player.getHealth()
                    + "| kills"
                    + player.getKills()
            );
        }


    }

    private static char getPlayerMarker(Player player){
        String id = player.getId();
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return c;
            }
        }
        return 'P';
    }

}
