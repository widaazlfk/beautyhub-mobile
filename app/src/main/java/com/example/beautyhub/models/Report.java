package com.example.beautyhub.models;

public class Report {
    private String reportId;
    private String senderId;     // Pengadu (Buyer atau Seller)
    private String reportType;   // BUYER_REPORT_SELLER atau SELLER_REPORT_BUYER
    private String targetId;     // Orang yang dilaporkan
    private String targetName;   // Nama orang yang dilaporkan
    private String reason;       // Dahulu 'title' atau 'subject'
    private String description;
    private String imageUrl;
    private long timestamp;
    private String status;       // "PENDING" atau "RESOLVED"

    public Report() {
        // Diperlukan untuk Firebase Realtime Database
    }

    public Report(String reportId, String senderId, String reportType, String targetId,
                  String targetName, String reason, String description,
                  String imageUrl, long timestamp) {
        this.reportId = reportId;
        this.senderId = senderId;
        this.reportType = reportType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.reason = reason;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.status = "PENDING";
    }

    // TAMBAHKAN GETTER DAN SETTER UNTUK reportId DI SINI
    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}