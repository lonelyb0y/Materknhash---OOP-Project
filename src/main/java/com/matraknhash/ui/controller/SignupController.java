package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Main;
import com.matraknhash.model.Role;
import com.matraknhash.model.User;
import com.matraknhash.util.Result;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Public sign-up flow. Customers register and are immediately ACTIVE.
 * Sellers register and land in PENDING_APPROVAL; an admin must approve
 * them in the "Pending Sellers" screen before they can log in.
 */
public class SignupController {

    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField     usernameField, fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel, okLabel, subtitleLabel;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Buy parts (Customer)", "Sell parts (Seller)"));
        roleCombo.setValue("Buy parts (Customer)");
        roleCombo.valueProperty().addListener((obs, oldV, newV) -> refreshHint());
        refreshHint();
    }

    private void refreshHint() {
        Role r = pickedRole();
        subtitleLabel.setText(r == Role.SELLER
                ? "Seller accounts need admin approval before they can list parts."
                : "Customers can start shopping immediately after sign-up.");
    }

    private Role pickedRole() {
        return "Sell parts (Seller)".equals(roleCombo.getValue()) ? Role.SELLER : Role.CUSTOMER;
    }

    @FXML
    private void onSignup() {
        hideMessages();
        Result<User> r = AppContext.get().authService.signup(
                usernameField.getText(),
                passwordField.getText(),
                fullNameField.getText(),
                pickedRole());
        if (r.isFail()) { showError(r.error()); return; }

        User u = r.value();
        String msg = (u.getRole() == Role.SELLER)
                ? "Application submitted! An admin will review your seller account shortly. " +
                  "You'll be able to log in once approved."
                : "Account created. You can log in now.";
        showOk(msg);
        usernameField.clear(); fullNameField.clear(); passwordField.clear();
    }

    @FXML
    private void onBackToLogin() {
        try { Main.setRoot("Login.fxml", "Metrkansh ERP - Login"); }
        catch (Exception e) { showError("Failed to open login: " + e.getMessage()); }
    }

    private void showError(String s) {
        errorLabel.setText(s);
        errorLabel.setVisible(true);  errorLabel.setManaged(true);
        okLabel.setVisible(false);    okLabel.setManaged(false);
    }
    private void showOk(String s) {
        okLabel.setText(s);
        okLabel.setVisible(true);     okLabel.setManaged(true);
        errorLabel.setVisible(false); errorLabel.setManaged(false);
    }
    private void hideMessages() {
        errorLabel.setVisible(false); errorLabel.setManaged(false);
        okLabel.setVisible(false);    okLabel.setManaged(false);
    }
}
