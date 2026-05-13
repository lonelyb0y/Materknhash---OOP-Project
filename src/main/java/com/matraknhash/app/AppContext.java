package com.matraknhash.app;

import com.matraknhash.dao.*;
import com.matraknhash.net.InvoiceServer;
import com.matraknhash.service.*;
import com.matraknhash.thread.LowStockEvent;
import com.matraknhash.thread.StockMonitorTask;
import com.matraknhash.util.EventBus;

/**
 * Minimal manual DI container - wires DAOs, services, threads, and the
 * socket server. Created once at startup, accessible from controllers.
 */
public final class AppContext {

    private static AppContext INSTANCE;

    public final UserDao userDao         = new UserDao();
    public final SupplierDao supplierDao = new SupplierDao();
    public final PartDao partDao         = new PartDao();
    public final SaleDao saleDao         = new SaleDao();
    public final PurchaseDao purchaseDao = new PurchaseDao();
    public final ServiceCenterDao serviceCenterDao = new ServiceCenterDao();

    public final AuthService authService         = new AuthService(userDao);
    public final UserService userService         = new UserService(userDao);
    public final SupplierService supplierService = new SupplierService(supplierDao);
    public final PartService partService         = new PartService(partDao);
    public final SaleService saleService         = new SaleService(saleDao);
    public final PurchaseService purchaseService = new PurchaseService(purchaseDao);
    public final ListingService listingService   = new ListingService(partDao, supplierDao);
    public final ServiceCenterService serviceCenterService = new ServiceCenterService(serviceCenterDao);

    public final EventBus<LowStockEvent> lowStockBus = new EventBus<>();
    public final StockMonitorTask stockMonitor       = new StockMonitorTask(partDao, lowStockBus, 30);

    public final int socketPort = Integer.parseInt(System.getProperty("matraknhash.port", "5555"));
    public final InvoiceServer invoiceServer = new InvoiceServer(socketPort, saleService);

    private AppContext() {}

    public static synchronized AppContext get() {
        if (INSTANCE == null) INSTANCE = new AppContext();
        return INSTANCE;
    }
}
