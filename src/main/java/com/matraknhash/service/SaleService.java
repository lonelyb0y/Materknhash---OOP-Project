package com.matraknhash.service;

import com.matraknhash.dao.SaleDao;
import com.matraknhash.model.Sale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SaleService {
    private final SaleDao dao;
    public SaleService(SaleDao dao) { this.dao = dao; }

    public Sale create(Sale s)                       { s.recomputeTotal(); return dao.create(s); }
    public void approve(int saleId, int approverId)  { dao.approve(saleId, approverId); }
    public void reject(int saleId, int approverId, String reason) { dao.reject(saleId, approverId, reason); }
    public List<Sale> listPending()                  { return dao.findByStatus(Sale.Status.PENDING); }
    public List<Sale> listByStatus(Sale.Status s)    { return dao.findByStatus(s); }
    public int countPending()                        { return dao.countPending(); }

    // --- marketplace order flow (M4) ---
    public Sale placeOrder(Sale s, int buyerId)               { s.recomputeTotal(); return dao.placeOrder(s, buyerId); }
    public void sellerAck(int saleId)                         { dao.sellerAck(saleId); }
    public List<Sale> ordersByBuyer(int buyerId)              { return dao.findByBuyer(buyerId); }
    public List<Sale> incomingForSeller(int sellerId)         {
        return dao.findForSeller(sellerId, Sale.Status.PLACED, Sale.Status.RETURN_REQUESTED);
    }
    public List<Sale> historyForSeller(int sellerId)          {
        return dao.findForSeller(sellerId,
                Sale.Status.APPROVED, Sale.Status.REJECTED, Sale.Status.CANCELLED);
    }
    public List<Sale> pendingAdminOrders() {
        return dao.findByStatus(Sale.Status.APPROVED);
    }
    public double totalSalesLast30Days()             { return dao.totalSalesSince(LocalDateTime.now().minusDays(30)); }
    public double totalSalesLast30DaysForSeller(int s) { return dao.totalSalesSinceForSeller(LocalDateTime.now().minusDays(30), s); }

    public double totalProfitLast30Days()            { return dao.totalProfitSince(LocalDateTime.now().minusDays(30)); }
    public double totalProfitLast30DaysForSeller(int s) { return dao.totalProfitSinceForSeller(LocalDateTime.now().minusDays(30), s); }

    public Map<String, Double> dailyTotals(int days) { return dao.dailyTotals(days); }
    public Map<String, Double> dailyTotalsForSeller(int days, int s) { return dao.dailyTotalsForSeller(days, s); }

    public List<Object[]> topSelling(int limit)      { return dao.topSelling(limit); }
    public List<Object[]> topSellersByRevenue(int limit) { return dao.topSellersByRevenue(limit); }
    public int countAll()                            { return dao.countAll(); }

    // --- returns (M6) ---
    public void requestReturn(int saleId, String reason) { dao.requestReturn(saleId, reason); }
    public void approveReturn(int saleId)                { dao.approveReturn(saleId); }
}
