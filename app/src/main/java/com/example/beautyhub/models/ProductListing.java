package com.example.beautyhub.models;

import java.util.List;

public class ProductListing {

    private String productId;
    private String name; // Nama produk
    private String sellerId;
    private String sellerName;
    private double price;
    private int stock;
    private String condition;
    private String listingDate;
    private boolean active;
    private String sellerProfileImageUrl;
    private List<String> imageUrls; // Tambahan: Untuk menyimpan imej bagi setiap listing

    // Konstruktor kosong diperlukan untuk Firebase
    public ProductListing() {
    }

    // Konstruktor lengkap (pilihan)
    public ProductListing(String productId, String name, String sellerId, String sellerName, double price, int stock, String condition, String listingDate, String sellerProfileImageUrl, List<String> imageUrls) {
        this.productId = productId;
        this.name = name;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.price = price;
        this.stock = stock;
        this.condition = condition;
        this.listingDate = listingDate;
        this.sellerProfileImageUrl = sellerProfileImageUrl;
        this.imageUrls = imageUrls;
        this.active = true; // Set senarai sebagai aktif secara lalai
    }

    // --- Getters ---
    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
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

    public String getSellerProfileImageUrl() {
        return sellerProfileImageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    // --- Setters ---
    public void setProductId(String productId) {
        this.productId = productId;
    }

    // Setter untuk 'name'. Parameter harus konsisten.
    public void setName(String name) {
        this.name = name;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
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

    public void setSellerProfileImageUrl(String sellerProfileImageUrl) {
        this.sellerProfileImageUrl = sellerProfileImageUrl;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
