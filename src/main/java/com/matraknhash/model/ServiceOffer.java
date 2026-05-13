package com.matraknhash.model;

import java.time.LocalDateTime;

/**
 * A service that a {@link ServiceCenter} publishes (e.g. "Front brake pad
 * replacement - 800 EGP"). Goes through admin review before going LIVE.
 */
public class ServiceOffer {

    public enum Status { PENDING_ADMIN, LIVE, REJECTED }

    private int id;
    private int centerId;
    private String centerName;   // hydrated by joins; never persisted
    private String title;
    private String description;
    private double price;
    private Status status = Status.PENDING_ADMIN;
    private String reason;
    private LocalDateTime createdAt;

    public ServiceOffer() {}

    public ServiceOffer(int centerId, String title, String description, double price) {
        this.centerId = centerId;
        this.title = title;
        this.description = description;
        this.price = price;
    }

    public int getId() { return id; }
    public int getCenterId() { return centerId; }
    public String getCenterName() { return centerName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public Status getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setCenterId(int centerId) { this.centerId = centerId; }
    public void setCenterName(String n) { this.centerName = n; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setStatus(Status status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override public String toString() {
        return title + " · " + price + " EGP";
    }
}
