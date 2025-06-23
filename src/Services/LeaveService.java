
package Services;

import DAOs.*;
import Models.*;
import Models.LeaveRequestModel.ApprovalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;

/**
 * LeaveService - Business logic for leave management
 * Handles leave requests, approvals, balance tracking, and leave-related calculations
 */

public class LeaveService {
    
    // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final LeaveDAO leaveDAO;
    private final LeaveBalanceDAO leaveBalanceDAO;
    private final EmployeeDAO employeeDAO;
    private final LeaveTypeDAO leaveTypeDAO;
    
    // Business Rules Configuration
    private static final int DEFAULT_ANNUAL_LEAVE_DAYS = 15;
    private static final int DEFAULT_SICK_LEAVE_DAYS = 10;
    private static final int MAX_ADVANCE_DAYS = 60; // Can't request leave more than 60 days in advance
    
    /**
     * Constructor - initializes required DAOs
     */
    public LeaveService() {
        this.databaseConnection = new DatabaseConnection();
        this.leaveDAO = new LeaveDAO(databaseConnection);
        this.leaveBalanceDAO = new LeaveBalanceDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.leaveTypeDAO = new LeaveTypeDAO(databaseConnection);
    }
    
    /**
     * Constructor with custom database connection
     */
    public LeaveService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.leaveDAO = new LeaveDAO(databaseConnection);
        this.leaveBalanceDAO = new LeaveBalanceDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.leaveTypeDAO = new LeaveTypeDAO(databaseConnection);
    }
    
    // ================================
    // LEAVE REQUEST OPERATIONS
    // ================================
    
    /**
     * Submits a new leave request
     * @param employeeId Employee requesting leave
     * @param leaveTypeId Type of leave
     * @param startDate Leave start date
     * @param endDate Leave end date
     * @param reason Reason for leave
     * @return LeaveRequestResult with success status and details
     */
    public LeaveRequestResult submitLeaveRequest(Integer employeeId, Integer leaveTypeId,
                                               LocalDate startDate, LocalDate endDate, String reason) {
        LeaveRequestResult result = new LeaveRequestResult();
        
        try {
            // Validate employee exists
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }
            
            // Validate leave type exists
            LeaveTypeModel leaveType = leaveTypeDAO.findById(leaveTypeId);
            if (leaveType == null) {
                result.setSuccess(false);
                result.setMessage("Leave type not found: " + leaveTypeId);
                return result;
            }
            
            // Validate dates
            LeaveValidationResult validation = validateLeaveRequest(employeeId, leaveTypeId, startDate, endDate);
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setMessage(validation.getErrorMessage());
                return result;
            }
            
            // Create leave request
            LeaveRequestModel leaveRequest = new LeaveRequestModel(employeeId, leaveTypeId, startDate, endDate, reason);
            
            boolean success = leaveDAO.save(leaveRequest);
            
            if (success) {
                result.setSuccess(true);
                result.setLeaveRequestId(leaveRequest.getLeaveRequestId());
                result.setMessage("Leave request submitted successfully for " + employee.getFullName());
                result.setLeaveDays(leaveRequest.getLeaveDays());
                
                System.out.println("✅ Leave request submitted: " + employee.getFullName() + 
                                 " (" + startDate + " to " + endDate + ", " + leaveRequest.getLeaveDays() + " days)");
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to submit leave request for " + employee.getFullName());
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error submitting leave request: " + e.getMessage());
            System.err.println("❌ Error submitting leave request for employee " + employeeId + ": " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Approves a leave request
     * @param leaveRequestId Leave request ID to approve
     * @param supervisorId ID of supervisor approving
     * @param supervisorNotes Optional notes from supervisor
     * @return LeaveApprovalResult with success status
     */
    public LeaveApprovalResult approveLeaveRequest(Integer leaveRequestId, Integer supervisorId, String supervisorNotes) {
        LeaveApprovalResult result = new LeaveApprovalResult();
        
        try {
            // Get leave request
            LeaveRequestModel leaveRequest = leaveDAO.findById(leaveRequestId);
            if (leaveRequest == null) {
                result.setSuccess(false);
                result.setMessage("Leave request not found: " + leaveRequestId);
                return result;
            }
            
            // Validate supervisor exists
            EmployeeModel supervisor = employeeDAO.findById(supervisorId);
            if (supervisor == null) {
                result.setSuccess(false);
                result.setMessage("Supervisor not found: " + supervisorId);
                return result;
            }
            
            // Check if already processed
            if (leaveRequest.isProcessed()) {
                result.setSuccess(false);
                result.setMessage("Leave request has already been " + leaveRequest.getApprovalStatus().getValue().toLowerCase());
                return result;
            }
            
            // Approve the request
            boolean success = leaveDAO.approveLeaveRequest(leaveRequestId, supervisorNotes);
            
            if (success) {
                // Update leave balance
                updateLeaveBalance(leaveRequest.getEmployeeId(), leaveRequest.getLeaveTypeId(), 
                                 (int)leaveRequest.getLeaveDays(), leaveRequest.getLeaveStart().getYear());
                
                result.setSuccess(true);
                result.setMessage("Leave request approved successfully");
                
                EmployeeModel employee = employeeDAO.findById(leaveRequest.getEmployeeId());
                System.out.println("✅ Leave request approved: " + (employee != null ? employee.getFullName() : "Employee " + leaveRequest.getEmployeeId()) + 
                                 " by " + supervisor.getFullName());
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to approve leave request");
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error approving leave request: " + e.getMessage());
            System.err.println("❌ Error approving leave request " + leaveRequestId + ": " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Rejects a leave request
     * @param leaveRequestId Leave request ID to reject
     * @param supervisorId ID of supervisor rejecting
     * @param supervisorNotes Required notes explaining rejection
     * @return LeaveApprovalResult with success status
     */
    public LeaveApprovalResult rejectLeaveRequest(Integer leaveRequestId, Integer supervisorId, String supervisorNotes) {
        LeaveApprovalResult result = new LeaveApprovalResult();
        
        try {
            // Validate supervisor notes are provided for rejection
            if (supervisorNotes == null || supervisorNotes.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Supervisor notes are required when rejecting a leave request");
                return result;
            }
            
            // Get leave request
            LeaveRequestModel leaveRequest = leaveDAO.findById(leaveRequestId);
            if (leaveRequest == null) {
                result.setSuccess(false);
                result.setMessage("Leave request not found: " + leaveRequestId);
                return result;
            }
            
            // Check if already processed
            if (leaveRequest.isProcessed()) {
                result.setSuccess(false);
                result.setMessage("Leave request has already been " + leaveRequest.getApprovalStatus().getValue().toLowerCase());
                return result;
            }
            
            // Reject the request
            boolean success = leaveDAO.rejectLeaveRequest(leaveRequestId, supervisorNotes);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("Leave request rejected successfully");
                
                EmployeeModel employee = employeeDAO.findById(leaveRequest.getEmployeeId());
                EmployeeModel supervisor = employeeDAO.findById(supervisorId);
                System.out.println("❌ Leave request rejected: " + (employee != null ? employee.getFullName() : "Employee " + leaveRequest.getEmployeeId()) + 
                                 " by " + (supervisor != null ? supervisor.getFullName() : "Supervisor " + supervisorId));
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to reject leave request");
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error rejecting leave request: " + e.getMessage());
            System.err.println("❌ Error rejecting leave request " + leaveRequestId + ": " + e.getMessage());
        }
        
        return result;
    }
    
    // ================================
    // LEAVE BALANCE MANAGEMENT
    // ================================
    
    /**
     * Gets current leave balance for an employee
     * @param employeeId Employee ID
     * @param leaveTypeId Leave type ID
     * @param year Year to check balance for
     * @return LeaveBalance object or null if not found
     */
    public LeaveBalance getLeaveBalance(Integer employeeId, Integer leaveTypeId, Integer year) {
        return leaveBalanceDAO.findByEmployeeLeaveTypeAndYear(employeeId, leaveTypeId, year);
    }
    
    /**
     * Gets all leave balances for an employee in a specific year
     * @param employeeId Employee ID
     * @param year Year to get balances for
     * @return List of leave balances
     */
    public List<LeaveBalance> getEmployeeLeaveBalances(Integer employeeId, Integer year) {
        return leaveBalanceDAO.findByEmployeeAndYear(employeeId, year);
    }
    
    /**
     * Initializes leave balances for a new employee
     * @param employeeId Employee ID
     * @param year Year to initialize for
     * @return true if successful
     */
    public boolean initializeEmployeeLeaveBalances(Integer employeeId, Integer year) {
        try {
            // Get all available leave types
            List<LeaveTypeModel> leaveTypes = leaveTypeDAO.findAll();
            
            for (LeaveTypeModel leaveType : leaveTypes) {
                // Check if balance already exists
                LeaveBalance existingBalance = leaveBalanceDAO.findByEmployeeLeaveTypeAndYear(
                    employeeId, leaveType.getLeaveTypeId(), year);
                
                if (existingBalance == null) {
                    // Create new balance
                    LeaveBalance newBalance = new LeaveBalance();
                    newBalance.setEmployeeId(employeeId);
                    newBalance.setLeaveTypeId(leaveType.getLeaveTypeId());
                    newBalance.setBalanceYear(year);
                    
                    // Set default days based on leave type
                    int defaultDays = getDefaultLeaveDays(leaveType);
                    newBalance.setTotalLeaveDays(defaultDays);
                    newBalance.setUsedLeaveDays(0);
                    newBalance.setRemainingLeaveDays(defaultDays);
                    newBalance.setCarryOverDays(0);
                    
                    leaveBalanceDAO.save(newBalance);
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error initializing leave balances for employee " + employeeId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates leave balance when leave is taken
     * @param employeeId Employee ID
     * @param leaveTypeId Leave type ID
     * @param daysUsed Number of days used
     * @param year Year to update
     * @return true if successful
     */
    private boolean updateLeaveBalance(Integer employeeId, Integer leaveTypeId, int daysUsed, int year) {
        try {
            LeaveBalance balance = leaveBalanceDAO.findByEmployeeLeaveTypeAndYear(employeeId, leaveTypeId, year);
            
            if (balance == null) {
                // Initialize if doesn't exist
                initializeEmployeeLeaveBalances(employeeId, year);
                balance = leaveBalanceDAO.findByEmployeeLeaveTypeAndYear(employeeId, leaveTypeId, year);
            }
            
            if (balance != null) {
                int newUsedDays = (balance.getUsedLeaveDays() != null ? balance.getUsedLeaveDays() : 0) + daysUsed;
                int totalAvailable = (balance.getTotalLeaveDays() != null ? balance.getTotalLeaveDays() : 0) + 
                                   (balance.getCarryOverDays() != null ? balance.getCarryOverDays() : 0);
                int newRemainingDays = totalAvailable - newUsedDays;
                
                return leaveBalanceDAO.updateUsedLeaveDays(balance.getLeaveBalanceId(), newUsedDays);
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Error updating leave balance: " + e.getMessage());
            return false;
        }
    }
    
    // ================================
    // VALIDATION AND BUSINESS RULES
    // ================================
    
    /**
     * Validates a leave request before submission
     */
    private LeaveValidationResult validateLeaveRequest(Integer employeeId, Integer leaveTypeId, 
                                                     LocalDate startDate, LocalDate endDate) {
        LeaveValidationResult result = new LeaveValidationResult();
        
        // Validate dates
        if (startDate == null || endDate == null) {
            result.setValid(false);
            result.setErrorMessage("Start date and end date are required");
            return result;
        }
        
        if (endDate.isBefore(startDate)) {
            result.setValid(false);
            result.setErrorMessage("End date cannot be before start date");
            return result;
        }
        
        // Check if dates are not too far in the future
        if (startDate.isAfter(LocalDate.now().plusDays(MAX_ADVANCE_DAYS))) {
            result.setValid(false);
            result.setErrorMessage("Cannot request leave more than " + MAX_ADVANCE_DAYS + " days in advance");
            return result;
        }
        
        // Check if dates are not in the past (except for emergency leave)
        if (startDate.isBefore(LocalDate.now().minusDays(1))) {
            result.setValid(false);
            result.setErrorMessage("Cannot request leave for past dates");
            return result;
        }
        
        // Check leave balance
        long requestedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LeaveBalance balance = getLeaveBalance(employeeId, leaveTypeId, startDate.getYear());
        
        if (balance != null) {
            int remainingDays = balance.getRemainingLeaveDays() != null ? balance.getRemainingLeaveDays() : 0;
            if (requestedDays > remainingDays) {
                result.setValid(false);
                result.setErrorMessage("Insufficient leave balance. Remaining: " + remainingDays + " days, Requested: " + requestedDays + " days");
                return result;
            }
        }
        
        // Check for overlapping leave requests
        List<LeaveRequestModel> existingRequests = leaveDAO.findByEmployeeId(employeeId);
        for (LeaveRequestModel existing : existingRequests) {
            if (existing.getApprovalStatus() == ApprovalStatus.APPROVED || existing.getApprovalStatus() == ApprovalStatus.PENDING) {
                if (datesOverlap(startDate, endDate, existing.getLeaveStart(), existing.getLeaveEnd())) {
                    result.setValid(false);
                    result.setErrorMessage("Leave request overlaps with existing request from " + 
                                         existing.getLeaveStart() + " to " + existing.getLeaveEnd());
                    return result;
                }
            }
        }
        
        result.setValid(true);
        return result;
    }
    
    /**
     * Checks if two date ranges overlap
     */
    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }
    
    /**
     * Gets default leave days for a leave type
     */
    private int getDefaultLeaveDays(LeaveTypeModel leaveType) {
        String typeName = leaveType.getLeaveTypeName().toLowerCase();
        
        if (typeName.contains("annual") || typeName.contains("vacation")) {
            return DEFAULT_ANNUAL_LEAVE_DAYS;
        } else if (typeName.contains("sick")) {
            return DEFAULT_SICK_LEAVE_DAYS;
        } else {
            return leaveType.getMaxDaysPerYear() != null ? leaveType.getMaxDaysPerYear() : DEFAULT_ANNUAL_LEAVE_DAYS;
        }
    }
    
    // ================================
    // REPORTING AND QUERIES
    // ================================
    
    /**
     * Gets pending leave requests for approval
     */
    public List<LeaveRequestModel> getPendingLeaveRequests() {
        return leaveDAO.findPendingRequests();
    }
    
    /**
     * Gets leave requests for a specific employee
     */
    public List<LeaveRequestModel> getEmployeeLeaveRequests(Integer employeeId) {
        return leaveDAO.findByEmployeeId(employeeId);
    }
    
    /**
     * Gets leave summary for an employee in a specific year
     */
    public LeaveSummary getEmployeeLeaveSummary(Integer employeeId, Integer year) {
        LeaveSummary summary = new LeaveSummary();
        summary.setEmployeeId(employeeId);
        summary.setYear(year);
        
        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee != null) {
                summary.setEmployeeName(employee.getFullName());
            }
            
            List<LeaveBalance> balances = getEmployeeLeaveBalances(employeeId, year);
            summary.setLeaveBalances(balances);
            
            // Calculate totals
            int totalAllocated = balances.stream().mapToInt(b -> b.getTotalLeaveDays() != null ? b.getTotalLeaveDays() : 0).sum();
            int totalUsed = balances.stream().mapToInt(b -> b.getUsedLeaveDays() != null ? b.getUsedLeaveDays() : 0).sum();
            int totalRemaining = balances.stream().mapToInt(b -> b.getRemainingLeaveDays() != null ? b.getRemainingLeaveDays() : 0).sum();
            
            summary.setTotalAllocatedDays(totalAllocated);
            summary.setTotalUsedDays(totalUsed);
            summary.setTotalRemainingDays(totalRemaining);
            
        } catch (Exception e) {
            System.err.println("Error generating leave summary: " + e.getMessage());
        }
        
        return summary;
    }
    
    // ================================
    // INNER CLASSES
    // ================================
    
    /**
     * Result of leave request operation
     */
    public static class LeaveRequestResult {
        private boolean success = false;
        private String message = "";
        private Integer leaveRequestId;
        private long leaveDays = 0;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getLeaveRequestId() { return leaveRequestId; }
        public void setLeaveRequestId(Integer leaveRequestId) { this.leaveRequestId = leaveRequestId; }
        public long getLeaveDays() { return leaveDays; }
        public void setLeaveDays(long leaveDays) { this.leaveDays = leaveDays; }
    }
    
    /**
     * Result of leave approval/rejection operation
     */
    public static class LeaveApprovalResult {
        private boolean success = false;
        private String message = "";
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * Result of leave request validation
     */
    public static class LeaveValidationResult {
        private boolean valid = false;
        private String errorMessage = "";
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * Leave summary for reporting
     */
    public static class LeaveSummary {
        private Integer employeeId;
        private String employeeName;
        private Integer year;
        private List<LeaveBalance> leaveBalances = new ArrayList<>();
        private int totalAllocatedDays = 0;
        private int totalUsedDays = 0;
        private int totalRemainingDays = 0;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public List<LeaveBalance> getLeaveBalances() { return leaveBalances; }
        public void setLeaveBalances(List<LeaveBalance> leaveBalances) { this.leaveBalances = leaveBalances; }
        public int getTotalAllocatedDays() { return totalAllocatedDays; }
        public void setTotalAllocatedDays(int totalAllocatedDays) { this.totalAllocatedDays = totalAllocatedDays; }
        public int getTotalUsedDays() { return totalUsedDays; }
        public void setTotalUsedDays(int totalUsedDays) { this.totalUsedDays = totalUsedDays; }
        public int getTotalRemainingDays() { return totalRemainingDays; }
        public void setTotalRemainingDays(int totalRemainingDays) { this.totalRemainingDays = totalRemainingDays; }
    }
}
