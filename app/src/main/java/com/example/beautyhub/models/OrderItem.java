package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;

public class OrderItem implements Parcelable {
    private String productId;
    private String productName;
    private int quantity;
    private double price;
    private String productImageUrl;
    private String subOrderId;
    private boolean reviewed;

    // NEW: Fields untuk variant
    private String variantId;
    private String variantName;
    private String variantSku;

    // NEW: Fields untuk product source
    private boolean fromJson;
    private boolean hasVariants;
    private boolean officialStore;

    // NEW: Fields untuk seller info
    private String sellerId;
    private String sellerName;

    // Constructor untuk product tanpa variant
    public OrderItem(String productId, String productName, int quantity, double price, String productImageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.productImageUrl = productImageUrl;
        this.reviewed = false;
        this.fromJson = false;
        this.hasVariants = false;
        this.officialStore = false;
    }

    // Constructor untuk product dengan variant
    public OrderItem(String productId, String productName, int quantity, double price,
                     String productImageUrl, String variantId, String variantName) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.productImageUrl = productImageUrl;
        this.variantId = variantId;
        this.variantName = variantName;
        this.reviewed = false;
        this.fromJson = false;
        this.hasVariants = true;
        this.officialStore = false;
    }

    // Constructor kosong ini WAJIB untuk Firebase
    public OrderItem() {
        // Diperlukan oleh Firebase untuk deserialization
    }

    // --- GETTERS & SETTERS ---
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getSubOrderId() { return subOrderId; }
    public void setSubOrderId(String subOrderId) { this.subOrderId = subOrderId; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

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

    // NEW: Product source getters & setters
    public boolean isFromJson() { return fromJson; }
    public void setFromJson(boolean fromJson) { this.fromJson = fromJson; }

    public boolean hasVariants() { return hasVariants; }
    public void setHasVariants(boolean hasVariants) { this.hasVariants = hasVariants; }

    public boolean isOfficialStore() { return officialStore; }
    public void setOfficialStore(boolean officialStore) { this.officialStore = officialStore; }

    // NEW: Seller info getters & setters
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    // Helper methods untuk display dan calculation
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

    public String getSourceInfo() {
        if (fromJson) {
            return "Official Store Product";
        } else if (officialStore) {
            return "Verified Seller";
        } else {
            return "Marketplace Seller";
        }
    }

    // --- KOD PARCELABLE (updated dengan semua fields baru) ---
    protected OrderItem(Parcel in) {
        productId = in.readString();
        productName = in.readString();
        quantity = in.readInt();
        price = in.readDouble();
        productImageUrl = in.readString();
        subOrderId = in.readString();
        reviewed = in.readByte() != 0;
        variantId = in.readString();
        variantName = in.readString();
        variantSku = in.readString();
        fromJson = in.readByte() != 0;
        hasVariants = in.readByte() != 0;
        officialStore = in.readByte() != 0;
        sellerId = in.readString();
        sellerName = in.readString();
    }

    public static final Creator<OrderItem> CREATOR = new Creator<OrderItem>() {
        @Override
        public OrderItem createFromParcel(Parcel in) {
            return new OrderItem(in);
        }

        @Override
        public OrderItem[] newArray(int size) {
            return new OrderItem[size];
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
        dest.writeInt(quantity);
        dest.writeDouble(price);
        dest.writeString(productImageUrl);
        dest.writeString(subOrderId);
        dest.writeByte((byte) (reviewed ? 1 : 0));
        dest.writeString(variantId);
        dest.writeString(variantName);
        dest.writeString(variantSku);
        dest.writeByte((byte) (fromJson ? 1 : 0));
        dest.writeByte((byte) (hasVariants ? 1 : 0));
        dest.writeByte((byte) (officialStore ? 1 : 0));
        dest.writeString(sellerId);
        dest.writeString(sellerName);
    }

    // toString() untuk debugging
    @Override
    public String toString() {
        return "OrderItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + getDisplayName() + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", total=" + getTotalPrice() +
                ", fromJson=" + fromJson +
                ", hasVariants=" + hasVariants +
                ", seller=" + sellerName +
                '}';
    }
}