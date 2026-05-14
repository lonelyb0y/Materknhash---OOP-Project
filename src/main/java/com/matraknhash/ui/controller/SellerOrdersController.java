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

/**
 * Seller view of incoming marketplace orders. "Print receipt & Fulfill" moves
 * a PLACED order directly to APPROVED. "Accept Return" restores stock and
 * marks the order as RETURNED. No further admin step is required.
 */
public class SellerOrdersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Sale> orderTable;
    @FXML private TableColumn<Sale, String> colId, colBuyer, colStat, colWhen;
    @FXML private TableColumn<Sale, Number> colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colLname;
    @FXML private TableColumn<SaleItem, Number> colLqty, colLprc, colLsub;

    @FXML private Label lblHeader, lblCount, status;
    @FXML private Button btnPrint, btnAcceptReturn;

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
            btnPrint.setDisable(true);
            btnAcceptReturn.setDisable(true);
        } else orderTable.getSelectionModel().selectFirst();
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        lblHeader.setText("Order #" + s.getId() + " · " + s.getBuyerName() + " · "
                + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());

        // Fulfill button: only for PLACED
        btnPrint.setDisable(s.getStatus() != Sale.Status.PLACED);

        // Accept Return button: only for RETURN_REQUESTED
        btnAcceptReturn.setDisable(s.getStatus() != Sale.Status.RETURN_REQUESTED);

        if (s.getStatus() == Sale.Status.RETURN_REQUESTED) {
            status.setText("⚠ Customer wants to return this order. Reason: " + s.getReturnReason());
        } else if (s.getStatus() == Sale.Status.APPROVED) {
            status.setText("Order fulfilled ✓");
        } else {
            status.setText("");
        }
    }

    @FXML
    private void onPrint() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            AppContext.get().saleService.sellerAck(sel.getId());
            status.setText("Order fulfilled for order #" + sel.getId() + ".");
            refresh();
        } catch (DaoException e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onAcceptReturn() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            AppContext.get().saleService.approveReturn(sel.getId());
            status.setText("Return accepted for order #" + sel.getId() + ". Stock has been restored.");
            refresh();
        } catch (DaoException e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    private static String pretty(Sale.Status s) {
        return switch (s) {
            case PLACED            -> "New (fulfill)";
            case RETURN_REQUESTED  -> "⚠ Return requested";
            case APPROVED          -> "Fulfilled";
            default                -> s.name();
        };
    }
}
