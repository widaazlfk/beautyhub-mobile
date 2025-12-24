package com.example.beautyhub.models;

public class AdminLog {
    private String logId;
    private String action;
    private String performedBy;
    private String details;
    private long timestamp;

    // Constructor kosong - WAJIB untuk Firebase
    public AdminLog() {
    }

    public AdminLog(String logId, String action, String performedBy, String details, long timestamp) {
        this.logId = logId;
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
        this.timestamp = timestamp;
    }

    // --- GETTERS ---
    public String getLogId() {
        return logId;
    }

    public String getAction() {
        return action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public String getDetails() {
        return details;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
