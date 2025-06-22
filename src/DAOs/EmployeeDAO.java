package DAOs;

import Models.EmployeeModel;
import Models.EmployeeModel.EmployeeStatus;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * Data Access Object for EmployeeModel entities.
 * This class handles all database operations related to employees.
 * It extends BaseDAO to inherit common CRUD operations and adds employee-specific methods.
 * @author User
 */
public class EmployeeDAO extends BaseDAO<EmployeeModel, Integer> {
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public EmployeeDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into an EmployeeModel object
     * This method reads each column from the ResultSet and creates an EmployeeModel
     * @param rs The ResultSet containing employee data from the database
     * @return A fully populated EmployeeModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected EmployeeModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        EmployeeModel employee = new EmployeeModel();
        
        // Set basic employee information
        employee.setEmployeeId(rs.getInt("employeeId"));
        employee.setFirstName(rs.getString("firstName"));
        employee.setLastName(rs.getString("lastName"));
        
        // Handle birth date (could be null in database)
        Date birthDate = rs.getDate("birthDate");
        if (birthDate != null) {
            employee.setBirthDate(birthDate.toLocalDate());
        }
        
        // Set contact information
        employee.setPhoneNumber(rs.getString("phoneNumber"));
        employee.setEmail(rs.getString("email"));
        
        // Handle salary information (BigDecimal for precise money calculations)
        BigDecimal basicSalary = rs.getBigDecimal("basicSalary");
        if (basicSalary != null) {
            employee.setBasicSalary(basicSalary);
        }
        
        BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
        if (hourlyRate != null) {
            employee.setHourlyRate(hourlyRate);
        }
        
        // Set system information
        employee.setUserRole(rs.getString("userRole"));
        employee.setPasswordHash(rs.getString("passwordHash"));
        
