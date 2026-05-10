package com.matraknhash.service;

import com.matraknhash.dao.PurchaseDao;
import com.matraknhash.model.Purchase;

public class PurchaseService {
    private final PurchaseDao dao;
    public PurchaseService(PurchaseDao dao) { this.dao = dao; }
    public Purchase create(Purchase p) { p.recomputeTotal(); return dao.create(p); }
    public double totalPurchases()     { return dao.totalPurchasesAllTime(); }
}
