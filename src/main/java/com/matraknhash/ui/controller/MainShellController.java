package com.matraknhash.ui.controller;

import com.matraknhash.app.Main;
import com.matraknhash.app.Session;
import com.matraknhash.model.Role;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainShellController {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Label userLabel;
    @FXML private Label roleLabel;

    @FXML private Button navDashboard,
            // customer
            navCatalog, navMyOrders, navServices, navMyBookings,
            // seller
            navMyListings, navIncoming,
            // service center
            navMyOffers, navCenterBookings,
            // staff
            navParts, navListingsReview, navServiceReview, navOrdersAdmin,
            navPurchases, navSuppliers, navReports,
            // admin
            navUsers, navPendingSellers, navSettings;

    private Button activeButton;

    @FXML
    public void initialize() {
        var u = Session.current();
        if (u != null) {
            userLabel.setText(u.getFullName());
            roleLabel.setText("(" + u.getRole().name() + ")");
            applyRoleVisibility(u.getRole());
            // Each role lands on the screen most useful to them.
            switch (u.getRole()) {
                case CUSTOMER       -> showCatalog();
                case SELLER         -> showDashboard();
                case SERVICE_CENTER -> showDashboard();
                default             -> showDashboard();
            }
        } else {
            showDashboard();
        }
    }

    /**
     * Per-role sidebar visibility. The marketplace pivot collapses things by
     * role: customers see only the storefront, sellers manage their own
     * listings + orders, service centers manage offers + bookings, employees
     * and admins drive the approval pipelines.
     */
    private void applyRoleVisibility(Role role) {
        boolean isCustomer = role == Role.CUSTOMER;
        boolean isSeller   = role == Role.SELLER;
        boolean isCenter   = role == Role.SERVICE_CENTER;
        boolean isEmployee = role == Role.EMPLOYEE;
        boolean isAdmin    = role == Role.ADMIN;
        boolean isStaff    = isAdmin || isEmployee;

        // Dashboard is a back-office KPI screen for staff, sellers, and centers.
        toggle(navDashboard,      isStaff || isSeller || isCenter);

        // Customer
        toggle(navCatalog,        isCustomer);
        toggle(navMyOrders,       isCustomer);
        toggle(navServices,       isCustomer);
        toggle(navMyBookings,     isCustomer);

        // Seller
        toggle(navMyListings,     isSeller);
        toggle(navIncoming,       isSeller);

        // Service Center
        toggle(navMyOffers,       isCenter);
        toggle(navCenterBookings, isCenter);

        // Staff (approval queues + back-office)
        toggle(navParts,          isStaff);
        toggle(navListingsReview, isStaff);
        toggle(navServiceReview,  isAdmin);
        toggle(navOrdersAdmin,    isAdmin);
        toggle(navPurchases,      isStaff);
        toggle(navSuppliers,      isStaff);
        toggle(navReports,        isStaff);

        // Admin only
        toggle(navUsers,          isAdmin);
        toggle(navPendingSellers, isAdmin);
        toggle(navSettings,       isAdmin);
    }

    private void toggle(Button b, boolean show) {
        if (b == null) return;
        b.setVisible(show);
        b.setManaged(show);
    }

    @FXML private void showDashboard()      { swap("Dashboard.fxml",          "Dashboard",              navDashboard); }

    // Customer
    @FXML private void showCatalog()        { swap("Catalog.fxml",            "Marketplace",            navCatalog); }
    @FXML private void showMyOrders()       { swap("MyOrders.fxml",           "My Orders",              navMyOrders); }
    @FXML private void showServices()       { swap("Services.fxml",           "Service Centers",        navServices); }
    @FXML private void showMyBookings()     { swap("MyBookings.fxml",         "My Service Bookings",    navMyBookings); }

    // Seller
    @FXML private void showMyListings()     { swap("MyListings.fxml",         "My Listings",            navMyListings); }
    @FXML private void showIncoming()       { swap("SellerOrders.fxml",       "Incoming Orders",        navIncoming); }

    // Service Center
    @FXML private void showMyOffers()       { swap("MyServiceOffers.fxml",    "My Service Offers",      navMyOffers); }
    @FXML private void showCenterBookings() { swap("CenterBookings.fxml",     "Incoming Bookings",      navCenterBookings); }

    // Staff
    @FXML private void showParts()          { swap("Parts.fxml",              "Spare Parts",            navParts); }
    @FXML private void showListingsReview() { swap("ListingReview.fxml",      "Listings Review",        navListingsReview); }
    @FXML private void showServiceReview()  { swap("ServiceOffersReview.fxml","Service Offers Review",  navServiceReview); }
    @FXML private void showOrdersAdmin()    { swap("AdminOrders.fxml",        "Approve Orders",         navOrdersAdmin); }
    @FXML private void showPurchases()      { swap("Purchases.fxml",          "Purchases",              navPurchases); }
    @FXML private void showSuppliers()      { swap("Suppliers.fxml",          "Suppliers",              navSuppliers); }
    @FXML private void showReports()        { swap("Reports.fxml",            "Reports & Analytics",    navReports); }

    // Admin
    @FXML private void showUsers()          { swap("Users.fxml",              "User Management",        navUsers); }
    @FXML private void showPendingSellers() { swap("PendingSellers.fxml",     "Pending Approvals",      navPendingSellers); }
    @FXML private void showSettings()       { swap("Settings.fxml",           "Settings",               navSettings); }

    @FXML
    private void onLogout() {
        Session.clear();
        try {
            Main.setRoot("Login.fxml", "Metrkansh ERP - Login");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void swap(String fxml, String title, Button active) {
        try {
            Node node = FXMLLoader.load(getClass().getResource("/fxml/" + fxml));
            contentArea.getChildren().setAll(node);
            pageTitle.setText(title);
            setActive(active);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load " + fxml + ":\n" + root.getClass().getSimpleName() + ": " + root.getMessage())
                    .showAndWait();
        }
    }

    private void setActive(Button b) {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        if (b != null && !b.getStyleClass().contains("active")) b.getStyleClass().add("active");
        activeButton = b;
    }
}
