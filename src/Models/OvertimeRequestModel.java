package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * OvertimeRequestModel class that matches the database table structure exactly.
 * This class represents an overtime request entity in the payroll system.
 * Each field corresponds to a column in the overtimerequest table.
 * @author User
 */
public class OvertimeRequestModel {
    
    // Primary key - auto-increment in database
    private Integer overtimeRequestId;
    
    // Foreign key
    private Integer employeeId;    // References employee.employeeId
    
    // Overtime details
    private LocalDateTime overtimeStart;  // When overtime starts
    private LocalDateTime overtimeEnd;    // When overtime ends
    private String overtimeReason;        // Reason for overtime
    
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
    public OvertimeRequestModel() {
        this.dateCreated = LocalDateTime.now();
        this.approvalStatus = ApprovalStatus.PENDING;
    }
    
    /**
     * Constructor with essential fields for creating a new overtime request
     * @param employeeId The employee requesting overtime
     * @param overtimeStart The start date/time of overtime
     * @param overtimeEnd The end date/time of overtime
     * @param overtimeReason The reason for overtime
     */
    public OvertimeRequestModel(Integer employeeId, LocalDateTime overtimeStart, 
                               LocalDateTime overtimeEnd, String overtimeReason) {
        this();
        this.employeeId = employeeId;
        this.overtimeStart = overtimeStart;
        this.overtimeEnd = overtimeEnd;
        this.overtimeReason = overtimeReason;
    }
    
    /**
     * Full constructor with all fields
     * Use this when loading overtime requests from the database
     * @param overtimeRequestId
     * @param employeeId
     * @param overtimeStart
     * @param overtimeEnd
     * @param overtimeReason
     * @param approvalStatus
     * @param dateCreated
     * @param dateApproved
     * @param supervisorNotes
     */
    public OvertimeRequestModel(Integer overtimeRequestId, Integer employeeId,
                               LocalDateTime overtimeStart, LocalDateTime overtimeEnd,
                               String overtimeReason, ApprovalStatus approvalStatus,
                               LocalDateTime dateCreated, LocalDateTime dateApproved,
                               String supervisorNotes) {
        this.overtimeRequestId = overtimeRequestId;
        this.employeeId = employeeId;
        this.overtimeStart = overtimeStart;
        this.overtimeEnd = overtimeEnd;
        this.overtimeReason = overtimeReason;
        this.approvalStatus = approvalStatus;
        this.dateCreated = dateCreated;
        this.dateApproved = dateApproved;
        this.supervisorNotes = supervisorNotes;
    }
    
    // ===============================
    // GETTERS AND SETTERS
    // ===============================
    
    public Integer getOvertimeRequestId() {
        return overtimeRequestId;
    }
    
