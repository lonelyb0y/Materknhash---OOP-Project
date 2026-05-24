package com.matraknhash.net;

import com.matraknhash.service.SaleService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class InvoiceServer {

    private final int port;
    private final SaleService saleService;
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "invoice-worker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket server;
    private Thread acceptThread;

    public InvoiceServer(int port, SaleService saleService) {
        this.port = port;
        this.saleService = saleService;
    }

    public void start() throws IOException {
        if (running.getAndSet(true)) return;
        server = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "invoice-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("[InvoiceServer] listening on tcp://localhost:" + port);
    }

    public void stop() {
        running.set(false);
        try { if (server != null) server.close(); } catch (IOException ignore) {}
        workers.shutdownNow();
    }

    private void acceptLoop() {
        while (running.get() && !server.isClosed()) {
            try {
                Socket sock = server.accept();
                workers.submit(() -> handle(sock));
            } catch (IOException e) {
                if (running.get()) System.err.println("[InvoiceServer] accept: " + e.getMessage());
            }
        }
    }

    private void handle(Socket sock) {
        try (sock;
             ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream in   = new ObjectInputStream(sock.getInputStream())) {

            Object obj = in.readObject();
            if (!(obj instanceof InvoiceMessage msg)) {
                out.writeObject(InvoiceMessage.error("bad message"));
                return;
            }
            switch (msg.getType()) {
                case INVOICE_REQUEST -> {
                    try {
                        var saved = saleService.create(msg.getSale());
                        out.writeObject(InvoiceMessage.ack(saved.getId()));
                    } catch (Exception e) {
                        out.writeObject(InvoiceMessage.error(e.getMessage()));
                    }
                }
                default -> out.writeObject(InvoiceMessage.error("unsupported type"));
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[InvoiceServer] handler: " + e.getMessage());
        }
    }

    public int port() { return port; }
    public boolean isRunning() { return running.get(); }
}
