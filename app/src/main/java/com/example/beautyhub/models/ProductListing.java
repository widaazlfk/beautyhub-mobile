package com.example.beautyhub.models;

public class ProductListing {

    private String productId;
    private String sellerId;
    private double price;
    private int stock;
    private String condition;
    private String listingDate;
    private boolean active;

    // Required empty constructor for Firebase
    public ProductListing() {
    }

    public ProductListing(String productId, String sellerId, double price, int stock, String condition, String listingDate) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.price = price;
        this.stock = stock;
        this.condition = condition;
        this.listingDate = listingDate;
        this.active = true; // Set listing to active by default
    }

    // --- Getters ---
    public String getProductId() {
        return productId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getCondition() {
        return condition;
    }

    public String getListingDate() {
        return listingDate;
    }

    public boolean isActive() {
        return active;
    }

    // --- Setters ---
    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setListingDate(String listingDate) {
        this.listingDate = listingDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
