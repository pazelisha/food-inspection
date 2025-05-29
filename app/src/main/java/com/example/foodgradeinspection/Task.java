package com.example.foodgradeinspection;

import java.util.Date;

public class Task {
    private String id;
    private String inspectorId;
    private String locationId;
    private String status;
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp scheduledDate;

    // Additional fields for display purposes (not stored in Firestore)
    private String locationName;
    private String locationAddress;
    private Double locationLat;
    private Double locationLng;

    // No-arg constructor required for Firestore
    public Task() {}

    public Task(String inspectorId, String locationId, String status, com.google.firebase.Timestamp createdAt) {
        this.inspectorId = inspectorId;
        this.locationId = locationId;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Existing getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInspectorId() {
        return inspectorId;
    }

    public void setInspectorId(String inspectorId) {
        this.inspectorId = inspectorId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public com.google.firebase.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(com.google.firebase.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // New getters and setters for additional fields
    public com.google.firebase.Timestamp getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(com.google.firebase.Timestamp scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public Double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(Double locationLat) {
        this.locationLat = locationLat;
    }

    public Double getLocationLng() {
        return locationLng;
    }

    public void setLocationLng(Double locationLng) {
        this.locationLng = locationLng;
    }

    // Convenience methods for date handling
    public Date getCreatedAtAsDate() {
        return createdAt != null ? createdAt.toDate() : null;
    }

    public Date getScheduledDateAsDate() {
        return scheduledDate != null ? scheduledDate.toDate() : null;
    }

    public long getCreatedAtAsLong() {
        return createdAt != null ? createdAt.toDate().getTime() : 0;
    }

    public long getScheduledDateAsLong() {
        return scheduledDate != null ? scheduledDate.toDate().getTime() : 0;
    }

    // Helper method to check if task is overdue
    public boolean isOverdue() {
        if (scheduledDate == null) return false;
        return scheduledDate.toDate().before(new Date()) && !"completed".equals(status);
    }

    // Helper method to get priority based on due date
    public String getPriority() {
        if (scheduledDate == null) return "Normal";

        long daysUntilDue = (scheduledDate.toDate().getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);

        if (daysUntilDue < 0) return "Overdue";
        if (daysUntilDue <= 1) return "High";
        if (daysUntilDue <= 3) return "Medium";
        return "Normal";
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", inspectorId='" + inspectorId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", locationName='" + locationName + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", scheduledDate=" + scheduledDate +
                '}';
    }
}