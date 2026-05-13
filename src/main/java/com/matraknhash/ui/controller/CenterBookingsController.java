package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.ServiceRequest;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

public class CenterBookingsController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<ServiceRequest> table;
    @FXML private TableColumn<ServiceRequest, String> colId, colService, colCustomer, colVehicle, colStatus, colWhen;
    @FXML private Button btnAccept, btnReject, btnComplete;
    @FXML private Label status;

    private final ObservableList<ServiceRequest> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colService.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOfferTitle()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        colVehicle.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getVehicleNote() == null ? "" : c.getValue().getVehicleNote()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));

        table.setItems(rows);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons(b));
        refresh();
    }

    private void updateButtons(ServiceRequest r) {
        if (r == null) { btnAccept.setDisable(true); btnReject.setDisable(true); btnComplete.setDisable(true); return; }
        boolean req = r.getStatus() == ServiceRequest.Status.REQUESTED;
        boolean acc = r.getStatus() == ServiceRequest.Status.ACCEPTED;
        btnAccept.setDisable(!req);
        btnReject.setDisable(!req);
        btnComplete.setDisable(!acc);
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        User me = Session.current();
        if (me == null) { rows.clear(); return; }
        rows.setAll(AppContext.get().serviceCenterService.incoming(me.getId()));
        updateButtons(null);
    }

    @FXML
    private void onAccept() {
        ServiceRequest r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        AppContext.get().serviceCenterService.acceptRequest(r.getId());
        status.setText("Accepted booking #" + r.getId() + ".");
        refresh();
    }

    @FXML
    private void onReject() {
        ServiceRequest r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        AppContext.get().serviceCenterService.rejectRequest(r.getId());
        status.setText("Declined booking #" + r.getId() + ".");
        refresh();
    }

    @FXML
    private void onComplete() {
        ServiceRequest r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        AppContext.get().serviceCenterService.completeRequest(r.getId());
        status.setText("Marked booking #" + r.getId() + " complete.");
        refresh();
    }
}
