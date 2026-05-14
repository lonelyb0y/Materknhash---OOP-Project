package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin-side, read-only history of every marketplace order, across every
 * status (placed, fulfilled, returned, cancelled, etc.). The status combo
 * lets the admin narrow the view. Stock changes are driven elsewhere
 * (seller fulfills from "Incoming Orders"); no actions live on this screen.
 */
public class AdminOrdersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String FILTER_ALL = "All statuses";

    @FXML private TableView<Sale> orderTable;
    @FXML private TableColumn<Sale, String> colId, colBuyer, colSeller, colStat, colWhen;
    @FXML private TableColumn<Sale, Number> colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colLname;
    @FXML private TableColumn<SaleItem, Number> colLqty, colLprc, colLsub;

    @FXML private Label lblHeader, lblCount, status;
    @FXML private ComboBox<String> statusFilter;

    private final ObservableList<Sale>     orders = FXCollections.observableArrayList();
    private final ObservableList<SaleItem> lines  = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);
    /** Full unfiltered list, refreshed from DB; the table shows the filtered view. */
    private List<Sale> allOrders = List.of();
    private Map<Integer, String> userNames = Map.of();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colBuyer.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBuyerName() != null ? c.getValue().getBuyerName()
                        : (c.getValue().getBuyerId() != null
                            ? userNames.getOrDefault(c.getValue().getBuyerId(), "Customer #" + c.getValue().getBuyerId())
                            : "—")));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSellerName() != null ? c.getValue().getSellerName()
                        : userNames.getOrDefault(c.getValue().getSellerId(), "Seller #" + c.getValue().getSellerId())));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colStat.setCellValueFactory(c -> new SimpleStringProperty(prettyStatus(c.getValue().getStatus())));
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));

        // Status filter combo
        statusFilter.getItems().add(FILTER_ALL);
        for (Sale.Status s : Sale.Status.values()) statusFilter.getItems().add(prettyStatus(s));
        statusFilter.getSelectionModel().select(FILTER_ALL);
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilter());

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
        userNames = new HashMap<>();
        AppContext.get().userService.all().forEach(u -> userNames.put(u.getId(), u.getFullName()));
        allOrders = AppContext.get().saleService.allMarketplaceOrders();
        applyFilter();
    }

    private void applyFilter() {
        String sel = statusFilter == null ? FILTER_ALL : statusFilter.getValue();
        List<Sale> view;
        if (sel == null || FILTER_ALL.equals(sel)) {
            view = allOrders;
        } else {
            view = allOrders.stream()
                    .filter(s -> prettyStatus(s.getStatus()).equals(sel))
                    .toList();
        }
        orders.setAll(view);
        lblCount.setText(view.size() + (FILTER_ALL.equals(sel) ? " total orders" : " matching"));
        if (view.isEmpty()) {
            lblHeader.setText("No orders match this filter.");
            lines.clear();
        } else {
            orderTable.getSelectionModel().selectFirst();
        }
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        lblHeader.setText("Order #" + s.getId() + " · " + prettyStatus(s.getStatus())
                + " · " + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());
        status.setText("");
    }

    private static String prettyStatus(Sale.Status s) {
        if (s == null) return "";
        return switch (s) {
            case PENDING           -> "Pending";
            case PLACED            -> "Placed (awaiting seller)";
            case SELLER_ACK        -> "Seller fulfilling";
            case APPROVED          -> "Approved / Fulfilled";
            case REJECTED          -> "Rejected";
            case CANCELLED         -> "Cancelled";
            case RETURN_REQUESTED  -> "⚠ Return requested";
            case RETURN_ACK        -> "Return in progress";
            case RETURNED          -> "Returned";
        };
    }
}
