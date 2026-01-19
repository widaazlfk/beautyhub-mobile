package com.example.beautyhub.models;

public class FinanceModel {
    private String orderId;
    private double amount;
    private long timestamp;

    public FinanceModel() {} // Diperlukan oleh Firebase

    public FinanceModel(String orderId, double amount, long timestamp) {
        this.orderId = orderId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }
}