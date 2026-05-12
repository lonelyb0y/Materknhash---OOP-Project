package com.matraknhash.ui.controller;

import com.matraknhash.app.AppContext;
import com.matraknhash.chart.SalesChartFXGL;
import com.matraknhash.model.Part;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    @FXML private Label lblSales;
    @FXML private Label lblProfit;
    @FXML private Label lblParts;
    @FXML private Label lblLow;
    @FXML private StackPane chartHolder;
    @FXML private ListView<String> lowStockList;

    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);
    private SalesChartFXGL chart;

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();

        // Stat cards
        double sales  = ctx.saleService.totalSalesLast30Days();
        double profit = ctx.saleService.totalProfitLast30Days();
        int parts     = ctx.partService.countAll();
        List<Part> low = ctx.partService.lowStock();

        lblSales.setText(money.format(sales) + " EGP");
        lblProfit.setText(money.format(profit) + " EGP");
        lblParts.setText(String.valueOf(parts));
        lblLow.setText(String.valueOf(low.size()));

        // Low-stock list
        lowStockList.setItems(FXCollections.observableArrayList(
                low.stream().map(p -> p.getName() + "  (qty " + p.getQuantity() + ")").toList()));

        // FXGL-driven sales chart
        chart = new SalesChartFXGL(ctx.saleService);
        chartHolder.getChildren().setAll(chart);
        chart.startLiveUpdates(15);

        // Subscribe to low-stock events for live counter updates
        ctx.lowStockBus.subscribe(ev ->
                Platform.runLater(() -> {
                    lblLow.setText(String.valueOf(ev.parts().size()));
                    lowStockList.setItems(FXCollections.observableArrayList(
                            ev.parts().stream()
                                    .map(p -> p.getName() + "  (qty " + p.getQuantity() + ")")
                                    .toList()));
                }));
    }
}
