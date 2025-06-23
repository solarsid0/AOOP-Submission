package oop.classes.management;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Models.EmployeeModel;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * LeaveRequestManagement handles employee leave request operations
 * Manages leave applications, approvals, rejections, and leave balance tracking
 * Integrates with your existing DAO structure and attendance tracking
 * @author Chadley
 */
public class LeaveRequestManagement {
   
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    
    // Leave status constants
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_CANCELLED = "Cancelled";
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public LeaveRequestManagement(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
    }
    
    /**
     * Submits a new leave request
     * @param employeeId Employee ID requesting leave
     * @param leaveTypeId Type of leave (from leavetype table)
     * @param startDate Start date of leave
     * @param endDate End date of leave
     * @param reason Reason for leave
     * @param isEmergency Whether this is an emergency leave
     * @return Leave request ID if successful, null if failed
     */
    public Integer submitLeaveRequest(Integer employeeId, Integer leaveTypeId, 
                                    LocalDate startDate, LocalDate endDate, 
                                    String reason, boolean isEmergency) {
        try {
            // Validate employee exists and is active
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                System.err.println("Employee not found: " + employeeId);
                return null;
            }
            
            if (!"Active".equals(employee.getStatus().getValue())) {
                System.err.println("Employee is not active: " + employeeId);
                return null;
            }
            
            // Validate leave type exists
            if (!referenceDataDAO.isValidLeaveTypeId(leaveTypeId)) {
                System.err.println("Invalid leave type ID: " + leaveTypeId);
                return null;
            }
            
            // Validate dates
            if (startDate.isAfter(endDate)) {
                System.err.println("Start date cannot be after end date");
                return null;
            }
            
            if (startDate.isBefore(LocalDate.now()) && !isEmergency) {
                System.err.println("Cannot request leave for past dates (except emergency leave)");
                return null;
            }
            
            // Calculate number of days
            long leaveDays = calculateLeaveDays(startDate, endDate);
            
            // Check for overlapping leave requests
            if (hasOverlappingLeave(employeeId, startDate, endDate)) {
                System.err.println("Overlapping leave request exists for this period");
                return null;
            }
            
            // Get supervisor ID for approval
            Integer supervisorId = employee.getSupervisorId();
            
            String sql = "INSERT INTO leaverequest " +
                        "(employeeId, leaveTypeId, startDate, endDate, leaveDays, reason, " +
                        "status, isEmergency, supervisorId, submittedDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setInt(1, employeeId);
                stmt.setInt(2, leaveTypeId);
                stmt.setDate(3, java.sql.Date.valueOf(startDate));
                stmt.setDate(4, java.sql.Date.valueOf(endDate));
                stmt.setLong(5, leaveDays);
                stmt.setString(6, reason);
                stmt.setString(7, STATUS_PENDING);
                stmt.setBoolean(8, isEmergency);
                stmt.setObject(9, supervisorId, Types.INTEGER);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Integer leaveRequestId = generatedKeys.getInt(1);
                            
                            System.out.println("Leave request submitted successfully:");
                            System.out.println("Request ID: " + leaveRequestId);
                            System.out.println("Employee: " + employee.getFirstName() + " " + employee.getLastName());
                            System.out.println("Period: " + startDate + " to " + endDate + " (" + leaveDays + " days)");
                            
                            // Create notification for supervisor if exists
                            if (supervisorId != null) {
                                createLeaveNotification(leaveRequestId, supervisorId, "PENDING_APPROVAL");
                            }
                            
                            return leaveRequestId;
                        }
                    }
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error submitting leave request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Approves a leave request
     * @param leaveRequestId Leave request ID
     * @param approvedBy Employee ID of approver (usually supervisor)
     * @param approverNotes Optional notes from approver
     * @return true if successful
     */
    public boolean approveLeaveRequest(Integer leaveRequestId, Integer approvedBy, String approverNotes) {
        try {
            // Get leave request details
            Map<String, Object> leaveRequest = getLeaveRequestById(leaveRequestId);
            if (leaveRequest == null) {
                System.err.println("Leave request not found: " + leaveRequestId);
                return false;
            }
            
            String currentStatus = (String) leaveRequest.get("status");
            if (!STATUS_PENDING.equals(currentStatus)) {
                System.err.println("Leave request is not in pending status: " + currentStatus);
                return false;
            }
            
            // Update leave request status
            String sql = "UPDATE leaverequest " +
                        "SET status = ?, approvedBy = ?, approvedDate = CURRENT_DATE, " +
                        "approverNotes = ? " +
                        "WHERE leaveRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_APPROVED);
                stmt.setInt(2, approvedBy);
                stmt.setString(3, approverNotes);
                stmt.setInt(4, leaveRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Get employee details for notifications
                    Integer employeeId = (Integer) leaveRequest.get("employeeId");
                    LocalDate startDate = ((java.sql.Date) leaveRequest.get("startDate")).toLocalDate();
                    LocalDate endDate = ((java.sql.Date) leaveRequest.get("endDate")).toLocalDate();
                    Long leaveDays = (Long) leaveRequest.get("leaveDays");
                    
                    System.out.println("Leave request approved:");
                    System.out.println("Request ID: " + leaveRequestId);
                    System.out.println("Period: " + startDate + " to " + endDate + " (" + leaveDays + " days)");
                    
                    // Update leave balance
                    updateLeaveBalance(employeeId, (Integer) leaveRequest.get("leaveTypeId"), -leaveDays);
                    
                    // Create notification for employee
                    createLeaveNotification(leaveRequestId, employeeId, "APPROVED");
                    
                    // Create absence records in attendance table for approved leave
                    createAttendanceRecordsForLeave(leaveRequestId, employeeId, startDate, endDate, approvedBy);
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error approving leave request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Rejects a leave request
     * @param leaveRequestId Leave request ID
     * @param rejectedBy Employee ID of person rejecting
     * @param rejectionReason Reason for rejection
     * @return true if successful
     */
    public boolean rejectLeaveRequest(Integer leaveRequestId, Integer rejectedBy, String rejectionReason) {
        try {
            // Get leave request details
            Map<String, Object> leaveRequest = getLeaveRequestById(leaveRequestId);
            if (leaveRequest == null) {
                System.err.println("Leave request not found: " + leaveRequestId);
                return false;
            }
            
            String currentStatus = (String) leaveRequest.get("status");
            if (!STATUS_PENDING.equals(currentStatus)) {
                System.err.println("Leave request is not in pending status: " + currentStatus);
                return false;
            }
            
            String sql = "UPDATE leaverequest " +
                        "SET status = ?, rejectedBy = ?, rejectedDate = CURRENT_DATE, " +
                        "rejectionReason = ? " +
                        "WHERE leaveRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_REJECTED);
                stmt.setInt(2, rejectedBy);
                stmt.setString(3, rejectionReason);
                stmt.setInt(4, leaveRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    Integer employeeId = (Integer) leaveRequest.get("employeeId");
                    
                    System.out.println("Leave request rejected:");
                    System.out.println("Request ID: " + leaveRequestId);
                    System.out.println("Reason: " + rejectionReason);
                    
                    // Create notification for employee
                    createLeaveNotification(leaveRequestId, employeeId, "REJECTED");
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error rejecting leave request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Cancels a leave request (by employee)
     * @param leaveRequestId Leave request ID
     * @param employeeId Employee ID (must match the original requester)
     * @param cancellationReason Reason for cancellation
     * @return true if successful
     */
    public boolean cancelLeaveRequest(Integer leaveRequestId, Integer employeeId, String cancellationReason) {
        try {
            // Get leave request details
            Map<String, Object> leaveRequest = getLeaveRequestById(leaveRequestId);
            if (leaveRequest == null) {
                System.err.println("Leave request not found: " + leaveRequestId);
                return false;
            }
            
            // Verify the employee owns this request
            Integer requestEmployeeId = (Integer) leaveRequest.get("employeeId");
            if (!employeeId.equals(requestEmployeeId)) {
                System.err.println("Employee can only cancel their own leave requests");
                return false;
            }
            
            String currentStatus = (String) leaveRequest.get("status");
            LocalDate startDate = ((java.sql.Date) leaveRequest.get("startDate")).toLocalDate();
            
            // Check if leave has already started (can't cancel leave that's already in progress)
            if (STATUS_APPROVED.equals(currentStatus) && !startDate.isAfter(LocalDate.now())) {
                System.err.println("Cannot cancel leave that has already started");
                return false;
            }
            
            String sql = "UPDATE leaverequest " +
                        "SET status = ?, cancelledDate = CURRENT_DATE, " +
                        "cancellationReason = ? " +
                        "WHERE leaveRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_CANCELLED);
                stmt.setString(2, cancellationReason);
                stmt.setInt(3, leaveRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Leave request cancelled:");
                    System.out.println("Request ID: " + leaveRequestId);
                    
                    // If leave was approved, restore leave balance
                    if (STATUS_APPROVED.equals(currentStatus)) {
                        Long leaveDays = (Long) leaveRequest.get("leaveDays");
                        updateLeaveBalance(employeeId, (Integer) leaveRequest.get("leaveTypeId"), leaveDays);
                        
                        // Remove attendance records for cancelled leave
                        removeAttendanceRecordsForLeave(leaveRequestId);
                    }
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error cancelling leave request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Gets leave request by ID
     * @param leaveRequestId Leave request ID
     * @return Leave request details or null if not found
     */
    public Map<String, Object> getLeaveRequestById(Integer leaveRequestId) {
        String sql = "SELECT lr.*, lt.leaveTypeName, " +
                    "e.firstName, e.lastName, " +
                    "s.firstName as supervisorFirstName, s.lastName as supervisorLastName " +
                    "FROM leaverequest lr " +
                    "LEFT JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId " +
                    "LEFT JOIN employee e ON lr.employeeId = e.employeeId " +
                    "LEFT JOIN employee s ON lr.supervisorId = s.employeeId " +
                    "WHERE lr.leaveRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, leaveRequestId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        record.put(columnName, value);
                    }
                    
                    return record;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave request: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gets leave requests for an employee
     * @param employeeId Employee ID
     * @param status Filter by status (null for all)
     * @param startDate Filter from date (null for no filter)
     * @param endDate Filter to date (null for no filter)
     * @return List of leave requests
     */
    public List<Map<String, Object>> getEmployeeLeaveRequests(Integer employeeId, String status, 
                                                             LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT lr.*, lt.leaveTypeName " +
            "FROM leaverequest lr " +
            "LEFT JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId " +
            "WHERE lr.employeeId = ?");
        
        List<Object> params = new ArrayList<>();
        params.add(employeeId);
        
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND lr.status = ?");
            params.add(status);
        }
        
        if (startDate != null) {
            sql.append(" AND lr.endDate >= ?");
            params.add(java.sql.Date.valueOf(startDate));
        }
        
        if (endDate != null) {
            sql.append(" AND lr.startDate <= ?");
            params.add(java.sql.Date.valueOf(endDate));
        }
        
        sql.append(" ORDER BY lr.submittedDate DESC");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    /**
     * Gets pending leave requests for approval by supervisor
     * @param supervisorId Supervisor's employee ID
     * @return List of pending leave requests
     */
    public List<Map<String, Object>> getPendingLeaveRequests(Integer supervisorId) {
        String sql = "SELECT lr.*, lt.leaveTypeName, " +
                    "e.firstName, e.lastName, p.positionTitle " +
                    "FROM leaverequest lr " +
                    "LEFT JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId " +
                    "LEFT JOIN employee e ON lr.employeeId = e.employeeId " +
                    "LEFT JOIN position p ON e.positionId = p.positionId " +
                    "WHERE lr.supervisorId = ? AND lr.status = ? " +
                    "ORDER BY lr.submittedDate ASC";
        
        return executeQuery(sql, supervisorId, STATUS_PENDING);
    }
    
    /**
     * Gets leave balance for an employee
     * @param employeeId Employee ID
     * @param leaveTypeId Leave type ID (null for all types)
     * @return Leave balance information
     */
    public List<Map<String, Object>> getLeaveBalance(Integer employeeId, Integer leaveTypeId) {
        StringBuilder sql = new StringBuilder(
            "SELECT lb.*, lt.leaveTypeName " +
            "FROM leavebalance lb " +
            "LEFT JOIN leavetype lt ON lb.leaveTypeId = lt.leaveTypeId " +
            "WHERE lb.employeeId = ?");
        
        List<Object> params = new ArrayList<>();
        params.add(employeeId);
        
        if (leaveTypeId != null) {
            sql.append(" AND lb.leaveTypeId = ?");
            params.add(leaveTypeId);
        }
        
        sql.append(" ORDER BY lt.leaveTypeName");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    /**
     * Initializes leave balance for a new employee
     * @param employeeId Employee ID
     * @param year Year for the leave balance
     * @return true if successful
     */
    public boolean initializeLeaveBalance(Integer employeeId, int year) {
        try {
            // Get all leave types
            List<Map<String, Object>> leaveTypes = referenceDataDAO.getAllLeaveTypes();
            
            for (Map<String, Object> leaveType : leaveTypes) {
                Integer leaveTypeId = (Integer) leaveType.get("leaveTypeId");
                String leaveTypeName = (String) leaveType.get("leaveTypeName");
                
                // Default leave entitlements (you can customize these)
                int defaultEntitlement = getDefaultLeaveEntitlement(leaveTypeName);
                
                String sql = "INSERT INTO leavebalance " +
                            "(employeeId, leaveTypeId, year, entitlement, used, remaining) " +
                            "VALUES (?, ?, ?, ?, 0, ?) " +
                            "ON DUPLICATE KEY UPDATE entitlement = VALUES(entitlement), remaining = VALUES(remaining)";
                
                try (Connection conn = databaseConnection.createConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, employeeId);
                    stmt.setInt(2, leaveTypeId);
                    stmt.setInt(3, year);
                    stmt.setInt(4, defaultEntitlement);
                    stmt.setInt(5, defaultEntitlement);
                    
                    stmt.executeUpdate();
                }
            }
            
            System.out.println("Leave balance initialized for employee " + employeeId + " for year " + year);
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error initializing leave balance: " + e.getMessage());
            return false;
        }
    }
    
    // UTILITY METHODS
    
    /**
     * Calculates number of leave days (excluding weekends)
     */
    private long calculateLeaveDays(LocalDate startDate, LocalDate endDate) {
        long totalDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                totalDays++;
            }
            current = current.plusDays(1);
        }
        
        return totalDays;
    }
    
    /**
     * Checks for overlapping leave requests
     */
    private boolean hasOverlappingLeave(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COUNT(*) FROM leaverequest " +
                    "WHERE employeeId = ? AND status IN (?, ?) " +
                    "AND ((startDate <= ? AND endDate >= ?) OR (startDate <= ? AND endDate >= ?))";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setString(2, STATUS_PENDING);
            stmt.setString(3, STATUS_APPROVED);
            stmt.setDate(4, java.sql.Date.valueOf(startDate));
            stmt.setDate(5, java.sql.Date.valueOf(startDate));
            stmt.setDate(6, java.sql.Date.valueOf(endDate));
            stmt.setDate(7, java.sql.Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking overlapping leave: " + e.getMessage());
            return true; // Err on the side of caution
        }
    }
    
    /**
     * Updates leave balance
     */
    private void updateLeaveBalance(Integer employeeId, Integer leaveTypeId, long daysChange) {
        String sql = "UPDATE leavebalance " +
                    "SET used = used + ?, remaining = remaining - ? " +
                    "WHERE employeeId = ? AND leaveTypeId = ? AND year = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, -daysChange); // Negative because we're subtracting from used
            stmt.setLong(2, -daysChange); // Negative because we're adding to remaining
            stmt.setInt(3, employeeId);
            stmt.setInt(4, leaveTypeId);
            stmt.setInt(5, LocalDate.now().getYear());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating leave balance: " + e.getMessage());
        }
    }
    
    /**
     * Creates attendance records for approved leave
     */
    private void createAttendanceRecordsForLeave(Integer leaveRequestId, Integer employeeId, 
                                               LocalDate startDate, LocalDate endDate, Integer recordedBy) {
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            // Only create records for weekdays
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                
                String sql = "INSERT INTO attendance " +
                            "(employeeId, attendanceDate, status, notes, isManualEntry, recordedBy) " +
                            "VALUES (?, ?, 'On Leave', ?, true, ?) " +
                            "ON DUPLICATE KEY UPDATE status = 'On Leave', notes = VALUES(notes)";
                
                try (Connection conn = databaseConnection.createConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, employeeId);
                    stmt.setDate(2, java.sql.Date.valueOf(current));
                    stmt.setString(3, "Leave Request ID: " + leaveRequestId);
                    stmt.setInt(4, recordedBy);
                    
                    stmt.executeUpdate();
                    
                } catch (SQLException e) {
                    System.err.println("Error creating attendance record for leave: " + e.getMessage());
                }
            }
            
            current = current.plusDays(1);
        }
    }
    
    /**
     * Removes attendance records for cancelled leave
     */
    private void removeAttendanceRecordsForLeave(Integer leaveRequestId) {
        String sql = "DELETE FROM attendance WHERE notes LIKE ? AND isManualEntry = true";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "Leave Request ID: " + leaveRequestId + "%");
            
            int deletedRows = stmt.executeUpdate();
            if (deletedRows > 0) {
                System.out.println("Removed " + deletedRows + " attendance records for cancelled leave");
            }
            
        } catch (SQLException e) {
            System.err.println("Error removing attendance records: " + e.getMessage());
        }
    }
    
    /**
     * Creates leave notification (placeholder for notification system)
     */
    private void createLeaveNotification(Integer leaveRequestId, Integer recipientId, String notificationType) {
        // Placeholder for notification system
        System.out.println("Notification created: " + notificationType + 
                          " for leave request " + leaveRequestId + 
                          " to employee " + recipientId);
    }
    
    /**
     * Gets default leave entitlement based on leave type
     */
    private int getDefaultLeaveEntitlement(String leaveTypeName) {
        switch (leaveTypeName.toUpperCase()) {
            case "VACATION LEAVE":
            case "ANNUAL LEAVE":
                return 15; // 15 days per year
            case "SICK LEAVE":
                return 10; // 10 days per year
            case "EMERGENCY LEAVE":
                return 5;  // 5 days per year
            case "MATERNITY LEAVE":
                return 105; // 15 weeks
            case "PATERNITY LEAVE":
                return 7;   // 1 week
            default:
                return 10;  // Default 10 days
        }
    }
    
    /**
     * Generic query execution method
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
        
        return results;
    }
}