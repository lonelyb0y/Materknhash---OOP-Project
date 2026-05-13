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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Seller view of incoming marketplace orders. Pure JavaFX (no FXML).
 * "Print receipt & Fulfill" moves a PLACED order directly to APPROVED.
 * "Accept Return" restores stock and marks the order as RETURNED.
 */
public class SellerOrdersController extends VBox {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TableView<Sale>     orderTable = new TableView<>();
    private final TableView<SaleItem> lineTable  = new TableView<>();

    private final Label lblHeader = new Label("Pick an order \u2192");
    private final Label lblCount  = new Label("0 awaiting you");
    private final Label status    = new Label();
    private final Button btnPrint        = new Button("\uD83D\uDDA8  Print receipt & Fulfill");
    private final Button btnAcceptReturn = new Button("\u2705  Accept Return");

    private final ObservableList<Sale>     orders = FXCollections.observableArrayList();
    private final ObservableList<SaleItem> lines  = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @SuppressWarnings("unchecked") // JavaFX TableView.getColumns().addAll(...) varargs
    public SellerOrdersController() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        getStyleClass().add("page-root");

        // ---- Header bar ----
        Label title = new Label("Incoming Orders");
        title.getStyleClass().add("page-h1");
        lblCount.getStyleClass().add("muted");
        Button btnRefresh = new Button("Refresh");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> refresh());
        Region headerSpring = new Region();
        HBox.setHgrow(headerSpring, Priority.ALWAYS);
        HBox header = new HBox(10, title, headerSpring, lblCount, btnRefresh);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- Orders table (left of split) ----
        Label ordersTitle = new Label("Orders for your listings");
        ordersTitle.getStyleClass().add("section-title");

        TableColumn<Sale, String> colId = new TableColumn<>("Order");
        colId.setPrefWidth(80);
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));

        TableColumn<Sale, String> colBuyer = new TableColumn<>("Buyer");
        colBuyer.setPrefWidth(140);
        colBuyer.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBuyerName() == null ? "Customer" : c.getValue().getBuyerName()));

        TableColumn<Sale, Number> colTotal = new TableColumn<>("Total");
        colTotal.setPrefWidth(100);
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });

        TableColumn<Sale, String> colStat = new TableColumn<>("Status");
        colStat.setPrefWidth(140);
        colStat.setCellValueFactory(c -> new SimpleStringProperty(pretty(c.getValue().getStatus())));

        TableColumn<Sale, String> colWhen = new TableColumn<>("Placed");
        colWhen.setPrefWidth(140);
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));

        orderTable.getColumns().addAll(colId, colBuyer, colTotal, colStat, colWhen);
        orderTable.setItems(orders);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        VBox leftPane = new VBox(8, ordersTitle, orderTable);

        // ---- Line items table (right of split) ----
        lblHeader.getStyleClass().add("section-title");

        TableColumn<SaleItem, String> colLname = new TableColumn<>("Part");
        colLname.setPrefWidth(220);
        colLname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartName()));

        TableColumn<SaleItem, Number> colLqty = new TableColumn<>("Qty");
        colLqty.setPrefWidth(60);
        colLqty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));

        TableColumn<SaleItem, Number> colLprc = new TableColumn<>("Unit");
        colLprc.setPrefWidth(90);
        colLprc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUnitPrice()));

        TableColumn<SaleItem, Number> colLsub = new TableColumn<>("Subtotal");
        colLsub.setPrefWidth(100);
        colLsub.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));

        lineTable.getColumns().addAll(colLname, colLqty, colLprc, colLsub);
        lineTable.setItems(lines);
        VBox.setVgrow(lineTable, Priority.ALWAYS);

        btnPrint.getStyleClass().add("btn-primary");
        btnPrint.setDisable(true);
        btnPrint.setOnAction(e -> onPrint());
        btnAcceptReturn.getStyleClass().add("btn-danger");
        btnAcceptReturn.setDisable(true);
        btnAcceptReturn.setOnAction(e -> onAcceptReturn());
        HBox actionBar = new HBox(10, btnPrint, btnAcceptReturn);
        actionBar.setAlignment(Pos.CENTER_RIGHT);

        status.setWrapText(true);
        status.getStyleClass().add("muted");

        VBox rightPane = new VBox(10, lblHeader, lineTable, actionBar, status);

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(header, split);

        orderTable.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> showDetail(b));

        refresh();
    }

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
        } else {
            orderTable.getSelectionModel().selectFirst();
        }
    }

    private void showDetail(Sale s) {
        if (s == null) return;
        lblHeader.setText("Order #" + s.getId() + " \u00B7 " + s.getBuyerName() + " \u00B7 "
                + money.format(s.getTotal()) + " EGP");
        lines.setAll(s.getItems());

        btnPrint.setDisable(s.getStatus() != Sale.Status.PLACED);
        btnAcceptReturn.setDisable(s.getStatus() != Sale.Status.RETURN_REQUESTED);

        if (s.getStatus() == Sale.Status.RETURN_REQUESTED) {
            status.setText("\u26A0 Customer wants to return this order. Reason: " + s.getReturnReason());
        } else if (s.getStatus() == Sale.Status.APPROVED) {
            status.setText("Order fulfilled \u2713");
        } else {
            status.setText("");
        }
    }

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
            case RETURN_REQUESTED  -> "\u26A0 Return requested";
            case APPROVED          -> "Fulfilled";
            default                -> s.name();
        };
    }
}
