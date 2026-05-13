package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.app.Session;
import com.matraknhash.model.Part;
import com.matraknhash.model.Supplier;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The customer-facing marketplace catalog: shows every LIVE listing as a
 * card with name, price, stock, trusted-supplier badge, and an Add to Cart
 * button. Includes a quick text filter + category filter on top.
 */
public class CatalogController {

    private static final String ALL_CATEGORIES = "All categories";

    @FXML private FlowPane grid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button cartButton;
    @FXML private Label emptyLabel;

    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);
    private List<Part> all = List.of();
    private Map<Integer, Supplier> suppliersById = Map.of();

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((o, a, b) -> render());
        categoryFilter.valueProperty().addListener((o, a, b) -> render());
        // Refresh the cart badge whenever cart changes.
        Session.cart().items().addListener((javafx.collections.ListChangeListener<? super com.matraknhash.model.SaleItem>) c -> refreshCartBadge());

        refreshCartBadge();
        reload();
    }

    private void reload() {
        all = AppContext.get().listingService.liveCatalog();
        suppliersById = AppContext.get().supplierService.all().stream()
                .collect(Collectors.toMap(Supplier::getId, s -> s, (a, b) -> a));

        List<String> categories = new ArrayList<>();
        categories.add(ALL_CATEGORIES);
        all.stream().map(Part::getCategory).filter(Objects::nonNull).filter(s -> !s.isBlank())
                .distinct().sorted().forEach(categories::add);
        categoryFilter.setItems(FXCollections.observableArrayList(categories));
        categoryFilter.setValue(ALL_CATEGORIES);

        render();
    }

    private void render() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        String cat = categoryFilter.getValue();

        List<Part> shown = all.stream()
                .filter(p -> cat == null || ALL_CATEGORIES.equals(cat) || cat.equalsIgnoreCase(p.getCategory()))
                .filter(p -> q.isEmpty()
                        || (p.getName()    != null && p.getName().toLowerCase(Locale.ROOT).contains(q))
                        || (p.getSku()     != null && p.getSku().toLowerCase(Locale.ROOT).contains(q))
                        || (p.getCarMake() != null && p.getCarMake().toLowerCase(Locale.ROOT).contains(q))
                        || (p.getCarModel()!= null && p.getCarModel().toLowerCase(Locale.ROOT).contains(q)))
                .collect(Collectors.toList());

        grid.getChildren().clear();
        if (shown.isEmpty()) {
            emptyLabel.setText("No parts match your search. Try clearing filters.");
            emptyLabel.setManaged(true); emptyLabel.setVisible(true);
        } else {
            emptyLabel.setManaged(false); emptyLabel.setVisible(false);
            for (Part p : shown) grid.getChildren().add(buildCard(p));
        }
    }

    /** One product card, ~ 240 x auto, with thumbnail placeholder + Add to Cart. */
    private Node buildCard(Part p) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "product-card");
        card.setMaxWidth(240); card.setMinWidth(240); card.setPrefWidth(240);

        // Placeholder "image" -- a coloured rectangle with the SKU in it.
        Region thumb = new Region();
        thumb.setPrefSize(208, 110);
        thumb.setStyle(thumbStyleFor(p));
        Label sku = new Label(p.getSku() == null ? "" : p.getSku());
        sku.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-weight: bold; -fx-font-size: 14px;");
        StackPane thumbBox = new StackPane(thumb, sku);
        thumbBox.setPrefHeight(110);

        Label name = new Label(p.getName());
        name.setWrapText(true);
        name.setFont(Font.font(null, FontWeight.BOLD, 14));

        Label meta = new Label(orDash(p.getCarMake()) + " · " + orDash(p.getCarModel()));
        meta.getStyleClass().add("muted");
        meta.setStyle("-fx-font-size: 11px;");

        Supplier sup = p.getSupplierId() == null ? null : suppliersById.get(p.getSupplierId());
        Label supplierLine = new Label(sup == null ? "" :
                (sup.isTrusted() ? "★ " + sup.getName() + " (Verified Source)" : sup.getName()));
        supplierLine.getStyleClass().add("muted");
        supplierLine.setStyle("-fx-font-size: 11px;" + (sup != null && sup.isTrusted() ? "-fx-text-fill: #d97706;" : ""));

        Label price = new Label(money.format(p.getSellPrice()) + " EGP");
        price.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label stock = new Label(p.getQuantity() + " in stock");
        stock.getStyleClass().add("muted");
        stock.setStyle("-fx-font-size: 11px;");

        Button add = new Button("🛒  Add to Cart");
        add.getStyleClass().add("btn-primary");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> onAdd(p));

        card.getChildren().addAll(thumbBox, name, meta, supplierLine, price, stock, add);
        return card;
    }

    /** Deterministic coloured background per part so the catalog has visual variety. */
    private String thumbStyleFor(Part p) {
        String[] hues = {"#0ea5e9", "#22c55e", "#a855f7", "#ef4444", "#f97316", "#14b8a6", "#6366f1"};
        int idx = Math.abs(Objects.hash(p.getCategory(), p.getId())) % hues.length;
        return "-fx-background-color: " + hues[idx] + "; -fx-background-radius: 8;";
    }

    private static String orDash(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private void onAdd(Part p) {
        try {
            Session.cart().add(p, 1);
            // remember the seller name once
            if (Session.cart().sellerName() == null) {
                AppContext.get().userService.all().stream()
                        .filter(u -> p.getSellerId() != null && u.getId() == p.getSellerId())
                        .findFirst()
                        .ifPresent(u -> Session.cart().rememberSellerName(u.getFullName()));
            }
            refreshCartBadge();
        } catch (IllegalStateException ex) {
            // Different seller -- ask the user if they want to clear cart.
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Your cart already has items from another seller. Empty it and start over with this part?",
                    ButtonType.OK, ButtonType.CANCEL);
            if (a.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                Session.cart().clear();
                Session.cart().add(p, 1);
                refreshCartBadge();
            }
        }
    }

    private void refreshCartBadge() {
        int n = Session.cart().size();
        cartButton.setText("🛒  Cart (" + n + ")");
    }

    @FXML
    private void onOpenCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cart.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.setTitle("Your Cart");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root, 600, 480));
            dialog.showAndWait();
            // After checkout the catalog should reflect new stock counts.
            reload();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open cart: " + e.getMessage()).showAndWait();
        }
    }
}
