package com.example.beautyhub.models;

import com.google.firebase.database.ServerValue;

public class Reward {
    private String rewardId;
    private String title;
    private String description;
    private int pointsRequired;
    private String rewardType; // "percentage" atau "fixed_amount"
    private double discountValue;
    private boolean active;
    private Object creationTimestamp; // Untuk menjejak bila ia dicipta
    private long redeemedAt; // 1. ADD THIS LINE to store the redemption timestamp

    // Constructor kosong - WAJIB untuk Firebase
    public Reward() {
    }

    public Reward(String rewardId, String title, String description, int pointsRequired, String rewardType, double discountValue, boolean active) {
        this.rewardId = rewardId;
        this.title = title;
        this.description = description;
        this.pointsRequired = pointsRequired;
        this.rewardType = rewardType;
        this.discountValue = discountValue;
        this.active = active;
        this.creationTimestamp = ServerValue.TIMESTAMP; // Tetapkan masa semasa penciptaan
    }

    // --- GETTERS ---
    public String getRewardId() {
        return rewardId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    public String getRewardType() {
        return rewardType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public boolean isActive() {
        return active;
    }

    public Object getCreationTimestamp() {
        return creationTimestamp;
    }

    // 2. ADD THIS GETTER METHOD
    public long getRedeemedAt() {
        return redeemedAt;
    }

    // --- SETTERS ---
    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPointsRequired(int pointsRequired) {
        this.pointsRequired = pointsRequired;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreationTimestamp(Object creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    // 3. ADD THIS SETTER METHOD
    public void setRedeemedAt(long redeemedAt) {
        this.redeemedAt = redeemedAt;
    }
}
