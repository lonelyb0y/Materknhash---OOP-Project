package com.matraknhash.model;

import java.time.LocalDateTime;

/** A customer's review of a seller, linked to a specific order. */
public class Review {
    private int id;
    private int saleId;
    private int customerId;
    private int sellerId;
    private int rating;          // 1-5
    private String comment;
    private LocalDateTime createdAt;
    // denormalized for UI
    private String customerName;
    private String sellerName;

    public Review() {}
    public Review(int saleId, int customerId, int sellerId, int rating, String comment) {
        this.saleId = saleId;
        this.customerId = customerId;
        this.sellerId = sellerId;
        this.rating = rating;
        this.comment = comment;
    }

    public int getId() { return id; }
    public int getSaleId() { return saleId; }
    public int getCustomerId() { return customerId; }
    public int getSellerId() { return sellerId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCustomerName() { return customerName; }
    public String getSellerName() { return sellerName; }

    public void setId(int id) { this.id = id; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
}
