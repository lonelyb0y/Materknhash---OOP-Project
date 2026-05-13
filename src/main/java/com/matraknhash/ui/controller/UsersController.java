package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.model.Role;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.Optional;

public class UsersController {

    @FXML private TextField usernameField, fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label formError;

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, Number> colId;
    @FXML private TableColumn<User, String> colUser, colName, colRole, colState;
    @FXML private TableColumn<User, Void>   colAct;

    private final ObservableList<User> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Employee", "Seller", "Customer"));

        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().name()));
        colState.setCellValueFactory(c -> {
            User u = c.getValue();
            if (!u.isActive()) return new SimpleStringProperty("Inactive");
            return new SimpleStringProperty(switch (u.getStatus()) {
                case ACTIVE           -> "Active";
                case PENDING_APPROVAL -> "Pending";
                case SUSPENDED        -> "Suspended";
            });
        });

        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button toggle = new Button("toggle");
            private final Button del = new Button("🗑");
            private final HBox box = new HBox(6, toggle, del);
            {
                toggle.getStyleClass().add("btn-secondary");
                del.getStyleClass().add("btn-danger");
                toggle.setOnAction(e -> {
                    AppContext.get().userService.toggleActive(getTableView().getItems().get(getIndex()));
                    reload();
                });
                del.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete user \"" + u.getUsername() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                    Optional<ButtonType> r = a.showAndWait();
                    if (r.isPresent() && r.get() == ButtonType.OK) {
                        AppContext.get().userService.delete(u.getId());
                        reload();
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : box); }
        });

        table.setItems(items);
        reload();
    }

    private void reload() { items.setAll(AppContext.get().userService.all()); }

    @FXML private void onClear() {
        usernameField.clear(); fullNameField.clear(); passwordField.clear();
        roleCombo.setValue(null);
        formError.setVisible(false); formError.setManaged(false);
    }

    @FXML private void onSave() {
        try {
            if (usernameField.getText().isBlank() || passwordField.getText().isEmpty() || roleCombo.getValue() == null)
                throw new IllegalArgumentException("Username, password and role are required");
            Role r = Role.of(roleCombo.getValue());
            AppContext.get().userService.createUser(
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    fullNameField.getText().trim(),
                    r);
            onClear();
            reload();
        } catch (Exception e) {
            formError.setText(e.getMessage());
            formError.setVisible(true); formError.setManaged(true);
        }
    }
}
