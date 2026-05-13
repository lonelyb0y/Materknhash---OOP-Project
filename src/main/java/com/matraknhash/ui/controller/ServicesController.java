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
import java.util.List;
import java.util.Locale;

/**
 * Customer-facing browse + book screen for approved service offers.
 */
public class ServicesController {

    @FXML private TableView<ServiceOffer> offerTable;
    @FXML private TableColumn<ServiceOffer, String> colTitle, colCenter;
    @FXML private TableColumn<ServiceOffer, Number> colPrice;

    @FXML private Label lblHeader, lblDesc, lblCount, status;
    @FXML private TextArea vehicleNote;
    @FXML private Button btnBook;

    private final ObservableList<ServiceOffer> offers = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
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

        offerTable.setItems(offers);
        offerTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));
        refresh();
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        List<ServiceOffer> live = AppContext.get().serviceCenterService.liveOffers();
        offers.setAll(live);
        lblCount.setText(live.size() + " service" + (live.size() == 1 ? "" : "s"));
        if (live.isEmpty()) {
            lblHeader.setText("No services published yet — check back soon.");
            btnBook.setDisable(true);
        }
    }

    private void showDetail(ServiceOffer o) {
        if (o == null) return;
        lblHeader.setText(o.getTitle() + " · " + money.format(o.getPrice()) + " EGP");
        lblDesc.setText(o.getDescription() == null || o.getDescription().isBlank()
                ? "No description provided." : o.getDescription());
        btnBook.setDisable(false);
        status.setText("");
    }

    @FXML
    private void onBook() {
        ServiceOffer sel = offerTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        User me = Session.current();
        if (me == null) return;
        try {
            AppContext.get().serviceCenterService.requestService(
                    me.getId(), sel.getId(), vehicleNote.getText());
            status.setText("Booked! The service center has been notified. Track it under My Service Bookings.");
            vehicleNote.clear();
        } catch (Exception ex) {
            status.setText("Booking failed: " + ex.getMessage());
        }
    }
}
