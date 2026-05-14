package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.db.ConnectionFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SettingsController {

    @FXML private Label lblDb;
    @FXML private Label lblSocket;
    @FXML private Label lblMonitor;

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        lblDb.setText(prettyDbUrl(ConnectionFactory.dbPath()));
        lblSocket.setText("tcp://localhost:" + ctx.socketPort
                + (ctx.invoiceServer.isRunning() ? "  (running)" : "  (offline)"));
        lblMonitor.setText(ctx.stockMonitor.isRunning() ? "running (30s interval)" : "stopped");
    }

    /** Strip the {@code jdbc:} prefix and any query string for a tidy display value. */
    private static String prettyDbUrl(String raw) {
        if (raw == null || raw.isBlank()) return "(unconfigured)";
        String s = raw.startsWith("jdbc:") ? raw.substring(5) : raw;
        int q = s.indexOf('?');
        return q >= 0 ? s.substring(0, q) : s;
    }
}
