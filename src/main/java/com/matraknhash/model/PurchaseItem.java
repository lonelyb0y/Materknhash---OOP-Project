package com.matraknhash.model;

import java.io.Serializable;

public class PurchaseItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int purchaseId;
    private int partId;
    private String partSku;
    private String partName;
    private int quantity;
    private double unitCost;

    public PurchaseItem() {}
    public PurchaseItem(int partId, String partSku, String partName, int quantity, double unitCost) {
        this.partId = partId; this.partSku = partSku; this.partName = partName;
        this.quantity = quantity; this.unitCost = unitCost;
    }

    public double getSubtotal() { return quantity * unitCost; }

    public int getId() { return id; }
    public int getPurchaseId() { return purchaseId; }
    public int getPartId() { return partId; }
    public String getPartSku() { return partSku; }
    public String getPartName() { return partName; }
    public int getQuantity() { return quantity; }
    public double getUnitCost() { return unitCost; }

    public void setId(int id) { this.id = id; }
    public void setPurchaseId(int v) { this.purchaseId = v; }
    public void setPartId(int v) { this.partId = v; }
    public void setPartSku(String v) { this.partSku = v; }
    public void setPartName(String v) { this.partName = v; }
    public void setQuantity(int v) { this.quantity = v; }
    public void setUnitCost(double v) { this.unitCost = v; }
}
