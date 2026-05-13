package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.model.Supplier;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.Optional;

public class SuppliersController {

    @FXML private TextField nameField, phoneField, emailField, addressField;
    @FXML private CheckBox  trustedBox;
    @FXML private Label formError;
    @FXML private TableView<Supplier> table;
    @FXML private TableColumn<Supplier, Number> colId;
    @FXML private TableColumn<Supplier, String> colName, colPhone, colEmail, colAddress, colTrusted;
    @FXML private TableColumn<Supplier, Void>   colAct;

    private final ObservableList<Supplier> items = FXCollections.observableArrayList();
    private Supplier editing;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        colTrusted.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isTrusted() ? "★ Yes" : "—"));
        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("✎");
            private final Button del  = new Button("🗑");
            private final HBox box = new HBox(6, edit, del);
            {
                edit.getStyleClass().add("btn-secondary");
                del.getStyleClass().add("btn-danger");
                edit.setOnAction(e -> load(getTableView().getItems().get(getIndex())));
                del.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : box); }
        });
        table.setItems(items);
        reload();
    }

    private void reload() { items.setAll(AppContext.get().supplierService.all()); }

    private void load(Supplier s) {
        editing = s;
        nameField.setText(s.getName());
        phoneField.setText(s.getPhone());
        emailField.setText(s.getEmail());
        addressField.setText(s.getAddress());
        trustedBox.setSelected(s.isTrusted());
    }

    private void delete(Supplier s) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete supplier \"" + s.getName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            AppContext.get().supplierService.delete(s.getId());
            reload();
        }
    }

    @FXML private void onClear() {
        editing = null;
        nameField.clear(); phoneField.clear(); emailField.clear(); addressField.clear();
        trustedBox.setSelected(false);
        formError.setVisible(false); formError.setManaged(false);
    }

    @FXML private void onSave() {
        try {
            if (nameField.getText().isBlank()) throw new IllegalArgumentException("Name is required");
            Supplier s = editing != null ? editing : new Supplier();
            s.setName(nameField.getText().trim());
            s.setPhone(phoneField.getText().trim());
            s.setEmail(emailField.getText().trim());
            s.setAddress(addressField.getText().trim());
            s.setTrusted(trustedBox.isSelected());
            AppContext.get().supplierService.save(s);
            onClear();
            reload();
        } catch (Exception e) {
            formError.setText(e.getMessage());
            formError.setVisible(true); formError.setManaged(true);
        }
    }
}
