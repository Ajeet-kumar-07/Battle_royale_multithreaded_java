package events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameEventBus {
    private final List<Consumer<GameEvent>> listeners = new ArrayList<>();

    public synchronized void registerListener(Consumer<GameEvent> listener) {
        listeners.add(listener);
    }

    public synchronized void publish(GameEvent event) {
        for (Consumer<GameEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
