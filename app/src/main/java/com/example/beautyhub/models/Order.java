package com.example.beautyhub.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Order implements Parcelable {

    // Basic Fields
    private String orderId;
    private String userId;
    private ShippingAddress shippingAddress;
    private double totalAmount;
    private String status;
    private long orderDate;
    private String paymentMethod;
    private String orderSource; // "cart" or "buy_now"

    // For parent orders only
    private Map<String, Order> subOrders;

    // For sub-orders only
    private List<OrderItem> orderItems;
    private String sellerId;
    private String sellerName;
    private boolean isOfficialStore; // Flag for official stores

    // Empty constructor for Firebase
    public Order() {
        this.subOrders = new HashMap<>();
        this.orderItems = new ArrayList<>();
    }

    // Constructor for Parent Orders
    public Order(String orderId, String userId, ShippingAddress shippingAddress, double totalAmount, String status, long orderDate, String paymentMethod) {
        this(); // Call empty constructor to initialize lists/maps
        this.orderId = orderId;
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.paymentMethod = paymentMethod;
    }

    // --- Getters & Setters ---

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    // Sub-order fields
    public Map<String, Order> getSubOrders() { return subOrders; }
    public void setSubOrders(Map<String, Order> subOrders) { this.subOrders = subOrders; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public boolean isOfficialStore() { return isOfficialStore; }
    public void setOfficialStore(boolean officialStore) { isOfficialStore = officialStore; }

    // --- NEW: Added Alias/Compatibility Methods ---
    // These methods fix the errors in your adapter by providing the missing methods.

    /**
     * Alias for getOrderDate(). Provides backward compatibility.
     * @return The order date as a long.
     */
    @Exclude
    public long getTimestamp() {
        return getOrderDate();
    }

    /**
     * Alias for getTotalAmount(). Provides backward compatibility.
     * @return The total amount of the order.
     */
    @Exclude
    public double getTotalPayment() {
        return getTotalAmount();
    }

    /**
     * Alias for getOrderItems(). Provides backward compatibility.
     * @return The list of order items.
     */
    @Exclude
    public List<OrderItem> getItems() {
        return getOrderItems();
    }


    // --- Helper Methods ---

    @Exclude
    public String getDisplayStatus() {
        if (status == null) return "Unknown";
        switch (status.toLowerCase()) {
            case "pending": return "Pending Payment";
            case "processing": return "Processing";
            case "shipped": return "Shipped";
            case "delivered": return "Delivered";
            case "cancelled": return "Cancelled";
            default: return status;
        }
    }

    @Exclude
    public boolean canBeCancelled() {
        if (status == null) return false;
        String lowerCaseStatus = status.toLowerCase();
        return "pending".equals(lowerCaseStatus) || "processing".equals(lowerCaseStatus);
    }
// In your Order.java file

    // CORRECTED CONSTRUCTOR
    public Order(String orderId, String buyerId, ShippingAddress shippingAddress,
                 List<OrderItem> items, double totalAmount, String status,
                 long orderDate, String paymentMethod) {

        this(); // Call empty constructor to initialize lists/maps
        this.orderId = orderId;
        this.userId = buyerId; // FIX 1: Changed 'buyerId' to 'userId'
        this.shippingAddress = shippingAddress;
        this.orderItems = items; // FIX 2: Changed 'items' to 'orderItems'
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.paymentMethod = paymentMethod;
    }


    // --- Parcelable Implementation (Updated) ---
    protected Order(Parcel in) {
        orderId = in.readString();
        userId = in.readString();
        shippingAddress = in.readParcelable(ShippingAddress.class.getClassLoader());
        totalAmount = in.readDouble();
        status = in.readString();
        orderDate = in.readLong();
        paymentMethod = in.readString();
        orderSource = in.readString();

        // Read subOrders
        subOrders = new HashMap<>();
        in.readMap(subOrders, Order.class.getClassLoader());

        // Read orderItems (for sub-orders)
        orderItems = in.createTypedArrayList(OrderItem.CREATOR);

        sellerId = in.readString();
        sellerName = in.readString();
        isOfficialStore = in.readByte() != 0;
    }

    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel in) {
            return new Order(in);
        }

        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(orderId);
        dest.writeString(userId);
        dest.writeParcelable(shippingAddress, flags);
        dest.writeDouble(totalAmount);
        dest.writeString(status);
        dest.writeLong(orderDate);
        dest.writeString(paymentMethod);
        dest.writeString(orderSource);

        // Write subOrders
        dest.writeMap(subOrders);

        // Write orderItems (for sub-orders)
        dest.writeTypedList(orderItems);

        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeByte((byte) (isOfficialStore ? 1 : 0));
    }
}
