import  engine.GameEngine;
import network.GameServer;
import network.WebUiServer;
import java.util.logging.Logger;
import java.util.logging.Level;



public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args)throws Exception {
        // Configure logging
        Logger.getLogger("").setLevel(Level.INFO);
        
        logger.info("🚀 Initializing Battle Royale Server...");
        
        GameEngine engine = new GameEngine();
        GameServer server = new GameServer(engine);
        WebUiServer webUiServer = new WebUiServer(engine);
        engine.setServer(server);

        logger.info("✅ Server components initialized");
        webUiServer.start();
        server.start();
//        Random random = new Random();
//
//        for(int i = 1 ; i <= 5 ; i++){
//            int x = random.nextInt(101);
//            int y = random.nextInt(101);
//            engine.addPlayer(new Player("player"+i, WeaponFactory.getRandomWeapon(),x,y));
//        }
//        engine.startGame();
    }
}