package DAOs;

import Models.OvertimeRequestModel;
import Models.OvertimeRequestModel.ApprovalStatus;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object for OvertimeRequestModel entities.
 * This class handles all database operations related to overtime requests.
 * It extends BaseDAO to inherit common CRUD operations and adds overtime-specific methods.
 * @author User
 */
public class OvertimeRequestDAO extends BaseDAO<OvertimeRequestModel, Integer> {
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public OvertimeRequestDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into an OvertimeRequestModel object
     * This method reads each column from the ResultSet and creates an OvertimeRequestModel
     * @param rs The ResultSet containing overtime request data from the database
     * @return A fully populated OvertimeRequestModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected OvertimeRequestModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        OvertimeRequestModel overtime = new OvertimeRequestModel();
        
        // Set basic overtime request information
        overtime.setOvertimeRequestId(rs.getInt("overtimeRequestId"));
        overtime.setEmployeeId(rs.getInt("employeeId"));
        
        // Handle datetime fields
        Timestamp overtimeStart = rs.getTimestamp("overtimeStart");
        if (overtimeStart != null) {
            overtime.setOvertimeStart(overtimeStart.toLocalDateTime());
        }
        
        Timestamp overtimeEnd = rs.getTimestamp("overtimeEnd");
        if (overtimeEnd != null) {
            overtime.setOvertimeEnd(overtimeEnd.toLocalDateTime());
        }
        
        // Handle optional fields
        overtime.setOvertimeReason(rs.getString("overtimeReason"));
        
        // Handle enum for approval status
        String statusStr = rs.getString("approvalStatus");
        if (statusStr != null) {
            overtime.setApprovalStatus(ApprovalStatus.fromString(statusStr));
        }
        
        // Handle timestamp fields
        Timestamp dateCreated = rs.getTimestamp("dateCreated");
        if (dateCreated != null) {
            overtime.setDateCreated(dateCreated.toLocalDateTime());
        }
        
        Timestamp dateApproved = rs.getTimestamp("dateApproved");
        if (dateApproved != null) {
            overtime.setDateApproved(dateApproved.toLocalDateTime());
        }
        
