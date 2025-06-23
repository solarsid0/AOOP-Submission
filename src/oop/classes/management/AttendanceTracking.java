
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
 * AttendanceTracking handles employee time tracking operations
 * Manages clock in/out, attendance records, late tracking, and attendance reports
 * Integrates with your existing DAO structure
 * @author Chadley
 */
public class AttendanceTracking {
    
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    
    // Standard work schedule constants
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0); // 8:00 AM
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0);   // 5:00 PM
    private static final int STANDARD_WORK_HOURS = 8;
    private static final int GRACE_PERIOD_MINUTES = 15; // 15 minutes grace for late arrival
    
    /**
     * Constructor
     */
    public AttendanceTracking(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
    }
    
    /**
     * Records employee clock-in
     */
    public boolean clockIn(Integer employeeId, LocalDateTime clockInTime, String location) {
        try {
            // Validate employee exists and is active
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                System.err.println("Employee not found: " + employeeId);
                return false;
            }
            
            if (!"Active".equals(employee.getStatus().getValue())) {
                System.err.println("Employee is not active: " + employeeId);
                return false;
            }
            
            // Use current time if not specified
            if (clockInTime == null) {
                clockInTime = LocalDateTime.now();
            }
            
            LocalDate attendanceDate = clockInTime.toLocalDate();
            
            // Check if already clocked in today
            if (hasActiveAttendance(employeeId, attendanceDate)) {
                System.err.println("Employee already clocked in today: " + employeeId);
                return false;
            }
            
            // Calculate if late
            boolean isLate = isClockInLate(clockInTime.toLocalTime());
            int lateMinutes = calculateLateMinutes(clockInTime.toLocalTime());
            
            // Insert attendance record
            String sql = "INSERT INTO attendance " +
                        "(employeeId, attendanceDate, timeIn, clockInLocation, isLate, lateMinutes, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'Present')";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, employeeId);
                stmt.setDate(2, java.sql.Date.valueOf(attendanceDate));
                stmt.setTime(3, Time.valueOf(clockInTime.toLocalTime()));
                stmt.setString(4, location);
                stmt.setBoolean(5, isLate);
                stmt.setInt(6, lateMinutes);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Clock-in successful for employee " + employeeId + 
                                     " at " + clockInTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    if (isLate) {
                        System.out.println("Note: Employee is " + lateMinutes + " minutes late");
                    }
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error recording clock-in: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Records employee clock-out
     */
    public boolean clockOut(Integer employeeId, LocalDateTime clockOutTime, String location) {
        try {
            // Use current time if not specified
            if (clockOutTime == null) {
                clockOutTime = LocalDateTime.now();
            }
            
            LocalDate attendanceDate = clockOutTime.toLocalDate();
            
            // Find today's attendance record
            Map<String, Object> attendanceRecord = getTodaysAttendance(employeeId, attendanceDate);
            if (attendanceRecord == null) {
                System.err.println("No clock-in record found for employee " + employeeId + " today");
                return false;
            }
            
            // Check if already clocked out
            if (attendanceRecord.get("timeOut") != null) {
                System.err.println("Employee already clocked out today: " + employeeId);
                return false;
            }
            
            // Get clock-in time to calculate hours worked
            Time timeIn = (Time) attendanceRecord.get("timeIn");
            LocalTime clockInTime = timeIn.toLocalTime();
            
            // Calculate hours worked
            Duration workDuration = Duration.between(clockInTime, clockOutTime.toLocalTime());
            double hoursWorked = workDuration.toMinutes() / 60.0;
            
            // Check for early departure
            boolean isEarlyDeparture = isClockOutEarly(clockOutTime.toLocalTime());
            int earlyMinutes = calculateEarlyMinutes(clockOutTime.toLocalTime());
            
            // Calculate overtime
            double overtimeHours = Math.max(0, hoursWorked - STANDARD_WORK_HOURS);
            
            // Update attendance record
            String sql = "UPDATE attendance " +
                        "SET timeOut = ?, clockOutLocation = ?, hoursWorked = ?, " +
                        "isEarlyDeparture = ?, earlyMinutes = ?, overtimeHours = ? " +
                        "WHERE employeeId = ? AND attendanceDate = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setTime(1, Time.valueOf(clockOutTime.toLocalTime()));
                stmt.setString(2, location);
                stmt.setDouble(3, hoursWorked);
                stmt.setBoolean(4, isEarlyDeparture);
                stmt.setInt(5, earlyMinutes);
                stmt.setDouble(6, overtimeHours);
                stmt.setInt(7, employeeId);
                stmt.setDate(8, java.sql.Date.valueOf(attendanceDate));
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Clock-out successful for employee " + employeeId + 
                                     " at " + clockOutTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    System.out.println("Hours worked: " + String.format("%.2f", hoursWorked));
                    
                    if (overtimeHours > 0) {
                        System.out.println("Overtime hours: " + String.format("%.2f", overtimeHours));
                    }
                    
                    if (isEarlyDeparture) {
                        System.out.println("Note: Early departure - " + earlyMinutes + " minutes early");
                    }
                    
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error recording clock-out: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Gets attendance record for today
     */
    public Map<String, Object> getTodaysAttendance(Integer employeeId) {
        return getTodaysAttendance(employeeId, LocalDate.now());
    }
    
    /**
     * Gets attendance record for specific date
     */
    public Map<String, Object> getTodaysAttendance(Integer employeeId, LocalDate date) {
        String sql = "SELECT a.*, e.firstName, e.lastName " +
                    "FROM attendance a " +
                    "JOIN employee e ON a.employeeId = e.employeeId " +
                    "WHERE a.employeeId = ? AND a.attendanceDate = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            
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
            System.err.println("Error getting attendance record: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gets attendance records for an employee within date range
     */
    public List<Map<String, Object>> getAttendanceHistory(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT a.*, e.firstName, e.lastName " +
                    "FROM attendance a " +
                    "JOIN employee e ON a.employeeId = e.employeeId " +
                    "WHERE a.employeeId = ? AND a.attendanceDate BETWEEN ? AND ? " +
                    "ORDER BY a.attendanceDate DESC";
        
        List<Map<String, Object>> records = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(startDate));
            stmt.setDate(3, java.sql.Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        record.put(columnName, value);
                    }
                    
                    records.add(record);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance history: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * Records absence for an employee
     */
    public boolean recordAbsence(Integer employeeId, LocalDate absenceDate, String reason) {
        String sql = "INSERT INTO attendance " +
                    "(employeeId, attendanceDate, status, notes) " +
                    "VALUES (?, ?, 'Absent', ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(absenceDate));
            stmt.setString(3, reason);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Absence recorded for employee " + employeeId + 
                                 " on " + absenceDate + " - " + reason);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error recording absence: " + e.getMessage());
        }
        
        return false;
    }
    
    // UTILITY METHODS
    
    /**
     * Checks if employee has active attendance for the date
     */
    private boolean hasActiveAttendance(Integer employeeId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employeeId = ? AND attendanceDate = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking active attendance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Checks if clock-in time is late
     */
    private boolean isClockInLate(LocalTime clockInTime) {
        LocalTime lateThreshold = STANDARD_START_TIME.plusMinutes(GRACE_PERIOD_MINUTES);
        return clockInTime.isAfter(lateThreshold);
    }
    
    /**
     * Calculates late minutes
     */
    private int calculateLateMinutes(LocalTime clockInTime) {
        if (!isClockInLate(clockInTime)) {
            return 0;
        }
        return (int) Duration.between(STANDARD_START_TIME, clockInTime).toMinutes();
    }
    
    /**
     * Checks if clock-out time is early
     */
    private boolean isClockOutEarly(LocalTime clockOutTime) {
        return clockOutTime.isBefore(STANDARD_END_TIME);
    }
    
    /**
     * Calculates early departure minutes
     */
    private int calculateEarlyMinutes(LocalTime clockOutTime) {
        if (!isClockOutEarly(clockOutTime)) {
            return 0;
        }
        return (int) Duration.between(clockOutTime, STANDARD_END_TIME).toMinutes();
    }
    
    /**
     * Gets current attendance status for all active employees
     */
    public List<Map<String, Object>> getCurrentAttendanceStatus() {
        String sql = "SELECT e.employeeId, e.firstName, e.lastName, " +
                    "a.attendanceDate, a.timeIn, a.timeOut, a.status " +
                    "FROM employee e " +
                    "LEFT JOIN attendance a ON e.employeeId = a.employeeId AND a.attendanceDate = CURDATE() " +
                    "WHERE e.status = 'Active' " +
                    "ORDER BY e.lastName, e.firstName";
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("employeeId", rs.getInt("employeeId"));
                    record.put("firstName", rs.getString("firstName"));
                    record.put("lastName", rs.getString("lastName"));
                    record.put("attendanceDate", rs.getDate("attendanceDate"));
                    record.put("timeIn", rs.getTime("timeIn"));
                    record.put("timeOut", rs.getTime("timeOut"));
                    record.put("status", rs.getString("status"));
                    
                    // Add derived status
                    Time timeIn = rs.getTime("timeIn");
                    Time timeOut = rs.getTime("timeOut");
                    
                    if (timeIn == null) {
                        record.put("currentStatus", "Not Clocked In");
                    } else if (timeOut == null) {
                        record.put("currentStatus", "Clocked In");
                    } else {
                        record.put("currentStatus", "Clocked Out");
                    }
                    
                    results.add(record);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting current attendance status: " + e.getMessage());
        }
        
        return results;
    }
}