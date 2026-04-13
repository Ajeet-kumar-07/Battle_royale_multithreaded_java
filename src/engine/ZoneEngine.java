package engine;

import events.GameEvent;
import events.DeathEvent;
import events.ZoneDamageEvent;
import model.GameState;
import model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ZoneEngine {
    private static final Logger logger = Logger.getLogger(ZoneEngine.class.getName());
    private static final int EARLY_ZONE_DAMAGE = 5;
    private static final int MID_ZONE_DAMAGE = 10;
    private static final int LATE_ZONE_DAMAGE = 15;
    private static final int FINAL_ZONE_DAMAGE = 20;

    public List<GameEvent> applyZoneDamage(GameState gameState) {
        List<GameEvent> events = new ArrayList<>();
        int zoneDamage = getZoneDamageByPhase(gameState);
        String phase = getPhaseLabel(gameState);
        
        for (Player player : gameState.getAllPlayers()) {
            if (!player.isAlive()) continue;
            
            if (!gameState.getSafeZone().isInside(player.getX(), player.getY())) {
                player.takeDamage(zoneDamage, "Zone");
                gameState.getStats().recordZoneDamage(player.getId(), zoneDamage);
                
                ZoneDamageEvent event = new ZoneDamageEvent(player.getId(), zoneDamage);
                events.add(event);
                logger.info(player.getId() + " took zone damage (" + zoneDamage + ", " + phase + ")");

                if (!player.isAlive()) {
                    gameState.getStats().recordDeath(player.getId());
                    events.add(new DeathEvent(player.getId(), "Zone"));
                }
            }
        }
        
        return events;
    }

    private int getZoneDamageByPhase(GameState gameState) {
        int radius = gameState.getSafeZone().getRadius();

        if (radius >= 40) return EARLY_ZONE_DAMAGE;
        if (radius >= 30) return MID_ZONE_DAMAGE;
        if (radius >= 20) return LATE_ZONE_DAMAGE;
        return FINAL_ZONE_DAMAGE;
    }

    private String getPhaseLabel(GameState gameState) {
        int radius = gameState.getSafeZone().getRadius();

        if (radius >= 40) return "EARLY";
        if (radius >= 30) return "MID";
        if (radius >= 20) return "LATE";
        return "FINAL";
    }

    public void shrinkZone(GameState gameState) {
        gameState.getSafeZone().shrinkZone();
        logger.fine("Zone shrunk to radius: " + gameState.getSafeZone().getRadius());
    }
}
