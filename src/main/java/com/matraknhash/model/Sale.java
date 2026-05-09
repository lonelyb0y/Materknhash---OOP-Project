package com.matraknhash.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A POS sale (invoice). Serializable so cashier client can send it. */
public class Sale implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int sellerId;
    private String sellerName;
    private double total;
    private LocalDateTime createdAt;
    private final List<SaleItem> items = new ArrayList<>();

    public Sale() {}
    public Sale(int sellerId, String sellerName) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
    }

    public void addItem(SaleItem item) {
        items.add(item);
        total += item.getSubtotal();
    }

    public int getId() { return id; }
    public int getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public double getTotal() { return total; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<SaleItem> getItems() { return items; }

    public void setId(int id) { this.id = id; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setTotal(double total) { this.total = total; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void recomputeTotal() {
        total = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
    }
}