    public void setOvertimeRequestId(Integer overtimeRequestId) {
        this.overtimeRequestId = overtimeRequestId;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public LocalDateTime getOvertimeStart() {
        return overtimeStart;
    }
    
    public void setOvertimeStart(LocalDateTime overtimeStart) {
        this.overtimeStart = overtimeStart;
    }
    
    public LocalDateTime getOvertimeEnd() {
        return overtimeEnd;
    }
    
    public void setOvertimeEnd(LocalDateTime overtimeEnd) {
        this.overtimeEnd = overtimeEnd;
    }
    
    public String getOvertimeReason() {
        return overtimeReason;
    }
    
    public void setOvertimeReason(String overtimeReason) {
        this.overtimeReason = overtimeReason;
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
     * Calculates the total overtime hours for this request
     * @return Total overtime hours as BigDecimal, or 0 if times are invalid
     */
    public BigDecimal getOvertimeHours() {
        if (overtimeStart == null || overtimeEnd == null) {
            return BigDecimal.ZERO;
        }
        
        Duration duration = Duration.between(overtimeStart, overtimeEnd);
        if (duration.isNegative()) {
            return BigDecimal.ZERO; // Invalid time range
        }
        
        // Convert to hours with 2 decimal places
        long totalMinutes = duration.toMinutes();
        return new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculates the total overtime minutes for this request
     * @return Total overtime minutes, or 0 if times are invalid
     */
    public long getOvertimeMinutes() {
        if (overtimeStart == null || overtimeEnd == null) {
            return 0;
        }
        
        Duration duration = Duration.between(overtimeStart, overtimeEnd);
        if (duration.isNegative()) {
            return 0; // Invalid time range
        }
        
        return duration.toMinutes();
    }
    
    /**
     * Calculates overtime pay based on hourly rate and multiplier
     * @param hourlyRate The employee's hourly rate
     * @param overtimeMultiplier The overtime pay multiplier (e.g., 1.5 for time-and-a-half)
     * @return The calculated overtime pay
     */
    public BigDecimal calculateOvertimePay(BigDecimal hourlyRate, BigDecimal overtimeMultiplier) {
        if (hourlyRate == null || overtimeMultiplier == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal overtimeHours = getOvertimeHours();
        return overtimeHours.multiply(hourlyRate).multiply(overtimeMultiplier).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Checks if this overtime request is still pending approval
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return approvalStatus == ApprovalStatus.PENDING;
    }
    
    /**
     * Checks if this overtime request has been approved
     * @return true if status is APPROVED
     */
    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
    
    /**
     * Checks if this overtime request has been rejected
     * @return true if status is REJECTED
     */
    public boolean isRejected() {
        return approvalStatus == ApprovalStatus.REJECTED;
    }
    
    /**
     * Checks if this overtime request has been processed (approved or rejected)
     * @return true if status is not PENDING
     */
    public boolean isProcessed() {
        return approvalStatus != ApprovalStatus.PENDING;
    }
    
    /**
     * Checks if the overtime times are valid (end time is after start time)
     * @return true if times are valid
     */
    public boolean hasValidTimes() {
        return overtimeStart != null && overtimeEnd != null && overtimeEnd.isAfter(overtimeStart);
    }
    
    /**
     * Checks if this overtime request is for future dates
     * @return true if overtime start time is in the future
     */
    public boolean isFutureOvertime() {
        return overtimeStart != null && overtimeStart.isAfter(LocalDateTime.now());
    }
    
    /**
     * Checks if this overtime request is for today
     * @return true if overtime start date is today
     */
    public boolean isToday() {
        if (overtimeStart == null) return false;
        return overtimeStart.toLocalDate().equals(java.time.LocalDate.now());
    }
    
    /**
     * Checks if this is a weekend overtime request
     * @return true if overtime start date falls on Saturday or Sunday
     */
    public boolean isWeekendOvertime() {
        if (overtimeStart == null) return false;
        java.time.DayOfWeek dayOfWeek = overtimeStart.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }
    
    /**
     * Checks if this is a night shift overtime (starts after 6 PM or before 6 AM)
     * @return true if overtime starts during night hours
     */
    public boolean isNightShiftOvertime() {
        if (overtimeStart == null) return false;
        int hour = overtimeStart.getHour();
        return hour >= 18 || hour < 6; // 6 PM to 6 AM
    }
    
    /**
     * Gets the date of the overtime request (date part of overtimeStart)
     * @return The date of overtime, or null if overtimeStart is null
     */
    public java.time.LocalDate getOvertimeDate() {
        return overtimeStart != null ? overtimeStart.toLocalDate() : null;
    }
    
    /**
     * Approves this overtime request with optional supervisor notes
     * @param supervisorNotes Optional notes from supervisor
     */
    public void approve(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.dateApproved = LocalDateTime.now();
        this.supervisorNotes = supervisorNotes;
    }
    
    /**
     * Rejects this overtime request with supervisor notes
     * @param supervisorNotes Required notes explaining rejection
     */
    public void reject(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.dateApproved = LocalDateTime.now();
        this.supervisorNotes = supervisorNotes;
    }
    
    /**
     * Formats the overtime period as a readable string
     * @return Formatted string like "2024-06-19 14:00 - 18:00 (4.0 hours)"
     */
    public String getFormattedOvertimePeriod() {
        if (overtimeStart == null || overtimeEnd == null) {
            return "Invalid time period";
        }
        
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        
        String startDate = overtimeStart.format(dateFormatter);
        String startTime = overtimeStart.format(timeFormatter);
        String endTime = overtimeEnd.format(timeFormatter);
        
        return String.format("%s %s - %s (%s hours)", startDate, startTime, endTime, getOvertimeHours());
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    /**
     * Returns a string representation of the overtime request
     */
    @Override
    public String toString() {
        return "OvertimeRequestModel{" +
                "overtimeRequestId=" + overtimeRequestId +
                ", employeeId=" + employeeId +
                ", overtimeStart=" + overtimeStart +
                ", overtimeEnd=" + overtimeEnd +
                ", overtimeHours=" + getOvertimeHours() +
                ", approvalStatus=" + approvalStatus +
                ", isPending=" + isPending() +
                '}';
    }
    
    /**
     * Checks if two OvertimeRequestModel objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        OvertimeRequestModel that = (OvertimeRequestModel) obj;
        return overtimeRequestId != null && overtimeRequestId.equals(that.overtimeRequestId);
    }
    
    /**
     * Generates a hash code for the overtime request
     */
    @Override
    public int hashCode() {
        return overtimeRequestId != null ? overtimeRequestId.hashCode() : 0;
    }
    
    /**
     * Returns a formatted display string for this overtime request
     * @return Human-readable string with date, times, hours, and status
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Overtime Request #").append(overtimeRequestId);
        sb.append(" - Employee: ").append(employeeId);
        sb.append(", Period: ").append(getFormattedOvertimePeriod());
        sb.append(", Status: ").append(approvalStatus.getValue());
        
        if (overtimeReason != null && !overtimeReason.trim().isEmpty()) {
            sb.append(", Reason: ").append(overtimeReason);
        }
        
        return sb.toString();
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