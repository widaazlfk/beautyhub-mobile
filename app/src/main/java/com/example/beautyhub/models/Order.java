package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import java.util.ArrayList;
import java.util.List;

public class Order implements Parcelable {

    // Basic Fields
    private String orderId;
    private String userId;
    private String username;
    private ShippingAddress shippingAddress;
    private double totalAmount;
    private String status;      // "Pending", "Shipped", "Completed", "Cancelled"
    private long orderDate;
    private String paymentMethod;
    private String orderSource;

    // Seller Info & Discount
    private List<OrderItem> orderItems;
    private String sellerId;
    private String sellerName;
    private String sellerProfileImageUrl;
    private boolean isOfficialStore;
    private double discountAmount; // Field untuk simpan jumlah baucar

    // Constructor Kosong untuk Firebase
    public Order() {
        this.orderItems = new ArrayList<>();
    }

    // Constructor Utama
    public Order(String orderId, String userId, String username, ShippingAddress shippingAddress,
                 double totalAmount, String status, long orderDate, String paymentMethod, double discountAmount) {
        this();
        this.orderId = orderId;
        this.userId = userId;
        this.username = username;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.paymentMethod = paymentMethod;
        this.discountAmount = discountAmount;
    }

    // --- Getters & Setters ---
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getOrderDate() { return orderDate; }
    public void setOrderDate(long orderDate) { this.orderDate = orderDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getOrderSource() { return orderSource; }
    public void setOrderSource(String orderSource) { this.orderSource = orderSource; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerProfileImageUrl() { return sellerProfileImageUrl; }
    public void setSellerProfileImageUrl(String sellerProfileImageUrl) { this.sellerProfileImageUrl = sellerProfileImageUrl; }

    public boolean isOfficialStore() { return isOfficialStore; }
    public void setOfficialStore(boolean officialStore) { isOfficialStore = officialStore; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    // --- Compatibility Methods ---
    @Exclude
    public long getTimestamp() { return orderDate; }

    @Exclude
    public double getTotalPayment() { return totalAmount; }

    // --- Parcelable Implementation (Dikemaskini dengan discountAmount) ---
    protected Order(Parcel in) {
        orderId = in.readString();
        userId = in.readString();
        username = in.readString();
        shippingAddress = in.readParcelable(ShippingAddress.class.getClassLoader());
        totalAmount = in.readDouble();
        status = in.readString();
        orderDate = in.readLong();
        paymentMethod = in.readString();
        orderSource = in.readString();
        orderItems = in.createTypedArrayList(OrderItem.CREATOR);
        sellerId = in.readString();
        sellerName = in.readString();
        sellerProfileImageUrl = in.readString();
        isOfficialStore = in.readByte() != 0;
        discountAmount = in.readDouble(); // Wajib tambah ini
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(orderId);
        dest.writeString(userId);
        dest.writeString(username);
        dest.writeParcelable(shippingAddress, flags);
        dest.writeDouble(totalAmount);
        dest.writeString(status);
        dest.writeLong(orderDate);
        dest.writeString(paymentMethod);
        dest.writeString(orderSource);
        dest.writeTypedList(orderItems);
        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeString(sellerProfileImageUrl);
        dest.writeByte((byte) (isOfficialStore ? 1 : 0));
        dest.writeDouble(discountAmount); // Wajib tambah ini
    }

    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel in) { return new Order(in); }
        @Override
        public Order[] newArray(int size) { return new Order[size]; }
    };

    @Override
    public int describeContents() { return 0; }
}
