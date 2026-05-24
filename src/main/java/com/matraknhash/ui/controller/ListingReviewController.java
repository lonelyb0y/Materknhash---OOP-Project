package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.Part;
import com.matraknhash.model.Role;
import com.matraknhash.model.Supplier;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.util.*;


public class ListingReviewController {

    private enum Mode { EMPLOYEE, ADMIN }

    @FXML private Label title, subtitle, lblCount, status;
    @FXML private TableView<Part> table;
    @FXML private TableColumn<Part, Number> colId, colPrice, colQty;
    @FXML private TableColumn<Part, String> colSku, colName, colSeller, colSup;
    @FXML private TableColumn<Part, Void>   colAct;

    private final ObservableList<Part> items = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);
    private Mode mode;
    private Map<Integer, String> sellerNames = Map.of();
    private Map<Integer, Supplier> suppliers = Map.of();

    @FXML
    public void initialize() {
        User me = Session.current();
        mode = (me != null && me.getRole() == Role.ADMIN) ? Mode.ADMIN : Mode.EMPLOYEE;
        title.setText(mode == Mode.ADMIN ? "Admin: Listings Approval" : "Listings Review");
        subtitle.setText(mode == Mode.ADMIN
                ? "Listings already cleared by an employee. Approving makes them LIVE on the customer catalog."
                : "Fresh listings submitted by sellers. Approving makes them LIVE on the marketplace instantly.");

        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSku()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSellerId() == null ? "—" :
                sellerNames.getOrDefault(c.getValue().getSellerId(), "Seller #" + c.getValue().getSellerId())));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSellPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colSup.setCellValueFactory(c -> {
            Supplier s = c.getValue().getSupplierId() == null ? null : suppliers.get(c.getValue().getSupplierId());
            return new SimpleStringProperty(s == null ? "—" :
                    (s.isTrusted() ? "★ " + s.getName() : s.getName()));
        });

        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button ok = new Button("Approve");
            private final Button no = new Button("Reject");
            private final HBox   box = new HBox(6, ok, no);
            {
                ok.getStyleClass().add("btn-primary");
                no.getStyleClass().add("btn-danger");
                ok.setOnAction(e -> approve(getTableView().getItems().get(getIndex())));
                no.setOnAction(e -> reject(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.setItems(items);
        refresh();
    }

    @FXML
    private void onRefresh() { refresh(); }

    private void refresh() {
        sellerNames = new HashMap<>();
        AppContext.get().userService.all().forEach(u -> sellerNames.put(u.getId(), u.getFullName()));
        suppliers = new HashMap<>();
        AppContext.get().supplierService.all().forEach(s -> suppliers.put(s.getId(), s));

        List<Part> pending = (mode == Mode.ADMIN)
                ? AppContext.get().listingService.pendingAdmin()
                : AppContext.get().listingService.pendingEmployee();
        items.setAll(pending);
        lblCount.setText(pending.size() + " awaiting");
        if (pending.isEmpty()) status.setText("Nothing in the queue right now.");
        else status.setText("");
    }

    private void approve(Part p) {
        User me = Session.current();
        if (me == null) return;
        boolean ok = (mode == Mode.ADMIN)
                ? AppContext.get().listingService.adminApprove(p.getId(), me.getId())
                : AppContext.get().listingService.employeeApprove(p.getId(), me.getId());
        status.setText(ok
                ? "Approved \"" + p.getName() + "\" — now LIVE on the marketplace!"
                : "Could not approve — state may have changed.");
        refresh();
    }

    private void reject(Part p) {
        User me = Session.current();
        if (me == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reject listing");
        dlg.setHeaderText("Reject \"" + p.getName() + "\"?");
        dlg.setContentText("Reason (shown to the seller):");
        Optional<String> reason = dlg.showAndWait();
        if (reason.isEmpty()) return;
        boolean ok = (mode == Mode.ADMIN)
                ? AppContext.get().listingService.adminReject(p.getId(), me.getId(), reason.get())
                : AppContext.get().listingService.employeeReject(p.getId(), me.getId(), reason.get());
        status.setText(ok ? "Rejected \"" + p.getName() + "\"." : "Could not reject.");
        refresh();
    }
}
