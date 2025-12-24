package com.example.beautyhub.admin;

public class Category {
    private String id;
    private String name;
    private String description;
    private String icon;
    private int productCount;
    private boolean isActive;
    private String createdDate;

    public Category(String id, String name, String description, String icon,
                    int productCount, boolean isActive, String createdDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.productCount = productCount;
        this.isActive = isActive;
        this.createdDate = createdDate;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getProductCount() { return productCount; }
    public boolean isActive() { return isActive; }
    public String getCreatedDate() { return createdDate; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active) { isActive = active; }
    public void setProductCount(int productCount) { this.productCount = productCount; }
}