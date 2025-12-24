package com.example.beautyhub.models;

import java.util.Date;
import java.util.List;

public class Promotion {
    private String promotionId;
    private String title;
    private String description;
    private String type; // seasonal_sale, welcome_offer, bundle_offer, flash_sale, shipping_offer, store_offer, clearance_sale
    private String discountType; // percentage, fixed_amount, shipping
    private double discountValue;
    private double minPurchase;
    private Date startDate;
    private Date endDate;
    private List<String> applicableCategories;
    private List<String> applicableBrands;
    private Double maxDiscountAmount;
    private Integer usageLimit;
    private int usedCount;
    private boolean active;
    private String imageUrl;
    private String promoCode;

    // Constructors
    public Promotion() {}

    public Promotion(String promotionId, String title, String description, String type,
                     String discountType, double discountValue, double minPurchase,
                     Date startDate, Date endDate, boolean active) {
        this.promotionId = promotionId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minPurchase = minPurchase;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    // Getters and Setters
    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMinPurchase() { return minPurchase; }
    public void setMinPurchase(double minPurchase) { this.minPurchase = minPurchase; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public List<String> getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(List<String> applicableCategories) { this.applicableCategories = applicableCategories; }

    public List<String> getApplicableBrands() { return applicableBrands; }
    public void setApplicableBrands(List<String> applicableBrands) { this.applicableBrands = applicableBrands; }

    public Double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(Double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    // Helper methods
    public boolean isApplicableToCategory(String category) {
        if (applicableCategories == null || applicableCategories.isEmpty()) return false;
        return applicableCategories.contains("all") || applicableCategories.contains(category);
    }

    public boolean isApplicableToBrand(String brand) {
        if (applicableBrands == null || applicableBrands.isEmpty()) return false;
        return applicableBrands.contains("all") || applicableBrands.contains(brand);
    }

    public boolean isValidForDate(Date currentDate) {
        return currentDate.after(startDate) && currentDate.before(endDate);
    }

    public boolean isUsageLimitReached() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    public double calculateDiscount(double purchaseAmount) {
        if (purchaseAmount < minPurchase) return 0.0;

        double discount = 0.0;

        if ("percentage".equals(discountType)) {
            discount = purchaseAmount * (discountValue / 100.0);
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else if ("fixed_amount".equals(discountType)) {
            discount = discountValue;
        }
        // For "shipping" type, discount would be handled separately

        return discount;
    }
}