package com.acc.dto;
import java.util.List;

public class BulkUploadResponse {
    private String message;
    private int totalRecordsProcessed;
    private int recordsAdded;
    private int recordsSkipped;
    private List<String> skippedRecordsDetails; 

    
    public BulkUploadResponse() {
    }

    public BulkUploadResponse(String message, int totalRecordsProcessed, int recordsAdded, int recordsSkipped, List<String> skippedRecordsDetails) {
        this.message = message;
        this.totalRecordsProcessed = totalRecordsProcessed;
        this.recordsAdded = recordsAdded;
        this.recordsSkipped = recordsSkipped;
        this.skippedRecordsDetails = skippedRecordsDetails;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }

    public void setTotalRecordsProcessed(int totalRecordsProcessed) {
        this.totalRecordsProcessed = totalRecordsProcessed;
    }

    public int getRecordsAdded() {
        return recordsAdded;
    }

    public void setRecordsAdded(int recordsAdded) {
        this.recordsAdded = recordsAdded;
    }

    public int getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(int recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public List<String> getSkippedRecordsDetails() {
        return skippedRecordsDetails;
    }

    public void setSkippedRecordsDetails(List<String> skippedRecordsDetails) {
        this.skippedRecordsDetails = skippedRecordsDetails;
    }
}