        overtime.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        return overtime;
    }
    
    /**
     * Returns the database table name for overtime requests
     * @return "overtimerequest" - the name of the overtimerequest table in the database
     */
    @Override
    protected String getTableName() {
        return "overtimerequest";
    }
    
    /**
     * Returns the primary key column name for the overtimerequest table
     * @return "overtimeRequestId" - the primary key column name
     */
    @Override
    protected String getPrimaryKeyColumn() {
        return "overtimeRequestId";
    }
    
    /**
     * Sets parameters for INSERT operations when creating new overtime requests
     * This method maps OvertimeRequestModel object properties to SQL parameters
     * @param stmt The PreparedStatement to set parameters on
     * @param overtime The OvertimeRequestModel object to get values from
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setInsertParameters(PreparedStatement stmt, OvertimeRequestModel overtime) throws SQLException {
        // Note: overtimeRequestId is auto-increment, so we don't include it in INSERT
        // dateCreated has DEFAULT CURRENT_TIMESTAMP, so we can let database handle it
        int paramIndex = 1;
        
        // Set required fields
        stmt.setInt(paramIndex++, overtime.getEmployeeId());
        stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeStart()));
        stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeEnd()));
        
        // Handle optional fields
        if (overtime.getOvertimeReason() != null) {
            stmt.setString(paramIndex++, overtime.getOvertimeReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Handle enum properly
        if (overtime.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, overtime.getApprovalStatus().getValue());
        } else {
            stmt.setString(paramIndex++, ApprovalStatus.PENDING.getValue()); // Default value
        }
        
        // Handle optional timestamp fields
        if (overtime.getDateApproved() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getDateApproved()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, overtime.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
    }
    
    /**
     * Sets parameters for UPDATE operations when modifying existing overtime requests
     * This method maps OvertimeRequestModel object properties to SQL parameters for updates
     * @param stmt The PreparedStatement to set parameters on
     * @param overtime The OvertimeRequestModel object with updated values
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, OvertimeRequestModel overtime) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT (excluding auto-increment ID)
        stmt.setInt(paramIndex++, overtime.getEmployeeId());
        stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeStart()));
        stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeEnd()));
        
        if (overtime.getOvertimeReason() != null) {
            stmt.setString(paramIndex++, overtime.getOvertimeReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (overtime.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, overtime.getApprovalStatus().getValue());
        } else {
            stmt.setString(paramIndex++, ApprovalStatus.PENDING.getValue());
        }
        
        if (overtime.getDateApproved() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getDateApproved()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, overtime.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Finally, set the overtime request ID for the WHERE clause
        stmt.setInt(paramIndex++, overtime.getOvertimeRequestId());
    }
    
    /**
     * Gets the ID from an OvertimeRequestModel object
     * This is used by BaseDAO for update and delete operations
     * @param overtime The OvertimeRequestModel object to get ID from
     * @return The overtime request's ID
     */
    @Override
    protected Integer getEntityId(OvertimeRequestModel overtime) {
        return overtime.getOvertimeRequestId();
    }
    
    /**
     * Handles auto-generated overtime request IDs after INSERT operations
     * This method sets the generated overtimeRequestId back on the OvertimeRequestModel object
     * @param entity The OvertimeRequestModel that was just inserted
     * @param generatedKeys The ResultSet containing the generated overtimeRequestId
     * @throws SQLException if there's an error reading the generated key
     */
    @Override
    protected void handleGeneratedKey(OvertimeRequestModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setOvertimeRequestId(generatedKeys.getInt(1));
        }
    }
    

    // CUSTOM SQL BUILDERS

    
    /**
     * Builds the complete INSERT SQL statement for overtime requests
     * @return The complete INSERT SQL statement
     */
    private String buildInsertSQL() {
        return "INSERT INTO overtimerequest " +
               "(employeeId, overtimeStart, overtimeEnd, overtimeReason, " +
               "approvalStatus, dateApproved, supervisorNotes) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * Builds the complete UPDATE SQL statement for overtime requests
     * @return The complete UPDATE SQL statement
     */
    private String buildUpdateSQL() {
        return "UPDATE overtimerequest SET " +
               "employeeId = ?, overtimeStart = ?, overtimeEnd = ?, overtimeReason = ?, " +
               "approvalStatus = ?, dateApproved = ?, supervisorNotes = ? " +
               "WHERE overtimeRequestId = ?";
    }
    

    // CUSTOM OVERTIME REQUEST METHODS - As requested
 
    
    /**
     * Finds all pending overtime requests that need approval
     * @return List of pending overtime requests ordered by date created
     */
    public List<OvertimeRequestModel> findPendingOvertimeRequests() {
        String sql = "SELECT * FROM overtimerequest WHERE approvalStatus = ? ORDER BY dateCreated ASC";
        return executeQuery(sql, ApprovalStatus.PENDING.getValue());
    }
    
    /**
     * Finds all overtime requests for a specific employee
     * @param employeeId The employee ID
     * @return List of overtime requests for the employee ordered by date created (newest first)
     */
    public List<OvertimeRequestModel> findByEmployee(Integer employeeId) {
        String sql = "SELECT * FROM overtimerequest WHERE employeeId = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Approves an overtime request
     * @param overtimeRequestId The overtime request ID to approve
     * @param supervisorNotes Optional notes from supervisor
     * @return true if approval was successful
     */
    public boolean approveOvertime(Integer overtimeRequestId, String supervisorNotes) {
        String sql = "UPDATE overtimerequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE overtimeRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ApprovalStatus.APPROVED.getValue());
            stmt.setString(2, supervisorNotes);
            stmt.setInt(3, overtimeRequestId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error approving overtime request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Calculates overtime pay for a specific overtime request
     * This method retrieves the employee's hourly rate and calculates overtime pay
     * @param overtimeRequestId The overtime request ID
     * @param overtimeMultiplier The overtime pay multiplier (e.g., 1.5 for time-and-a-half)
     * @return The calculated overtime pay amount, or null if calculation fails
     */
    public BigDecimal calculateOvertimePay(Integer overtimeRequestId, BigDecimal overtimeMultiplier) {
        String sql = "SELECT o.overtimeStart, o.overtimeEnd, e.hourlyRate " +
                    "FROM overtimerequest o " +
                    "JOIN employee e ON o.employeeId = e.employeeId " +
                    "WHERE o.overtimeRequestId = ? AND o.approvalStatus = 'Approved'";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, overtimeRequestId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Get overtime period and hourly rate
                    Timestamp overtimeStart = rs.getTimestamp("overtimeStart");
                    Timestamp overtimeEnd = rs.getTimestamp("overtimeEnd");
                    BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
                    
                    if (overtimeStart != null && overtimeEnd != null && hourlyRate != null) {
                        // Calculate overtime hours
                        LocalDateTime start = overtimeStart.toLocalDateTime();
                        LocalDateTime end = overtimeEnd.toLocalDateTime();
                        
                        // Calculate duration in minutes and convert to hours
                        long minutes = java.time.Duration.between(start, end).toMinutes();
                        BigDecimal overtimeHours = new BigDecimal(minutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
                        
                        // Calculate overtime pay: hours × hourly rate × multiplier
                        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(overtimeMultiplier);
                        
                        return overtimePay.setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error calculating overtime pay: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null; // Return null if calculation fails
    }
    

    // ADDITIONAL CUSTOM METHODS

    
    /**
     * Finds overtime requests by approval status
     * @param status The approval status to search for
     * @return List of overtime requests with the specified status
     */
    public List<OvertimeRequestModel> findByStatus(ApprovalStatus status) {
        String sql = "SELECT * FROM overtimerequest WHERE approvalStatus = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, status.getValue());
    }
    
    /**
     * Rejects an overtime request
     * @param overtimeRequestId The overtime request ID to reject
     * @param supervisorNotes Required notes explaining rejection
     * @return true if rejection was successful
     */
    public boolean rejectOvertime(Integer overtimeRequestId, String supervisorNotes) {
        String sql = "UPDATE overtimerequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE overtimeRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ApprovalStatus.REJECTED.getValue());
            stmt.setString(2, supervisorNotes);
            stmt.setInt(3, overtimeRequestId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error rejecting overtime request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Finds overtime requests within a date range
     * @param startDate The start date/time
     * @param endDate The end date/time
     * @return List of overtime requests within the specified range
     */
    public List<OvertimeRequestModel> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM overtimerequest WHERE overtimeStart >= ? AND overtimeEnd <= ? ORDER BY overtimeStart";
        return executeQuery(sql, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }
    
    /**
     * Gets total overtime hours for an employee in a specific month
     * Only includes approved overtime requests
     * @param employeeId The employee ID
     * @param year The year
     * @param month The month (1-12)
     * @return Total overtime hours as BigDecimal
     */
    public BigDecimal getTotalOvertimeHours(Integer employeeId, int year, int month) {
        String sql = "SELECT SUM(TIMESTAMPDIFF(MINUTE, overtimeStart, overtimeEnd)) as totalMinutes " +
                    "FROM overtimerequest " +
                    "WHERE employeeId = ? AND approvalStatus = 'Approved' " +
                    "AND YEAR(overtimeStart) = ? AND MONTH(overtimeStart) = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
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
            System.err.println("Error calculating total overtime hours: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Gets total overtime pay for an employee in a specific month
     * @param employeeId The employee ID
     * @param year The year
     * @param month The month (1-12)
     * @param overtimeMultiplier The overtime pay multiplier (e.g., 1.5)
     * @return Total overtime pay as BigDecimal
     */
    public BigDecimal getTotalOvertimePay(Integer employeeId, int year, int month, BigDecimal overtimeMultiplier) {
        String sql = "SELECT o.overtimeStart, o.overtimeEnd, e.hourlyRate " +
                    "FROM overtimerequest o " +
                    "JOIN employee e ON o.employeeId = e.employeeId " +
                    "WHERE o.employeeId = ? AND o.approvalStatus = 'Approved' " +
                    "AND YEAR(o.overtimeStart) = ? AND MONTH(o.overtimeStart) = ?";
        
        BigDecimal totalPay = BigDecimal.ZERO;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp overtimeStart = rs.getTimestamp("overtimeStart");
                    Timestamp overtimeEnd = rs.getTimestamp("overtimeEnd");
                    BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
                    
                    if (overtimeStart != null && overtimeEnd != null && hourlyRate != null) {
                        // Calculate overtime hours for this request
                        long minutes = java.time.Duration.between(
                            overtimeStart.toLocalDateTime(),
                            overtimeEnd.toLocalDateTime()
                        ).toMinutes();
                        
                        BigDecimal overtimeHours = new BigDecimal(minutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
                        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(overtimeMultiplier);
                        
                        totalPay = totalPay.add(overtimePay);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating total overtime pay: " + e.getMessage());
            e.printStackTrace();
        }
        
        return totalPay.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    

    // OVERRIDE METHODS

    
    /**
     * Override the save method to use custom INSERT SQL
     * @param overtime The overtime request to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(OvertimeRequestModel overtime) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, overtime);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(overtime, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving overtime request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param overtime The overtime request to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(OvertimeRequestModel overtime) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, overtime);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating overtime request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}