        // Handle employee status (convert string to enum)
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            employee.setStatus(EmployeeStatus.fromString(statusStr));
        }
        
        // Handle timestamp fields
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            employee.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        if (updatedAt != null) {
            employee.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        Timestamp lastLogin = rs.getTimestamp("lastLogin");
        if (lastLogin != null) {
            employee.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        // Handle foreign key relationships (could be null)
        int positionId = rs.getInt("positionId");
        if (!rs.wasNull()) {
            employee.setPositionId(positionId);
        }
        
        int supervisorId = rs.getInt("supervisorId");
        if (!rs.wasNull()) {
            employee.setSupervisorId(supervisorId);
        }
        
        return employee;
    }
    
    /**
     * Returns the database table name for employees
     * @return "employee" - the name of the employee table in the database
     */
    @Override
    protected String getTableName() {
        return "employee";
    }
    
    /**
     * Returns the primary key column name for the employee table
     * @return "employeeId" - the primary key column name
     */
    @Override
    protected String getPrimaryKeyColumn() {
        return "employeeId";
    }
    
    /**
     * Sets parameters for INSERT operations when creating new employees
     * This method maps EmployeeModel object properties to SQL parameters
     * @param stmt The PreparedStatement to set parameters on
     * @param employee The EmployeeModel object to get values from
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setInsertParameters(PreparedStatement stmt, EmployeeModel employee) throws SQLException {
        // Note: employeeId is auto-increment, so we don't include it in INSERT
        // createdAt and updatedAt have default values, so we don't need to set them
        int paramIndex = 1;
        
        // Set basic employee information
        stmt.setString(paramIndex++, employee.getFirstName());
        stmt.setString(paramIndex++, employee.getLastName());
        
        // Handle birth date (could be null)
        if (employee.getBirthDate() != null) {
            stmt.setDate(paramIndex++, Date.valueOf(employee.getBirthDate()));
        } else {
            stmt.setNull(paramIndex++, Types.DATE);
        }
        
        // Set contact information
        stmt.setString(paramIndex++, employee.getPhoneNumber());
        stmt.setString(paramIndex++, employee.getEmail());
        
        // Set salary information
        stmt.setBigDecimal(paramIndex++, employee.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, employee.getHourlyRate());
        
        // Set system information
        stmt.setString(paramIndex++, employee.getUserRole());
        stmt.setString(paramIndex++, employee.getPasswordHash());
        stmt.setString(paramIndex++, employee.getStatus().getValue());
        
        // Handle last login (could be null for new employees)
        if (employee.getLastLogin() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(employee.getLastLogin()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        // Set required position ID
        stmt.setInt(paramIndex++, employee.getPositionId());
        
        // Handle supervisor ID (could be null for top-level employees)
        if (employee.getSupervisorId() != null) {
            stmt.setInt(paramIndex++, employee.getSupervisorId());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
    }
    
    /**
     * Sets parameters for UPDATE operations when modifying existing employees
     * This method maps EmployeeModel object properties to SQL parameters for updates
     * @param stmt The PreparedStatement to set parameters on
     * @param employee The EmployeeModel object with updated values
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, EmployeeModel employee) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT
        stmt.setString(paramIndex++, employee.getFirstName());
        stmt.setString(paramIndex++, employee.getLastName());
        
        if (employee.getBirthDate() != null) {
            stmt.setDate(paramIndex++, Date.valueOf(employee.getBirthDate()));
        } else {
            stmt.setNull(paramIndex++, Types.DATE);
        }
        
        stmt.setString(paramIndex++, employee.getPhoneNumber());
        stmt.setString(paramIndex++, employee.getEmail());
        stmt.setBigDecimal(paramIndex++, employee.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, employee.getHourlyRate());
        stmt.setString(paramIndex++, employee.getUserRole());
        stmt.setString(paramIndex++, employee.getPasswordHash());
        stmt.setString(paramIndex++, employee.getStatus().getValue());
        
        if (employee.getLastLogin() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(employee.getLastLogin()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        stmt.setInt(paramIndex++, employee.getPositionId());
        
        if (employee.getSupervisorId() != null) {
            stmt.setInt(paramIndex++, employee.getSupervisorId());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        // Finally, set the employee ID for the WHERE clause
        stmt.setInt(paramIndex++, employee.getEmployeeId());
    }
    
    /**
     * Gets the ID from an EmployeeModel object
     * This is used by BaseDAO for update and delete operations
     * @param employee The EmployeeModel object to get ID from
     * @return The employee's ID
     */
    @Override
    protected Integer getEntityId(EmployeeModel employee) {
        return employee.getEmployeeId();
    }
    
    /**
     * Handles auto-generated employee IDs after INSERT operations
     * This method sets the generated employeeId back on the EmployeeModel object
     * @param entity The EmployeeModel that was just inserted
     * @param generatedKeys The ResultSet containing the generated employeeId
     * @throws SQLException if there's an error reading the generated key
     */
    @Override
    protected void handleGeneratedKey(EmployeeModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setEmployeeId(generatedKeys.getInt(1));
        }
    }
    
 
    // CUSTOM SQL BUILDERS - Override BaseDAO methods with specific SQL

    
    /**
     * Builds the complete INSERT SQL statement for employees
     * This creates the specific SQL for inserting employee records
     * @return The complete INSERT SQL statement
     */
    private String buildInsertSQL() {
        return "INSERT INTO employee " +
               "(firstName, lastName, birthDate, phoneNumber, email, basicSalary, " +
               "hourlyRate, userRole, passwordHash, status, lastLogin, positionId, supervisorId) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * Builds the complete UPDATE SQL statement for employees
     * This creates the specific SQL for updating employee records
     * @return The complete UPDATE SQL statement
     */
    private String buildUpdateSQL() {
        return "UPDATE employee SET " +
               "firstName = ?, lastName = ?, birthDate = ?, phoneNumber = ?, email = ?, " +
               "basicSalary = ?, hourlyRate = ?, userRole = ?, passwordHash = ?, status = ?, " +
               "lastLogin = ?, positionId = ?, supervisorId = ?, updatedAt = CURRENT_TIMESTAMP " +
               "WHERE employeeId = ?";
    }
    

    // CUSTOM EMPLOYEE METHODS - Employee-specific database operations

    
    /**
     * Finds an employee by their email address
     * This is useful for login operations and email validation
     * @param email The email address to search for
     * @return The EmployeeModel if found, null if not found
     */
    public EmployeeModel findByEmail(String email) {
        String sql = "SELECT * FROM employee WHERE email = ?";
        return executeSingleQuery(sql, email);
    }
    
    /**
     * Finds all employees with a specific status
     * This is useful for reports and filtering employees
     * @param status The employee status to search for
     * @return List of employees with the specified status
     */
    public List<EmployeeModel> findByStatus(EmployeeStatus status) {
        String sql = "SELECT * FROM employee WHERE status = ?";
        return executeQuery(sql, status.getValue());
    }
    
    /**
     * Finds all employees in a specific position
     * This is useful for organizational reports
     * @param positionId The position ID to search for
     * @return List of employees in the specified position
     */
    public List<EmployeeModel> findByPosition(Integer positionId) {
        String sql = "SELECT * FROM employee WHERE positionId = ?";
        return executeQuery(sql, positionId);
    }
    
    /**
     * Updates the salary information for an employee
     * This is a convenient method for salary adjustments
     * @param employeeId The employee ID to update
     * @param basicSalary The new basic salary
     * @param hourlyRate The new hourly rate
     * @return true if update was successful, false otherwise
     */
    public boolean updateSalary(Integer employeeId, BigDecimal basicSalary, BigDecimal hourlyRate) {
        String sql = "UPDATE employee SET basicSalary = ?, hourlyRate = ?, updatedAt = CURRENT_TIMESTAMP WHERE employeeId = ?";
        int rowsAffected = executeUpdate(sql, basicSalary, hourlyRate, employeeId);
        return rowsAffected > 0;
    }
    
    /**
     * Gets all employees in the system
     * This is an alias for findAll() with a more descriptive name
     * @return List of all employees
     */
    public List<EmployeeModel> getAllEmployees() {
        return findAll();
    }
    
    /**
     * Finds employees by department name
     * This requires a JOIN with the position table to get department information
     * @param department The department name to search for
     * @return List of employees in the specified department
     */
    public List<EmployeeModel> getEmployeesByDepartment(String department) {
        String sql = "SELECT e.* FROM employee e " +
                    "JOIN position p ON e.positionId = p.positionId " +
                    "WHERE p.department = ?";
        return executeQuery(sql, department);
    }
    
    /**
     * Finds all employees supervised by a specific supervisor
     * This is useful for organizational hierarchy reports
     * @param supervisorId The supervisor's employee ID
     * @return List of employees supervised by the specified supervisor
     */
    public List<EmployeeModel> getEmployeesBySupervisor(Integer supervisorId) {
        String sql = "SELECT * FROM employee WHERE supervisorId = ?";
        return executeQuery(sql, supervisorId);
    }
    
    /**
     * Validates employee login credentials
     * This checks both email and password and ensures the employee is not terminated
     * @param email The email address
     * @param passwordHash The hashed password
     * @return The EmployeeModel if credentials are valid, null otherwise
     */
    public EmployeeModel validateCredentials(String email, String passwordHash) {
        String sql = "SELECT * FROM employee WHERE email = ? AND passwordHash = ? AND status != 'Terminated'";
        return executeSingleQuery(sql, email, passwordHash);
    }
    
    /**
     * Updates the last login timestamp for an employee
     * This is called when an employee successfully logs in
     * @param employeeId The employee ID
     * @return true if update was successful, false otherwise
     */
    public boolean updateLastLogin(Integer employeeId) {
        String sql = "UPDATE employee SET lastLogin = CURRENT_TIMESTAMP WHERE employeeId = ?";
        int rowsAffected = executeUpdate(sql, employeeId);
        return rowsAffected > 0;
    }
    
    /**
     * Gets all active employees (not terminated)
     * This is useful for reports that should only include current employees
     * @return List of active employees
     */
    public List<EmployeeModel> getActiveEmployees() {
        String sql = "SELECT * FROM employee WHERE status != 'Terminated'";
        return executeQuery(sql);
    }
    
    /**
     * Finds employees by their user role
     * This is useful for permission and access control reports
     * @param userRole The user role to search for
     * @return List of employees with the specified role
     */
    public List<EmployeeModel> getEmployeesByRole(String userRole) {
        String sql = "SELECT * FROM employee WHERE userRole = ?";
        return executeQuery(sql, userRole);
    }
    

    // PLACEHOLDER METHODS - These would need additional implementation

    
    /**
     * Updates employee address information
     * Note: This would require implementing Address model and AddressDAO
     * @param employeeId The employee ID
     * @param address The address object
     * @return true if update was successful
     */
    public boolean updateEmployeeAddress(Integer employeeId, Object address) {
        // This would require implementing address handling with the address table
        System.out.println("updateEmployeeAddress method called for employee: " + employeeId);
        return true;
    }
    
    /**
     * Gets employee addresses from the address table
     * Note: This would require implementing Address model and JOIN operations
     * @param employeeId The employee ID
     * @return List of addresses for the employee
     */
    public List<Object> getEmployeeAddresses(Integer employeeId) {
        // This would require implementing address handling with JOIN operations
        System.out.println("getEmployeeAddresses method called for employee: " + employeeId);
        return new java.util.ArrayList<>();
    }
    
    /**
     * Updates employee government ID information
     * Note: This would require implementing GovId model and GovIdDAO
     * @param employeeId The employee ID
     * @param govId The government ID object
     * @return true if update was successful
     */
    public boolean updateEmployeeGovId(Integer employeeId, Object govId) {
        // This would require implementing government ID handling with the govid table
        System.out.println("updateEmployeeGovId method called for employee: " + employeeId);
        return true;
    }
    
    /**
     * Gets employee government ID information
     * Note: This would require implementing GovId model and JOIN operations
     * @param employeeId The employee ID
     * @return The government ID object
     */
    public Object getEmployeeGovId(Integer employeeId) {
        // This would require implementing government ID handling with JOIN operations
        System.out.println("getEmployeeGovId method called for employee: " + employeeId);
        return null;
    }
    
    /**
     * Validates uniqueness of government ID fields
     * Note: This would require querying the govid table
     * @param field The field name (e.g., "sss", "tin", "philhealth", "pagibig")
     * @param value The value to check for uniqueness
     * @return true if the value is unique
     */
    public boolean validateGovIdUniqueness(String field, String value) {
        // This would require implementing government ID validation with the govid table
        System.out.println("validateGovIdUniqueness called for field: " + field + ", value: " + value);
        return true;
    }
    

    // OVERRIDE METHODS - Use custom SQL instead of BaseDAO defaults

    
    /**
     * Override the save method to use custom INSERT SQL
     * @param employee The employee to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(EmployeeModel employee) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, employee);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(employee, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving employee: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param employee The employee to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(EmployeeModel employee) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, employee);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating employee: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}