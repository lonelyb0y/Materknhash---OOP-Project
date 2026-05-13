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
    public double totalSalesLast30Days()             { return dao.totalSalesSince(LocalDateTime.now().minusDays(30)); }
    public double totalProfitLast30Days()            { return dao.totalProfitSince(LocalDateTime.now().minusDays(30)); }
    public Map<String, Double> dailyTotals(int days) { return dao.dailyTotals(days); }
    public List<Object[]> topSelling(int limit)      { return dao.topSelling(limit); }
    public int countAll()                            { return dao.countAll(); }
}
