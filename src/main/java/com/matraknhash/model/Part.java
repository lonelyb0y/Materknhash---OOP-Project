package com.matraknhash.model;

public class Part {

    /**
     * A listing's lifecycle on the marketplace. Seeded parts are LIVE.
     * A new seller listing flows DRAFT → PENDING_EMPLOYEE → PENDING_ADMIN → LIVE.
     * REJECTED or HIDDEN listings don't appear in the customer catalog.
     */
    public enum ListingStatus { DRAFT, PENDING_EMPLOYEE, PENDING_ADMIN, LIVE, REJECTED, HIDDEN }

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
    private String imageUrl;
    // --- marketplace fields (M1) ---
    private Integer sellerId;
    private ListingStatus listingStatus = ListingStatus.LIVE;
    private String listingReason;
    private Integer employeeReviewerId;
    private Integer adminReviewerId;

    public Part() {}

    public int getId() { return id; }
    public String getImageUrl() { return imageUrl; }
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
    public Integer getSellerId() { return sellerId; }
    public ListingStatus getListingStatus() { return listingStatus; }
    public String getListingReason() { return listingReason; }
    public Integer getEmployeeReviewerId() { return employeeReviewerId; }
    public Integer getAdminReviewerId() { return adminReviewerId; }

    public void setId(int id) { this.id = id; }
    public void setImageUrl(String url) { this.imageUrl = url; }
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
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }
    public void setListingStatus(ListingStatus listingStatus) { this.listingStatus = listingStatus; }
    public void setListingReason(String listingReason) { this.listingReason = listingReason; }
    public void setEmployeeReviewerId(Integer id) { this.employeeReviewerId = id; }
    public void setAdminReviewerId(Integer id) { this.adminReviewerId = id; }

    public boolean isLowStock() { return quantity <= minQty; }

    @Override public String toString() { return sku + " - " + name; }
}
