package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.ServiceRequest;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MyBookingsController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<ServiceRequest> table;
    @FXML private TableColumn<ServiceRequest, String> colId, colTitle, colCenter, colStatus, colWhen;
    @FXML private TableColumn<ServiceRequest, Number> colPrice;

    private final ObservableList<ServiceRequest> rows = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOfferTitle()));
        colCenter.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCenterName()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getOfferPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(switch (c.getValue().getStatus()) {
            case REQUESTED -> "Awaiting workshop";
            case ACCEPTED  -> "Accepted · in progress";
            case COMPLETED -> "Completed";
            case REJECTED  -> "Declined";
        }));
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));

        table.setItems(rows);
        refresh();
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        User me = Session.current();
        if (me == null) { rows.clear(); return; }
        rows.setAll(AppContext.get().serviceCenterService.myRequests(me.getId()));
    }
}
