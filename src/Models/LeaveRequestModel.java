package Models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * LeaveRequestModel class that matches the database table structure exactly.
 * This class represents a leave request entity in the payroll system.
 * Each field corresponds to a column in the leaverequest table.
 * @author User
 */
public class LeaveRequestModel {
    
    // Primary key - auto-increment in database
    private Integer leaveRequestId;
    
    // Foreign keys
    private Integer employeeId;    // References employee.employeeId
    private Integer leaveTypeId;   // References leavetype.leaveTypeId
    
    // Leave details
    private LocalDate leaveStart;
    private LocalDate leaveEnd;
    private String leaveReason;
    
    // Approval information
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING; // Default value
    private LocalDateTime dateCreated;   // When request was created
    private LocalDateTime dateApproved;  // When request was approved/rejected
    private String supervisorNotes;      // Notes from supervisor
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor
     * Initializes dateCreated to current time
     */
    public LeaveRequestModel() {
        this.dateCreated = LocalDateTime.now();
        this.approvalStatus = ApprovalStatus.PENDING;
    }
    
    /**
     * Constructor with essential fields for creating a new leave request
     * @param employeeId The employee requesting leave
     * @param leaveTypeId The type of leave being requested
     * @param leaveStart The start date of the leave
     * @param leaveEnd The end date of the leave
     * @param leaveReason The reason for the leave
     */
    public LeaveRequestModel(Integer employeeId, Integer leaveTypeId, 
                           LocalDate leaveStart, LocalDate leaveEnd, String leaveReason) {
        this();
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.leaveStart = leaveStart;
        this.leaveEnd = leaveEnd;
        this.leaveReason = leaveReason;
    }
    
    /**
     * Full constructor with all fields
     * Use this when loading leave requests from the database
     * @param leaveRequestId
     * @param employeeId
     * @param leaveTypeId
     * @param leaveStart
     * @param leaveEnd
     * @param leaveReason
     * @param approvalStatus
     * @param dateCreated
     * @param dateApproved
     * @param supervisorNotes
     */
    public LeaveRequestModel(Integer leaveRequestId, Integer employeeId, Integer leaveTypeId,
                           LocalDate leaveStart, LocalDate leaveEnd, String leaveReason,
                           ApprovalStatus approvalStatus, LocalDateTime dateCreated,
                           LocalDateTime dateApproved, String supervisorNotes) {
        this.leaveRequestId = leaveRequestId;
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.leaveStart = leaveStart;
        this.leaveEnd = leaveEnd;
        this.leaveReason = leaveReason;
        this.approvalStatus = approvalStatus;
        this.dateCreated = dateCreated;
        this.dateApproved = dateApproved;
        this.supervisorNotes = supervisorNotes;
    }
    
    // ===============================
    // GETTERS AND SETTERS
    // ===============================
    
    public Integer getLeaveRequestId() {
        return leaveRequestId;
    }
    
