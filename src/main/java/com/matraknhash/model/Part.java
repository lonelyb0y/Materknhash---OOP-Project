package com.matraknhash.model;

public class Part {
    private int id;
    private String sku;
    private String name;
    private String category;
    private String carMake;
    private String carModel;
    private double costPrice;
    private double sellPrice;
    private int quantity;
    private int minQty;
    private Integer supplierId;

    public Part() {}

    public int getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getCarMake() { return carMake; }
    public String getCarModel() { return carModel; }
    public double getCostPrice() { return costPrice; }
    public double getSellPrice() { return sellPrice; }
    public int getQuantity() { return quantity; }
    public int getMinQty() { return minQty; }
    public Integer getSupplierId() { return supplierId; }

    public void setId(int id) { this.id = id; }
    public void setSku(String sku) { this.sku = sku; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setCarMake(String carMake) { this.carMake = carMake; }
    public void setCarModel(String carModel) { this.carModel = carModel; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setMinQty(int minQty) { this.minQty = minQty; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public boolean isLowStock() { return quantity <= minQty; }

    @Override public String toString() { return sku + " - " + name; }
}
