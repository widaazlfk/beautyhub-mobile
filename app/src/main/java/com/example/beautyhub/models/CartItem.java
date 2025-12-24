// CartItem.java
package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;

public class CartItem implements Parcelable {

    private String productId;
    private String productName;
    private double price;
    private int quantity;
    private String imageUrl;
    private String sellerId;
    private String sellerName;
    private boolean selected = true;
    private String cartItemId; // Firebase key

    // NEW: Fields untuk variants
    private String variantId;
    private String variantName;
    private String variantSku;

    // NEW: Untuk tracking product source
    private boolean isFromJson = false;
    private boolean hasVariants = false;

    // Constructor kosong untuk Firebase
    public CartItem() {}

    // Existing constructor (compatible)
    public CartItem(String productId, String productName, double price, int quantity, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    // NEW: Constructor dengan variant support
    public CartItem(String productId, String productName, double price, int quantity,
                    String imageUrl, String variantId, String variantName) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.variantId = variantId;
        this.variantName = variantName;
        this.hasVariants = (variantId != null && !variantId.isEmpty());
    }

    // --- Getters & Setters ---
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public String getCartItemId() { return cartItemId; }
    public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }

    // NEW: Variant getters & setters
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) {
        this.variantId = variantId;
        this.hasVariants = (variantId != null && !variantId.isEmpty());
    }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    public String getVariantSku() { return variantSku; }
    public void setVariantSku(String variantSku) { this.variantSku = variantSku; }

    public boolean isFromJson() { return isFromJson; }
    public void setFromJson(boolean fromJson) { isFromJson = fromJson; }

    public boolean hasVariants() { return hasVariants; }
    public void setHasVariants(boolean hasVariants) { this.hasVariants = hasVariants; }

    // NEW: Helper method untuk display
    public String getDisplayName() {
        if (variantName != null && !variantName.isEmpty()) {
            return productName + " (" + variantName + ")";
        }
        return productName;
    }

    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(productName);

        if (variantName != null && !variantName.isEmpty()) {
            sb.append(" - ").append(variantName);
        }

        if (variantSku != null && !variantSku.isEmpty()) {
            sb.append(" [").append(variantSku).append("]");
        }

        return sb.toString();
    }

    public double getTotalPrice() {
        return price * quantity;
    }

    // --- Parcelable Implementation ---
    protected CartItem(Parcel in) {
        productId = in.readString();
        productName = in.readString();
        price = in.readDouble();
        quantity = in.readInt();
        imageUrl = in.readString();
        sellerId = in.readString();
        sellerName = in.readString();
        selected = in.readByte() != 0;
        cartItemId = in.readString();
        variantId = in.readString();
        variantName = in.readString();
        variantSku = in.readString();
        isFromJson = in.readByte() != 0;
        hasVariants = in.readByte() != 0;
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productId);
        dest.writeString(productName);
        dest.writeDouble(price);
        dest.writeInt(quantity);
        dest.writeString(imageUrl);
        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeString(cartItemId);
        dest.writeString(variantId);
        dest.writeString(variantName);
        dest.writeString(variantSku);
        dest.writeByte((byte) (isFromJson ? 1 : 0));
        dest.writeByte((byte) (hasVariants ? 1 : 0));
    }
}