package com.example.beautyhub.models;

import com.google.firebase.database.Exclude;

public class Category {
    private String categoryId;
    private String categoryName;

    // Constructor kosong - WAJIB untuk Firebase
    public Category() {}

    public Category(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // --- GETTERS ---
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }

    // --- SETTERS ---
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
