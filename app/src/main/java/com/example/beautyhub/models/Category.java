package com.example.beautyhub.models;

public class Category {
    private String categoryId;
    private String categoryName; // Nama field di Firebase
    private String categoryImage;
    private boolean isActive;

    // Empty constructor untuk Firebase
    public Category() {}

    // Constructor yang betul
    public Category(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName; // Set categoryName
        this.isActive = true;
    }

    // Constructor dengan semua field
    public Category(String categoryId, String categoryName, String categoryImage, boolean isActive) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryImage = categoryImage;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryImage() {
        return categoryImage;
    }

    public void setCategoryImage(String categoryImage) {
        this.categoryImage = categoryImage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}