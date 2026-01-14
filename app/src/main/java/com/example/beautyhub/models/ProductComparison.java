package com.example.beautyhub.models;

import com.google.firebase.database.PropertyName;

import java.util.List;

public class ProductComparison {
    private String productId;
    private String name;
    private double price;
    private String sellerName;
    private String sellerId;
    private int matchPercentage;
    private List<String> imageUrls;
    private String ingredients; // WAJIB TAMBAH UNTUK LOGIK MATCH SCORE

    // Constructor Kosong (Wajib untuk Firebase)
    public ProductComparison() {}

    // Constructor Lengkap
    public ProductComparison(String productId, String name, double price, String sellerName, String sellerId, int matchPercentage, List<String> imageUrls, String ingredients) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.sellerName = sellerName;
        this.sellerId = sellerId;
        this.matchPercentage = matchPercentage;
        this.imageUrls = imageUrls;
        this.ingredients = ingredients;
    }

    // --- GETTER & SETTER ---

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public int getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(int matchPercentage) { this.matchPercentage = matchPercentage; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
}
