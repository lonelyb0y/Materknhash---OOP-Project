package com.matraknhash.ui.controller;
import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.dao.DaoException;
import com.matraknhash.model.Sale;
import com.matraknhash.model.SaleItem;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.text.NumberFormat;
import java.util.Locale;


public class CartController {

    @FXML private TableView<SaleItem> table;
    @FXML private TableColumn<SaleItem, String> colPart;
    @FXML private TableColumn<SaleItem, Number> colQty, colPrice, colSub;
    @FXML private TableColumn<SaleItem, Void>   colDel;
    @FXML private Label sellerLine, lblTotal, status;
    @FXML private Button btnCheckout, btnEmpty;
    @FXML private TextField addressField;

    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        colPart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartName()));
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUnitPrice()));
        colSub.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
        colDel.setCellFactory(col -> new TableCell<>() {
            private final Button x = new Button("✕");
            {
                x.getStyleClass().add("btn-danger");
                x.setOnAction(e -> {
                    Session.cart().remove(getIndex());
                    refresh();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(x));
            }
        });
        table.setItems(Session.cart().items());
        refresh();
    }

    private void refresh() {
        boolean empty = Session.cart().isEmpty();
        btnCheckout.setDisable(empty);
        btnEmpty.setDisable(empty);
        lblTotal.setText(money.format(Session.cart().subtotal()) + " EGP");
        sellerLine.setText(empty ? "Cart is empty — go back and add some parts."
                : "Sold by " + (Session.cart().sellerName() == null ? "seller #" + Session.cart().sellerId() : Session.cart().sellerName()));
    }

    @FXML
    private void onEmpty() {
        Session.cart().clear();
        refresh();
    }

    @FXML
    private void onCheckout() {
        User me = Session.current();
        if (me == null) { showError("Please log in again."); return; }
        if (Session.cart().sellerId() == null) { showError("Cart is empty."); return; }
 
        String addr = addressField.getText() == null ? "" : addressField.getText().trim();
        if (addr.isEmpty()) {
            showError("❌ Please enter a valid shipping address to place the order.");
            return;
        }
 
        Sale s = new Sale(Session.cart().sellerId(), Session.cart().sellerName());
        s.setShippingAddress(addr);
        for (SaleItem it : Session.cart().items()) s.addItem(new SaleItem(
                it.getPartId(), it.getPartSku(), it.getPartName(), it.getQuantity(), it.getUnitPrice()));
        try {
            Sale placed = AppContext.get().saleService.placeOrder(s, me.getId());
            Session.cart().clear();
            // Show confirmation + close.
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Order #" + placed.getId() + " placed! The seller will receive it shortly and the admin " +
                    "will finalise it. You can track it in 'My Orders'.",
                    ButtonType.OK);
            ok.setHeaderText("Order placed");
            ok.showAndWait();
            close();
        } catch (DaoException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        Stage st = (Stage) table.getScene().getWindow();
        st.close();
    }

    private void showError(String s) {
        status.setText(s);
        if (!status.getStyleClass().contains("error")) status.getStyleClass().add("error");
    }
}
