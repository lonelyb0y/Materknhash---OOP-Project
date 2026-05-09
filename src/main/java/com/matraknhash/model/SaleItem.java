package com.matraknhash.model;

import java.io.Serializable;

/** Single line in a sale. Serializable so it can travel over sockets. */
public class SaleItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int saleId;
    private int partId;
    private String partSku;     // denormalized for display/transport
    private String partName;
    private int quantity;
    private double unitPrice;

    public SaleItem() {}
    public SaleItem(int partId, String partSku, String partName, int quantity, double unitPrice) {
        this.partId = partId;
        this.partSku = partSku;
        this.partName = partName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() { return id; }
    public int getSaleId() { return saleId; }
    public int getPartId() { return partId; }
    public String getPartSku() { return partSku; }
    public String getPartName() { return partName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getSubtotal() { return quantity * unitPrice; }

    public void setId(int id) { this.id = id; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public void setPartId(int partId) { this.partId = partId; }
    public void setPartSku(String partSku) { this.partSku = partSku; }
    public void setPartName(String partName) { this.partName = partName; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
