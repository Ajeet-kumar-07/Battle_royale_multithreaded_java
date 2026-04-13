package engine;

import events.GameEvent;
import events.LootEvent;
import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class MovementEngine {
    private static final Logger logger = Logger.getLogger(MovementEngine.class.getName());
    private final Random random = new Random();
    private static final int LOOT_PICKUP_RANGE = 5;

    public List<GameEvent> moveAllPlayers(GameState gameState) {
        List<GameEvent> events = new ArrayList<>();
        
        for (Player player : gameState.getAllPlayers()) {
            if (!player.isAlive()) continue;

            // Check for nearby loot first
            Item nearbyItem = findNearestItem(player, gameState);
            if (nearbyItem != null && distance(player.getX(), player.getY(), nearbyItem.getX(), nearbyItem.getY()) < LOOT_PICKUP_RANGE) {
                events.addAll(collectItem(player, nearbyItem, gameState));
                continue;
            }

            // AI decision making based on health
            if (player.getHealth() < 30) {
                // Low health: look for medkit or run to safe zone
                Item medkit = findNearestMedkit(player, gameState);
                if (medkit != null) {
                    player.moveTowards(medkit.getX(), medkit.getY());
                    logger.fine(player.getId() + " seeking medkit");
                    continue;
                }
            }

            if (!gameState.getSafeZone().isInside(player.getX(), player.getY())) {
                player.moveTowards(
                    gameState.getSafeZone().getCenterX(),
                    gameState.getSafeZone().getCenterY()
                );
                logger.fine(player.getId() + " moving toward safe zone");
            } else {
                Player enemy = findNearestEnemy(player, gameState);
                
                if (enemy != null) {
                    double dist = distance(player.getX(), player.getY(), enemy.getX(), enemy.getY());
                    
                    // Smarter AI: if low health, run away
                    if (player.getHealth() < 40 && enemy.getHealth() > player.getHealth()) {
                        // Run away from enemy
                        int dx = player.getX() - enemy.getX();
                        int dy = player.getY() - enemy.getY();
                        player.move(dx > 0 ? 5 : -5, dy > 0 ? 5 : -5);
                        logger.fine(player.getId() + " running from " + enemy.getId());
                    } else if (dist < 30) {
                        player.moveTowards(enemy.getX(), enemy.getY());
                        logger.fine(player.getId() + " chasing " + enemy.getId());
                    } else {
                        int dx = random.nextInt(11) - 5;
                        int dy = random.nextInt(11) - 5;
                        player.move(dx, dy);
                        logger.fine(player.getId() + " roaming to (" + player.getX() + "," + player.getY() + ")");
                    }
                }
            }
        }
        
        return events;
    }

    private List<GameEvent> collectItem(Player player, Item item, GameState gameState) {
        List<GameEvent> events = new ArrayList<>();
        
        if (item.isCollected()) {
            return events;
        }
        
        item.collect();
        gameState.removeItem(item);
        
        switch (item.getType()) {
            case MEDKIT:
                player.heal(item.getType().getValue());
                break;
            case ARMOR:
                player.addArmor(item.getType().getValue());
                break;
            case SNIPER:
                // Could upgrade weapon here
                break;
            case SHIELD:
                player.addArmor(item.getType().getValue());
                break;
        }
        
        LootEvent lootEvent = new LootEvent(player.getId(), item.getType().getDisplayName());
        events.add(lootEvent);
        logger.info(player.getId() + " collected " + item.getType().getDisplayName());
        
        return events;
    }

    private Item findNearestItem(Player player, GameState gameState) {
        Item nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Item item : gameState.getItems()) {
            if (item.isCollected()) continue;
            
            double dist = distance(player.getX(), player.getY(), item.getX(), item.getY());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = item;
            }
        }
        
        return nearest;
    }

    private Item findNearestMedkit(Player player, GameState gameState) {
        Item nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Item item : gameState.getItems()) {
            if (item.isCollected() || item.getType() != ItemType.MEDKIT) continue;
            
            double dist = distance(player.getX(), player.getY(), item.getX(), item.getY());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = item;
            }
        }
        
        return nearest;
    }

    private Player findNearestEnemy(Player player, GameState gameState) {
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player other : gameState.getAllPlayers()) {
            if (!other.isAlive() || other == player) continue;

            double dist = distance(player.getX(), player.getY(), other.getX(), other.getY());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = other;
            }
        }
        return nearest;
    }

    private double distance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
