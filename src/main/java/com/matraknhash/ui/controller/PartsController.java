package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.model.Part;
import com.matraknhash.model.Supplier;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class PartsController {

    @FXML private TextField skuField, nameField, categoryField, makeField, modelField,
            costField, priceField, qtyField, minField, searchField, imgUrlField;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private Label formError;

    @FXML private TableView<Part> table;
    @FXML private TableColumn<Part, Number> colId;
    @FXML private TableColumn<Part, String> colSku, colName, colCat;
    @FXML private TableColumn<Part, Number> colQty;
    @FXML private TableColumn<Part, Number> colPrice;
    @FXML private TableColumn<Part, Void>   colAct;

    private final ObservableList<Part> items = FXCollections.observableArrayList();
    private Part editing;
    private List<Part> allParts = List.of();

    @FXML
    public void initialize() {
        table.setPlaceholder(new Label("Loading..."));

        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSku()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSellPrice()));
        addActionsColumn();

        table.setItems(items);
        
        // Load initial data asynchronously
        new Thread(() -> {
            try {
                List<Supplier> sups = AppContext.get().supplierService.all();
                List<Part> list = AppContext.get().partService.all();
                javafx.application.Platform.runLater(() -> {
                    supplierCombo.setItems(FXCollections.observableArrayList(sups));
                    allParts = list;
                    items.setAll(list);
                    table.setPlaceholder(new Label("No content in table"));
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> table.setPlaceholder(new Label("Error loading data")));
            }
        }).start();

        searchField.textProperty().addListener((o, ov, nv) -> {
            if (nv == null || nv.isBlank()) {
                items.setAll(allParts);
            } else {
                String term = nv.trim().toLowerCase();
                List<Part> filtered = allParts.stream()
                        .filter(p -> (p.getSku() != null && p.getSku().toLowerCase().contains(term))
                                  || (p.getName() != null && p.getName().toLowerCase().contains(term)))
                        .toList();
                items.setAll(filtered);
            }
        });
    }

    private void addActionsColumn() {
        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("✎");
            private final Button del  = new Button("🗑");
            private final HBox box = new HBox(6, edit, del);
            {
                edit.getStyleClass().add("btn-secondary");
                del.getStyleClass().add("btn-danger");
                edit.setOnAction(e -> loadIntoForm(getTableView().getItems().get(getIndex())));
                del.setOnAction(e -> deleteRow(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void reload() {
        new Thread(() -> {
            try {
                List<Part> list = AppContext.get().partService.all();
                javafx.application.Platform.runLater(() -> {
                    allParts = list;
                    // Apply current search term if present
                    String nv = searchField.getText();
                    if (nv == null || nv.isBlank()) {
                        items.setAll(allParts);
                    } else {
                        String term = nv.trim().toLowerCase();
                        List<Part> filtered = allParts.stream()
                                .filter(p -> (p.getSku() != null && p.getSku().toLowerCase().contains(term))
                                          || (p.getName() != null && p.getName().toLowerCase().contains(term)))
                                .toList();
                        items.setAll(filtered);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadIntoForm(Part p) {
        editing = p;
        skuField.setText(p.getSku());
        nameField.setText(p.getName());
        categoryField.setText(p.getCategory());
        makeField.setText(p.getCarMake());
        modelField.setText(p.getCarModel());
        costField.setText(String.valueOf(p.getCostPrice()));
        priceField.setText(String.valueOf(p.getSellPrice()));
        qtyField.setText(String.valueOf(p.getQuantity()));
        minField.setText(String.valueOf(p.getMinQty()));
        imgUrlField.setText(p.getImageUrl() == null ? "" : p.getImageUrl());
        if (p.getSupplierId() != null) {
            supplierCombo.getItems().stream()
                    .filter(s -> s.getId() == p.getSupplierId()).findFirst()
                    .ifPresent(supplierCombo::setValue);
        }
    }

    private void deleteRow(Part p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete part \"" + p.getName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            AppContext.get().partService.delete(p.getId());
            reload();
        }
    }

    @FXML
    private void onClear() {
        editing = null;
        for (TextField f : new TextField[]{skuField,nameField,categoryField,makeField,modelField,costField,priceField,qtyField,minField,imgUrlField}) f.clear();
        supplierCombo.setValue(null);
        hideError();
    }

    @FXML
    private void onSave() {
        try {
            Part p = editing != null ? editing : new Part();
            p.setSku(skuField.getText().trim());
            p.setName(nameField.getText().trim());
            p.setCategory(blankToNull(categoryField.getText()));
            p.setCarMake(blankToNull(makeField.getText()));
            p.setCarModel(blankToNull(modelField.getText()));
            p.setCostPrice(parseD(costField.getText(), "cost"));
            p.setSellPrice(parseD(priceField.getText(), "price"));
            p.setQuantity(parseI(qtyField.getText(), "qty"));
            p.setMinQty(parseI(minField.getText(), "min qty"));
            p.setImageUrl(blankToNull(imgUrlField.getText()));
            Supplier s = supplierCombo.getValue();
            p.setSupplierId(s == null ? null : s.getId());

            if (p.getSku().isEmpty() || p.getName().isEmpty()) throw new IllegalArgumentException("SKU and Name are required");

            AppContext.get().partService.save(p);
            onClear();
            reload();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static double parseD(String s, String f) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { throw new IllegalArgumentException("Invalid number for " + f); } }
    private static int parseI(String s, String f)    { try { return Integer.parseInt(s.trim());   } catch (Exception e) { throw new IllegalArgumentException("Invalid integer for " + f); } }

    @FXML
    private void onBrowseImage() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Select Product Image");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.io.File file = fc.showOpenDialog(skuField.getScene().getWindow());
        if (file == null) return;

        imgUrlField.setText("Uploading...");
        new Thread(() -> {
            try {
                String cloudUrl = com.matraknhash.util.ImageUploader.upload(file);
                javafx.application.Platform.runLater(() -> imgUrlField.setText(cloudUrl));
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    imgUrlField.setText("");
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to upload image: " + e.getMessage(), ButtonType.OK);
                    a.showAndWait();
                });
            }
        }).start();
    }

    private void showError(String msg) { formError.setText(msg); formError.setVisible(true); formError.setManaged(true); }
    private void hideError()           { formError.setVisible(false); formError.setManaged(false); }
}
