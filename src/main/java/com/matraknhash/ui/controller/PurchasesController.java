package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.Part;
import com.matraknhash.model.Purchase;
import com.matraknhash.model.PurchaseItem;
import com.matraknhash.model.Supplier;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

public class PurchasesController {

    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private ComboBox<Part> partCombo;
    @FXML private TextField qtyField, costField;

    @FXML private TableView<PurchaseItem> itemsTable;
    @FXML private TableColumn<PurchaseItem, String> colPart;
    @FXML private TableColumn<PurchaseItem, Number> colCost, colQty, colSub;
    @FXML private TableColumn<PurchaseItem, Void>   colDel;

    @FXML private Label lblDate, lblLines, lblTotal, status;

    private final ObservableList<PurchaseItem> items = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        supplierCombo.setItems(FXCollections.observableArrayList(AppContext.get().supplierService.all()));
        partCombo.setItems(FXCollections.observableArrayList(AppContext.get().partService.all()));
        partCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Part p) { return p == null ? "" : p.getSku() + "  " + p.getName(); }
            @Override public Part fromString(String s) { return null; }
        });

        colPart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartName()));
        colCost.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUnitCost()));
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colSub.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
        colDel.setCellFactory(col -> new TableCell<>() {
            private final Button x = new Button("✕");
            { x.getStyleClass().add("btn-danger"); x.setOnAction(e -> items.remove(getIndex())); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : new HBox(x)); }
        });
        itemsTable.setItems(items);

        items.addListener((javafx.collections.ListChangeListener<? super PurchaseItem>) c -> recompute());
        lblDate.setText(LocalDate.now().toString());
        recompute();
    }

    @FXML private void onAddItem() {
        Part p = partCombo.getValue();
        if (p == null) { showStatus("Select a part."); return; }
        try {
            int q = Integer.parseInt(qtyField.getText().trim());
            double cost = Double.parseDouble(costField.getText().trim());
            if (q <= 0 || cost < 0) throw new IllegalArgumentException();
            items.add(new PurchaseItem(p.getId(), p.getSku(), p.getName(), q, cost));
            qtyField.clear(); costField.clear();
            hideStatus();
        } catch (Exception e) {
            showStatus("Enter valid quantity and unit cost.");
        }
    }

    @FXML private void onClear() {
        items.clear();
        supplierCombo.setValue(null);
        partCombo.setValue(null);
        qtyField.clear(); costField.clear();
        hideStatus();
    }

    @FXML private void onSave() {
        if (items.isEmpty()) { showStatus("Add at least one item."); return; }
        Supplier sup = supplierCombo.getValue();
        if (sup == null) { showStatus("Select supplier."); return; }
        var u = Session.current();
        if (u == null) { showStatus("Not signed in."); return; }

        Purchase pur = new Purchase(sup.getId(), sup.getName(), u.getId());
        for (PurchaseItem it : items) pur.addItem(it);
        try {
            AppContext.get().purchaseService.create(pur);
            showStatus("Saved. Stock updated.");
            items.clear();
            partCombo.setItems(FXCollections.observableArrayList(AppContext.get().partService.all()));
        } catch (Exception e) {
            showStatus("Save failed: " + e.getMessage());
        }
    }

    private void recompute() {
        double sum = items.stream().mapToDouble(PurchaseItem::getSubtotal).sum();
        lblLines.setText(String.valueOf(items.size()));
        lblTotal.setText(money.format(sum) + " EGP");
    }

    private void showStatus(String s) { status.setText(s); status.setVisible(true); status.setManaged(true); }
    private void hideStatus()         { status.setVisible(false); status.setManaged(false); }
}
