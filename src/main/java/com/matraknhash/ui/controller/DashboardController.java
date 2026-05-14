package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.chart.SalesChartFXGL;
import com.matraknhash.model.Part;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    @FXML private Label lblSalesTitle, lblProfitTitle, lblPartsTitle, lblLowTitle, lblChartTitle, lblListTitle;
    @FXML private Label lblSales, lblProfit, lblParts, lblLow;
    @FXML private StackPane chartHolder;
    @FXML private ListView<String> lowStockList;

    // Admin-only bar chart holders
    @FXML private HBox adminChartsRow;
    @FXML private StackPane topSellersHolder;
    @FXML private StackPane topCentersHolder;

    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);
    private SalesChartFXGL chart;

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        var me = com.matraknhash.app.Session.current();
        boolean isSeller = me != null && me.getRole() == com.matraknhash.model.Role.SELLER;
        boolean isCenter = me != null && me.getRole() == com.matraknhash.model.Role.SERVICE_CENTER;
        boolean isAdmin  = me != null && (me.getRole() == com.matraknhash.model.Role.ADMIN
                                       || me.getRole() == com.matraknhash.model.Role.EMPLOYEE);
        Integer userId = (isSeller || isCenter) ? me.getId() : null;

        if (isCenter) {
            lblSalesTitle.setText("Total Revenue (30d)");
            lblProfitTitle.setText("Active Bookings");
            lblPartsTitle.setText("Total Offers");
            lblLowTitle.setText("Pending Requests");
            lblChartTitle.setText("Revenue Overview (Last 30 Days)");
            lblListTitle.setText("Pending Requests");

            double rev = ctx.serviceCenterService.totalRevenueLast30Days(userId);
            int act = ctx.serviceCenterService.countActiveBookings(userId);
            int off = ctx.serviceCenterService.countOffers(userId);
            var reqs = ctx.serviceCenterService.incoming(userId).stream()
                    .filter(r -> r.getStatus() == com.matraknhash.model.ServiceRequest.Status.REQUESTED)
                    .toList();

            lblSales.setText(money.format(rev) + " EGP");
            lblProfit.setText(String.valueOf(act));
            lblParts.setText(String.valueOf(off));
            lblLow.setText(String.valueOf(reqs.size()));

            lowStockList.setItems(FXCollections.observableArrayList(
                    reqs.stream().map(r -> "Booking #" + r.getId() + " - " + r.getOfferTitle()).toList()));

        } else {
            // Admin or Seller logic
            double sales  = isSeller ? ctx.saleService.totalSalesLast30DaysForSeller(userId) : ctx.saleService.totalSalesLast30Days();
            double profit = isSeller ? ctx.saleService.totalProfitLast30DaysForSeller(userId) : ctx.saleService.totalProfitLast30Days();
            int parts     = isSeller ? ctx.partService.countBySeller(userId) : ctx.partService.countAll();
            List<Part> low = isSeller ? ctx.partService.lowStockForSeller(userId) : ctx.partService.lowStock();

            lblSales.setText(money.format(sales) + " EGP");
            lblProfit.setText(money.format(profit) + " EGP");
            lblParts.setText(String.valueOf(parts));
            lblLow.setText(String.valueOf(low.size()));

            // Seller-specific: show average rating on title labels
            if (isSeller) {
                double avgRating = ctx.reviewService.averageRating(userId);
                int reviewCount = ctx.reviewService.countBySeller(userId);
                if (reviewCount > 0) {
                    lblListTitle.setText("Low Stock Alerts  ·  ⭐ " +
                            String.format("%.1f", avgRating) + "/5 (" + reviewCount + " reviews)");
                }
            }

            lowStockList.setItems(FXCollections.observableArrayList(
                    low.stream().map(p -> p.getName() + "  (qty " + p.getQuantity() + ")").toList()));

            ctx.lowStockBus.subscribe(ev ->
                    Platform.runLater(() -> {
                        lblLow.setText(String.valueOf(ev.parts().size()));
                        lowStockList.setItems(FXCollections.observableArrayList(
                                ev.parts().stream()
                                        .map(p -> p.getName() + "  (qty " + p.getQuantity() + ")")
                                        .toList()));
                    }));
        }

        // Daily sales/revenue line chart (all roles)
        chart = new SalesChartFXGL(ctx, isCenter, userId);
        chartHolder.getChildren().setAll(chart);
        chart.startLiveUpdates(15);

        // Admin-only: Top Sellers + Top Service Centers bar charts
        if (isAdmin && adminChartsRow != null) {
            adminChartsRow.setVisible(true);
            adminChartsRow.setManaged(true);
            buildTopSellersChart(ctx);
            buildTopCentersChart(ctx);
        }
    }

    private void buildTopSellersChart(AppContext ctx) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Seller");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (EGP)");
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setAnimated(false);
        bar.getStyleClass().add("sales-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        List<Object[]> data = ctx.saleService.topSellersByRevenue(5);
        for (Object[] row : data) {
            series.getData().add(new XYChart.Data<>((String) row[0], (Number) row[1]));
        }
        bar.getData().add(series);

        if (topSellersHolder != null) topSellersHolder.getChildren().setAll(bar);
    }

    private void buildTopCentersChart(AppContext ctx) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Service Center");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Completed Bookings");
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setAnimated(false);
        bar.getStyleClass().add("sales-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bookings");
        List<Object[]> data = ctx.serviceCenterService.topCentersByBookings(5);
        for (Object[] row : data) {
            series.getData().add(new XYChart.Data<>((String) row[0], (Number) row[1]));
        }
        bar.getData().add(series);

        if (topCentersHolder != null) topCentersHolder.getChildren().setAll(bar);
    }
}
