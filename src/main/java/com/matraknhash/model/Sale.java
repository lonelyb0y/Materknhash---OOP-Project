package com.matraknhash.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A POS sale (invoice). Serializable so cashier client can send it. */
public class Sale implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Lifecycle of a sale in the approval workflow. */
    public enum Status { PENDING, APPROVED, REJECTED }

    private int id;
    private int sellerId;
    private String sellerName;
    private double total;
    private LocalDateTime createdAt;
    private Status status = Status.PENDING;
    private Integer approverId;     // null until approved/rejected
    private String approverName;    // denormalized for UI
    private LocalDateTime approvedAt;
    private String rejectReason;
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
    public Status getStatus() { return status; }
    public Integer getApproverId() { return approverId; }
    public String getApproverName() { return approverName; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public String getRejectReason() { return rejectReason; }

    public void setId(int id) { this.id = id; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setTotal(double total) { this.total = total; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setStatus(Status status) { this.status = status; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public void recomputeTotal() {
        total = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
    }
}
