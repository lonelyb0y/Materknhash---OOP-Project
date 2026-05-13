package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.Part;
import com.matraknhash.model.Supplier;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Seller dashboard for product listings. Pure JavaFX (no FXML).
 * Adds new ones (auto-routed through the approval pipeline based on supplier
 * trust), and shows current status of every part the seller owns.
 */
public class MyListingsController extends HBox {

    private final TextField skuField      = new TextField();
    private final TextField nameField     = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField makeField     = new TextField();
    private final TextField modelField    = new TextField();
    private final TextField priceField    = new TextField();
    private final TextField qtyField      = new TextField();
    private final ComboBox<Supplier> supplierCombo = new ComboBox<>();
    private final Label formError = new Label();
    private final Label formInfo  = new Label();
    private final Button btnSubmit = new Button("Submit for review");
    private final Button btnUpdate = new Button("Update listing");
    private final Button btnDelete = new Button("Delete");
    private Part selectedPart;

    private final TableView<Part> table = new TableView<>();
    private final ObservableList<Part> items = FXCollections.observableArrayList();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    public MyListingsController() {
        setSpacing(18);
        setPadding(new Insets(14, 18, 14, 18));

        getChildren().addAll(buildFormCard(), buildListCard());

        wireSupplierCombo();
        wireTable();
        refresh();
    }

    // ---------- left card: add/edit form ----------
    private VBox buildFormCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMinWidth(320);
        card.setPrefWidth(340);

        Label heading = new Label("Add New Listing");
        heading.getStyleClass().add("section-title");

        HBox makeModelRow = new HBox(8,
                labelled("Car make", makeField),
                labelled("Car model", modelField));
        HBox priceQtyRow = new HBox(8,
                labelled("Sell price (EGP)", priceField),
                labelled("Initial stock", qtyField));

        supplierCombo.setPrefWidth(320);

        formError.getStyleClass().add("error-label");
        formError.setWrapText(true);
        formError.setManaged(false);
        formError.setVisible(false);
        formInfo.getStyleClass().add("muted");
        formInfo.setWrapText(true);
        formInfo.setManaged(false);
        formInfo.setVisible(false);

