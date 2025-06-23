
package DAOs;

import Models.LeaveTypeModel;
import java.sql.*;
import java.util.List;

/**
 * Data Access Object for LeaveTypeModel entities.
 * This class handles all database operations related to leave types.
 * It extends BaseDAO to inherit common CRUD operations and adds leave type-specific methods.
 * Supports the following leave types: Sick, Vacation, Emergency, Maternity, Paternity, Bereavement
 */

public class LeaveTypeDAO extends BaseDAO<LeaveTypeModel, Integer> {

    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public LeaveTypeDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }

    /**
     * Default constructor using default database connection
     */
    public LeaveTypeDAO() {
        super(new DatabaseConnection());
    }

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    /**
     * Converts a database row into a LeaveTypeModel object
     * @param rs The ResultSet containing leave type data from the database
     * @return A fully populated LeaveTypeModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected LeaveTypeModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        LeaveTypeModel leaveType = new LeaveTypeModel();
        
        leaveType.setLeaveTypeId(rs.getInt("leaveTypeId"));
        leaveType.setLeaveTypeName(rs.getString("leaveTypeName"));
        leaveType.setLeaveDescription(rs.getString("leaveDescription"));
        
        // Handle nullable maxDaysPerYear
        int maxDays = rs.getInt("maxDaysPerYear");
        if (!rs.wasNull()) {
            leaveType.setMaxDaysPerYear(maxDays);
        }
        
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            leaveType.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return leaveType;
    }

    @Override
    protected String getTableName() {
        return "leavetype";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "leaveTypeId";
    }

    @Override
    protected void setInsertParameters(PreparedStatement stmt, LeaveTypeModel leaveType) throws SQLException {
        int paramIndex = 1;
        
        stmt.setString(paramIndex++, leaveType.getLeaveTypeName());
        stmt.setString(paramIndex++, leaveType.getLeaveDescription());
        
        if (leaveType.getMaxDaysPerYear() != null) {
            stmt.setInt(paramIndex++, leaveType.getMaxDaysPerYear());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
    }

    @Override
    protected void setUpdateParameters(PreparedStatement stmt, LeaveTypeModel leaveType) throws SQLException {
        int paramIndex = 1;
        
        stmt.setString(paramIndex++, leaveType.getLeaveTypeName());
        stmt.setString(paramIndex++, leaveType.getLeaveDescription());
        
        if (leaveType.getMaxDaysPerYear() != null) {
            stmt.setInt(paramIndex++, leaveType.getMaxDaysPerYear());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        // Set the ID for WHERE clause
        stmt.setInt(paramIndex++, leaveType.getLeaveTypeId());
    }

    @Override
    protected Integer getEntityId(LeaveTypeModel leaveType) {
        return leaveType.getLeaveTypeId();
    }

    @Override
    protected void handleGeneratedKey(LeaveTypeModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setLeaveTypeId(generatedKeys.getInt(1));
        }
    }

    // CUSTOM SQL BUILDERS

    private String buildInsertSQL() {
        return "INSERT INTO leavetype " +
               "(leaveTypeName, leaveDescription, maxDaysPerYear) " +
               "VALUES (?, ?, ?)";
    }

    private String buildUpdateSQL() {
        return "UPDATE leavetype SET " +
               "leaveTypeName = ?, leaveDescription = ?, maxDaysPerYear = ? " +
               "WHERE leaveTypeId = ?";
    }

    // CUSTOM LEAVE TYPE METHODS

    /**
     * Finds a leave type by name (works with your LeaveName enum values)
     * @param leaveTypeName The leave type name to search for
     * @return LeaveTypeModel if found, null otherwise
     */
    public LeaveTypeModel findByName(String leaveTypeName) {
        String sql = "SELECT * FROM leavetype WHERE leaveTypeName = ?";
        return executeSingleQuery(sql, leaveTypeName);
    }

    /**
     * Finds leave type by LeaveName enum value
     * @param leaveName The enum value (Sick, Vacation, Emergency, etc.)
     * @return LeaveTypeModel if found, null otherwise
     */
    public LeaveTypeModel findByLeaveName(String leaveName) {
        // Map enum values to database names
        String dbName = mapEnumToDbName(leaveName);
        return findByName(dbName);
    }

    /**
     * Gets all leave types ordered by name
     * @return List of all leave types
     */
    public List<LeaveTypeModel> findAllActive() {
        String sql = "SELECT * FROM leavetype ORDER BY leaveTypeName";
        return executeQuery(sql);
    }

    /**
     * Finds leave types with unlimited days (maxDaysPerYear is null or <= 0)
     * @return List of unlimited leave types
     */
    public List<LeaveTypeModel> findUnlimitedLeaveTypes() {
        String sql = "SELECT * FROM leavetype WHERE maxDaysPerYear IS NULL OR maxDaysPerYear <= 0 ORDER BY leaveTypeName";
        return executeQuery(sql);
    }

    /**
     * Finds leave types with specific maximum days
     * @param maxDays The maximum days to filter by
     * @return List of leave types with the specified maximum days
     */
    public List<LeaveTypeModel> findByMaxDays(Integer maxDays) {
        String sql = "SELECT * FROM leavetype WHERE maxDaysPerYear = ? ORDER BY leaveTypeName";
        return executeQuery(sql, maxDays);
    }

    /**
     * Checks if a leave type name already exists (for validation)
     * @param leaveTypeName The leave type name to check
     * @param excludeId Optional ID to exclude from check (for updates)
     * @return true if name exists, false otherwise
     */
    public boolean leaveTypeNameExists(String leaveTypeName, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM leavetype WHERE leaveTypeName = ?";
        
        if (excludeId != null) {
            sql += " AND leaveTypeId != ?";
        }

        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, leaveTypeName);
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking leave type name existence: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Gets count of leave types in the system
     * @return Number of leave types
     */
    public int getLeaveTypeCount() {
        String sql = "SELECT COUNT(*) FROM leavetype";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting leave type count: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * Creates default leave types based on your LeaveName enum if none exist
     * @return Number of default leave types created
     */
    public int createDefaultLeaveTypes() {
        int created = 0;
        
        // Check if any leave types already exist
        if (getLeaveTypeCount() > 0) {
            System.out.println("Leave types already exist. Skipping default creation.");
            return 0;
        }
        
        try {
            // Create leave types based on your LeaveName enum
            LeaveTypeModel[] defaultTypes = {
                new LeaveTypeModel("Sick", "Medical leave for illness or medical appointments", 15),
                new LeaveTypeModel("Vacation", "Annual vacation leave for rest and recreation", 15), 
                new LeaveTypeModel("Emergency", "Emergency leave for urgent personal matters", 5),
                new LeaveTypeModel("Maternity", "Maternity leave for mothers", 105), // 15 weeks = 105 days
                new LeaveTypeModel("Paternity", "Paternity leave for fathers", 7),   // 1 week = 7 days
                new LeaveTypeModel("Bereavement", "Leave for death in family", 3)     // 3 days
            };
            
            for (LeaveTypeModel leaveType : defaultTypes) {
                if (save(leaveType)) {
                    created++;
                    System.out.println("✅ Created default leave type: " + leaveType.getLeaveTypeName() + 
                                     " (" + leaveType.getMaxDaysPerYear() + " days)");
                } else {
                    System.err.println("❌ Failed to create leave type: " + leaveType.getLeaveTypeName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error creating default leave types: " + e.getMessage());
        }
        
        return created;
    }

    /**
     * Maps your LeaveName enum values to database-friendly names
     * @param enumValue The enum value (Sick, Vacation, etc.)
     * @return Database name
     */
    private String mapEnumToDbName(String enumValue) {
        return switch (enumValue.toUpperCase()) {
            case "SICK" -> "Sick";
            case "VACATION" -> "Vacation";
            case "EMERGENCY" -> "Emergency";
            case "MATERNITY" -> "Maternity";
            case "PATERNITY" -> "Paternity";
            case "BEREAVEMENT" -> "Bereavement";
            default -> enumValue; // Return as-is if not recognized
        };
    }

    /**
     * Gets leave type ID by LeaveName enum
     * @param leaveName The enum name
     * @return Leave type ID or null if not found
     */
    public Integer getLeaveTypeIdByName(String leaveName) {
        LeaveTypeModel leaveType = findByLeaveName(leaveName);
        return leaveType != null ? leaveType.getLeaveTypeId() : null;
    }

    /**
     * Updates leave type maximum days
     * @param leaveTypeId Leave type ID
     * @param maxDays New maximum days
     * @return true if successful
     */
    public boolean updateMaxDays(Integer leaveTypeId, Integer maxDays) {
        String sql = "UPDATE leavetype SET maxDaysPerYear = ? WHERE leaveTypeId = ?";
        return executeUpdate(sql, maxDays, leaveTypeId) > 0;
    }

    // OVERRIDE METHODS

    @Override
    public boolean save(LeaveTypeModel leaveType) {
        // Validate before saving
        if (!leaveType.isValid()) {
            System.err.println("Cannot save invalid leave type: " + leaveType);
            return false;
        }
        
        // Check for duplicate names
        if (leaveTypeNameExists(leaveType.getLeaveTypeName(), null)) {
            System.err.println("Leave type name already exists: " + leaveType.getLeaveTypeName());
            return false;
        }
        
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, leaveType);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(leaveType, generatedKeys);
                    }
                }
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving leave type: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(LeaveTypeModel leaveType) {
        // Validate before updating
        if (!leaveType.isValid()) {
            System.err.println("Cannot update invalid leave type: " + leaveType);
            return false;
        }
        
        // Check for duplicate names (excluding current record)
        if (leaveTypeNameExists(leaveType.getLeaveTypeName(), leaveType.getLeaveTypeId())) {
            System.err.println("Leave type name already exists: " + leaveType.getLeaveTypeName());
            return false;
        }
        
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, leaveType);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave type: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer leaveTypeId) {
        // Check if leave type is being used in leave requests or balances
        if (isLeaveTypeInUse(leaveTypeId)) {
            System.err.println("Cannot delete leave type: It is being used in leave requests or balances");
            return false;
        }
        
        return super.delete(leaveTypeId);
    }

    /**
     * Checks if a leave type is being used in leave requests or balances
     * @param leaveTypeId The leave type ID to check
     * @return true if in use, false otherwise
     */
    private boolean isLeaveTypeInUse(Integer leaveTypeId) {
        try (Connection conn = databaseConnection.createConnection()) {
            
            // Check leave requests
            String leaveRequestSql = "SELECT COUNT(*) FROM leaverequest WHERE leaveTypeId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(leaveRequestSql)) {
                stmt.setInt(1, leaveTypeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
            
            // Check leave balances
            String leaveBalanceSql = "SELECT COUNT(*) FROM leavebalance WHERE leaveTypeId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(leaveBalanceSql)) {
                stmt.setInt(1, leaveTypeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking leave type usage: " + e.getMessage());
            return true; // Assume in use if we can't check
        }
        
        return false;
    }
}