package engine;

import events.*;
import model.GameState;
import model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class CombatEngine {
    private static final Logger logger = Logger.getLogger(CombatEngine.class.getName());
    private static final int ATTACK_RANGE = 20;
    private final Random random = new Random();

    public List<GameEvent> processCombat(GameState gameState) {
        List<GameEvent> events = new ArrayList<>();
        List<Player> alivePlayers = gameState.getAlivePlayers();
        
        if (alivePlayers.size() < 2) {
            return events;
        }

        Player attacker = alivePlayers.get(random.nextInt(alivePlayers.size()));
        Player target;

        do {
            target = alivePlayers.get(random.nextInt(alivePlayers.size()));
        } while (attacker.equals(target));

        double dist = distance(attacker, target);

        if (dist <= ATTACK_RANGE) {
            int damage = attacker.attack();
            int actualDamage = target.calculateDamageAfterArmor(damage);
            
            logger.info(attacker.getId() + " attacked " + target.getId() + " (" + actualDamage + " dmg)");
            
            target.takeDamage(actualDamage, attacker.getId());
            gameState.getStats().recordAttack(true, actualDamage);
            
            AttackEvent attackEvent = new AttackEvent(
                attacker.getId(), 
                target.getId(), 
                actualDamage, 
                true
            );
            events.add(attackEvent);

            if (!target.isAlive()) {
                attacker.addKill();
                gameState.getStats().recordElimination(attacker.getId(), target.getId());
                DeathEvent deathEvent = new DeathEvent(target.getId(), attacker.getId());
                events.add(deathEvent);
                logger.info(target.getId() + " was eliminated by " + attacker.getId());
            }
        } else {
            String missMsg = attacker.getId() + " tried to attack " + target.getId() + 
                           " but they are too far away (distance: " + String.format("%.2f", dist) + ")";
            logger.info(missMsg);
            gameState.getStats().recordAttack(false, 0);
            
            AttackEvent missEvent = new AttackEvent(
                attacker.getId(), 
                target.getId(), 
                0, 
                false
            );
            events.add(missEvent);
        }

        return events;
    }

    private double distance(Player p1, Player p2) {
        int dx = p1.getX() - p2.getX();
        int dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
