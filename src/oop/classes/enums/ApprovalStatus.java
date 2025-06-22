
package oop.classes.enums;

/**
 * Enum for approval status values that match the database schema
 * Used for leave requests, overtime requests, and other approval workflows
 */
public enum ApprovalStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");
    
    private final String value;
    
    ApprovalStatus(String value) {
        this.value = value;
    }
    
    /**
     * Gets the string value that matches the database enum
     * @return String value for database operations
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Gets ApprovalStatus from string value
     * @param value String value from database
     * @return ApprovalStatus enum or null if not found
     */
    public static ApprovalStatus fromValue(String value) {
        if (value == null) return null;
        
        for (ApprovalStatus status : ApprovalStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Checks if the status represents an approved state
     * @return true if approved, false otherwise
     */
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    /**
     * Checks if the status represents a pending state
     * @return true if pending, false otherwise
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * Checks if the status represents a rejected state
     * @return true if rejected, false otherwise
     */
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    @Override
    public String toString() {
        return value;
    }
}