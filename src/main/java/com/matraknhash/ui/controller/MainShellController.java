package com.matraknhash.ui.controller;

import com.matraknhash.app.Main;
import com.matraknhash.app.Session;
import com.matraknhash.model.Role;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

public class MainShellController {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Label userLabel;
    @FXML private Label roleLabel;

    @FXML private Button navDashboard, navParts, navSales, navPurchases,
            navSuppliers, navReports, navUsers, navSettings;

    private final List<Button> navItems = java.util.List.of();
    private Button activeButton;

    @FXML
    public void initialize() {
        var u = Session.current();
        if (u != null) {
            userLabel.setText(u.getFullName());
            roleLabel.setText("(" + u.getRole().name() + ")");
            applyRoleVisibility(u.getRole());
        }
        // default screen
        showDashboard();
    }

    private void applyRoleVisibility(Role role) {
        // Sellers only see Dashboard + Sales
        boolean isSeller = role == Role.SELLER;
        boolean isAdmin  = role == Role.ADMIN;

        toggle(navParts,     !isSeller);
        toggle(navPurchases, !isSeller);
        toggle(navSuppliers, !isSeller);
        toggle(navReports,   !isSeller);
        toggle(navUsers,     isAdmin);
        toggle(navSettings,  isAdmin);
    }

    private void toggle(Button b, boolean show) {
        b.setVisible(show);
        b.setManaged(show);
    }

    @FXML private void showDashboard() { swap("Dashboard.fxml", "Dashboard", navDashboard); }
    @FXML private void showParts()     { swap("Parts.fxml",     "Spare Parts", navParts); }
    @FXML private void showSales()     { swap("Sales.fxml",     "Sales (New Invoice)", navSales); }
    @FXML private void showPurchases() { swap("Purchases.fxml", "Purchases", navPurchases); }
    @FXML private void showSuppliers() { swap("Suppliers.fxml", "Suppliers", navSuppliers); }
    @FXML private void showReports()   { swap("Reports.fxml",   "Reports & Analytics", navReports); }
    @FXML private void showUsers()     { swap("Users.fxml",     "User Management", navUsers); }
    @FXML private void showSettings()  { swap("Settings.fxml",  "Settings", navSettings); }

    @FXML
    private void onLogout() {
        Session.clear();
        try {
            Main.setRoot("Login.fxml", "Metrkansh ERP - Login");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void swap(String fxml, String title, Button active) {
        try {
            Node node = FXMLLoader.load(getClass().getResource("/fxml/" + fxml));
            contentArea.getChildren().setAll(node);
            pageTitle.setText(title);
            setActive(active);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load " + fxml + ":\n" + root.getClass().getSimpleName() + ": " + root.getMessage()).showAndWait();
        }
    }

    private void setActive(Button b) {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        if (b != null && !b.getStyleClass().contains("active")) b.getStyleClass().add("active");
        activeButton = b;
    }
}
