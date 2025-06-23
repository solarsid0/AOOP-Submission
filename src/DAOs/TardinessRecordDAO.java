
package DAOs;

import Models.TardinessRecordModel;
import Models.TardinessRecordModel.TardinessType;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Data Access Object for TardinessRecordModel entities.
 * This class handles all database operations related to tardiness records.
 * It extends BaseDAO to inherit common CRUD operations and adds tardiness-specific methods.
 
 */

public class TardinessRecordDAO extends BaseDAO<TardinessRecordModel, Integer> {

    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public TardinessRecordDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }

    /**
     * Default constructor using default database connection
     */
    public TardinessRecordDAO() {
        super(new DatabaseConnection());
    }

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    /**
     * Converts a database row into a TardinessRecordModel object
     * @param rs The ResultSet containing tardiness data from the database
     * @return A fully populated TardinessRecordModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected TardinessRecordModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        TardinessRecordModel tardiness = new TardinessRecordModel();
        
        tardiness.setTardinessId(rs.getInt("tardinessId"));
        tardiness.setAttendanceId(rs.getInt("attendanceId"));
        tardiness.setTardinessHours(rs.getBigDecimal("tardinessHours"));
        
        // Handle enum for tardiness type
        String typeStr = rs.getString("tardinessType");
        if (typeStr != null) {
            tardiness.setTardinessType(TardinessType.fromString(typeStr));
        }
        
        tardiness.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            tardiness.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return tardiness;
    }

    @Override
    protected String getTableName() {
        return "tardinessrecord";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "tardinessId";
    }

    @Override
    protected void setInsertParameters(PreparedStatement stmt, TardinessRecordModel tardiness) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, tardiness.getAttendanceId());
        stmt.setBigDecimal(paramIndex++, tardiness.getTardinessHours());
        stmt.setString(paramIndex++, tardiness.getTardinessType().getDisplayName());
        
        if (tardiness.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, tardiness.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
    }

    @Override
    protected void setUpdateParameters(PreparedStatement stmt, TardinessRecordModel tardiness) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, tardiness.getAttendanceId());
        stmt.setBigDecimal(paramIndex++, tardiness.getTardinessHours());
        stmt.setString(paramIndex++, tardiness.getTardinessType().getDisplayName());
        
        if (tardiness.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, tardiness.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Set the ID for WHERE clause
        stmt.setInt(paramIndex++, tardiness.getTardinessId());
    }

    @Override
    protected Integer getEntityId(TardinessRecordModel tardiness) {
        return tardiness.getTardinessId();
    }

    @Override
    protected void handleGeneratedKey(TardinessRecordModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setTardinessId(generatedKeys.getInt(1));
        }
    }

    // CUSTOM SQL BUILDERS

    private String buildInsertSQL() {
        return "INSERT INTO tardinessrecord " +
               "(attendanceId, tardinessHours, tardinessType, supervisorNotes) " +
               "VALUES (?, ?, ?, ?)";
    }

    private String buildUpdateSQL() {
        return "UPDATE tardinessrecord SET " +
               "attendanceId = ?, tardinessHours = ?, tardinessType = ?, supervisorNotes = ? " +
               "WHERE tardinessId = ?";
    }

    // CUSTOM TARDINESS METHODS

    /**
     * Finds all tardiness records for a specific attendance record
     * @param attendanceId The attendance ID
     * @return List of tardiness records
     */
    public List<TardinessRecordModel> findByAttendanceId(Integer attendanceId) {
        String sql = "SELECT * FROM tardinessrecord WHERE attendanceId = ? ORDER BY createdAt DESC";
        return executeQuery(sql, attendanceId);
    }

    /**
     * Finds tardiness records by type
     * @param tardinessType The tardiness type to search for
     * @return List of tardiness records with the specified type
     */
    public List<TardinessRecordModel> findByType(TardinessType tardinessType) {
        String sql = "SELECT * FROM tardinessrecord WHERE tardinessType = ? ORDER BY createdAt DESC";
        return executeQuery(sql, tardinessType.getDisplayName());
    }

    /**
     * Gets tardiness records for an employee within a date range
     * This method joins with the attendance table to get employee information
     * @param employeeId The employee ID
     * @param startDate The start date
     * @param endDate The end date
     * @return List of tardiness records within the date range
     */
    public List<TardinessRecordModel> getTardinessRecordsForEmployee(Integer employeeId, 
                                                                   LocalDateTime startDate, 
                                                                   LocalDateTime endDate) {
        String sql = "SELECT t.* FROM tardinessrecord t " +
                    "JOIN attendance a ON t.attendanceId = a.attendanceId " +
                    "WHERE a.employeeId = ? AND t.createdAt BETWEEN ? AND ? " +
                    "ORDER BY t.createdAt DESC";
        
        return executeQuery(sql, employeeId, 
                          Timestamp.valueOf(startDate), 
                          Timestamp.valueOf(endDate));
    }

    /**
     * Gets tardiness records for a specific month
     * @param employeeId The employee ID
     * @param yearMonth The year and month
     * @return List of tardiness records for the month
     */
    public List<TardinessRecordModel> getTardinessRecordsForMonth(Integer employeeId, YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        return getTardinessRecordsForEmployee(employeeId, startDate, endDate);
    }

    // OVERRIDE METHODS

    @Override
    public boolean save(TardinessRecordModel tardiness) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, tardiness);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(tardiness, generatedKeys);
                    }
                }
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving tardiness record: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(TardinessRecordModel tardiness) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, tardiness);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating tardiness record: " + e.getMessage());
            return false;
        }
    }
}
