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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Approval queue UI. Admins and employees see all PENDING invoices submitted by sellers
 * and either approve them (which deducts stock and finalises the sale) or reject them
 * with a reason. Filtered out from sellers via MainShellController role visibility.
 */
public class PendingInvoicesController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Sale> invoiceTable;
    @FXML private TableColumn<Sale, String> colInv, colSeller, colWhen;
    @FXML private TableColumn<Sale, Number> colItems, colTotal;

    @FXML private TableView<SaleItem> lineTable;
    @FXML private TableColumn<SaleItem, String> colSku, colName;
    @FXML private TableColumn<SaleItem, Number> colQty, colPrice, colSub;

    @FXML private Label lblHeader, lblTotal, lblCount, status;
    @FXML private Button btnApprove, btnReject;

    private final ObservableList<Sale>     pending = FXCollections.observableArrayList();
    private final ObservableList<SaleItem> lines   = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        configureInvoiceTable();
        configureLineTable();

        invoiceTable.setItems(pending);
        lineTable.setItems(lines);

        invoiceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldS, newS) -> showSelected(newS));
        refresh();
    }

    private void configureInvoiceTable() {
        colInv.setCellValueFactory(c -> new SimpleStringProperty("INV-" + c.getValue().getId()));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSellerName() == null ? "—" : c.getValue().getSellerName()));
        colItems.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getItems().size()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));
    }

    private void configureLineTable() {
        colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartSku()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartName()));
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUnitPrice()));
        colSub.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
    }

    @FXML
    private void onRefresh() { refresh(); }

    private void refresh() {
        try {
            List<Sale> all = AppContext.get().saleService.listPending();
            pending.setAll(all);
            lblCount.setText(all.size() + (all.size() == 1 ? " awaiting approval" : " awaiting approval"));
            if (all.isEmpty()) clearDetails();
            else invoiceTable.getSelectionModel().selectFirst();
        } catch (DaoException e) {
            showError(e.getMessage());
        }
    }

    private void showSelected(Sale s) {
        if (s == null) { clearDetails(); return; }
        lblHeader.setText("INV-" + s.getId() + "  ·  " + (s.getSellerName() == null ? "" : s.getSellerName())
                + (s.getCreatedAt() == null ? "" : "  ·  " + s.getCreatedAt().format(WHEN)));
        lines.setAll(s.getItems());
        lblTotal.setText("Total: " + money.format(s.getTotal()) + " EGP");
        btnApprove.setDisable(false);
        btnReject.setDisable(false);
        hideStatus();
    }

    private void clearDetails() {
        lines.clear();
        lblHeader.setText("Nothing pending — sellers' invoices will show up here.");
        lblTotal.setText("");
        btnApprove.setDisable(true);
        btnReject.setDisable(true);
    }

    @FXML
    private void onApprove() {
        Sale sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User me = Session.current();
        if (me == null) { showError("Not signed in."); return; }
        try {
            AppContext.get().saleService.approve(sel.getId(), me.getId());
            showOk("Approved INV-" + sel.getId() + ". Stock updated.");
            refresh();
        } catch (DaoException e) {
            showError("Approval failed: " + e.getMessage());
        }
    }

    @FXML
    private void onReject() {
        Sale sel = invoiceTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User me = Session.current();
        if (me == null) { showError("Not signed in."); return; }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reject invoice");
        dlg.setHeaderText("Reject INV-" + sel.getId() + " (" + money.format(sel.getTotal()) + " EGP)");
        dlg.setContentText("Reason:");
        Optional<String> reason = dlg.showAndWait();
        if (reason.isEmpty()) return;
        try {
            AppContext.get().saleService.reject(sel.getId(), me.getId(), reason.get().trim());
            showOk("Rejected INV-" + sel.getId() + ".");
            refresh();
        } catch (DaoException e) {
            showError("Rejection failed: " + e.getMessage());
        }
    }

    private void showOk(String msg)    { status.setText(msg); status.getStyleClass().remove("error"); }
    private void showError(String msg) { status.setText(msg); if (!status.getStyleClass().contains("error")) status.getStyleClass().add("error"); }
    private void hideStatus()          { status.setText(""); }
}
