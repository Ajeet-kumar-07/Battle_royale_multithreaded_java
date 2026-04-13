package network;

import engine.GameEngine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public class GameServer {
    private static final Logger logger = Logger.getLogger(GameServer.class.getName());
    private List<ClientHandler> clients = new ArrayList<>();
    private boolean gameStarted = false;

    private static final int MIN_PLAYERS = 3;


    private static final int PORT = 8080;
    private GameEngine engine;

    public GameServer(GameEngine engine) {
        this.engine = engine;
    }

    public void start() throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);

        logger.info("🎮 Game server started on port " + PORT);
        System.out.println("🎮 Game server started on port " + PORT);

        try {
            while (true) {

                Socket clientSocket = serverSocket.accept();

                logger.info("New player connected from " + clientSocket.getInetAddress());

                ClientHandler handler =
                        new ClientHandler(clientSocket, engine , this);

                clients.add(handler);

                new Thread(handler).start();
            }
        } finally {
            serverSocket.close();
        }
    }
    public synchronized void PlayerJoined(String playerName){
        int currentPlayers = clients.size();
        logger.info("Lobby size: " + currentPlayers + "/" + MIN_PLAYERS);
        
        broadcast("🎮 Player " + playerName + " joined the lobby");
        broadcast("🎮 Players in lobby: " + currentPlayers + "/" + MIN_PLAYERS);
        
        if(currentPlayers >= MIN_PLAYERS && !gameStarted){
            gameStarted = true;
            logger.info("✅ Minimum players reached. Starting game!");
            broadcast("⚔️ Game is starting! Get ready...");
            new Thread(()-> engine.startGame()).start();
        }
    }

    public synchronized void removeClient(ClientHandler handler, String playerName) {
        clients.remove(handler);
        logger.info("Player disconnected: " + playerName + " | Active clients: " + clients.size());
        broadcast("❌ Player " + playerName + " disconnected");
    }

    public synchronized void broadcast(String message){
        for(ClientHandler handler : clients){
            handler.sendMessage(message);
        }
    }

}
