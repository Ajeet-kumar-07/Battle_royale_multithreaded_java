package util;

import model.GameState;
import model.Item;
import model.ItemType;

import java.util.Random;
import java.util.logging.Logger;

public class LootSpawner {
    private static final Logger logger = Logger.getLogger(LootSpawner.class.getName());
    private static final Random random = new Random();
    private static final int ITEMS_PER_ROUND = 2;

    public static void spawnLoot(GameState gameState) {
        for (int i = 0; i < ITEMS_PER_ROUND; i++) {
            ItemType type = getRandomItemType();
            int x = random.nextInt(101);
            int y = random.nextInt(101);
            
            Item item = new Item(type, x, y);
            gameState.addItem(item);
            
            logger.fine("Spawned " + type.getDisplayName() + " at (" + x + ", " + y + ")");
        }
    }

    private static ItemType getRandomItemType() {
        ItemType[] types = ItemType.values();
        return types[random.nextInt(types.length)];
    }
}
