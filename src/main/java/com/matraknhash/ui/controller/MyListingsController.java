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

/**
 * Seller dashboard for product listings. Adds new ones (auto-routed through
 * the approval pipeline based on supplier trust), and shows current status
 * of every part the seller owns.
 */
public class MyListingsController {

    @FXML private TextField skuField, nameField, categoryField, makeField, modelField, priceField, qtyField;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private Label formError, formInfo;
    @FXML private Button btnSubmit, btnUpdate, btnDelete;
    private Part selectedPart;

    @FXML private TableView<Part> table;
    @FXML private TableColumn<Part, Number> colId, colPrice, colQty;
    @FXML private TableColumn<Part, String> colSku, colName, colStat, colNote;

    private final ObservableList<Part> items = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        supplierCombo.setItems(FXCollections.observableArrayList(AppContext.get().supplierService.all()));
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
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectItem(newV));
        refresh();
    }

    private void selectItem(Part p) {
        selectedPart = p;
        boolean selected = (p != null);
        btnSubmit.setDisable(selected);
        btnUpdate.setDisable(!selected);
        btnDelete.setDisable(!selected);

        if (selected) {
            skuField.setText(p.getSku());
            nameField.setText(p.getName());
            categoryField.setText(p.getCategory());
            makeField.setText(p.getCarMake());
            modelField.setText(p.getCarModel());
            priceField.setText(String.valueOf(p.getSellPrice()));
            qtyField.setText(String.valueOf(p.getQuantity()));
            if (p.getSupplierId() != null) {
                supplierCombo.getItems().stream().filter(s -> s.getId() == p.getSupplierId())
                        .findFirst().ifPresent(s -> supplierCombo.setValue(s));
            } else {
                supplierCombo.setValue(null);
            }
        }
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
        priceField.clear(); qtyField.clear();
        supplierCombo.setValue(null);
        hideMessages();
        selectItem(null);
    }

    @FXML
    private void onSubmit() {
        hideMessages();
        try {
            if (skuField.getText().isBlank() || nameField.getText().isBlank())
                throw new IllegalArgumentException("SKU and Name are required.");
            double price = Double.parseDouble(priceField.getText().trim());
            int qty      = Integer.parseInt(qtyField.getText().trim());
            if (price <= 0 || qty < 0) throw new IllegalArgumentException("Price > 0 and stock >= 0.");

            Part p = new Part();
            p.setSku(skuField.getText().trim());
            p.setName(nameField.getText().trim());
            p.setCategory(categoryField.getText().trim());
            p.setCarMake(makeField.getText().trim());
            p.setCarModel(modelField.getText().trim());
            p.setCostPrice(price * 0.7);  // placeholder until we add a cost field
            p.setSellPrice(price);
            p.setQuantity(qty);
            p.setMinQty(0);
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

    @FXML
    private void onUpdate() {
        if (selectedPart == null) return;
        hideMessages();
        try {
            if (skuField.getText().isBlank() || nameField.getText().isBlank())
                throw new IllegalArgumentException("SKU and Name are required.");
            double price = Double.parseDouble(priceField.getText().trim());
            int qty      = Integer.parseInt(qtyField.getText().trim());
            if (price <= 0 || qty < 0) throw new IllegalArgumentException("Price > 0 and stock >= 0.");

            selectedPart.setSku(skuField.getText().trim());
            selectedPart.setName(nameField.getText().trim());
            selectedPart.setCategory(categoryField.getText().trim());
            selectedPart.setCarMake(makeField.getText().trim());
            selectedPart.setCarModel(modelField.getText().trim());
            selectedPart.setSellPrice(price);
            selectedPart.setQuantity(qty);
            Supplier sup = supplierCombo.getValue();
            selectedPart.setSupplierId(sup != null ? sup.getId() : null);

            AppContext.get().partService.save(selectedPart);
            showInfo("Listing updated successfully.");
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Price and stock must be valid numbers.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selectedPart == null) return;
        AppContext.get().partService.delete(selectedPart.getId());
        showInfo("Listing deleted.");
        onClear();
        refresh();
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

    private void showError(String s) { formError.setText(s); formError.setManaged(true); formError.setVisible(true); formInfo.setManaged(false); formInfo.setVisible(false); }
    private void showInfo(String s)  { formInfo.setText(s);  formInfo.setManaged(true);  formInfo.setVisible(true);  formError.setManaged(false); formError.setVisible(false); }
    private void hideMessages()      { formError.setManaged(false); formError.setVisible(false); formInfo.setManaged(false); formInfo.setVisible(false); }
}
