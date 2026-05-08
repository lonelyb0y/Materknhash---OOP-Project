package com.matraknhash.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Generic, thread-safe pub/sub bus. Used by the stock-monitor thread
 * to push LowStockEvent to the JavaFX UI without coupling to it.
 */
public class EventBus<E> {
    private final List<Consumer<E>> subs = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<E> s) { subs.add(s); }
    public void unsubscribe(Consumer<E> s) { subs.remove(s); }

    public void publish(E event) {
        for (Consumer<E> s : subs) {
            try { s.accept(event); }
            catch (Exception ignored) { /* one bad subscriber must not stop the others */ }
        }
    }
}
