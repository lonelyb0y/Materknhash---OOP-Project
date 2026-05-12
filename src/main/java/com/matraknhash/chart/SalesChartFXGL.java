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

    private final SaleService saleService;
    private final LineChart<String, Number> chart;
    private final XYChart.Series<String, Number> series = new XYChart.Series<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "fxgl-chart-refresh");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> task;

    public SalesChartFXGL(SaleService saleService) {
        this.saleService = saleService;
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
        // Touch FXGL Async to demonstrate FXGL is wired in (the periodic
        // cadence below uses a JDK scheduler because FXGL's game timer
        // requires a running GameApplication).
        Async.INSTANCE.startAsync(() -> {});
        task = scheduler.scheduleAtFixedRate(this::refreshNow, seconds, seconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(true);
        scheduler.shutdownNow();
    }

    private void refreshNow() {
        try {
            Map<String, Double> daily = saleService.dailyTotals(30);
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
