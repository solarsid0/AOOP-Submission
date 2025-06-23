
package DAOs;

import Models.LeaveRequestModel;
import Models.LeaveRequestModel.ApprovalStatus;
import java.sql.*;
import java.util.List;


/**
 *
 * @author chadley
 */


public class LeaveRequestDAO extends BaseDAO<LeaveRequestModel, Integer> {
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public LeaveRequestDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into a LeaveRequestModel object
     * This method reads each column from the ResultSet and creates a LeaveRequestModel
     * @param rs The ResultSet containing leave request data from the database
     * @return A fully populated LeaveRequestModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected LeaveRequestModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        LeaveRequestModel leave = new LeaveRequestModel();
        
        leave.setLeaveRequestId(rs.getInt("leaveRequestId"));
        leave.setEmployeeId(rs.getInt("employeeId"));
        leave.setLeaveTypeId(rs.getInt("leaveTypeId"));
        
        // Handle date fields
        Date leaveStart = rs.getDate("leaveStart");
        if (leaveStart != null) {
            leave.setLeaveStart(leaveStart.toLocalDate());
        }
        
        Date leaveEnd = rs.getDate("leaveEnd");
        if (leaveEnd != null) {
            leave.setLeaveEnd(leaveEnd.toLocalDate());
        }
        
        leave.setLeaveReason(rs.getString("leaveReason"));
        
        // Handle enum for approval status
        String statusStr = rs.getString("approvalStatus");
        if (statusStr != null) {
            leave.setApprovalStatus(ApprovalStatus.fromString(statusStr));
        }
        
        // Handle timestamp fields
        Timestamp dateCreated = rs.getTimestamp("dateCreated");
        if (dateCreated != null) {
            leave.setDateCreated(dateCreated.toLocalDateTime());
        }
        
        Timestamp dateApproved = rs.getTimestamp("dateApproved");
        if (dateApproved != null) {
            leave.setDateApproved(dateApproved.toLocalDateTime());
        }
        
