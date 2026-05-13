package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.model.ServiceOffer;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ServiceOffersReviewController {

    @FXML private TableView<ServiceOffer> table;
    @FXML private TableColumn<ServiceOffer, String> colId, colTitle, colCenter;
    @FXML private TableColumn<ServiceOffer, Number> colPrice;

    @FXML private Label lblHeader, lblDesc, lblCount, status;
    @FXML private Button btnApprove, btnReject;

    private final ObservableList<ServiceOffer> rows = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colCenter.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCenterName() == null ? "" : c.getValue().getCenterName()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });

        table.setItems(rows);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));
        refresh();
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        List<ServiceOffer> pending = AppContext.get().serviceCenterService.pendingOffers();
        rows.setAll(pending);
        lblCount.setText(pending.size() + " awaiting review");
        if (pending.isEmpty()) {
            lblHeader.setText("Nothing in the queue.");
            lblDesc.setText("");
            btnApprove.setDisable(true);
            btnReject.setDisable(true);
        } else table.getSelectionModel().selectFirst();
    }

    private void showDetail(ServiceOffer o) {
        if (o == null) return;
        lblHeader.setText(o.getTitle() + " · " + money.format(o.getPrice()) + " EGP");
        lblDesc.setText((o.getDescription() == null || o.getDescription().isBlank() ? "No description." : o.getDescription())
                + "\n\nFrom: " + (o.getCenterName() == null ? "Center #" + o.getCenterId() : o.getCenterName()));
        btnApprove.setDisable(false);
        btnReject.setDisable(false);
        status.setText("");
    }

    @FXML
    private void onApprove() {
        ServiceOffer sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        AppContext.get().serviceCenterService.approveOffer(sel.getId());
        status.setText("Published '" + sel.getTitle() + "'.");
        refresh();
    }

    @FXML
    private void onReject() {
        ServiceOffer sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reject offer");
        dlg.setHeaderText("Reject '" + sel.getTitle() + "'");
        dlg.setContentText("Reason (shown to the centre):");
        Optional<String> reason = dlg.showAndWait();
        if (reason.isEmpty()) return;
        AppContext.get().serviceCenterService.rejectOffer(sel.getId(), reason.get().trim());
        status.setText("Rejected '" + sel.getTitle() + "'.");
        refresh();
    }
}
