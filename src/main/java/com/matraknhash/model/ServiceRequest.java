package com.matraknhash.model;

import java.time.LocalDateTime;

/**
 * Booking placed by a {@link Customer} against a LIVE {@link ServiceOffer}.
 * Lifecycle: REQUESTED -> ACCEPTED -> COMPLETED, or REJECTED.
 */
public class ServiceRequest {

    public enum Status { REQUESTED, ACCEPTED, COMPLETED, REJECTED }

    private int id;
    private int customerId;
    private int offerId;
    private String vehicleNote;
    private Status status = Status.REQUESTED;
    private LocalDateTime createdAt;

    // hydrated by joins for the UI
    private String customerName;
    private String offerTitle;
    private double offerPrice;
    private int    centerId;
    private String centerName;

    public ServiceRequest() {}

    public ServiceRequest(int customerId, int offerId, String vehicleNote) {
        this.customerId = customerId;
        this.offerId = offerId;
        this.vehicleNote = vehicleNote;
    }

    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getOfferId() { return offerId; }
    public String getVehicleNote() { return vehicleNote; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCustomerName() { return customerName; }
    public String getOfferTitle() { return offerTitle; }
    public double getOfferPrice() { return offerPrice; }
    public int getCenterId() { return centerId; }
    public String getCenterName() { return centerName; }

    public void setId(int id) { this.id = id; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setOfferId(int offerId) { this.offerId = offerId; }
    public void setVehicleNote(String n) { this.vehicleNote = n; }
    public void setStatus(Status status) { this.status = status; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public void setCustomerName(String n) { this.customerName = n; }
    public void setOfferTitle(String t) { this.offerTitle = t; }
    public void setOfferPrice(double p) { this.offerPrice = p; }
    public void setCenterId(int id) { this.centerId = id; }
    public void setCenterName(String n) { this.centerName = n; }
}
