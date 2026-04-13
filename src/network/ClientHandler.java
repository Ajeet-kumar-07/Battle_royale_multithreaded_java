package network;

import engine.GameEngine;
import model.Player;
import util.WeaponFactory;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;





public class ClientHandler implements Runnable{
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private Socket socket;
    private GameEngine engine;
    private GameServer server;
    private PrintWriter out;
    private Player player;

    public ClientHandler(Socket socket, GameEngine engine, GameServer gameServer) {
        this.socket = socket;
        this.engine = engine;
        this.server = gameServer;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
            out.println("enter your player name;_" );

            String playerName = in.readLine();

            Random random = new Random();

            int x = random.nextInt(100);
            int y = random.nextInt(100);

                player = new Player(
                    playerName,
                    WeaponFactory.getRandomWeapon(),
                    x,
                    y
            );
            engine.addPlayer(player);
            
            logger.info("Player joined: " + playerName + " from " + socket.getInetAddress());
            out.println("✅ Welcome " + playerName + "! Waiting for game to start...");
            out.println("Type 'help' to see available commands.");
            
            server.PlayerJoined(playerName);

            String command;
            while ((command = in.readLine()) != null) {
                handleCommand(command);
            }

        }catch (Exception e){
            logger.severe("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (player != null) {
                server.removeClient(this, player.getId());
            }
            try {
                if (in != null) {
                    in.close();
                }
                socket.close();
            } catch (IOException ignored) {
            }
        }

    }

    public void sendMessage(String message){
        if(out != null){
            out.println(message);
        }
    }

    private void handleCommand(String command) {
        if (command == null) {
            return;
        }

        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        String[] parts = trimmed.split("\\s+");
        String action = parts[0].toLowerCase();

        switch (action) {
            case "move":
                handleMove(parts);
                break;
            case "attack":
                handleAttack(parts);
                break;
            case "status":
                sendStatus();
                break;
            case "scan":
                handleScan();
                break;
            case "say":
            case "chat":
                sendMessage(engine.executeCommand(player.getId(), trimmed));
                break;
            case "help":
                sendHelp();
                break;
            case "quit":
            case "exit":
                sendMessage("Disconnecting...");
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                break;
            default:
                sendMessage("Unknown command. Type 'help' for usage.");
        }
    }

    private void handleMove(String[] parts) {
        if (parts.length < 2) {
            sendMessage("Usage: move north|south|east|west");
            return;
        }

        String result = engine.movePlayer(player, parts[1]);
        sendMessage(result);
    }

    private void handleAttack(String[] parts) {
        if (parts.length < 2) {
            sendMessage("Usage: attack <player>");
            return;
        }

        String result = engine.attackPlayer(player, parts[1]);
        sendMessage(result);
    }

    private void sendStatus() {
        if (player == null) {
            sendMessage("Player not initialized.");
            return;
        }

        sendMessage("=== STATUS ===");
        sendMessage("Health: " + player.getHealth());
        sendMessage("Armor: " + player.getArmor());
        sendMessage("Weapon: " + player.getWeapon().getName());
        sendMessage("Kills: " + player.getKills());
        sendMessage("Position: (" + player.getX() + "," + player.getY() + ")");
        sendMessage("Alive: " + player.isAlive());
    }

    private void handleScan() {
        if (player == null) {
            sendMessage("Player not initialized.");
            return;
        }

        StringBuilder nearby = new StringBuilder();
        for (Player other : engine.getGameState().getAlivePlayers()) {
            if (other == player) continue;

            double dist = distance(player, other);
            if (dist <= 25) {
                if (nearby.length() > 0) {
                    nearby.append(", ");
                }
                nearby.append(other.getId()).append(" (").append((int) dist).append("m)");
            }
        }

        if (nearby.length() == 0) {
            sendMessage("Nearby players: none");
        } else {
            sendMessage("Nearby players: " + nearby);
        }
    }

    private void sendHelp() {
        sendMessage("Available commands:");
        sendMessage("move north|south|east|west");
        sendMessage("attack <player>");
        sendMessage("status");
        sendMessage("scan");
        sendMessage("say <message>");
        sendMessage("help");
        sendMessage("quit");
    }

    private double distance(Player p1, Player p2) {
        int dx = p1.getX() - p2.getX();
        int dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }


}


