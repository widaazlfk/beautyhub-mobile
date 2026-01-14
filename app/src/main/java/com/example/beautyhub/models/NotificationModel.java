package com.example.beautyhub.models;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private String type;
    private String productName;
    private String sellerName;
    private String productImageUrl;
    private String orderId; // Tambahan field baru untuk navigasi
    private boolean unread;

    // Constructor kosong diperlukan untuk Firebase
    public NotificationModel() {
    }

    // Update Constructor untuk sertakan 'orderId' dan 'unread'
    public NotificationModel(String id, String title, String message, long timestamp, String type,
                             String productName, String sellerName, String productImageUrl,
                             String orderId, boolean unread) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.productName = productName;
        this.sellerName = sellerName;
        this.productImageUrl = productImageUrl;
        this.orderId = orderId; // Set orderId
        this.unread = unread;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }
}