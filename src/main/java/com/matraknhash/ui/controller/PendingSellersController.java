package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin queue of seller-account applications. Sellers self-register via the
 * sign-up screen and land here with status=PENDING_APPROVAL; this controller
 * lets the admin flip them to ACTIVE (login allowed) or SUSPENDED (rejected).
 */
public class PendingSellersController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, Number> colId;
    @FXML private TableColumn<User, String> colUser, colName, colWhen;
    @FXML private TableColumn<User, Void>   colAct;
    @FXML private Label lblCount, status;

    private final ObservableList<User> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colWhen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(WHEN)));

        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button ok  = new Button("Approve");
            private final Button no  = new Button("Reject");
            private final HBox   box = new HBox(6, ok, no);
            {
                ok.getStyleClass().add("btn-primary");
                no.getStyleClass().add("btn-danger");
                ok.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    AppContext.get().userService.approveSeller(u.getId());
                    showOk("Approved " + u.getUsername() + " — they can now log in.");
                    refresh();
                });
                no.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                            "Reject seller application for \"" + u.getUsername() + "\"?",
                            ButtonType.OK, ButtonType.CANCEL);
                    if (a.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                        AppContext.get().userService.rejectSeller(u.getId());
                        showOk("Rejected " + u.getUsername() + ".");
                        refresh();
                    }
                });
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
        List<User> pending = AppContext.get().userService.listPendingSellers();
        items.setAll(pending);
        lblCount.setText(pending.size() + " awaiting review");
        if (pending.isEmpty()) showInfo("No pending seller applications. Sellers self-register from the Sign Up screen.");
    }

    private void showOk(String s)   { status.setText(s); }
    private void showInfo(String s) { status.setText(s); }
}
