
package Models;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Models.EmployeeModel;
import oop.classes.management.AttendanceTracking;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.*;


/**
 * OvertimeManagement handles overtime request workflows and approvals
 * Manages overtime applications, supervisor approvals, overtime tracking, and integration with payroll
 * Links overtime request system to attendance tracking and payroll calculations
 * @author Chadley
 */
public class OvertimeManagement {
 
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    private final AttendanceTracking attendanceTracking;
    
    // Overtime status constants
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_COMPLETED = "Completed";
    
    // Overtime type constants
    public static final String TYPE_REGULAR = "Regular";
    public static final String TYPE_HOLIDAY = "Holiday";
    public static final String TYPE_WEEKEND = "Weekend";
    public static final String TYPE_EMERGENCY = "Emergency";
    public static final String TYPE_PROJECT = "Project";
    public static final String TYPE_SPECIAL = "Special";
    
    // Business rules constants
    private static final int MAX_DAILY_OVERTIME_HOURS = 12;
    private static final int MAX_WEEKLY_OVERTIME_HOURS = 60;
    private static final int ADVANCE_REQUEST_HOURS = 24; // Must request 24 hours in advance
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public OvertimeManagement(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.attendanceTracking = new AttendanceTracking(databaseConnection);
    }
    
    /**
     * Submits a new overtime request
     * @param employeeId Employee ID requesting overtime
     * @param overtimeDate Date of overtime work
     * @param startTime Start time of overtime
     * @param endTime End time of overtime
     * @param overtimeHours Number of overtime hours
     * @param overtimeType Type of overtime
     * @param reason Reason for overtime
     * @param projectCode Project code (if applicable)
     * @param isEmergency Whether this is emergency overtime
     * @return Overtime request ID if successful, null if failed
     */
    public Integer submitOvertimeRequest(Integer employeeId, LocalDate overtimeDate, 
                                       LocalTime startTime, LocalTime endTime, 
                                       Double overtimeHours, String overtimeType, 
                                       String reason, String projectCode, boolean isEmergency) {
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
            
            // Validate overtime request
            if (!validateOvertimeRequest(employeeId, overtimeDate, startTime, endTime, 
                                       overtimeHours, overtimeType, isEmergency)) {
                return null;
            }
            
            // Calculate overtime details
            Duration duration = Duration.between(startTime, endTime);
            double calculatedHours = duration.toMinutes() / 60.0;
            
            // Use calculated hours if overtime hours not provided
            if (overtimeHours == null || overtimeHours <= 0) {
                overtimeHours = calculatedHours;
            }
            
            // Get supervisor for approval
            Integer supervisorId = employee.getSupervisorId();
            
            String sql = "INSERT INTO overtime_request " +
                        "(employeeId, overtimeDate, startTime, endTime, overtimeHours, " +
                        "overtimeType, reason, projectCode, isEmergency, status, " +
                        "supervisorId, submittedDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setInt(1, employeeId);
                stmt.setDate(2, java.sql.Date.valueOf(overtimeDate));
                stmt.setTime(3, Time.valueOf(startTime));
                stmt.setTime(4, Time.valueOf(endTime));
                stmt.setDouble(5, overtimeHours);
                stmt.setString(6, overtimeType);
                stmt.setString(7, reason);
                stmt.setString(8, projectCode);
                stmt.setBoolean(9, isEmergency);
                stmt.setString(10, STATUS_PENDING);
                stmt.setObject(11, supervisorId, Types.INTEGER);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Integer overtimeRequestId = generatedKeys.getInt(1);
                            
                            System.out.println("Overtime request submitted successfully:");
                            System.out.println("Request ID: " + overtimeRequestId);
                            System.out.println("Employee: " + employee.getFirstName() + " " + employee.getLastName());
                            System.out.println("Date: " + overtimeDate + " (" + overtimeHours + " hours)");
                            
                            // Create notification for supervisor if exists
                            if (supervisorId != null) {
                                createOvertimeNotification(overtimeRequestId, supervisorId, "PENDING_APPROVAL");
                            }
                            
                            return overtimeRequestId;
                        }
                    }
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error submitting overtime request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Approves an overtime request
     * @param overtimeRequestId Overtime request ID
     * @param approvedBy Employee ID of approver (usually supervisor)
     * @param approverNotes Optional notes from approver
     * @param adjustedHours Adjusted overtime hours (if different from requested)
     * @return true if successful
     */
    public boolean approveOvertimeRequest(Integer overtimeRequestId, Integer approvedBy, 
                                        String approverNotes, Double adjustedHours) {
        try {
            // Get overtime request details
            Map<String, Object> overtimeRequest = getOvertimeRequestById(overtimeRequestId);
            if (overtimeRequest == null) {
                System.err.println("Overtime request not found: " + overtimeRequestId);
                return false;
            }
            
            String currentStatus = (String) overtimeRequest.get("status");
            if (!STATUS_PENDING.equals(currentStatus)) {
                System.err.println("Overtime request is not in pending status: " + currentStatus);
                return false;
            }
            
            // Use original hours if no adjustment provided
            if (adjustedHours == null || adjustedHours <= 0) {
                adjustedHours = (Double) overtimeRequest.get("overtimeHours");
            }
            
            // Update overtime request status
            String sql = "UPDATE overtime_request " +
                        "SET status = ?, approvedBy = ?, approvedDate = CURRENT_TIMESTAMP, " +
                        "approverNotes = ?, approvedHours = ? " +
                        "WHERE overtimeRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_APPROVED);
                stmt.setInt(2, approvedBy);
                stmt.setString(3, approverNotes);
                stmt.setDouble(4, adjustedHours);
                stmt.setInt(5, overtimeRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    Integer employeeId = (Integer) overtimeRequest.get("employeeId");
                    LocalDate overtimeDate = ((java.sql.Date) overtimeRequest.get("overtimeDate")).toLocalDate();
                    
                    System.out.println("Overtime request approved:");
                    System.out.println("Request ID: " + overtimeRequestId);
                    System.out.println("Date: " + overtimeDate + " (" + adjustedHours + " hours)");
                    
                    // Create notification for employee
                    createOvertimeNotification(overtimeRequestId, employeeId, "APPROVED");
                    
                    // Create overtime allowance record if applicable
                    createOvertimeAllowance(overtimeRequestId, employeeId, adjustedHours);
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error approving overtime request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Rejects an overtime request
     * @param overtimeRequestId Overtime request ID
     * @param rejectedBy Employee ID of person rejecting
     * @param rejectionReason Reason for rejection
     * @return true if successful
     */
    public boolean rejectOvertimeRequest(Integer overtimeRequestId, Integer rejectedBy, String rejectionReason) {
        try {
            // Get overtime request details
            Map<String, Object> overtimeRequest = getOvertimeRequestById(overtimeRequestId);
            if (overtimeRequest == null) {
                System.err.println("Overtime request not found: " + overtimeRequestId);
                return false;
            }
            
            String currentStatus = (String) overtimeRequest.get("status");
            if (!STATUS_PENDING.equals(currentStatus)) {
                System.err.println("Overtime request is not in pending status: " + currentStatus);
                return false;
            }
            
            String sql = "UPDATE overtime_request " +
                        "SET status = ?, rejectedBy = ?, rejectedDate = CURRENT_TIMESTAMP, " +
                        "rejectionReason = ? " +
                        "WHERE overtimeRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_REJECTED);
                stmt.setInt(2, rejectedBy);
                stmt.setString(3, rejectionReason);
                stmt.setInt(4, overtimeRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    Integer employeeId = (Integer) overtimeRequest.get("employeeId");
                    
                    System.out.println("Overtime request rejected:");
                    System.out.println("Request ID: " + overtimeRequestId);
                    System.out.println("Reason: " + rejectionReason);
                    
                    // Create notification for employee
                    createOvertimeNotification(overtimeRequestId, employeeId, "REJECTED");
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error rejecting overtime request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Cancels an overtime request (by employee)
     * @param overtimeRequestId Overtime request ID
     * @param employeeId Employee ID (must match the original requester)
     * @param cancellationReason Reason for cancellation
     * @return true if successful
     */
    public boolean cancelOvertimeRequest(Integer overtimeRequestId, Integer employeeId, String cancellationReason) {
        try {
            // Get overtime request details
            Map<String, Object> overtimeRequest = getOvertimeRequestById(overtimeRequestId);
            if (overtimeRequest == null) {
                System.err.println("Overtime request not found: " + overtimeRequestId);
                return false;
            }
            
            // Verify the employee owns this request
            Integer requestEmployeeId = (Integer) overtimeRequest.get("employeeId");
            if (!employeeId.equals(requestEmployeeId)) {
                System.err.println("Employee can only cancel their own overtime requests");
                return false;
            }
            
            String currentStatus = (String) overtimeRequest.get("status");
            LocalDate overtimeDate = ((java.sql.Date) overtimeRequest.get("overtimeDate")).toLocalDate();
            
            // Check if overtime has already been completed
            if (STATUS_COMPLETED.equals(currentStatus)) {
                System.err.println("Cannot cancel completed overtime");
                return false;
            }
            
            // Check if overtime date has passed (for approved overtime)
            if (STATUS_APPROVED.equals(currentStatus) && !overtimeDate.isAfter(LocalDate.now())) {
                System.err.println("Cannot cancel overtime that has already occurred");
                return false;
            }
            
            String sql = "UPDATE overtime_request " +
                        "SET status = ?, cancelledDate = CURRENT_TIMESTAMP, " +
                        "cancellationReason = ? " +
                        "WHERE overtimeRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_CANCELLED);
                stmt.setString(2, cancellationReason);
                stmt.setInt(3, overtimeRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Overtime request cancelled:");
                    System.out.println("Request ID: " + overtimeRequestId);
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error cancelling overtime request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Marks overtime as completed (for tracking actual vs planned overtime)
     * @param overtimeRequestId Overtime request ID
     * @param actualHours Actual hours worked
     * @param completionNotes Notes about completion
     * @return true if successful
     */
    public boolean completeOvertimeRequest(Integer overtimeRequestId, Double actualHours, String completionNotes) {
        try {
            Map<String, Object> overtimeRequest = getOvertimeRequestById(overtimeRequestId);
            if (overtimeRequest == null) {
                System.err.println("Overtime request not found: " + overtimeRequestId);
                return false;
            }
            
            if (!STATUS_APPROVED.equals(overtimeRequest.get("status"))) {
                System.err.println("Only approved overtime can be marked as completed");
                return false;
            }
            
            String sql = "UPDATE overtime_request " +
                        "SET status = ?, actualHours = ?, completionNotes = ?, " +
                        "completedDate = CURRENT_TIMESTAMP " +
                        "WHERE overtimeRequestId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, STATUS_COMPLETED);
                stmt.setDouble(2, actualHours);
                stmt.setString(3, completionNotes);
                stmt.setInt(4, overtimeRequestId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Overtime request marked as completed:");
                    System.out.println("Request ID: " + overtimeRequestId);
                    System.out.println("Actual hours: " + actualHours);
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error completing overtime request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Gets overtime request by ID
     * @param overtimeRequestId Overtime request ID
     * @return Overtime request details or null if not found
     */
    public Map<String, Object> getOvertimeRequestById(Integer overtimeRequestId) {
        String sql = "SELECT or.*, ot.overtimeTypeName, " +
                    "e.firstName, e.lastName, " +
                    "s.firstName as supervisorFirstName, s.lastName as supervisorLastName " +
                    "FROM overtime_request or " +
                    "LEFT JOIN overtime_type ot ON or.overtimeType = ot.overtimeTypeName " +
                    "LEFT JOIN employee e ON or.employeeId = e.employeeId " +
                    "LEFT JOIN employee s ON or.supervisorId = s.employeeId " +
                    "WHERE or.overtimeRequestId = ?";
        
        List<Map<String, Object>> results = executeQuery(sql, overtimeRequestId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Gets overtime requests for an employee
     * @param employeeId Employee ID
     * @param status Filter by status (null for all)
     * @param startDate Filter from date (null for no filter)
     * @param endDate Filter to date (null for no filter)
     * @return List of overtime requests
     */
    public List<Map<String, Object>> getEmployeeOvertimeRequests(Integer employeeId, String status, 
                                                               LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT or.*, ot.overtimeTypeName " +
            "FROM overtime_request or " +
            "LEFT JOIN overtime_type ot ON or.overtimeType = ot.overtimeTypeName " +
            "WHERE or.employeeId = ?");
        
        List<Object> params = new ArrayList<>();
        params.add(employeeId);
        
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND or.status = ?");
            params.add(status);
        }
        
        if (startDate != null) {
            sql.append(" AND or.overtimeDate >= ?");
            params.add(java.sql.Date.valueOf(startDate));
        }
        
        if (endDate != null) {
            sql.append(" AND or.overtimeDate <= ?");
            params.add(java.sql.Date.valueOf(endDate));
        }
        
        sql.append(" ORDER BY or.overtimeDate DESC");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    /**
     * Gets pending overtime requests for approval by supervisor
     * @param supervisorId Supervisor's employee ID
     * @return List of pending overtime requests
     */
    public List<Map<String, Object>> getPendingOvertimeRequests(Integer supervisorId) {
        String sql = "SELECT or.*, ot.overtimeTypeName, " +
                    "e.firstName, e.lastName, p.positionTitle " +
                    "FROM overtime_request or " +
                    "LEFT JOIN overtime_type ot ON or.overtimeType = ot.overtimeTypeName " +
                    "LEFT JOIN employee e ON or.employeeId = e.employeeId " +
                    "LEFT JOIN position p ON e.positionId = p.positionId " +
                    "WHERE or.supervisorId = ? AND or.status = ? " +
                    "ORDER BY or.submittedDate ASC";
        
        return executeQuery(sql, supervisorId, STATUS_PENDING);
    }
    
    /**
     * Gets overtime statistics for an employee
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return Overtime statistics
     */
    public Map<String, Object> getEmployeeOvertimeStatistics(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                    "COUNT(*) as totalRequests, " +
                    "SUM(CASE WHEN status = 'Approved' THEN 1 ELSE 0 END) as approvedRequests, " +
                    "SUM(CASE WHEN status = 'Rejected' THEN 1 ELSE 0 END) as rejectedRequests, " +
                    "SUM(CASE WHEN status = 'Completed' THEN actualHours ELSE 0 END) as totalActualHours, " +
                    "SUM(CASE WHEN status IN ('Approved', 'Completed') THEN overtimeHours ELSE 0 END) as totalApprovedHours, " +
                    "AVG(CASE WHEN status IN ('Approved', 'Completed') THEN overtimeHours ELSE NULL END) as averageOvertimeHours " +
                    "FROM overtime_request " +
                    "WHERE employeeId = ? AND overtimeDate BETWEEN ? AND ?";
        
        List<Map<String, Object>> results = executeQuery(sql, employeeId, 
            java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
        
        if (!results.isEmpty()) {
            Map<String, Object> stats = results.get(0);
            
            // Calculate approval rate
            Integer totalRequests = (Integer) stats.get("totalRequests");
            Integer approvedRequests = (Integer) stats.get("approvedRequests");
            
            if (totalRequests > 0) {
                double approvalRate = (approvedRequests * 100.0) / totalRequests;
                stats.put("approvalRate", approvalRate);
            } else {
                stats.put("approvalRate", 0.0);
            }
            
            return stats;
        }
        
        return new HashMap<>();
    }
    
    // VALIDATION AND BUSINESS RULES
    
    /**
     * Validates overtime request against business rules
     */
    private boolean validateOvertimeRequest(Integer employeeId, LocalDate overtimeDate, 
                                          LocalTime startTime, LocalTime endTime, 
                                          Double overtimeHours, String overtimeType, 
                                          boolean isEmergency) {
        
        // Check if overtime date is not in the past (unless emergency)
        if (!isEmergency && overtimeDate.isBefore(LocalDate.now())) {
            System.err.println("Cannot request overtime for past dates (except emergency)");
            return false;
        }
        
        // Check advance request requirement (unless emergency)
        if (!isEmergency) {
            LocalDateTime requestDeadline = overtimeDate.atTime(startTime).minusHours(ADVANCE_REQUEST_HOURS);
            if (LocalDateTime.now().isAfter(requestDeadline)) {
                System.err.println("Overtime must be requested at least " + ADVANCE_REQUEST_HOURS + " hours in advance");
                return false;
            }
        }
        
        // Check maximum daily overtime hours
        if (overtimeHours > MAX_DAILY_OVERTIME_HOURS) {
            System.err.println("Overtime hours cannot exceed " + MAX_DAILY_OVERTIME_HOURS + " hours per day");
            return false;
        }
        
        // Check for overlapping overtime requests
        if (hasOverlappingOvertime(employeeId, overtimeDate, startTime, endTime)) {
            System.err.println("Overlapping overtime request exists for this period");
            return false;
        }
        
        // Check weekly overtime limit
        if (!isEmergency && exceedsWeeklyOvertimeLimit(employeeId, overtimeDate, overtimeHours)) {
            System.err.println("This would exceed the weekly overtime limit of " + MAX_WEEKLY_OVERTIME_HOURS + " hours");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks for overlapping overtime requests
     */
    private boolean hasOverlappingOvertime(Integer employeeId, LocalDate overtimeDate, 
                                         LocalTime startTime, LocalTime endTime) {
        String sql = "SELECT COUNT(*) FROM overtime_request " +
                    "WHERE employeeId = ? AND overtimeDate = ? AND status IN (?, ?) " +
                    "AND ((startTime <= ? AND endTime >= ?) OR (startTime <= ? AND endTime >= ?))";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(overtimeDate));
            stmt.setString(3, STATUS_PENDING);
            stmt.setString(4, STATUS_APPROVED);
            stmt.setTime(5, Time.valueOf(startTime));
            stmt.setTime(6, Time.valueOf(startTime));
            stmt.setTime(7, Time.valueOf(endTime));
            stmt.setTime(8, Time.valueOf(endTime));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking overlapping overtime: " + e.getMessage());
            return true; // Err on the side of caution
        }
    }
    
    /**
     * Checks if weekly overtime limit would be exceeded
     */
    private boolean exceedsWeeklyOvertimeLimit(Integer employeeId, LocalDate overtimeDate, Double requestedHours) {
        // Get start of week (Monday)
        LocalDate weekStart = overtimeDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        String sql = "SELECT SUM(overtimeHours) as weeklyHours FROM overtime_request " +
                    "WHERE employeeId = ? AND overtimeDate BETWEEN ? AND ? AND status IN (?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(weekStart));
            stmt.setDate(3, java.sql.Date.valueOf(weekEnd));
            stmt.setString(4, STATUS_APPROVED);
            stmt.setString(5, STATUS_PENDING);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Double currentWeeklyHours = rs.getDouble("weeklyHours");
                    return (currentWeeklyHours + requestedHours) > MAX_WEEKLY_OVERTIME_HOURS;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking weekly overtime limit: " + e.getMessage());
            return true; // Err on the side of caution
        }
        
        return false;
    }
    
    // UTILITY METHODS
    
    /**
     * Creates overtime allowance record for approved overtime
     */
    private void createOvertimeAllowance(Integer overtimeRequestId, Integer employeeId, Double approvedHours) {
        // This could create additional allowance records for overtime work
        // For now, just log the action
        System.out.println("Overtime allowance created for request " + overtimeRequestId + 
                          " - " + approvedHours + " hours");
    }
    
    /**
     * Creates overtime notification (placeholder for notification system)
     */
    private void createOvertimeNotification(Integer overtimeRequestId, Integer recipientId, String notificationType) {
        // Placeholder for notification system
        System.out.println("Notification created: " + notificationType + 
                          " for overtime request " + overtimeRequestId + 
                          " to employee " + recipientId);
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
