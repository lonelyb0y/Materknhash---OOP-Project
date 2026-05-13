package com.matraknhash.thread;

import com.matraknhash.dao.PartDao;
import com.matraknhash.model.Part;
import com.matraknhash.util.EventBus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background thread that polls the database every {@code intervalSeconds}
 * seconds and publishes a LowStockEvent if any parts are at/below their
 * minimum quantity. Designed to be started once at application launch
 * and stopped at shutdown.
 */
public class StockMonitorTask implements Runnable {

    private final PartDao partDao;
    private final EventBus<LowStockEvent> bus;
    private final int intervalSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public StockMonitorTask(PartDao partDao, EventBus<LowStockEvent> bus, int intervalSeconds) {
        this.partDao = partDao;
        this.bus = bus;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        if (running.getAndSet(true)) return;
        thread = new Thread(this, "stock-monitor");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (thread != null) thread.interrupt();
    }

    public boolean isRunning() { return running.get(); }

    @Override public void run() {
        while (running.get()) {
            try {
                List<Part> low = partDao.findLowStock();
                if (!low.isEmpty()) bus.publish(new LowStockEvent(low));
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[stock-monitor] " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ie) { break; }
            }
        }
    }
}
