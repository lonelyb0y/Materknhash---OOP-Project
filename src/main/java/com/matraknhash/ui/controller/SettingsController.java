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
        lblDb.setText(ConnectionFactory.dbPath());
        lblSocket.setText("tcp://localhost:" + ctx.socketPort
                + (ctx.invoiceServer.isRunning() ? "  (running)" : "  (offline)"));
        lblMonitor.setText(ctx.stockMonitor.isRunning() ? "running (30s interval)" : "stopped");
    }
}
