package com.matraknhash.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Purchase implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int supplierId;
    private String supplierName;
    private int userId;
    private double total;
    private LocalDateTime createdAt;
    private final List<PurchaseItem> items = new ArrayList<>();

    public Purchase() {}
    public Purchase(int supplierId, String supplierName, int userId) {
        this.supplierId = supplierId; this.supplierName = supplierName; this.userId = userId;
    }

    public void addItem(PurchaseItem it) { items.add(it); total += it.getSubtotal(); }
    public void recomputeTotal() { total = items.stream().mapToDouble(PurchaseItem::getSubtotal).sum(); }

    public int getId() { return id; }
    public int getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public int getUserId() { return userId; }
    public double getTotal() { return total; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<PurchaseItem> getItems() { return items; }

    public void setId(int id) { this.id = id; }
    public void setSupplierId(int v) { this.supplierId = v; }
    public void setSupplierName(String v) { this.supplierName = v; }
    public void setUserId(int v) { this.userId = v; }
    public void setTotal(double v) { this.total = v; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
