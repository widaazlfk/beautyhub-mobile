package com.example.beautyhub.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

public class Review {

    // --- Medan Sedia Ada ---
    private String productId;
    private String userId;
    private String username;
    private float rating;
    private String comment;
    private Object timestamp; // Kekalkan sebagai Object untuk ServerValue.TIMESTAMP

    private String productName;
    private String status;

    @Exclude
    private String reviewId;

    public Review() {
        this.timestamp = ServerValue.TIMESTAMP;
        this.status = "PENDING";
    }

    // --- GETTERS ---
    public String getProductId() { return productId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public Object getTimestamp() { return timestamp; }

    // --- ▼▼▼ PERBAIKAN DI SINI ▼▼▼ ---
    // Kaedah baru untuk mendapatkan timestamp sebagai Long dengan selamat.
    // Anotasi @Exclude menghalangnya daripada diproses oleh Firebase semasa menulis/membaca.
    @Exclude
    public long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (long) timestamp;
        }
        return 0; // Kembalikan nilai lalai jika data belum ada atau bukan Long
    }
    // --- ▲▲▲ AKHIR PERBAIKAN ▲▲▲

    public String getProductName() { return productName; }
    public String getStatus() { return status; }

    @Exclude
    public String getReviewId() { return reviewId; }

    // --- SETTERS ---
    public void setProductId(String productId) { this.productId = productId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setRating(float rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public void setProductName(String productName) { this.productName = productName; }
    public void setStatus(String status) { this.status = status; }

    @Exclude
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    @Exclude
    public String getContent() {
        return comment;
    }
}
