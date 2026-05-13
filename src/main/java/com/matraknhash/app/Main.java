package com.matraknhash.app;

import com.matraknhash.db.DatabaseBootstrap;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    private static Stage primary;

    public static Stage primaryStage() { return primary; }

    @Override
    public void start(Stage stage) throws IOException, java.sql.SQLException {
        primary = stage;
        DatabaseBootstrap.run();
        AppContext ctx = AppContext.get();
        ctx.stockMonitor.start();
        try { ctx.invoiceServer.start(); }
        catch (IOException e) { System.err.println("[Main] socket server not started: " + e.getMessage()); }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1024, 640);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());

        stage.setTitle("Metrkansh ERP - Login");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
    }

    private void shutdown() {
        try { AppContext.get().stockMonitor.stop(); } catch (Exception ignore) {}
        try { AppContext.get().invoiceServer.stop(); } catch (Exception ignore) {}
        try { com.matraknhash.db.ConnectionFactory.shutdown(); } catch (Exception ignore) {}
    }

    public static void main(String[] args) {
        // -Dprism.lcdtext=false improves font rendering on Linux JavaFX
        System.setProperty("prism.lcdtext", "false");
        launch(args);
    }

    /** Helper for switching the main scene (used after login). */
    public static void setRoot(String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(Main.class.getResource("/fxml/" + fxml)));
        primary.getScene().setRoot(root);
        primary.setTitle(title);
    }

    /** Suppress "unused" warning placeholder used by tooling. */
    @SuppressWarnings("unused")
    private static Image appIcon() { return null; }
}