        leave.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        return leave;
    }
    
    @Override
    protected String getTableName() {
        return "leaverequest";
    }
    
    @Override
    protected String getPrimaryKeyColumn() {
        return "leaveRequestId";
    }
    
    @Override
    protected void setInsertParameters(PreparedStatement stmt, LeaveRequestModel leave) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, leave.getEmployeeId());
        stmt.setInt(paramIndex++, leave.getLeaveTypeId());
        stmt.setDate(paramIndex++, Date.valueOf(leave.getLeaveStart()));
        stmt.setDate(paramIndex++, Date.valueOf(leave.getLeaveEnd()));
        
        if (leave.getLeaveReason() != null) {
            stmt.setString(paramIndex++, leave.getLeaveReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (leave.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, leave.getApprovalStatus().getValue());
        } else {
            stmt.setString(paramIndex++, ApprovalStatus.PENDING.getValue());
        }
        
        if (leave.getDateApproved() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(leave.getDateApproved()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (leave.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, leave.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
    }
    
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, LeaveRequestModel leave) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, leave.getEmployeeId());
        stmt.setInt(paramIndex++, leave.getLeaveTypeId());
        stmt.setDate(paramIndex++, Date.valueOf(leave.getLeaveStart()));
        stmt.setDate(paramIndex++, Date.valueOf(leave.getLeaveEnd()));
        
        if (leave.getLeaveReason() != null) {
            stmt.setString(paramIndex++, leave.getLeaveReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (leave.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, leave.getApprovalStatus().getValue());
        } else {
            stmt.setString(paramIndex++, ApprovalStatus.PENDING.getValue());
        }
        
        if (leave.getDateApproved() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(leave.getDateApproved()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (leave.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, leave.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Set the ID for WHERE clause
        stmt.setInt(paramIndex++, leave.getLeaveRequestId());
    }
    
    @Override
    protected Integer getEntityId(LeaveRequestModel leave) {
        return leave.getLeaveRequestId();
    }
    
    @Override
    protected void handleGeneratedKey(LeaveRequestModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setLeaveRequestId(generatedKeys.getInt(1));
        }
    }
    

    // CUSTOM LEAVE REQUEST METHODS

    
    /**
     * Finds all leave requests for a specific employee
     * @param employeeId The employee ID
     * @return List of leave requests for the employee
     */
    public List<LeaveRequestModel> findByEmployeeId(Integer employeeId) {
        String sql = "SELECT * FROM leaverequest WHERE employeeId = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Finds leave requests by approval status
     * @param status The approval status to search for
     * @return List of leave requests with the specified status
     */
    public List<LeaveRequestModel> findByStatus(ApprovalStatus status) {
        String sql = "SELECT * FROM leaverequest WHERE approvalStatus = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, status.getValue());
    }
    
    /**
     * Finds pending leave requests that need approval
     * @return List of pending leave requests
     */
    public List<LeaveRequestModel> findPendingRequests() {
        return findByStatus(ApprovalStatus.PENDING);
    }
    
    /**
     * Finds leave requests by supervisor (for team management)
     * @param supervisorId The supervisor's employee ID
     * @return List of leave requests for the supervisor's team
     */
    public List<LeaveRequestModel> findBySupervisor(Integer supervisorId) {
        String sql = "SELECT lr.* FROM leaverequest lr " +
                    "INNER JOIN employee e ON lr.employeeId = e.employeeId " +
                    "WHERE e.immediatesupervisorid = ? " +
                    "ORDER BY lr.dateCreated DESC";
        return executeQuery(sql, supervisorId);
    }
    
    /**
     * Finds leave requests for employees in a specific department
     * @param department The department name
     * @return List of leave requests for employees in the department
     */
    public List<LeaveRequestModel> findByDepartment(String department) {
        String sql = "SELECT lr.* FROM leaverequest lr " +
                    "INNER JOIN employee e ON lr.employeeId = e.employeeId " +
                    "WHERE e.department = ? " +
                    "ORDER BY lr.dateCreated DESC";
        return executeQuery(sql, department);
    }
    
    /**
     * Approves a leave request
     * @param leaveRequestId The leave request ID
     * @param supervisorNotes Optional notes from supervisor
     * @return true if approval was successful
     */
    public boolean approveLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        String sql = "UPDATE leaverequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE leaveRequestId = ?";
        int rowsAffected = executeUpdate(sql, ApprovalStatus.APPROVED.getValue(), supervisorNotes, leaveRequestId);
        return rowsAffected > 0;
    }
    
    /**
     * Rejects a leave request
     * @param leaveRequestId The leave request ID
     * @param supervisorNotes Required notes explaining rejection
     * @return true if rejection was successful
     */
    public boolean rejectLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        String sql = "UPDATE leaverequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE leaveRequestId = ?";
        int rowsAffected = executeUpdate(sql, ApprovalStatus.REJECTED.getValue(), supervisorNotes, leaveRequestId);
        return rowsAffected > 0;
    }
    
    /**
     * Gets leave requests for a specific date range
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of leave requests within the date range
     */
    public List<LeaveRequestModel> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        String sql = "SELECT * FROM leaverequest WHERE leaveStart >= ? AND leaveEnd <= ? ORDER BY leaveStart";
        return executeQuery(sql, Date.valueOf(startDate), Date.valueOf(endDate));
    }
    
    /**
     * Checks if an employee has conflicting leave requests
     * @param employeeId The employee ID
     * @param startDate Start date to check
     * @param endDate End date to check
     * @param excludeRequestId Optional request ID to exclude from check (for updates)
     * @return true if there are conflicting requests
     */
    public boolean hasConflictingLeaveRequests(Integer employeeId, java.time.LocalDate startDate, 
            java.time.LocalDate endDate, Integer excludeRequestId) {
        
        String sql = "SELECT COUNT(*) FROM leaverequest WHERE employeeId = ? " +
                    "AND approvalStatus IN ('PENDING', 'APPROVED') " +
                    "AND ((leaveStart <= ? AND leaveEnd >= ?) OR " +
                    "     (leaveStart <= ? AND leaveEnd >= ?) OR " +
                    "     (leaveStart >= ? AND leaveEnd <= ?))";
        
        if (excludeRequestId != null) {
            sql += " AND leaveRequestId != ?";
        }

        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            stmt.setInt(paramIndex++, employeeId);
            stmt.setDate(paramIndex++, Date.valueOf(endDate));   // leaveStart <= endDate
            stmt.setDate(paramIndex++, Date.valueOf(startDate)); // leaveEnd >= startDate
            stmt.setDate(paramIndex++, Date.valueOf(startDate)); // leaveStart <= startDate
            stmt.setDate(paramIndex++, Date.valueOf(startDate)); // leaveEnd >= startDate
            stmt.setDate(paramIndex++, Date.valueOf(startDate)); // leaveStart >= startDate
            stmt.setDate(paramIndex++, Date.valueOf(endDate));   // leaveEnd <= endDate
            
            if (excludeRequestId != null) {
                stmt.setInt(paramIndex++, excludeRequestId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking conflicting leave requests: " + e.getMessage());
        }
        
        return false;
    }

    // OVERRIDE METHODS

    
    /**
     * Override the save method to use custom INSERT SQL
     * @param leave The leave request to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(LeaveRequestModel leave) {
        String sql = "INSERT INTO leaverequest " +
                    "(employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
                    "approvalStatus, dateApproved, supervisorNotes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, leave);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(leave, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving leave request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param leave The leave request to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(LeaveRequestModel leave) {
        String sql = "UPDATE leaverequest SET " +
                    "employeeId = ?, leaveTypeId = ?, leaveStart = ?, leaveEnd = ?, " +
                    "leaveReason = ?, approvalStatus = ?, dateApproved = ?, supervisorNotes = ? " +
                    "WHERE leaveRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, leave);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
