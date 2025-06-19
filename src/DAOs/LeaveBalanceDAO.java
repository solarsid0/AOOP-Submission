package DAOs;

import Models.LeaveBalance;
import java.sql.*;
import java.util.List;

/**
 * Data Access Object for LeaveBalance entities.
 * This class handles all database operations related to employee leave balances.
 * It extends BaseDAO to inherit common CRUD operations and adds leave balance-specific methods.
 * @author User
 */
public class LeaveBalanceDAO extends BaseDAO<LeaveBalance, Integer> {
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public LeaveBalanceDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into a LeaveBalance object
     * @param rs The ResultSet containing leave balance data from the database
     * @return A fully populated LeaveBalance object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected LeaveBalance mapResultSetToEntity(ResultSet rs) throws SQLException {
        LeaveBalance leaveBalance = new LeaveBalance();
        
        leaveBalance.setLeaveBalanceId(rs.getInt("leaveBalanceId"));
        leaveBalance.setEmployeeId(rs.getInt("employeeId"));
        leaveBalance.setLeaveTypeId(rs.getInt("leaveTypeId"));
        leaveBalance.setTotalLeaveDays(rs.getInt("totalLeaveDays"));
        leaveBalance.setUsedLeaveDays(rs.getInt("usedLeaveDays"));
        leaveBalance.setRemainingLeaveDays(rs.getInt("remainingLeaveDays"));
        leaveBalance.setCarryOverDays(rs.getInt("carryOverDays"));
        leaveBalance.setBalanceYear(rs.getInt("balanceYear"));
        
        Timestamp lastUpdated = rs.getTimestamp("lastUpdated");
        if (lastUpdated != null) {
            leaveBalance.setLastUpdated(lastUpdated.toLocalDateTime());
        }
        
        return leaveBalance;
    }
    
    @Override
    protected String getTableName() {
        return "leavebalance";
    }
    
    @Override
    protected String getPrimaryKeyColumn() {
        return "leaveBalanceId";
    }
    
