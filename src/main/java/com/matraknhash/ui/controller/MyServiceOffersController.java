package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.ServiceOffer;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.Locale;

public class MyServiceOffersController {

    @FXML private TextField title, price;
    @FXML private TextArea  desc;
    @FXML private Label status;

    @FXML private TableView<ServiceOffer> table;
    @FXML private TableColumn<ServiceOffer, String> colId, colTitle, colStatus, colReason;
    @FXML private TableColumn<ServiceOffer, Number> colPrice;

    private final ObservableList<ServiceOffer> rows = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(switch (c.getValue().getStatus()) {
            case PENDING_ADMIN -> "Awaiting admin review";
            case LIVE          -> "Live · visible to customers";
            case REJECTED      -> "Rejected";
        }));
        colReason.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReason() == null ? "" : c.getValue().getReason()));

        table.setItems(rows);
        refresh();
    }

    private void refresh() {
        User me = Session.current();
        if (me == null) { rows.clear(); return; }
        rows.setAll(AppContext.get().serviceCenterService.myOffers(me.getId()));
    }

    @FXML
    private void onSubmit() {
        User me = Session.current();
        if (me == null) return;
        try {
            double p = Double.parseDouble(price.getText().trim());
            AppContext.get().serviceCenterService.submitOffer(
                    me.getId(), title.getText().trim(), desc.getText(), p);
            status.setText("Submitted '" + title.getText().trim() + "' — admin review pending.");
            title.clear(); price.clear(); desc.clear();
            refresh();
        } catch (NumberFormatException ex) {
            status.setText("Price must be a number.");
        } catch (Exception ex) {
            status.setText("Submission failed: " + ex.getMessage());
        }
    }
}
