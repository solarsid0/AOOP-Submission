package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * OvertimeManagement - Interface for overtime operations
 * Defines methods for overtime request management and calculations
 * To be implemented by OvertimeService.java
 * @author User
 */
public interface OvertimeManagement {
    
    /**
     * Submits a new overtime request
     * @param employeeId Employee ID requesting overtime
     * @param overtimeStart Start date/time of overtime
     * @param overtimeEnd End date/time of overtime
     * @param reason Reason for overtime
     * @return Overtime request ID if successful, null if failed
     */
    Integer submitOvertimeRequest(Integer employeeId, LocalDateTime overtimeStart, 
                                LocalDateTime overtimeEnd, String reason);
    
    /**
     * Approves an overtime request
     * @param overtimeRequestId Overtime request ID
     * @param approvedBy Employee ID of approver
     * @param approverNotes Optional notes from approver
     * @return true if successful
     */
    boolean approveOvertimeRequest(Integer overtimeRequestId, Integer approvedBy, String approverNotes);
    
    /**
     * Rejects an overtime request
     * @param overtimeRequestId Overtime request ID
     * @param rejectedBy Employee ID of person rejecting
     * @param rejectionReason Reason for rejection
     * @return true if successful
     */
    boolean rejectOvertimeRequest(Integer overtimeRequestId, Integer rejectedBy, String rejectionReason);
    
    /**
     * Calculates overtime pay for a specific request
     * @param overtimeRequestId Overtime request ID
     * @param hourlyRate Employee's hourly rate
     * @return Calculated overtime pay
     */
    BigDecimal calculateOvertimePay(Integer overtimeRequestId, BigDecimal hourlyRate);
    
    /**
     * Gets overtime history for an employee
     * @param employeeId Employee ID
     * @param startDate Start date filter
     * @param endDate End date filter
     * @return List of overtime records
     */
    List<Map<String, Object>> getOvertimeHistory(Integer employeeId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Gets pending overtime requests for approval
     * @param supervisorId Supervisor's employee ID
     * @return List of pending overtime requests
     */
    List<Map<String, Object>> getPendingOvertimeRequests(Integer supervisorId);
    
    /**
     * Gets overtime statistics for an employee
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return Overtime statistics
     */
    Map<String, Object> getOvertimeStatistics(Integer employeeId, LocalDate startDate, LocalDate endDate);
}