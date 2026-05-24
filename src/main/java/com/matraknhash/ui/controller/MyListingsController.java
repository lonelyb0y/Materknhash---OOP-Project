package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.Part;
import com.matraknhash.model.Supplier;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.Locale;


public class MyListingsController {

    @FXML private TextField skuField, nameField, categoryField, makeField, modelField, costField, priceField, qtyField, imgUrlField;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private Label formError, formInfo;

    @FXML private TableView<Part> table;
    @FXML private TableColumn<Part, Number> colId, colPrice, colQty;
    @FXML private TableColumn<Part, String> colSku, colName, colStat, colNote;

    private final ObservableList<Part> items = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        supplierCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Supplier s) {
                if (s == null) return "";
                return (s.isTrusted() ? "★ " : "") + s.getName();
            }
            @Override public Supplier fromString(String s) { return null; }
        });

        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSku()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSellPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colStat.setCellValueFactory(c -> new SimpleStringProperty(pretty(c.getValue())));
        colNote.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getListingReason() == null ? "" : c.getValue().getListingReason()));

        table.setItems(items);

        // Load suppliers and listings in background to avoid freezing UI
        new Thread(() -> {
            try {
                var sups = AppContext.get().supplierService.all();
                var me = Session.current();
                var listings = me != null ? AppContext.get().listingService.bySeller(me.getId()) : java.util.List.<Part>of();
                javafx.application.Platform.runLater(() -> {
                    supplierCombo.setItems(FXCollections.observableArrayList(sups));
                    items.setAll(listings);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onRefresh() { refresh(); }

    private void refresh() {
        var me = Session.current();
        if (me == null) return;
        items.setAll(AppContext.get().listingService.bySeller(me.getId()));
    }

    @FXML
    private void onClear() {
        skuField.clear(); nameField.clear(); categoryField.clear();
        makeField.clear(); modelField.clear();
        costField.clear(); priceField.clear(); qtyField.clear(); imgUrlField.clear();
        supplierCombo.setValue(null);
        hideMessages();
    }

    @FXML
    private void onSubmit() {
        hideMessages();
        try {
            if (skuField.getText().isBlank() || nameField.getText().isBlank())
                throw new IllegalArgumentException("SKU and Name are required.");
            double cost  = Double.parseDouble(costField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            int qty      = Integer.parseInt(qtyField.getText().trim());
            if (cost <= 0 || price <= 0 || qty < 0) throw new IllegalArgumentException("Cost > 0, Price > 0 and stock >= 0.");

            Part p = new Part();
            p.setSku(skuField.getText().trim());
            p.setName(nameField.getText().trim());
            p.setCategory(categoryField.getText().trim());
            p.setCarMake(makeField.getText().trim());
            p.setCarModel(modelField.getText().trim());
            p.setCostPrice(cost);
            p.setSellPrice(price);
            p.setQuantity(qty);
            p.setMinQty(0);
            p.setImageUrl(imgUrlField.getText() == null || imgUrlField.getText().isBlank() ? null : imgUrlField.getText().trim());
            Supplier sup = supplierCombo.getValue();
            if (sup != null) p.setSupplierId(sup.getId());

            Part saved = AppContext.get().listingService.submit(p, Session.current().getId());
            String msg = saved.getListingStatus() == Part.ListingStatus.LIVE
                    ? "✅ Listing is LIVE on the marketplace (trusted supplier)."
                    : "Listing submitted — awaiting employee review.";
            showInfo(msg);
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Price and stock must be valid numbers.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private static String pretty(Part p) {
        return switch (p.getListingStatus() == null ? Part.ListingStatus.LIVE : p.getListingStatus()) {
            case DRAFT             -> "Draft";
            case PENDING_EMPLOYEE  -> "Awaiting employee";
            case PENDING_ADMIN     -> "Awaiting admin";
            case LIVE              -> "★ Live";
            case REJECTED          -> "Rejected";
            case HIDDEN            -> "Hidden";
        };
    }

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

    private void showError(String s) { formError.setText(s); formError.setManaged(true); formError.setVisible(true); formInfo.setManaged(false); formInfo.setVisible(false); }
    private void showInfo(String s)  { formInfo.setText(s);  formInfo.setManaged(true);  formInfo.setVisible(true);  formError.setManaged(false); formError.setVisible(false); }
    private void hideMessages()      { formError.setManaged(false); formError.setVisible(false); formInfo.setManaged(false); formInfo.setVisible(false); }
}
