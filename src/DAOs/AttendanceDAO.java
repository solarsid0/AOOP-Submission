package DAOs;

import Models.AttendanceModel;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Data Access Object for AttendanceModel entities.
 * This class handles all database operations related to attendance records.
 * It extends BaseDAO to inherit common CRUD operations and adds attendance-specific methods.
 * @author User
 */
public class AttendanceDAO extends BaseDAO<AttendanceModel, Integer> {
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public AttendanceDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into an AttendanceModel object
     * This method reads each column from the ResultSet and creates an AttendanceModel
     * @param rs The ResultSet containing attendance data from the database
     * @return A fully populated AttendanceModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected AttendanceModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        AttendanceModel attendance = new AttendanceModel();
        
        // Set basic attendance information
        attendance.setAttendanceId(rs.getInt("attendanceId"));
        attendance.setEmployeeId(rs.getInt("employeeId"));
        
        // Handle date (required field)
        Date date = rs.getDate("date");
        if (date != null) {
            attendance.setDate(date.toLocalDate());
        }
        
        // Handle time fields (could be null)
        Time timeIn = rs.getTime("timeIn");
        if (timeIn != null) {
            attendance.setTimeIn(timeIn.toLocalTime());
        }
        
        Time timeOut = rs.getTime("timeOut");
        if (timeOut != null) {
            attendance.setTimeOut(timeOut.toLocalTime());
        }
        
