package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.ServiceOffer;
import com.matraknhash.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Service Center: manage and submit service offers. Pure JavaFX (no FXML).
 */
public class MyServiceOffersController extends VBox {

    private final TextField title = new TextField();
    private final TextField price = new TextField();
    private final TextArea  desc  = new TextArea();
    private final Label status    = new Label();
    private final Button btnSubmit = new Button("Submit for review");
    private final Button btnUpdate = new Button("Update offer");
    private final Button btnDelete = new Button("Delete");
    private ServiceOffer selectedOffer;

    private final TableView<ServiceOffer> table = new TableView<>();
    private final ObservableList<ServiceOffer> rows = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    public MyServiceOffersController() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        getStyleClass().add("page-root");

        Label h1 = new Label("My Service Offers");
        h1.getStyleClass().add("page-h1");
        Label intro = new Label(
                "Publish services your workshop offers. New offers go to the admin for review " +
                        "before they appear on the customer marketplace.");
        intro.getStyleClass().add("muted");
        intro.setWrapText(true);

        TitledPane addPane = new TitledPane("Add a new offer", buildForm());
        addPane.setExpanded(true);

        status.setWrapText(true);
        status.getStyleClass().add("muted");

        Label tableTitle = new Label("Your offers");
        tableTitle.getStyleClass().add("section-title");

        wireTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(h1, intro, addPane, status, tableTitle, table);

        refresh();
    }

    private GridPane buildForm() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(8);
        g.setPadding(new Insets(10));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(100);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);

        title.setPromptText("Front brake pad replacement");
        price.setPromptText("800");
        desc.setPrefRowCount(3);

        g.add(new Label("Title"), 0, 0);       g.add(title, 1, 0);
        g.add(new Label("Price (EGP)"), 0, 1); g.add(price, 1, 1);
        g.add(new Label("Description"), 0, 2); g.add(desc, 1, 2);

        Button btnClear = new Button("Clear");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> onClear());
        btnDelete.getStyleClass().add("btn-danger");
        btnDelete.setDisable(true);
        btnDelete.setOnAction(e -> onDelete());
        Region springTop = new Region();
        HBox.setHgrow(springTop, Priority.ALWAYS);
        HBox topRow = new HBox(10, btnClear, springTop, btnDelete);
        topRow.setAlignment(Pos.CENTER_LEFT);

        btnUpdate.getStyleClass().add("btn-primary");
        btnUpdate.setDisable(true);
        btnUpdate.setOnAction(e -> onUpdate());
        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setOnAction(e -> onSubmit());
        HBox bottomRow = new HBox(10, btnUpdate, btnSubmit);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);

        VBox actions = new VBox(6, topRow, bottomRow);
        g.add(actions, 1, 3);

        return g;
    }

    @SuppressWarnings("unchecked") // JavaFX TableView.getColumns().addAll(...) varargs
    private void wireTable() {
        TableColumn<ServiceOffer, String> colId = new TableColumn<>("#");
        colId.setPrefWidth(60);
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));

        TableColumn<ServiceOffer, String> colTitle = new TableColumn<>("Title");
        colTitle.setPrefWidth(220);
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));

        TableColumn<ServiceOffer, Number> colPrice = new TableColumn<>("Price");
        colPrice.setPrefWidth(100);
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });

        TableColumn<ServiceOffer, String> colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(160);
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(switch (c.getValue().getStatus()) {
            case PENDING_ADMIN -> "Awaiting admin review";
            case LIVE          -> "Live \u00B7 visible to customers";
            case REJECTED      -> "Rejected";
        }));

        TableColumn<ServiceOffer, String> colReason = new TableColumn<>("Reason");
        colReason.setPrefWidth(240);
        colReason.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReason() == null ? "" : c.getValue().getReason()));

        table.getColumns().addAll(colId, colTitle, colPrice, colStatus, colReason);
        table.setItems(rows);
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldV, newV) -> selectItem(newV));
    }

    private void selectItem(ServiceOffer o) {
        selectedOffer = o;
        boolean selected = (o != null);
        btnSubmit.setDisable(selected);
        btnUpdate.setDisable(!selected);
        btnDelete.setDisable(!selected);

        if (selected) {
            title.setText(o.getTitle());
            price.setText(String.valueOf(o.getPrice()));
            desc.setText(o.getDescription());
        }
    }

    private void onClear() {
        title.clear(); price.clear(); desc.clear();
        status.setText("");
        selectItem(null);
    }

    private void refresh() {
        User me = Session.current();
        if (me == null) { rows.clear(); return; }
        rows.setAll(AppContext.get().serviceCenterService.myOffers(me.getId()));
    }

    private void onSubmit() {
        User me = Session.current();
        if (me == null) return;
        try {
            double p = Double.parseDouble(price.getText().trim());
            AppContext.get().serviceCenterService.submitOffer(
                    me.getId(), title.getText().trim(), desc.getText(), p);
            status.setText("Submitted '" + title.getText().trim() + "' \u2014 admin review pending.");
            onClear();
            refresh();
        } catch (NumberFormatException ex) {
            status.setText("Price must be a number.");
        } catch (Exception ex) {
            status.setText("Submission failed: " + ex.getMessage());
        }
    }

    private void onUpdate() {
        if (selectedOffer == null) return;
        try {
            double p = Double.parseDouble(price.getText().trim());
            selectedOffer.setTitle(title.getText().trim());
            selectedOffer.setPrice(p);
            selectedOffer.setDescription(desc.getText());
            AppContext.get().serviceCenterService.updateOffer(selectedOffer);
            status.setText("Offer updated successfully.");
            onClear();
            refresh();
        } catch (NumberFormatException ex) {
            status.setText("Price must be a number.");
        } catch (Exception ex) {
            status.setText("Update failed: " + ex.getMessage());
        }
    }

    private void onDelete() {
        if (selectedOffer == null) return;
        AppContext.get().serviceCenterService.deleteOffer(selectedOffer.getId());
        status.setText("Offer deleted.");
        onClear();
        refresh();
    }
}
