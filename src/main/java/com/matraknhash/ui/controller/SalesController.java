package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.Part;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;
import com.matraknhash.net.InvoiceClient;
import com.matraknhash.net.InvoiceMessage;
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

public class SalesController {

    private static final double TAX_RATE = 0.14;

    @FXML private TextField customerName, customerPhone, qtyField;
    @FXML private ComboBox<Part> partCombo;

    @FXML private TableView<SaleItem> itemsTable;
    @FXML private TableColumn<SaleItem, String> colPart;
    @FXML private TableColumn<SaleItem, Number> colPrice, colQty, colSub;
    @FXML private TableColumn<SaleItem, Void>   colDel;

    @FXML private Label lblInvNo, lblDate, lblSubtotal, lblTax, lblTotal, status;

    private final ObservableList<SaleItem> items = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        // Show placeholder while loading
        partCombo.setPromptText("Loading parts...");
        partCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Part p) { return p == null ? "" : p.getSku() + "  " + p.getName(); }
            @Override public Part fromString(String s) { return null; }
        });

        colPart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartName()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUnitPrice()));
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colSub.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
        addRemoveColumn();

        itemsTable.setItems(items);
        items.addListener((javafx.collections.ListChangeListener<? super SaleItem>) c -> recompute());
        lblDate.setText(LocalDate.now().toString());
        lblInvNo.setText("INV-...");
        recompute();

        // Load parts list in background to avoid freezing UI
        new Thread(() -> {
            try {
                var allParts = AppContext.get().partService.all();
                int count = AppContext.get().saleService.countAll();
                javafx.application.Platform.runLater(() -> {
                    partCombo.setItems(FXCollections.observableArrayList(allParts));
                    partCombo.setPromptText("Select a part");
                    lblInvNo.setText("INV-" + (count + 1));
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> partCombo.setPromptText("Error loading parts"));
            }
        }).start();
    }

    private void addRemoveColumn() {
        colDel.setCellFactory(col -> new TableCell<>() {
            private final Button x = new Button("✕");
            {
                x.getStyleClass().add("btn-danger");
                x.setOnAction(e -> items.remove(getIndex()));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(x));
            }
        });
    }

    @FXML
    private void onAddItem() {
        Part p = partCombo.getValue();
        if (p == null) { showStatus("Select a part."); return; }
        int qty;
        try { qty = Integer.parseInt(qtyField.getText().trim()); }
        catch (Exception e) { showStatus("Enter a valid quantity."); return; }
        if (qty <= 0) { showStatus("Quantity must be > 0."); return; }
        if (qty > p.getQuantity()) { showStatus("Only " + p.getQuantity() + " in stock."); return; }

        items.add(new SaleItem(p.getId(), p.getSku(), p.getName(), qty, p.getSellPrice()));
        qtyField.clear();
        hideStatus();
    }

    @FXML
    private void onClear() {
        items.clear();
        customerName.clear();
        customerPhone.clear();
        partCombo.setValue(null);
        qtyField.clear();
        hideStatus();
    }

    @FXML
    private void onSaveInvoice() {
        if (items.isEmpty()) { showStatus("Add at least one item."); return; }
        var u = Session.current();
        if (u == null) { showStatus("Not signed in."); return; }

        Sale sale = new Sale(u.getId(), u.getFullName());
        for (SaleItem it : items) sale.addItem(it);

        // Submit via socket to the local InvoiceServer.
        AppContext ctx = AppContext.get();
        InvoiceClient client = new InvoiceClient("localhost", ctx.socketPort);
        try {
            InvoiceMessage reply = client.send(sale);
            switch (reply.getType()) {
                case INVOICE_ACK -> {
                    // Stock won't change yet — invoice is PENDING until an admin/employee approves.
                    showStatus("Submitted (#" + reply.getInfo() + ") — waiting for approval.");
                    items.clear();
                    lblInvNo.setText("INV-" + (ctx.saleService.countAll() + 1));
                }
                case INVOICE_ERROR -> showStatus("Server rejected invoice: " + reply.getInfo());
                default -> showStatus("Unexpected reply: " + reply.getType());
            }
        } catch (Exception e) {
            showStatus("Socket error: " + e.getMessage());
        }
    }

    private void recompute() {
        double sub = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
        double tax = sub * TAX_RATE;
        double total = sub + tax;
        lblSubtotal.setText(money.format(sub) + " EGP");
        lblTax.setText(money.format(tax) + " EGP");
        lblTotal.setText(money.format(total) + " EGP");
    }

    private void showStatus(String s) { status.setText(s); status.setVisible(true); status.setManaged(true); }
    private void hideStatus()         { status.setVisible(false); status.setManaged(false); }
}
