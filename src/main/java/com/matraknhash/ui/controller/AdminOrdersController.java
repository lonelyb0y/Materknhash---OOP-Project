package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.dao.DaoException;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;
import com.matraknhash.model.User;
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
import java.util.Optional;

/**
 * Admin queue of marketplace orders that the seller has already acknowledged
 * (status = SELLER_ACK). Approving deducts stock atomically and flips the
 * order to APPROVED so the buyer can see the confirmation in My Orders.
 */
public class AdminOrdersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Sale> orderTable;
    @FXML private TableColumn<Sale, String> colId, colBuyer, colSeller, colWhen;
    @FXML private TableColumn<Sale, Number> colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colLname;
    @FXML private TableColumn<SaleItem, Number> colLqty, colLprc, colLsub;

    @FXML private Label lblHeader, lblCount, status;
    @FXML private Button btnApprove, btnReject;

    private final ObservableList<Sale>     orders = FXCollections.observableArrayList();
    private final ObservableList<SaleItem> lines  = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);
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
        userNames = new HashMap<>();
        AppContext.get().userService.all().forEach(u -> userNames.put(u.getId(), u.getFullName()));
        List<Sale> pending = AppContext.get().saleService.pendingAdminOrders();
        orders.setAll(pending);
        lblCount.setText(pending.size() + " awaiting you");
        if (pending.isEmpty()) {
            lblHeader.setText("Nothing in the queue right now.");
            lines.clear();
            btnApprove.setDisable(true);
            btnReject.setDisable(true);
        } else orderTable.getSelectionModel().selectFirst();
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        lblHeader.setText("Order #" + s.getId() + " · " + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());
        btnApprove.setDisable(false);
        btnReject.setDisable(false);
        status.setText("");
    }

    @FXML
    private void onApprove() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User me = Session.current();
        if (me == null) return;
        try {
            AppContext.get().saleService.approveOrder(sel.getId(), me.getId());
            status.setText("Approved order #" + sel.getId() + ". Stock has been deducted.");
            refresh();
        } catch (DaoException e) {
            status.setText("Approval failed: " + e.getMessage());
        }
    }

    @FXML
    private void onReject() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User me = Session.current();
        if (me == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reject order");
        dlg.setHeaderText("Reject order #" + sel.getId() + " (" + money.format(sel.getTotal()) + " EGP)");
        dlg.setContentText("Reason (shown to the customer):");
        Optional<String> reason = dlg.showAndWait();
        if (reason.isEmpty()) return;
        try {
            AppContext.get().saleService.rejectOrder(sel.getId(), me.getId(), reason.get().trim());
            status.setText("Rejected order #" + sel.getId() + ".");
            refresh();
        } catch (DaoException e) {
            status.setText("Rejection failed: " + e.getMessage());
        }
    }
}
