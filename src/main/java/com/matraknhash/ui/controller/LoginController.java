package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Main;
import com.matraknhash.app.Session;
import com.matraknhash.model.Role;
import com.matraknhash.model.User;
import com.matraknhash.util.Result;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("Admin", "Employee", "Seller");
    }

    @FXML
    private void onLogin() {
        hideError();
        Result<User> result = AppContext.get().authService.login(
                usernameField.getText(), passwordField.getText());

        if (result.isFail()) { showError(result.error()); return; }

        User u = result.value();

        // optional: if user picked a role in the dropdown enforce it matches
        String picked = roleCombo.getValue();
        if (picked != null) {
            try {
                Role wanted = Role.of(picked);
                if (wanted != u.getRole()) {
                    showError("This account is not a " + picked + ".");
                    return;
                }
            } catch (Exception ignore) {}
        }

        Session.set(u);
        try {
            Main.setRoot("MainShell.fxml", "Metrkansh ERP - Dashboard");
        } catch (Exception e) {
            showError("Failed to open dashboard: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
