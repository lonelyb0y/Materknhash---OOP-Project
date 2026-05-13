package com.matraknhash.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A POS sale (invoice). Serializable so cashier client can send it. */
public class Sale implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Order lifecycle. The original POS path uses PENDING → APPROVED / REJECTED.
     * The marketplace path uses PLACED → SELLER_ACK → APPROVED, with optional
     * RETURN_REQUESTED → RETURN_ACK → RETURNED on top. CANCELLED is a hard stop
     * before APPROVED (no stock change).
     */
    public enum Status {
        PENDING,            // legacy POS: waiting on admin/employee approval
        APPROVED,           // finalised, stock deducted, counted in reports
        REJECTED,           // killed by admin (or employee on the listing path)
        // --- marketplace states (M1) ---
        PLACED,             // customer just clicked "Buy"
        SELLER_ACK,         // seller "printed the receipt" / is shipping it
        CANCELLED,          // either side aborted before approval
        RETURN_REQUESTED,   // customer wants the item back
        RETURN_ACK,         // seller acknowledged the return
        RETURNED            // admin approved the return, stock restored
    }

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
    // --- marketplace fields (M1) ---
    private Integer buyerId;             // null on legacy POS sales
    private String  buyerName;           // denormalized for UI
    private LocalDateTime sellerAckAt;
    private String  returnReason;
    private LocalDateTime returnRequestedAt;
    private LocalDateTime returnApprovedAt;
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
    public Integer getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public LocalDateTime getSellerAckAt() { return sellerAckAt; }
    public String getReturnReason() { return returnReason; }
    public LocalDateTime getReturnRequestedAt() { return returnRequestedAt; }
    public LocalDateTime getReturnApprovedAt() { return returnApprovedAt; }

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
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public void setSellerAckAt(LocalDateTime sellerAckAt) { this.sellerAckAt = sellerAckAt; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public void setReturnRequestedAt(LocalDateTime t) { this.returnRequestedAt = t; }
    public void setReturnApprovedAt(LocalDateTime t) { this.returnApprovedAt = t; }

    public void recomputeTotal() {
        total = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
    }
}
