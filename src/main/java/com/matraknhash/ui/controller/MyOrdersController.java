package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.dao.DaoException;
import com.matraknhash.model.Review;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Customer-side "My Orders" view with returns and seller rating support.
 */
public class MyOrdersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Sale> orderTable;
    @FXML private TableColumn<Sale, String> colId, colSeller, colStat, colWhen;
    @FXML private TableColumn<Sale, Number> colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colLname;
    @FXML private TableColumn<SaleItem, Number> colLqty, colLprc, colLsub;

    @FXML private Label detailHeader, detailFooter, status;
    @FXML private VBox returnBox;
    @FXML private TextArea returnReason;
    @FXML private Button btnReturn, btnRate;

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
            btnReturn.setDisable(true);
            btnRate.setDisable(true);
        }
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        detailHeader.setText("Order #" + s.getId() + " · " + prettyStatus(s.getStatus())
                + " · " + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());
        detailFooter.setText(humanStatus(s));
        status.setText("");

        // Return button: only for APPROVED orders
        boolean canReturn = s.getStatus() == Sale.Status.APPROVED;
        btnReturn.setDisable(!canReturn);
        returnBox.setManaged(canReturn);
        returnBox.setVisible(canReturn);

        // Rate button: only for APPROVED or RETURNED orders, and not yet reviewed
        boolean canRate = (s.getStatus() == Sale.Status.APPROVED || s.getStatus() == Sale.Status.RETURNED);
        if (canRate) {
            Review existing = AppContext.get().reviewService.findBySale(s.getId());
            if (existing != null) {
                canRate = false;
                status.setText("⭐ You rated this seller " + existing.getRating() + "/5");
            }
        }
        btnRate.setDisable(!canRate);
    }

    @FXML
    private void onRequestReturn() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String reason = returnReason.getText() == null ? "" : returnReason.getText().trim();
        if (reason.isEmpty()) {
            status.setText("Please provide a reason for the return.");
            return;
        }
        try {
            AppContext.get().saleService.requestReturn(sel.getId(), reason);
            status.setText("Return requested for order #" + sel.getId() + ". The seller will review it.");
            returnReason.clear();
            refresh();
        } catch (DaoException e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onRateSeller() {
        Sale sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // Build a rating dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rate Seller");
        dialog.setHeaderText("Rate " + (sel.getSellerName() == null ? "this seller" : sel.getSellerName()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.setValue(5);
        ratingBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer v) {
                return v == null ? "" : "⭐".repeat(v) + " (" + v + "/5)";
            }
            @Override public Integer fromString(String s) { return null; }
        });

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Optional comment...");
        commentArea.setPrefRowCount(3);

        grid.add(new Label("Rating:"), 0, 0);
        grid.add(ratingBox, 1, 0);
        grid.add(new Label("Comment:"), 0, 1);
        grid.add(commentArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                var me = Session.current();
                if (me == null) return;
                AppContext.get().reviewService.submit(
                        sel.getId(), me.getId(), sel.getSellerId(),
                        ratingBox.getValue(), commentArea.getText());
                status.setText("⭐ Thank you! You rated this seller " + ratingBox.getValue() + "/5.");
                btnRate.setDisable(true);
            } catch (Exception e) {
                status.setText("Rating failed: " + e.getMessage());
            }
        }
    }

    private static String prettyStatus(Sale.Status s) {
        return switch (s) {
            case PLACED            -> "Awaiting seller";
            case SELLER_ACK        -> "Preparing (seller)";
            case APPROVED          -> "Confirmed ✓";
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
            case PLACED            -> "Waiting for the seller to fulfill your order.";
            case SELLER_ACK        -> "The seller is preparing your order.";
            case APPROVED          -> "Your order is confirmed and fulfilled!";
            case REJECTED          -> "This order was rejected" +
                    (s.getRejectReason() == null || s.getRejectReason().isBlank() ? "." : ": " + s.getRejectReason());
            case RETURN_REQUESTED  -> "Your return request is pending seller review. Reason: " + s.getReturnReason();
            case RETURNED          -> "This order has been returned and the items were restocked.";
            default                -> "";
        };
    }
}
