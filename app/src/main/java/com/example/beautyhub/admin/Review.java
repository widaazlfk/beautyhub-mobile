package com.example.beautyhub.admin;

public class Review {
    private String id;
    private String userName;
    private String rating;
    private String productName;
    private String content;
    private String status;

    public Review(String id, String userName, String rating, String productName, String content, String status) {
        this.id = id;
        this.userName = userName;
        this.rating = rating;
        this.productName = productName;
        this.content = content;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}