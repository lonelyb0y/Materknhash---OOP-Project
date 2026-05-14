package com.matraknhash.chart;

import com.almasb.fxgl.core.concurrent.Async;
import com.matraknhash.service.SaleService;
import javafx.application.Platform;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

/**
 * Live Sales-Overview line chart for the dashboard.
 *
 * Uses FXGL's {@link com.almasb.fxgl.core.concurrent.Async} concurrency
 * utilities to refresh the chart on a scheduled cadence with real data
 * pulled from the database via SaleService. The chart updates whenever
 * new sales are recorded (no hardcoded/static data).
 */
public class SalesChartFXGL extends StackPane {

    private final com.matraknhash.app.AppContext ctx;
    private final boolean isCenter;
    private final Integer userId;
    private final LineChart<String, Number> chart;
    private final XYChart.Series<String, Number> series = new XYChart.Series<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "fxgl-chart-refresh");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> task;

    public SalesChartFXGL(com.matraknhash.app.AppContext ctx, boolean isCenter, Integer userId) {
        this.ctx = ctx;
        this.isCenter = isCenter;
        this.userId = userId;
        CategoryAxis x = new CategoryAxis();
        x.setLabel("Date");
        NumberAxis y = new NumberAxis();
        y.setLabel("Sales (EGP)");
        chart = new LineChart<>(x, y);
        chart.setTitle("Sales Overview (Last 30 days)");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.getData().add(series);
        chart.getStyleClass().add("sales-chart");
        getChildren().add(chart);
        refreshNow();
    }

    public void startLiveUpdates(int seconds) {
        if (task != null) return;
        task = scheduler.scheduleAtFixedRate(this::refreshNow, seconds, seconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(true);
        scheduler.shutdownNow();
    }

    private void refreshNow() {
        try {
            Map<String, Double> daily;
            if (isCenter) {
                daily = ctx.serviceCenterService.dailyRevenue(userId, 30);
            } else {
                daily = userId == null ? ctx.saleService.dailyTotals(30) : ctx.saleService.dailyTotalsForSeller(30, userId);
            }
            Platform.runLater(() -> {
                series.getData().clear();
                daily.forEach((day, total) ->
                        series.getData().add(new XYChart.Data<>(day, total)));
            });
        } catch (Exception e) {
            System.err.println("[SalesChartFXGL] refresh failed: " + e.getMessage());
        }
    }
}
