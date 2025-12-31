package com.starter.springboot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkUserImportResponse {
    
    private long timestamp;
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<BulkUserImportResult> results;

    public BulkUserImportResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public BulkUserImportResponse(int totalRows, int successCount, int failureCount, List<BulkUserImportResult> results) {
        this();
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<BulkUserImportResult> getResults() {
        return results;
    }

    public void setResults(List<BulkUserImportResult> results) {
        this.results = results;
    }
}