        return attendance;
    }
    
    /**
     * Returns the database table name for attendance
     * @return "attendance" - the name of the attendance table in the database
     */
    @Override
    protected String getTableName() {
        return "attendance";
    }
    
    /**
     * Returns the primary key column name for the attendance table
     * @return "attendanceId" - the primary key column name
     */
    @Override
    protected String getPrimaryKeyColumn() {
        return "attendanceId";
    }
    
    /**
     * Sets parameters for INSERT operations when creating new attendance records
     * This method maps AttendanceModel object properties to SQL parameters
     * @param stmt The PreparedStatement to set parameters on
     * @param attendance The AttendanceModel object to get values from
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setInsertParameters(PreparedStatement stmt, AttendanceModel attendance) throws SQLException {
        // Note: attendanceId is auto-increment, so we don't include it in INSERT
        int paramIndex = 1;
        
        // Set required fields
        stmt.setDate(paramIndex++, Date.valueOf(attendance.getDate()));
        stmt.setInt(paramIndex++, attendance.getEmployeeId());
        
        // Handle time fields (could be null)
        if (attendance.getTimeIn() != null) {
            stmt.setTime(paramIndex++, Time.valueOf(attendance.getTimeIn()));
        } else {
            stmt.setNull(paramIndex++, Types.TIME);
        }
        
        if (attendance.getTimeOut() != null) {
            stmt.setTime(paramIndex++, Time.valueOf(attendance.getTimeOut()));
        } else {
            stmt.setNull(paramIndex++, Types.TIME);
        }
    }
    
    /**
     * Sets parameters for UPDATE operations when modifying existing attendance records
     * This method maps AttendanceModel object properties to SQL parameters for updates
     * @param stmt The PreparedStatement to set parameters on
     * @param attendance The AttendanceModel object with updated values
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, AttendanceModel attendance) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT
        stmt.setDate(paramIndex++, Date.valueOf(attendance.getDate()));
        stmt.setInt(paramIndex++, attendance.getEmployeeId());
        
        if (attendance.getTimeIn() != null) {
            stmt.setTime(paramIndex++, Time.valueOf(attendance.getTimeIn()));
        } else {
            stmt.setNull(paramIndex++, Types.TIME);
        }
        
        if (attendance.getTimeOut() != null) {
            stmt.setTime(paramIndex++, Time.valueOf(attendance.getTimeOut()));
        } else {
            stmt.setNull(paramIndex++, Types.TIME);
        }
        
        // Finally, set the attendance ID for the WHERE clause
        stmt.setInt(paramIndex++, attendance.getAttendanceId());
    }
    
    /**
     * Gets the ID from an AttendanceModel object
     * This is used by BaseDAO for update and delete operations
     * @param attendance The AttendanceModel object to get ID from
     * @return The attendance record's ID
     */
    @Override
    protected Integer getEntityId(AttendanceModel attendance) {
        return attendance.getAttendanceId();
    }
    
    /**
     * Handles auto-generated attendance IDs after INSERT operations
     * This method sets the generated attendanceId back on the AttendanceModel object
     * @param entity The AttendanceModel that was just inserted
     * @param generatedKeys The ResultSet containing the generated attendanceId
     * @throws SQLException if there's an error reading the generated key
     */
    @Override
    protected void handleGeneratedKey(AttendanceModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setAttendanceId(generatedKeys.getInt(1));
        }
    }
    

    // CUSTOM SQL BUILDERS - Override BaseDAO methods with specific SQL

    
    /**
     * Builds the complete INSERT SQL statement for attendance
     * This creates the specific SQL for inserting attendance records
     * @return The complete INSERT SQL statement
     */
    private String buildInsertSQL() {
        return "INSERT INTO attendance (date, employeeId, timeIn, timeOut) VALUES (?, ?, ?, ?)";
    }
    
    /**
     * Builds the complete UPDATE SQL statement for attendance
     * This creates the specific SQL for updating attendance records
     * @return The complete UPDATE SQL statement
     */
    private String buildUpdateSQL() {
        return "UPDATE attendance SET date = ?, employeeId = ?, timeIn = ?, timeOut = ? WHERE attendanceId = ?";
    }
    

    // CUSTOM ATTENDANCE METHODS - Attendance-specific database operations

    
    /**
     * Finds an attendance record by employee ID and date
     * This is useful for checking if an employee has already marked attendance for a specific date
     * @param employeeId The employee ID to search for
     * @param date The date to search for
     * @return The AttendanceModel if found, null if not found
     */
    public AttendanceModel findByEmployeeAndDate(Integer employeeId, LocalDate date) {
        String sql = "SELECT * FROM attendance WHERE employeeId = ? AND date = ?";
        return executeSingleQuery(sql, employeeId, Date.valueOf(date));
    }
    
    /**
     * Calculates the total hours worked by an employee in a specific month
     * This method considers only records with both timeIn and timeOut
     * @param employeeId The employee ID
     * @param yearMonth The year and month to calculate hours for
     * @return The total hours worked as BigDecimal
     */
    public BigDecimal calculateMonthlyHours(Integer employeeId, YearMonth yearMonth) {
        String sql = "SELECT SUM(TIMESTAMPDIFF(MINUTE, timeIn, timeOut)) as totalMinutes " +
                    "FROM attendance " +
                    "WHERE employeeId = ? AND YEAR(date) = ? AND MONTH(date) = ? " +
                    "AND timeIn IS NOT NULL AND timeOut IS NOT NULL";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, yearMonth.getYear());
            stmt.setInt(3, yearMonth.getMonthValue());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long totalMinutes = rs.getLong("totalMinutes");
                    if (rs.wasNull()) {
                        return BigDecimal.ZERO;
                    }
                    // Convert minutes to hours
                    return new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating monthly hours: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Gets the attendance history for an employee within a date range
     * This is useful for generating attendance reports
     * @param employeeId The employee ID
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @return List of attendance records within the date range
     */
    public List<AttendanceModel> getAttendanceHistory(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM attendance WHERE employeeId = ? AND date BETWEEN ? AND ? ORDER BY date DESC";
        return executeQuery(sql, employeeId, Date.valueOf(startDate), Date.valueOf(endDate));
    }
    
    /**
     * Marks attendance for an employee (time in or time out)
     * This method handles both time in and time out operations
     * @param employeeId The employee ID
     * @param date The date of attendance
     * @param timeIn The time in (null if marking time out only)
     * @param timeOut The time out (null if marking time in only)
     * @return true if attendance was successfully marked, false otherwise
     */
    public boolean markAttendance(Integer employeeId, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        // First, check if attendance record already exists for this employee and date
        AttendanceModel existingRecord = findByEmployeeAndDate(employeeId, date);
        
        if (existingRecord != null) {
            // Update existing record
            if (timeIn != null) {
                existingRecord.setTimeIn(timeIn);
            }
            if (timeOut != null) {
                existingRecord.setTimeOut(timeOut);
            }
            return update(existingRecord);
        } else {
            // Create new record
            AttendanceModel newRecord = new AttendanceModel();
            newRecord.setEmployeeId(employeeId);
            newRecord.setDate(date);
            newRecord.setTimeIn(timeIn);
            newRecord.setTimeOut(timeOut);
            return save(newRecord);
        }
    }
    
    /**
     * Marks time in for an employee
     * Convenience method for marking time in only
     * @param employeeId The employee ID
     * @param date The date
     * @param timeIn The time in
     * @return true if successful, false otherwise
     */
    public boolean markTimeIn(Integer employeeId, LocalDate date, LocalTime timeIn) {
        return markAttendance(employeeId, date, timeIn, null);
    }
    
    /**
     * Marks time out for an employee
     * Convenience method for marking time out only
     * @param employeeId The employee ID
     * @param date The date
     * @param timeOut The time out
     * @return true if successful, false otherwise
     */
    public boolean markTimeOut(Integer employeeId, LocalDate date, LocalTime timeOut) {
        return markAttendance(employeeId, date, null, timeOut);
    }
    
    /**
     * Gets all attendance records for a specific employee
     * This is useful for employee-specific reports
     * @param employeeId The employee ID
     * @return List of all attendance records for the employee
     */
    public List<AttendanceModel> getAttendanceByEmployee(Integer employeeId) {
        String sql = "SELECT * FROM attendance WHERE employeeId = ? ORDER BY date DESC";
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Gets all attendance records for a specific date
     * This is useful for daily attendance reports
     * @param date The date to search for
     * @return List of attendance records for the specified date
     */
    public List<AttendanceModel> getAttendanceByDate(LocalDate date) {
        String sql = "SELECT * FROM attendance WHERE date = ? ORDER BY employeeId";
        return executeQuery(sql, Date.valueOf(date));
    }
    
    /**
     * Gets attendance records for employees who haven't marked time out
     * This is useful for finding employees who are still "clocked in"
     * @param date The date to check (optional, uses current date if null)
     * @return List of attendance records without time out
     */
    public List<AttendanceModel> getIncompleteAttendance(LocalDate date) {
        LocalDate searchDate = date != null ? date : LocalDate.now();
        String sql = "SELECT * FROM attendance WHERE date = ? AND timeIn IS NOT NULL AND timeOut IS NULL";
        return executeQuery(sql, Date.valueOf(searchDate));
    }
    
    /**
     * Gets attendance records for employees who are late
     * This compares time in with a standard work start time
     * @param date The date to check
     * @param standardStartTime The standard work start time (e.g., 8:00 AM)
     * @return List of attendance records where time in is after standard start time
     */
    public List<AttendanceModel> getLateAttendance(LocalDate date, LocalTime standardStartTime) {
        String sql = "SELECT * FROM attendance WHERE date = ? AND timeIn > ?";
        return executeQuery(sql, Date.valueOf(date), Time.valueOf(standardStartTime));
    }
    
    /**
     * Gets attendance statistics for an employee in a specific month
     * This returns various statistics like total days worked, total hours, etc.
     * @param employeeId The employee ID
     * @param yearMonth The year and month
     * @return AttendanceStatistics object containing various metrics
     */
    public AttendanceStatistics getMonthlyAttendanceStatistics(Integer employeeId, YearMonth yearMonth) {
        String sql = "SELECT " +
                    "COUNT(*) as totalDays, " +
                    "COUNT(CASE WHEN timeIn IS NOT NULL AND timeOut IS NOT NULL THEN 1 END) as completeDays, " +
                    "COUNT(CASE WHEN timeIn IS NOT NULL AND timeOut IS NULL THEN 1 END) as incompleteDays, " +
                    "SUM(CASE WHEN timeIn IS NOT NULL AND timeOut IS NOT NULL " +
                    "THEN TIMESTAMPDIFF(MINUTE, timeIn, timeOut) ELSE 0 END) as totalMinutes " +
                    "FROM attendance " +
                    "WHERE employeeId = ? AND YEAR(date) = ? AND MONTH(date) = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, yearMonth.getYear());
            stmt.setInt(3, yearMonth.getMonthValue());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AttendanceStatistics stats = new AttendanceStatistics();
                    stats.setTotalDays(rs.getInt("totalDays"));
                    stats.setCompleteDays(rs.getInt("completeDays"));
                    stats.setIncompleteDays(rs.getInt("incompleteDays"));
                    
                    long totalMinutes = rs.getLong("totalMinutes");
                    if (!rs.wasNull()) {
                        stats.setTotalHours(new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP));
                    } else {
                        stats.setTotalHours(BigDecimal.ZERO);
                    }
                    
                    return stats;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting attendance statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new AttendanceStatistics(); // Return empty statistics if error occurs
    }
    
    /**
     * Deletes attendance records older than a specified number of days
     * This is useful for data cleanup and archiving
     * @param daysToKeep Number of days to keep (records older than this will be deleted)
     * @return Number of records deleted
     */
    public int deleteOldAttendanceRecords(int daysToKeep) {
        String sql = "DELETE FROM attendance WHERE date < DATE_SUB(CURRENT_DATE, INTERVAL ? DAY)";
        return executeUpdate(sql, daysToKeep);
    }
    
    /**
     * Checks if an employee has already marked time in for today
     * @param employeeId The employee ID
     * @return true if time in is already marked for today, false otherwise
     */
    public boolean hasMarkedTimeInToday(Integer employeeId) {
        AttendanceModel todayRecord = findByEmployeeAndDate(employeeId, LocalDate.now());
        return todayRecord != null && todayRecord.getTimeIn() != null;
    }
    
    /**
     * Checks if an employee has already marked time out for today
     * @param employeeId The employee ID
     * @return true if time out is already marked for today, false otherwise
     */
    public boolean hasMarkedTimeOutToday(Integer employeeId) {
        AttendanceModel todayRecord = findByEmployeeAndDate(employeeId, LocalDate.now());
        return todayRecord != null && todayRecord.getTimeOut() != null;
    }
    

    // OVERRIDE METHODS - Use custom SQL instead of BaseDAO defaults

    
    /**
     * Override the save method to use custom INSERT SQL
     * @param attendance The attendance record to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(AttendanceModel attendance) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, attendance);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(attendance, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param attendance The attendance record to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(AttendanceModel attendance) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, attendance);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    

    // INNER CLASS - For attendance statistics

    
    /**
     * Inner class to hold attendance statistics
     */
    public static class AttendanceStatistics {
        private int totalDays;
        private int completeDays;
        private int incompleteDays;
        private BigDecimal totalHours;
        
        public AttendanceStatistics() {
            this.totalHours = BigDecimal.ZERO;
        }
        
        // Getters and setters
        public int getTotalDays() { return totalDays; }
        public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
        
        public int getCompleteDays() { return completeDays; }
        public void setCompleteDays(int completeDays) { this.completeDays = completeDays; }
        
        public int getIncompleteDays() { return incompleteDays; }
        public void setIncompleteDays(int incompleteDays) { this.incompleteDays = incompleteDays; }
        
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
        
        @Override
        public String toString() {
            return String.format("AttendanceStatistics{totalDays=%d, completeDays=%d, incompleteDays=%d, totalHours=%s}",
                    totalDays, completeDays, incompleteDays, totalHours);
        }
    }
}