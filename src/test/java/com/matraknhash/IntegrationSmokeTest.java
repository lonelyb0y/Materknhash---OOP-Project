package com.matraknhash;

import com.matraknhash.app.AppContext;
import com.matraknhash.db.ConnectionFactory;
import com.matraknhash.db.DatabaseBootstrap;
import com.matraknhash.model.*;
import com.matraknhash.net.InvoiceClient;
import com.matraknhash.net.InvoiceMessage;
import com.matraknhash.net.InvoiceServer;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end smoke test that talks to the live Railway MySQL. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationSmokeTest {

    private static AppContext ctx;
    private static InvoiceServer srv;

    @BeforeAll
    static void boot() throws Exception {
        DatabaseBootstrap.run();
        ctx = AppContext.get();
        // Use a non-default port to avoid clashing with the desktop app.
        srv = new InvoiceServer(5566, ctx.saleService);
        srv.start();
    }

    @AfterAll
    static void shutdown() {
        if (srv != null) srv.stop();
    }

    @Test @Order(1)
    void dbReachableAndSeeded() throws SQLException {
        try (Connection c = ConnectionFactory.get(); Statement st = c.createStatement()) {
            assertCount(st, "users",      3);
            assertTrue(countOf(st, "suppliers") >= 10, "suppliers should be seeded");
            assertTrue(countOf(st, "parts")     >= 30, "parts should be seeded");
            assertTrue(countOf(st, "sales")     >= 30, "historical sales should be seeded");
        }
    }

    @Test @Order(2)
    void partsCrud() {
        int before = ctx.partService.countAll();
        Part p = new Part();
        p.setSku("TEST-SKU-" + System.currentTimeMillis());
        p.setName("Smoke Test Part");
        p.setCategory("Test");
        p.setCarMake("Test"); p.setCarModel("Test");
        p.setCostPrice(10); p.setSellPrice(20); p.setQuantity(5); p.setMinQty(1);
        Part saved = ctx.partService.save(p);
        assertTrue(saved.getId() > 0);
        assertEquals(before + 1, ctx.partService.countAll());
        assertTrue(ctx.partService.delete(saved.getId()));
        assertEquals(before, ctx.partService.countAll());
    }

    @Test @Order(3)
    void saleOverSocketIsPendingThenApprovalDecrementsStock() throws Exception {
        // Pick any part with plenty of stock
        List<Part> stocked = ctx.partService.all().stream()
                .filter(p -> p.getQuantity() >= 2).toList();
        assertFalse(stocked.isEmpty(), "need at least one part with stock >= 2");
        Part target = stocked.get(0);
        int stockBefore = target.getQuantity();

        Sale sale = new Sale(3, "Default Seller"); // seller id 3
        sale.addItem(new SaleItem(target.getId(), target.getSku(), target.getName(),
                2, target.getSellPrice()));

        InvoiceClient client = new InvoiceClient("localhost", 5566);
        InvoiceMessage resp = client.send(sale);
        assertEquals(InvoiceMessage.Type.INVOICE_ACK, resp.getType(),
                "server should ACK, got " + resp.getType() + " / " + resp.getInfo());

        // Stock must NOT be touched while invoice is pending.
        Part stillBefore = ctx.partService.all().stream()
                .filter(p -> p.getId() == target.getId()).findFirst().orElseThrow();
        assertEquals(stockBefore, stillBefore.getQuantity(),
                "pending invoice must not deduct stock");

        // Find that pending sale and approve it as admin (id=1).
        int saleId = Integer.parseInt(resp.getInfo().replace("sale#", ""));
        ctx.saleService.approve(saleId, 1);

        Part after = ctx.partService.all().stream()
                .filter(p -> p.getId() == target.getId()).findFirst().orElseThrow();
        assertEquals(stockBefore - 2, after.getQuantity(),
                "approval must decrement stock by 2");
    }

    @Test @Order(4)
    void rejectingPendingSaleLeavesStockUntouched() throws Exception {
        Part target = ctx.partService.all().stream()
                .filter(p -> p.getQuantity() >= 1).findFirst().orElseThrow();
        int stockBefore = target.getQuantity();

        Sale sale = new Sale(3, "Default Seller");
        sale.addItem(new SaleItem(target.getId(), target.getSku(), target.getName(),
                1, target.getSellPrice()));

        InvoiceMessage resp = new InvoiceClient("localhost", 5566).send(sale);
        assertEquals(InvoiceMessage.Type.INVOICE_ACK, resp.getType());
        int saleId = Integer.parseInt(resp.getInfo().replace("sale#", ""));

        ctx.saleService.reject(saleId, 1, "smoke-test reject");

        Part after = ctx.partService.all().stream()
                .filter(p -> p.getId() == target.getId()).findFirst().orElseThrow();
        assertEquals(stockBefore, after.getQuantity(),
                "rejection must leave stock unchanged");
    }

    @Test @Order(5)
    void purchaseIncrementsStock() {
        Part target = ctx.partService.all().get(0);
        int before = target.getQuantity();

        Purchase pur = new Purchase(1, "Bosch Egypt", 1);
        pur.addItem(new PurchaseItem(target.getId(), target.getSku(), target.getName(),
                5, target.getCostPrice()));
        ctx.purchaseService.create(pur);

        Part after = ctx.partService.all().stream()
                .filter(p -> p.getId() == target.getId()).findFirst().orElseThrow();
        assertEquals(before + 5, after.getQuantity(), "stock must increment by 5");
    }

    @Test @Order(6)
    void reportsQueries() {
        assertDoesNotThrow(() -> ctx.saleService.totalSalesLast30Days());
        assertDoesNotThrow(() -> ctx.saleService.totalProfitLast30Days());
        assertDoesNotThrow(() -> ctx.purchaseService.totalPurchases());
        Map<String, Double> daily = ctx.saleService.dailyTotals(30);
        assertFalse(daily.isEmpty(), "dailyTotals must return at least one day");
        List<Object[]> top = ctx.saleService.topSelling(5);
        assertFalse(top.isEmpty(), "topSelling must return at least one row");
    }

    @Test @Order(7)
    void lowStockMonitor() {
        List<Part> low = ctx.partService.lowStock();
        assertNotNull(low);
        // Clutch Kit Valeo has quantity=4/min=3 OR Timing Belt qty=8/min=5 — at least one should hit
        System.out.println("[smoke] low-stock parts: " + low.size());
    }

    @Test @Order(8)
    void authServiceWorks() {
        assertTrue(ctx.authService.login("admin", "admin123").isOk());
        assertTrue(ctx.authService.login("employee", "emp123").isOk());
        assertTrue(ctx.authService.login("seller", "sell123").isOk());
        assertTrue(ctx.authService.login("admin", "wrong").isFail());
        assertTrue(ctx.authService.login("nobody", "x").isFail());
    }

    // ---------- helpers ----------
    private static void assertCount(Statement st, String table, int expected) throws SQLException {
        assertEquals(expected, countOf(st, table), table + " row count mismatch");
    }
    private static int countOf(Statement st, String table) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
