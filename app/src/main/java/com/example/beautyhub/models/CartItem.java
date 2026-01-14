package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;

public class CartItem implements Parcelable {

    private String userId;
    private String productId;
    private String name;
    private double price;
    private double discountPrice;
    private int quantity;
    private String sellerProfileImageUrl;
    private String sellerId;
    private String sellerName;
    private boolean selected = true;
    private String cartItemId; // Firebase key

    // Tracking product source & state
    private boolean isFromJson = false;
    private String imageUrls;
    private boolean isAvailable = true;

    // 1. Empty Constructor required for Firebase
    public CartItem() {}

    // 2. Full Constructor (8 parameters) - Resolves the "Cannot resolve constructor" error
    public CartItem(String productId, String name, double price, int quantity,
                    String imageUrls, String sellerId, String sellerName,
                    String sellerProfileImageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrls = imageUrls;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerProfileImageUrl = sellerProfileImageUrl;

        // Initialize defaults
        this.selected = true;
        this.isAvailable = true;
    }

    // 3. Basic Constructor (6 parameters) - For backward compatibility if used elsewhere
    public CartItem(String productId, String name, double price, int quantity, String imageUrls, String sellerProfileImageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrls = imageUrls;
        this.sellerProfileImageUrl = sellerProfileImageUrl;
        this.selected = true;
        this.isAvailable = true;
    }

    // --- Getters & Setters ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(double discountPrice) { this.discountPrice = discountPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSellerProfileImageUrl() { return sellerProfileImageUrl; }
    public void setSellerProfileImageUrl(String sellerProfileImageUrl) { this.sellerProfileImageUrl = sellerProfileImageUrl; }

    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public String getCartItemId() { return cartItemId; }
    public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }

    public boolean isFromJson() { return isFromJson; }
    public void setFromJson(boolean fromJson) { isFromJson = fromJson; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    // --- Helper Methods ---
    public String getDisplayName() { return name; }

    public double getTotalPrice() { return price * quantity; }

    // --- Parcelable Implementation ---
    protected CartItem(Parcel in) {
        userId = in.readString();
        productId = in.readString();
        name = in.readString();
        price = in.readDouble();
        discountPrice = in.readDouble();
        quantity = in.readInt();
        sellerProfileImageUrl = in.readString();
        sellerId = in.readString();
        sellerName = in.readString();
        selected = in.readByte() != 0;
        cartItemId = in.readString();
        isFromJson = in.readByte() != 0;
        imageUrls = in.readString();
        isAvailable = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(productId);
        dest.writeString(name);
        dest.writeDouble(price);
        dest.writeDouble(discountPrice);
        dest.writeInt(quantity);
        dest.writeString(sellerProfileImageUrl);
        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeString(cartItemId);
        dest.writeByte((byte) (isFromJson ? 1 : 0));
        dest.writeString(imageUrls);
        dest.writeByte((byte) (isAvailable ? 1 : 0));
    }

    public static final Creator<CartItem> CREATOR = new Creator<CartItem>() {
        @Override
        public CartItem createFromParcel(Parcel in) {
            return new CartItem(in);
        }

        @Override
        public CartItem[] newArray(int size) {
            return new CartItem[size];
        }
    };

    @Override
    public int describeContents() { return 0; }
}
