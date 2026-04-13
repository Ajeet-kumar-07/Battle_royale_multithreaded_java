package engine;

import events.*;
import model.*;
import util.LootSpawner;
import util.MapRenderer;
import util.WeaponFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class GameEngine {
    private static final Logger logger = Logger.getLogger(GameEngine.class.getName());
    private static final boolean COMMAND_DRIVEN_MODE = true;
    private static final int COMMAND_ATTACK_RANGE = 20;
    
    private final GameState gameState;
    private final CombatEngine combatEngine;
    private final MovementEngine movementEngine;
    private final ZoneEngine zoneEngine;
    private final GameEventBus eventBus;
    private final ScheduledExecutorService scheduler;
    
    private network.GameServer server;

    public GameEngine() {
        this.gameState = new GameState();
        this.combatEngine = new CombatEngine();
        this.movementEngine = new MovementEngine();
        this.zoneEngine = new ZoneEngine();
        this.eventBus = new GameEventBus();
        this.scheduler = Executors.newScheduledThreadPool(1);

        this.eventBus.registerListener(event -> {
            logger.info(event.getEventType() + ": " + event.getMessage());
            if (server != null) {
                server.broadcast(event.getMessage());
            }
        });
    }

    public void setServer(network.GameServer server) {
        this.server = server;
    }

    public void addPlayer(Player player) {
        gameState.addPlayer(player);
        logger.info("Player added: " + player.getId());
    }

    public synchronized Player addPlayerIfAbsent(String playerId) {
        Player existing = gameState.getPlayer(playerId);
        if (existing != null) {
            return existing;
        }

        Random random = new Random();
        int x = random.nextInt(100);
        int y = random.nextInt(100);
        Player created = new Player(playerId, WeaponFactory.getRandomWeapon(), x, y);
        addPlayer(created);
        return created;
    }

    public Player getPlayer(String playerId) {
        return gameState.getPlayer(playerId);
    }

    public boolean isGameRunning() {
        return gameState.isGameRunning();
    }

    public void registerEventListener(Consumer<GameEvent> listener) {
        eventBus.registerListener(listener);
    }

    public synchronized String executeCommand(String playerId, String command) {
        Player player = gameState.getPlayer(playerId);
        if (player == null) {
            return "Player not found: " + playerId;
        }
        if (command == null || command.trim().isEmpty()) {
            return "Empty command.";
        }

        String[] parts = command.trim().split("\\s+");
        String action = parts[0].toLowerCase();
        String remainder = "";
        int firstSpace = command.trim().indexOf(' ');
        if (firstSpace >= 0 && firstSpace + 1 < command.trim().length()) {
            remainder = command.trim().substring(firstSpace + 1).trim();
        }

        switch (action) {
            case "move":
                if (parts.length < 2) return "Usage: move north|south|east|west";
                return movePlayer(player, parts[1]);
            case "attack":
                if (parts.length < 2) return "Usage: attack <player>";
                return attackPlayer(player, parts[1]);
            case "status":
                return getStatus(playerId);
            case "scan":
                return scanNearby(playerId);
            case "pickup":
                return pickupItem(player);
            case "say":
            case "chat":
                if (remainder.isEmpty()) return "Usage: say <message>";
                return sendChat(player, remainder);
            case "help":
                return getHelpText();
            default:
                return "Unknown command. Type 'help' for usage.";
        }
    }

    public synchronized String getStatus(String playerId) {
        Player player = gameState.getPlayer(playerId);
        if (player == null) {
            return "Player not found: " + playerId;
        }

        return "Health: " + player.getHealth()
                + " | Armor: " + player.getArmor()
                + " | Weapon: " + player.getWeapon().getName()
                + " | Kills: " + player.getKills()
                + " | Pos: (" + player.getX() + "," + player.getY() + ")"
                + " | Alive: " + player.isAlive();
    }

    public synchronized String scanNearby(String playerId) {
        Player player = gameState.getPlayer(playerId);
        if (player == null) {
            return "Player not found: " + playerId;
        }

        StringBuilder nearby = new StringBuilder();
        for (Player other : gameState.getAlivePlayers()) {
            if (other == player) continue;
            double dist = distance(player, other);
            if (dist <= 25) {
                if (nearby.length() > 0) nearby.append(", ");
                nearby.append(other.getId()).append(" (").append((int) dist).append("m)");
            }
        }

        if (nearby.length() == 0) {
            return "Nearby players: none";
        }
        return "Nearby players: " + nearby;
    }

    public synchronized String pickupItem(Player player) {
        if (!gameState.isGameRunning()) return "Game has not started yet.";
        if (!player.isAlive()) return "You are eliminated.";

        Item nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Item item : gameState.getItems()) {
            if (item.isCollected()) continue;
            double dx = player.getX() - item.getX();
            double dy = player.getY() - item.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = item;
            }
        }

        if (nearest == null || nearestDist > 10) {
            return "No items nearby to pick up.";
        }

        nearest.collect();
        String result;
        switch (nearest.getType()) {
            case MEDKIT:
                player.heal(nearest.getType().getValue());
                result = "Picked up MEDKIT! Healed " + nearest.getType().getValue() + " HP.";
                break;
            case ARMOR:
                player.addArmor(nearest.getType().getValue());
                result = "Picked up ARMOR! +" + nearest.getType().getValue() + " armor.";
                break;
            case SHIELD:
                player.addArmor(nearest.getType().getValue());
                result = "Picked up SHIELD! +" + nearest.getType().getValue() + " armor.";
                break;
            case SNIPER:
                player.setWeapon(new Weapon("Sniper", 50, 30));
                result = "Picked up SNIPER! New weapon equipped.";
                break;
            default:
                result = "Picked up " + nearest.getType().getDisplayName() + ".";
                break;
        }

        processCommandDrivenUpdate();
        return result;
    }

    public String getHelpText() {
        return "move north|south|east|west | attack <player> | pickup | say <message> | status | scan | help";
    }

    public synchronized String sendChat(Player player, String message) {
        if (player == null) {
            return "Player not found.";
        }

        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            return "Message cannot be empty.";
        }

        if (text.length() > 140) {
            text = text.substring(0, 140);
        }

        eventBus.publish(new ChatEvent(player.getId(), text));
        return "You: " + text;
    }

    public synchronized String getMapSnapshot() {
        return MapRenderer.renderToString(gameState.getAllPlayers(), gameState.getSafeZone());
    }

    public synchronized String movePlayer(Player player, String direction) {
        if (player == null) {
            return "Player not found.";
        }
        if (!gameState.isGameRunning()) {
            return "Game has not started yet.";
        }
        if (!player.isAlive()) {
            return "You are eliminated and cannot move.";
        }

        String normalizedDirection = direction.toLowerCase();
        switch (normalizedDirection) {
            case "north":
                player.move(0, -5);
                break;
            case "south":
                player.move(0, 5);
                break;
            case "east":
                player.move(5, 0);
                break;
            case "west":
                player.move(-5, 0);
                break;
            default:
                return "Usage: move north|south|east|west";
        }

        String update = "🚶 " + player.getId() + " moved " + normalizedDirection +
                " to (" + player.getX() + "," + player.getY() + ")";
        if (server != null) {
            server.broadcast(update);
        }

        processCommandDrivenUpdate();

        return "You moved " + normalizedDirection;
    }

    public synchronized String attackPlayer(Player attacker, String targetId) {
        if (attacker == null) {
            return "Player not found.";
        }
        if (!gameState.isGameRunning()) {
            return "Game has not started yet.";
        }
        if (!attacker.isAlive()) {
            return "You are eliminated and cannot attack.";
        }

        Player target = gameState.getPlayer(targetId);
        if (target == null) {
            return "Player not found: " + targetId;
        }
        if (target == attacker) {
            return "You cannot attack yourself.";
        }
        if (!target.isAlive()) {
            return target.getId() + " is already eliminated.";
        }

        double dist = distance(attacker, target);
        if (dist > COMMAND_ATTACK_RANGE) {
            gameState.getStats().recordAttack(false, 0);
            eventBus.publish(new AttackEvent(attacker.getId(), target.getId(), 0, false));
            processCommandDrivenUpdate();
            return "Target is out of range (" + String.format("%.1f", dist) + "m).";
        }

        int damage = attacker.attack();
        int actualDamage = target.calculateDamageAfterArmor(damage);
        target.takeDamage(actualDamage, attacker.getId());
        gameState.getStats().recordAttack(true, actualDamage);

        eventBus.publish(new AttackEvent(attacker.getId(), target.getId(), actualDamage, true));

        processCommandDrivenUpdate();

        if (!target.isAlive()) {
            attacker.addKill();
            List<String> unlocked = gameState.getStats().recordElimination(attacker.getId(), target.getId());
            String killMsg = "⚔️ " + attacker.getId() + " killed " + target.getId() + " with " + attacker.getWeapon().getName() + "!";
            if (server != null) {
                server.broadcast(killMsg);
            }
            eventBus.publish(new DeathEvent(target.getId(), attacker.getId()));
            publishAchievementMessages(unlocked);
            return "You eliminated " + target.getId() + " (" + actualDamage + " dmg).";
        }

        String attackMsg = "⚡ " + attacker.getId() + " hit " + target.getId() + " for " + actualDamage + " dmg!";
        if (server != null) {
            server.broadcast(attackMsg);
        }
        return "You attacked " + target.getId() + " (" + actualDamage + " dmg).";
    }

    public void startGame() {
        if (gameState.getAllPlayers().isEmpty()) {
            logger.warning("No players were added.");
            return;
        }

        gameState.setGameRunning(true);
        gameState.resetZoneShrinkTimer();
        logger.info("🎮 Game started with " + gameState.getAllPlayers().size() + " players");
        
        // Spawn initial loot
        LootSpawner.spawnLoot(gameState);

        if (server != null) {
            server.broadcast("⌨️ Command mode active: world updates happen only when a player sends a command.");
            String map = MapRenderer.renderToString(gameState.getAllPlayers(), gameState.getSafeZone());
            server.broadcast(map);
        }
    }

    private void processCommandDrivenUpdate() {
        if (!gameState.isGameRunning()) {
            return;
        }

        if (gameState.getAlivePlayers().size() <= 1) {
            endGame();
            return;
        }

        gameState.incrementRound();
        int round = gameState.getCurrentRound();
        List<GameEvent> events = new ArrayList<>();
        events.add(new RoundEvent(round));

        // Zone damage happens on command ticks only
        events.addAll(zoneEngine.applyZoneDamage(gameState));

        // Zone shrink check (applies when at least one command is sent after interval elapsed)
        if (gameState.getTimeUntilZoneShrink() <= 0) {
            zoneEngine.shrinkZone(gameState);
            gameState.resetZoneShrinkTimer();
            events.add(new GameEvent("⚠️ Safe zone has shrunk!") {
                @Override
                public String getEventType() {
                    return "ZONE_SHRINK";
                }
            });
        }

        if (server != null) {
            String map = MapRenderer.renderToString(gameState.getAllPlayers(), gameState.getSafeZone());
            server.broadcast(map);
        }

        broadcastEvents(events);

        for (Player p : gameState.getAlivePlayers()) {
            p.setLastRoundAlive(round);
        }

        if (gameState.getAlivePlayers().size() <= 1) {
            endGame();
        }
    }

    // Note: gameLoop() is not used in COMMAND_DRIVEN_MODE but kept for reference
    @SuppressWarnings("unused")
    private void gameLoop() {
        try {
            if (!gameState.isGameRunning()) {
                return;
            }

            if (gameState.getAlivePlayers().size() <= 1) {
                endGame();
                return;
            }

            simulateRound();
            
        } catch (Exception e) {
            logger.severe("Error in game loop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void simulateRound() {
        gameState.incrementRound();
        int round = gameState.getCurrentRound();
        
        // Clear console (server side only)
        System.out.print("\033[H\033[2J");
        System.out.flush();

        List<GameEvent> allEvents = new ArrayList<>();

        // Round start event
        RoundEvent roundEvent = new RoundEvent(round);
        allEvents.add(roundEvent);
        logger.info("=== ROUND " + round + " ===");

        // Spawn loot every 5 rounds
        if (round % 5 == 0) {
            LootSpawner.spawnLoot(gameState);
        }

        // Movement phase (disabled in command-driven mode)
        if (!COMMAND_DRIVEN_MODE) {
            List<GameEvent> movementEvents = movementEngine.moveAllPlayers(gameState);
            allEvents.addAll(movementEvents);
        }

        // Render map locally
        MapRenderer.render(gameState.getAllPlayers(), gameState.getSafeZone());

        // Broadcast map to all clients
        if (server != null) {
            String map = MapRenderer.renderToString(gameState.getAllPlayers(), gameState.getSafeZone());
            server.broadcast(map);
        }

        // Combat phase (disabled in command-driven mode)
        if (!COMMAND_DRIVEN_MODE) {
            List<GameEvent> combatEvents = combatEngine.processCombat(gameState);
            allEvents.addAll(combatEvents);
        }

        // Zone damage phase
        List<GameEvent> zoneEvents = zoneEngine.applyZoneDamage(gameState);
        allEvents.addAll(zoneEvents);

        // Shrink zone every 2 minutes
        if (gameState.getTimeUntilZoneShrink() <= 0) {
            zoneEngine.shrinkZone(gameState);
            gameState.resetZoneShrinkTimer();
            allEvents.add(new GameEvent("⚠️ Safe zone has shrunk!") {
                @Override
                public String getEventType() {
                    return "ZONE_SHRINK";
                }
            });
        }

        // Broadcast all events
        broadcastEvents(allEvents);

        // Update survival stats
        for (Player player : gameState.getAlivePlayers()) {
            player.setLastRoundAlive(round);
        }
    }

    private void broadcastEvents(List<GameEvent> events) {
        for (GameEvent event : events) {
            eventBus.publish(event);
        }
    }

    private void endGame() {
        gameState.setGameRunning(false);
        scheduler.shutdown();

        List<Player> survivors = gameState.getAlivePlayers();
        
        String winMsg;
        if (survivors.isEmpty()) {
            winMsg = "No winner. All players were eliminated.";
        } else {
            Player winner = survivors.get(0);
            winMsg = "🏆 WINNER: " + winner.getId() + " with " + winner.getKills() + " kills!";
            gameState.getStats().updateLongestSurvivor(winner.getId(), winner.getLastRoundAlive());

            String clutch = gameState.getStats().recordClutchWin(winner.getId(), winner.getHealth());
            if (clutch != null) {
                eventBus.publish(new AchievementEvent(clutch));
            }
        }

        String zoneSurvivorAchievement = gameState.getStats().finalizeZoneSurvivorAchievement();
        if (zoneSurvivorAchievement != null) {
            eventBus.publish(new AchievementEvent(zoneSurvivorAchievement));
        }

        logger.info(winMsg);
        System.out.println(winMsg);
        
        if (server != null) {
            server.broadcast(winMsg);
        }

        // Update match stats for all players
        for (Player player : gameState.getAllPlayers()) {
            gameState.getStats().updateLongestSurvivor(player.getId(), player.getLastRoundAlive());
        }

        printLeaderboard();
        printMatchStats();
    }

    private void printLeaderboard() {
        StringBuilder leaderboard = new StringBuilder("\n=== LEADERBOARD ===\n");
        List<Player> players = new ArrayList<>(gameState.getAllPlayers());
        players.sort((a, b) -> b.getKills() - a.getKills());

        int rank = 1;
        for (Player p : players) {
            leaderboard.append(rank++).append(". ")
                      .append(p.getId())
                      .append(" - ")
                      .append(p.getKills())
                      .append(" kills | Survived ")
                      .append(p.getLastRoundAlive())
                      .append(" rounds\n");
        }

        String leaderboardStr = leaderboard.toString();
        logger.info(leaderboardStr);
        System.out.println(leaderboardStr);
        
        if (server != null) {
            server.broadcast(leaderboardStr);
        }
    }

    private void printMatchStats() {
        String statsReport = gameState.getStats().getReport();
        logger.info(statsReport);
        System.out.println(statsReport);
        
        if (server != null) {
            server.broadcast(statsReport);
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public Zone getSafeZone() {
        return gameState.getSafeZone();
    }

    public void setSafeZone(Zone safeZone) {
        gameState.setSafeZone(safeZone);
    }

    private double distance(Player p1, Player p2) {
        int dx = p1.getX() - p2.getX();
        int dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void publishAchievementMessages(List<String> achievementMessages) {
        if (achievementMessages == null || achievementMessages.isEmpty()) {
            return;
        }

        for (String achievement : achievementMessages) {
            eventBus.publish(new AchievementEvent(achievement));
        }
    }
}
