package com.example.beautyhub.models;

public class BuyerReward {
    private String buyerRewardId;
    private String buyerId;
    private String rewardId;
    private long earnedDate;
    private long redeemedDate; // 0 jika belum ditebus

    public BuyerReward() {
        // Diperlukan Firebase
    }

    // --- Getters and Setters ---
    public String getBuyerRewardId() { return buyerRewardId; }
    public void setBuyerRewardId(String buyerRewardId) { this.buyerRewardId = buyerRewardId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getRewardId() { return rewardId; }
    public void setRewardId(String rewardId) { this.rewardId = rewardId; }

    public long getEarnedDate() { return earnedDate; }
    public void setEarnedDate(long earnedDate) { this.earnedDate = earnedDate; }

    public long getRedeemedDate() { return redeemedDate; }
    public void setRedeemedDate(long redeemedDate) { this.redeemedDate = redeemedDate; }
}
