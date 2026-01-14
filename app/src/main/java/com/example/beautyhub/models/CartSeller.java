package com.example.beautyhub.models;

import java.util.List;

public class CartSeller {
    private String sellerId;
    private String sellerName;
    private String sellerProfileImageUrl;
    private List<CartItem> cartItems;
    private boolean isSelected = true; // Untuk checkbox "pilih semua" bagi setiap penjual
    private boolean isFromJsonSeller = false;

    // Default constructor for Firebase
    public CartSeller() {}

    // Original constructor (4 arguments)
    public CartSeller(String sellerId, String sellerName, String sellerProfileImageUrl, List<CartItem> cartItems) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerProfileImageUrl = sellerProfileImageUrl;
        this.cartItems = cartItems;
    }

    // New constructor with the isFromJsonSeller flag (5 arguments)
    public CartSeller(String sellerId, String sellerName, String sellerProfileImageUrl, List<CartItem> cartItems, boolean isFromJsonSeller) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerProfileImageUrl = sellerProfileImageUrl;
        this.cartItems = cartItems;
        this.isFromJsonSeller = isFromJsonSeller;
    }

    // --- Getters and Setters ---

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerProfileImageUrl() {
        return sellerProfileImageUrl;
    }

    public void setSellerProfileImageUrl(String sellerProfileImageUrl) {
        this.sellerProfileImageUrl = sellerProfileImageUrl;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isFromJsonSeller() {
        return isFromJsonSeller;
    }

    public void setFromJsonSeller(boolean fromJsonSeller) {
        isFromJsonSeller = fromJsonSeller;
    }
}
