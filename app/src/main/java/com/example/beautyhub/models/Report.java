package com.example.beautyhub.models;

public class Report {
    private String reportId;
    private String userId;
    private String title;      // Ditukar dari subject supaya sepadan dengan Activity
    private String description;
    private String imageUrl;   // Tambah field imej (Cloudinary url)
    private long timestamp;
    private String status;     // "PENDING" atau "RESOLVED"

    public Report() {
        // Diperlukan untuk Firebase Realtime Database
    }

    public Report(String reportId, String userId, String title, String description, String imageUrl, long timestamp) {
        this.reportId = reportId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.status = "PENDING"; // Default status
    }

    // Getter dan Setter
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
