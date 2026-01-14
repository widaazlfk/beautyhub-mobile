package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;

public class OrderItem implements Parcelable {
    private String productId;
    private String productName;
    private int quantity;
    private double price;
    private String imageUrls;
    private String subOrderId;
    private boolean reviewed;

    // Fields untuk product source
    private boolean fromJson;
    private boolean officialStore;

    // Fields untuk seller info
    private String sellerId;
    private String sellerName;
    private String sellerProfileImageUrl;

    // Constructor kosong ini WAJIB untuk Firebase
    public OrderItem() {
        // Diperlukan oleh Firebase untuk deserialization
    }

    // Constructor universal untuk semua produk
    public OrderItem(String productId, String productName, int quantity, double price,
                     String imageUrls, String sellerProfileImageUrl, String sellerId, String sellerName) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.imageUrls = imageUrls;
        this.sellerProfileImageUrl = sellerProfileImageUrl;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.reviewed = false; // Nilai lalai
        this.fromJson = false; // Nilai lalai
        this.officialStore = false; // Nilai lalai
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

    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }

    public String getSubOrderId() { return subOrderId; }
    public void setSubOrderId(String subOrderId) { this.subOrderId = subOrderId; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    // Product source getters & setters
    public boolean isFromJson() { return fromJson; }
    public void setFromJson(boolean fromJson) { this.fromJson = fromJson; }

    public boolean isOfficialStore() { return officialStore; }
    public void setOfficialStore(boolean officialStore) { this.officialStore = officialStore; }

    // Seller info getters & setters
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerProfileImageUrl() { return sellerProfileImageUrl; }
    public void setSellerProfileImageUrl(String sellerProfileImageUrl) { this.sellerProfileImageUrl = sellerProfileImageUrl; }

    // Helper methods
    public String getDisplayName() {
        return productName;
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

    // --- KOD PARCELABLE DIKEMAS KINI ---
    protected OrderItem(Parcel in) {
        productId = in.readString();
        productName = in.readString();
        quantity = in.readInt();
        price = in.readDouble();
        imageUrls = in.readString();
        subOrderId = in.readString();
        reviewed = in.readByte() != 0;
        fromJson = in.readByte() != 0;
        officialStore = in.readByte() != 0;
        sellerId = in.readString();
        sellerName = in.readString();
        sellerProfileImageUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productId);
        dest.writeString(productName);
        dest.writeInt(quantity);
        dest.writeDouble(price);
        dest.writeString(imageUrls);
        dest.writeString(subOrderId);
        dest.writeByte((byte) (reviewed ? 1 : 0));
        dest.writeByte((byte) (fromJson ? 1 : 0));
        dest.writeByte((byte) (officialStore ? 1 : 0));
        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeString(sellerProfileImageUrl);
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
    public String toString() {
        return "OrderItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", total=" + getTotalPrice() +
                ", seller='" + sellerName + '\'' +
                ", imageUrls='" + imageUrls + '\'' + // Ditambah di sini
                ", reviewed=" + reviewed +
                '}';
    }
}