    @Override
    protected void setInsertParameters(PreparedStatement stmt, LeaveBalance leaveBalance) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, leaveBalance.getEmployeeId());
        stmt.setInt(paramIndex++, leaveBalance.getLeaveTypeId());
        
        if (leaveBalance.getTotalLeaveDays() != null) {
            stmt.setInt(paramIndex++, leaveBalance.getTotalLeaveDays());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        stmt.setInt(paramIndex++, leaveBalance.getUsedLeaveDays() != null ? leaveBalance.getUsedLeaveDays() : 0);
        
        if (leaveBalance.getRemainingLeaveDays() != null) {
            stmt.setInt(paramIndex++, leaveBalance.getRemainingLeaveDays());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        stmt.setInt(paramIndex++, leaveBalance.getCarryOverDays() != null ? leaveBalance.getCarryOverDays() : 0);
        stmt.setInt(paramIndex++, leaveBalance.getBalanceYear());
    }
    
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, LeaveBalance leaveBalance) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, leaveBalance.getEmployeeId());
        stmt.setInt(paramIndex++, leaveBalance.getLeaveTypeId());
        
        if (leaveBalance.getTotalLeaveDays() != null) {
            stmt.setInt(paramIndex++, leaveBalance.getTotalLeaveDays());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        stmt.setInt(paramIndex++, leaveBalance.getUsedLeaveDays() != null ? leaveBalance.getUsedLeaveDays() : 0);
        
        if (leaveBalance.getRemainingLeaveDays() != null) {
            stmt.setInt(paramIndex++, leaveBalance.getRemainingLeaveDays());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        stmt.setInt(paramIndex++, leaveBalance.getCarryOverDays() != null ? leaveBalance.getCarryOverDays() : 0);
        stmt.setInt(paramIndex++, leaveBalance.getBalanceYear());
        
        // Set the ID for WHERE clause
        stmt.setInt(paramIndex++, leaveBalance.getLeaveBalanceId());
    }
    
    @Override
    protected Integer getEntityId(LeaveBalance leaveBalance) {
        return leaveBalance.getLeaveBalanceId();
    }
    
    @Override
    protected void handleGeneratedKey(LeaveBalance entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setLeaveBalanceId(generatedKeys.getInt(1));
        }
    }
    
    // ===============================
    // CUSTOM SQL BUILDERS
    // ===============================
    
    private String buildInsertSQL() {
        return "INSERT INTO leavebalance " +
               "(employeeId, leaveTypeId, totalLeaveDays, usedLeaveDays, " +
               "remainingLeaveDays, carryOverDays, balanceYear) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
    
    private String buildUpdateSQL() {
        return "UPDATE leavebalance SET " +
               "employeeId = ?, leaveTypeId = ?, totalLeaveDays = ?, usedLeaveDays = ?, " +
               "remainingLeaveDays = ?, carryOverDays = ?, balanceYear = ? " +
               "WHERE leaveBalanceId = ?";
    }
    

    // CUSTOM LEAVE BALANCE METHODS

    
    /**
     * Finds leave balance for a specific employee, leave type, and year
     * @param employeeId The employee ID
     * @param leaveTypeId The leave type ID
     * @param year The balance year
     * @return LeaveBalance if found, null otherwise
     */
    public LeaveBalance findByEmployeeLeaveTypeAndYear(Integer employeeId, Integer leaveTypeId, Integer year) {
        String sql = "SELECT * FROM leavebalance WHERE employeeId = ? AND leaveTypeId = ? AND balanceYear = ?";
        return executeSingleQuery(sql, employeeId, leaveTypeId, year);
    }
    
    /**
     * Gets all leave balances for a specific employee in a given year
     * @param employeeId The employee ID
     * @param year The balance year
     * @return List of leave balances
     */
    public List<LeaveBalance> findByEmployeeAndYear(Integer employeeId, Integer year) {
        String sql = "SELECT * FROM leavebalance WHERE employeeId = ? AND balanceYear = ?";
        return executeQuery(sql, employeeId, year);
    }
    
    /**
     * Updates used leave days and recalculates remaining days
     * @param leaveBalanceId The leave balance ID
     * @param usedDays Number of days used
     * @return true if update was successful
     */
    public boolean updateUsedLeaveDays(Integer leaveBalanceId, Integer usedDays) {
        String sql = "UPDATE leavebalance SET usedLeaveDays = ?, " +
                    "remainingLeaveDays = totalLeaveDays + carryOverDays - ?, " +
                    "lastUpdated = CURRENT_TIMESTAMP " +
                    "WHERE leaveBalanceId = ?";
        return executeUpdate(sql, usedDays, usedDays, leaveBalanceId) > 0;
    }
    
    /**
     * Initializes leave balances for all employees for a new year
     * @param year The year to initialize
     * @param defaultLeaveDays Default number of leave days
     * @return Number of records created
     */
    public int initializeYearlyLeaveBalances(Integer year, Integer defaultLeaveDays) {
        String sql = "INSERT INTO leavebalance (employeeId, leaveTypeId, totalLeaveDays, usedLeaveDays, " +
                    "remainingLeaveDays, carryOverDays, balanceYear) " +
                    "SELECT e.employeeId, lt.leaveTypeId, ?, 0, ?, 0, ? " +
                    "FROM employee e " +
                    "CROSS JOIN leavetype lt " +
                    "WHERE e.status != 'Terminated' " +
                    "AND NOT EXISTS (SELECT 1 FROM leavebalance lb " +
                    "WHERE lb.employeeId = e.employeeId AND lb.leaveTypeId = lt.leaveTypeId AND lb.balanceYear = ?)";
        
        return executeUpdate(sql, defaultLeaveDays, defaultLeaveDays, year, year);
    }
    

    // OVERRIDE METHODS

    
    @Override
    public boolean save(LeaveBalance leaveBalance) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, leaveBalance);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(leaveBalance, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving leave balance: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean update(LeaveBalance leaveBalance) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, leaveBalance);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave balance: " + e.getMessage());
            return false;
        }
    }
}