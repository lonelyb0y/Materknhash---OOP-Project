package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public class ReportsController {

    @FXML private Label lblSales, lblProfit, lblOrders, lblPurchases;
    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;

    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();

        lblSales.setText(money.format(ctx.saleService.totalSalesLast30Days())   + " EGP");
        lblProfit.setText(money.format(ctx.saleService.totalProfitLast30Days()) + " EGP");
        lblOrders.setText(String.valueOf(ctx.saleService.countAll()));
        lblPurchases.setText(money.format(ctx.purchaseService.totalPurchases()) + " EGP");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");
        Map<String, Double> daily = ctx.saleService.dailyTotals(30);
        daily.forEach((d, v) -> series.getData().add(new XYChart.Data<>(d, v)));
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.getData().add(series);

        var pieData = FXCollections.<PieChart.Data>observableArrayList();
        ctx.saleService.topSelling(5)
                .forEach(row -> pieData.add(new PieChart.Data((String) row[0], ((Number) row[1]).doubleValue())));
        pieChart.setData(pieData);
        pieChart.setLegendVisible(true);
    }
}
