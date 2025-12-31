package com.starter.springboot.exception;

public class InvalidSortFieldException extends RuntimeException {

    private String sortField;
    private String[] validFields;

    public InvalidSortFieldException(String sortField) {
        super("Invalid sort field: " + sortField);
        this.sortField = sortField;
    }

    public InvalidSortFieldException(String sortField, String[] validFields) {
        super("Invalid sort field: " + sortField + ". Valid fields: " + String.join(", ", validFields));
        this.sortField = sortField;
        this.validFields = validFields;
    }

    public InvalidSortFieldException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getSortField() {
        return sortField;
    }

    public String[] getValidFields() {
        return validFields;
    }
}
