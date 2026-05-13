package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
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
import java.util.Locale;

/**
 * Customer-side "My Orders" view: every order the current user has placed,
 * with status and line items. Read-only for now -- returns flow lands later.
 */
public class MyOrdersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Sale> orderTable;
    @FXML private TableColumn<Sale, String> colId, colSeller, colStat, colWhen;
    @FXML private TableColumn<Sale, Number> colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colLname;
    @FXML private TableColumn<SaleItem, Number> colLqty, colLprc, colLsub;

    @FXML private Label detailHeader, detailFooter;

    private final ObservableList<Sale> orders   = FXCollections.observableArrayList();
    private final ObservableList<SaleItem> lines = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSellerName() == null ? "Seller" : c.getValue().getSellerName()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colStat.setCellValueFactory(c -> new SimpleStringProperty(prettyStatus(c.getValue().getStatus())));
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
        orders.setAll(AppContext.get().saleService.ordersByBuyer(me.getId()));
        if (!orders.isEmpty()) orderTable.getSelectionModel().selectFirst();
        else {
            detailHeader.setText("You haven't placed any orders yet.");
            lines.clear();
            detailFooter.setText("");
        }
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        detailHeader.setText("Order #" + s.getId() + " · " + prettyStatus(s.getStatus())
                + " · " + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());
        detailFooter.setText(humanStatus(s));
    }

    private static String prettyStatus(Sale.Status s) {
        return switch (s) {
            case PLACED            -> "Awaiting seller";
            case SELLER_ACK        -> "Preparing (seller)";
            case APPROVED          -> "Confirmed";
            case REJECTED          -> "Rejected";
            case CANCELLED         -> "Cancelled";
            case RETURN_REQUESTED  -> "Return requested";
            case RETURN_ACK        -> "Return accepted";
            case RETURNED          -> "Returned";
            default                -> s.name();
        };
    }

    private static String humanStatus(Sale s) {
        return switch (s.getStatus()) {
            case PLACED     -> "Waiting for the seller to acknowledge your order.";
            case SELLER_ACK -> "The seller has accepted your order and an admin will finalize it.";
            case APPROVED   -> "Your order is confirmed.";
            case REJECTED   -> "The admin rejected this order" +
                    (s.getRejectReason() == null || s.getRejectReason().isBlank() ? "." : ": " + s.getRejectReason());
            default         -> "";
        };
    }
}
