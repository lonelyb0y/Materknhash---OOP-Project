package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.dao.DaoException;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;


public class SellerOrdersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Sale> orderTable;
    @FXML private TableColumn<Sale, String> colId, colBuyer, colStat, colWhen;
    @FXML private TableColumn<Sale, Number> colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colLname;
    @FXML private TableColumn<SaleItem, Number> colLqty, colLprc, colLsub;

    @FXML private Label lblHeader, lblCount, status;
    @FXML private Label lblAddress;
    @FXML private TextField txtCourier, txtTracking;
    @FXML private Button btnShip;

    private final ObservableList<Sale>     orders = FXCollections.observableArrayList();
    private final ObservableList<SaleItem> lines  = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colBuyer.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBuyerName() == null ? "Customer" : c.getValue().getBuyerName()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colStat.setCellValueFactory(c -> new SimpleStringProperty(pretty(c.getValue().getStatus())));
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));

        colLname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartName()));
        colLqty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colLprc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUnitPrice()));
        colLsub.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));

        orderTable.setItems(orders);
        lineTable.setItems(lines);
        orderTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));
        refresh();
    }

    @FXML
    private void onRefresh() { refresh(); }

    private void refresh() {
        var me = Session.current();
        if (me == null) return;
        List<Sale> incoming = AppContext.get().saleService.incomingForSeller(me.getId());
        orders.setAll(incoming);
        lblCount.setText(incoming.size() + " awaiting you");
        if (incoming.isEmpty()) {
            lblHeader.setText("No incoming orders right now.");
            lines.clear();
            btnShip.setDisable(true);
            lblAddress.setText("");
            txtCourier.setText("");
            txtTracking.setText("");
        } else orderTable.getSelectionModel().selectFirst();
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        lblHeader.setText("Order #" + s.getId() + " · " + s.getBuyerName() + " · "
                + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());
        
        lblAddress.setText(s.getShippingAddress() == null || s.getShippingAddress().isBlank() 
                ? "No address provided." : s.getShippingAddress());
        
        boolean canShip = (s.getStatus() == Sale.Status.PLACED || s.getStatus() == Sale.Status.SELLER_ACK);
        btnShip.setDisable(!canShip);
        txtCourier.setEditable(canShip);
        txtTracking.setEditable(canShip);
        
        if (!canShip) {
            txtCourier.setText(s.getCourierName() == null ? "" : s.getCourierName());
            txtTracking.setText(s.getTrackingNumber() == null ? "" : s.getTrackingNumber());
            status.setText("Order already shipped and forwarded — waiting for admin approval.");
        } else {
            txtCourier.setText("");
            txtTracking.setText("");
            status.setText("");
        }
    }

    @FXML
    private void onShip() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        
        String courier = txtCourier.getText() == null ? "" : txtCourier.getText().trim();
        String tracking = txtTracking.getText() == null ? "" : txtTracking.getText().trim();
        
        if (courier.isEmpty() || tracking.isEmpty()) {
            status.setText("❌ Courier Name and Tracking Number are required to ship this order.");
            return;
        }
        
        try {
            AppContext.get().saleService.shipOrder(sel.getId(), courier, tracking);
            status.setText("✅ Order #" + sel.getId() + " successfully shipped via " + courier + "!");
            refresh();
        } catch (DaoException e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    private static String pretty(Sale.Status s) {
        return switch (s) {
            case PLACED     -> "New";
            case SELLER_ACK -> "Preparing";
            case SHIPPED    -> "Shipped (out for delivery)";
            case APPROVED   -> "Delivered & Confirmed";
            default         -> s.name();
        };
    }
}