        Button btnClear = new Button("Clear");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> onClear());

        btnDelete.getStyleClass().add("btn-danger");
        btnDelete.setDisable(true);
        btnDelete.setOnAction(e -> onDelete());

        Region topSpring = new Region();
        HBox.setHgrow(topSpring, Priority.ALWAYS);
        HBox topRow = new HBox(8, btnClear, topSpring, btnDelete);
        topRow.setAlignment(Pos.CENTER_LEFT);

        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setOnAction(e -> onSubmit());
        btnUpdate.getStyleClass().add("btn-primary");
        btnUpdate.setDisable(true);
        btnUpdate.setOnAction(e -> onUpdate());
        HBox bottomRow = new HBox(8, btnUpdate, btnSubmit);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);

        VBox actions = new VBox(6, topRow, bottomRow);
        actions.setPadding(new Insets(6, 0, 0, 0));

        card.getChildren().addAll(
                heading,
                new Label("SKU (unique)"),  skuField,
                new Label("Name"),          nameField,
                new Label("Category"),      categoryField,
                makeModelRow,
                priceQtyRow,
                new Label("Source supplier (\u2605 trusted skips review)"),
                supplierCombo,
                formError, formInfo,
                actions);
        return card;
    }

    private static VBox labelled(String text, TextField field) {
        VBox box = new VBox(2, new Label(text), field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    // ---------- right card: table ----------
    private VBox buildListCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label heading = new Label("My Listings");
        heading.getStyleClass().add("section-title");
        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        Button btnRefresh = new Button("Refresh");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> refresh());
        HBox header = new HBox(10, heading, spring, btnRefresh);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().addAll(header, table);
        return card;
    }

    private void wireSupplierCombo() {
        supplierCombo.setItems(FXCollections.observableArrayList(
                AppContext.get().supplierService.all()));
        supplierCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Supplier s) {
                if (s == null) return "";
                return (s.isTrusted() ? "\u2605 " : "") + s.getName();
            }
            @Override public Supplier fromString(String s) { return null; }
        });
    }

    @SuppressWarnings("unchecked") // JavaFX TableView.getColumns().addAll(...) varargs
    private void wireTable() {
        TableColumn<Part, Number> colId = new TableColumn<>("ID");
        colId.setPrefWidth(50);
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));

        TableColumn<Part, String> colSku = new TableColumn<>("SKU");
        colSku.setPrefWidth(100);
        colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSku()));

        TableColumn<Part, String> colName = new TableColumn<>("Name");
        colName.setPrefWidth(200);
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Part, Number> colPrice = new TableColumn<>("Price");
        colPrice.setPrefWidth(90);
        colPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSellPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean e) {
                super.updateItem(n, e);
                setText(e || n == null ? null : money.format(n.doubleValue()) + " EGP");
            }
        });

        TableColumn<Part, Number> colQty = new TableColumn<>("Stock");
        colQty.setPrefWidth(60);
        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantity()));

        TableColumn<Part, String> colStat = new TableColumn<>("Status");
        colStat.setPrefWidth(140);
        colStat.setCellValueFactory(c -> new SimpleStringProperty(pretty(c.getValue())));

        TableColumn<Part, String> colNote = new TableColumn<>("Note");
        colNote.setPrefWidth(200);
        colNote.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getListingReason() == null ? "" : c.getValue().getListingReason()));

        table.getColumns().addAll(colId, colSku, colName, colPrice, colQty, colStat, colNote);
        table.setItems(items);
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldV, newV) -> selectItem(newV));
    }

    private void selectItem(Part p) {
        selectedPart = p;
        boolean selected = (p != null);
        btnSubmit.setDisable(selected);
        btnUpdate.setDisable(!selected);
        btnDelete.setDisable(!selected);

        if (selected) {
            skuField.setText(p.getSku());
            nameField.setText(p.getName());
            categoryField.setText(p.getCategory());
            makeField.setText(p.getCarMake());
            modelField.setText(p.getCarModel());
            priceField.setText(String.valueOf(p.getSellPrice()));
            qtyField.setText(String.valueOf(p.getQuantity()));
            if (p.getSupplierId() != null) {
                supplierCombo.getItems().stream()
                        .filter(s -> s.getId() == p.getSupplierId())
                        .findFirst().ifPresent(supplierCombo::setValue);
            } else {
                supplierCombo.setValue(null);
            }
        }
    }

    private void refresh() {
        var me = Session.current();
        if (me == null) return;
        items.setAll(AppContext.get().listingService.bySeller(me.getId()));
    }

    private void onClear() {
        skuField.clear(); nameField.clear(); categoryField.clear();
        makeField.clear(); modelField.clear();
        priceField.clear(); qtyField.clear();
        supplierCombo.setValue(null);
        hideMessages();
        selectItem(null);
    }

    private void onSubmit() {
        hideMessages();
        try {
            if (skuField.getText().isBlank() || nameField.getText().isBlank())
                throw new IllegalArgumentException("SKU and Name are required.");
            double price = Double.parseDouble(priceField.getText().trim());
            int qty      = Integer.parseInt(qtyField.getText().trim());
            if (price <= 0 || qty < 0) throw new IllegalArgumentException("Price > 0 and stock >= 0.");

            Part p = new Part();
            p.setSku(skuField.getText().trim());
            p.setName(nameField.getText().trim());
            p.setCategory(categoryField.getText().trim());
            p.setCarMake(makeField.getText().trim());
            p.setCarModel(modelField.getText().trim());
            p.setCostPrice(price * 0.7);
            p.setSellPrice(price);
            p.setQuantity(qty);
            p.setMinQty(0);
            Supplier sup = supplierCombo.getValue();
            if (sup != null) p.setSupplierId(sup.getId());

            Part saved = AppContext.get().listingService.submit(p, Session.current().getId());
            String msg = saved.getListingStatus() == Part.ListingStatus.LIVE
                    ? "\u2705 Listing is LIVE on the marketplace (trusted supplier)."
                    : "Listing submitted \u2014 awaiting employee review.";
            showInfo(msg);
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Price and stock must be valid numbers.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void onUpdate() {
        if (selectedPart == null) return;
        hideMessages();
        try {
            if (skuField.getText().isBlank() || nameField.getText().isBlank())
                throw new IllegalArgumentException("SKU and Name are required.");
            double price = Double.parseDouble(priceField.getText().trim());
            int qty      = Integer.parseInt(qtyField.getText().trim());
            if (price <= 0 || qty < 0) throw new IllegalArgumentException("Price > 0 and stock >= 0.");

            selectedPart.setSku(skuField.getText().trim());
            selectedPart.setName(nameField.getText().trim());
            selectedPart.setCategory(categoryField.getText().trim());
            selectedPart.setCarMake(makeField.getText().trim());
            selectedPart.setCarModel(modelField.getText().trim());
            selectedPart.setSellPrice(price);
            selectedPart.setQuantity(qty);
            Supplier sup = supplierCombo.getValue();
            selectedPart.setSupplierId(sup != null ? sup.getId() : null);

            AppContext.get().partService.save(selectedPart);
            showInfo("Listing updated successfully.");
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Price and stock must be valid numbers.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void onDelete() {
        if (selectedPart == null) return;
        AppContext.get().partService.delete(selectedPart.getId());
        showInfo("Listing deleted.");
        onClear();
        refresh();
    }

    private static String pretty(Part p) {
        return switch (p.getListingStatus() == null ? Part.ListingStatus.LIVE : p.getListingStatus()) {
            case DRAFT             -> "Draft";
            case PENDING_EMPLOYEE  -> "Awaiting employee";
            case PENDING_ADMIN     -> "Awaiting admin";
            case LIVE              -> "\u2605 Live";
            case REJECTED          -> "Rejected";
            case HIDDEN            -> "Hidden";
        };
    }

    private void showError(String s) {
        formError.setText(s); formError.setManaged(true); formError.setVisible(true);
        formInfo.setManaged(false); formInfo.setVisible(false);
    }
    private void showInfo(String s) {
        formInfo.setText(s); formInfo.setManaged(true); formInfo.setVisible(true);
        formError.setManaged(false); formError.setVisible(false);
    }
    private void hideMessages() {
        formError.setManaged(false); formError.setVisible(false);
        formInfo.setManaged(false); formInfo.setVisible(false);
    }
}