    public void setLeaveRequestId(Integer leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public Integer getLeaveTypeId() {
        return leaveTypeId;
    }
    
    public void setLeaveTypeId(Integer leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }
    
    public LocalDate getLeaveStart() {
        return leaveStart;
    }
    
    public void setLeaveStart(LocalDate leaveStart) {
        this.leaveStart = leaveStart;
    }
    
    public LocalDate getLeaveEnd() {
        return leaveEnd;
    }
    
    public void setLeaveEnd(LocalDate leaveEnd) {
        this.leaveEnd = leaveEnd;
    }
    
    public String getLeaveReason() {
        return leaveReason;
    }
    
    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }
    
    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }
    
    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
    
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }
    
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    public LocalDateTime getDateApproved() {
        return dateApproved;
    }
    
    public void setDateApproved(LocalDateTime dateApproved) {
        this.dateApproved = dateApproved;
    }
    
    public String getSupervisorNotes() {
        return supervisorNotes;
    }
    
    public void setSupervisorNotes(String supervisorNotes) {
        this.supervisorNotes = supervisorNotes;
    }
    
    // ===============================
    // BUSINESS METHODS
    // ===============================
    
    /**
     * Calculates the number of days for this leave request
     * @return Number of days between start and end date (inclusive)
     */
    public long getLeaveDays() {
        if (leaveStart == null || leaveEnd == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(leaveStart, leaveEnd) + 1; // +1 to include both start and end
    }
    
    /**
     * Checks if this leave request is still pending approval
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return approvalStatus == ApprovalStatus.PENDING;
    }
    
    /**
     * Checks if this leave request has been approved
     * @return true if status is APPROVED
     */
    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
    
    /**
     * Checks if this leave request has been rejected
     * @return true if status is REJECTED
     */
    public boolean isRejected() {
        return approvalStatus == ApprovalStatus.REJECTED;
    }
    
    /**
     * Checks if this leave request has been processed (approved or rejected)
     * @return true if status is not PENDING
     */
    public boolean isProcessed() {
        return approvalStatus != ApprovalStatus.PENDING;
    }
    
    /**
     * Checks if the leave dates are valid (end date is not before start date)
     * @return true if dates are valid
     */
    public boolean hasValidDates() {
        return leaveStart != null && leaveEnd != null && !leaveEnd.isBefore(leaveStart);
    }
    
    /**
     * Checks if this leave request is for future dates
     * @return true if leave start date is in the future
     */
    public boolean isFutureLeave() {
        return leaveStart != null && leaveStart.isAfter(LocalDate.now());
    }
    
    /**
     * Approves this leave request with optional supervisor notes
     * @param supervisorNotes Optional notes from supervisor
     */
    public void approve(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.dateApproved = LocalDateTime.now();
        this.supervisorNotes = supervisorNotes;
    }
    
    /**
     * Rejects this leave request with supervisor notes
     * @param supervisorNotes Required notes explaining rejection
     */
    public void reject(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.dateApproved = LocalDateTime.now();
        this.supervisorNotes = supervisorNotes;
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    /**
     * Returns a string representation of the leave request
     */
    @Override
    public String toString() {
        return "LeaveRequestModel{" +
                "leaveRequestId=" + leaveRequestId +
                ", employeeId=" + employeeId +
                ", leaveTypeId=" + leaveTypeId +
                ", leaveStart=" + leaveStart +
                ", leaveEnd=" + leaveEnd +
                ", leaveDays=" + getLeaveDays() +
                ", approvalStatus=" + approvalStatus +
                ", isPending=" + isPending() +
                '}';
    }
    
    /**
     * Checks if two LeaveRequestModel objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LeaveRequestModel that = (LeaveRequestModel) obj;
        return leaveRequestId != null && leaveRequestId.equals(that.leaveRequestId);
    }
    
    /**
     * Generates a hash code for the leave request
     */
    @Override
    public int hashCode() {
        return leaveRequestId != null ? leaveRequestId.hashCode() : 0;
    }
    
    // ===============================
    // APPROVAL STATUS ENUM
    // ===============================
    
    /**
     * Approval status enum that matches the database enum values exactly
     * These are the only valid approval status values in the database
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
         * Gets the database string value for this status
         * @return The string value stored in the database
         */
        public String getValue() {
            return value;
        }
        
        /**
         * Converts a database string value back to the enum
         * @param value The string value from the database
         * @return The corresponding enum value
         * @throws IllegalArgumentException if the value is not recognized
         */
        public static ApprovalStatus fromString(String value) {
            if (value == null) {
                return PENDING; // Default value
            }
            
            for (ApprovalStatus status : ApprovalStatus.values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown approval status: " + value);
        }
        
        /**
         * Gets all possible status values as strings
         * @return Array of all status values
         */
        public static String[] getAllValues() {
            ApprovalStatus[] statuses = values();
            String[] values = new String[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
                values[i] = statuses[i].getValue();
            }
            return values;
        }
    }
}