
package DAOs;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for reference data operations
 * Handles lookup tables and static data used throughout the payroll system
 * @author Chadley
 */
public class ReferenceDataDAO {
   private final DatabaseConnection databaseConnection;
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public ReferenceDataDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    // POSITION REFERENCE DATA
    
    /**
     * Gets all positions in the system
     * @return List of position maps with id, title, department, salary info
     */
    public List<Map<String, Object>> getAllPositions() {
        String sql = "SELECT positionId, positionTitle, department, basicSalary, hourlyRate FROM position ORDER BY department, positionTitle";
        return executeQuery(sql);
    }
    
    /**
     * Gets positions by department
     * @param department The department name
     * @return List of positions in the specified department
     */
    public List<Map<String, Object>> getPositionsByDepartment(String department) {
        String sql = "SELECT positionId, positionTitle, department, basicSalary, hourlyRate FROM position WHERE department = ? ORDER BY positionTitle";
        return executeQuery(sql, department);
    }
    
    /**
     * Gets position details by ID
     * @param positionId The position ID
     * @return Position details or null if not found
     */
    public Map<String, Object> getPositionById(Integer positionId) {
        String sql = "SELECT positionId, positionTitle, department, basicSalary, hourlyRate FROM position WHERE positionId = ?";
        List<Map<String, Object>> results = executeQuery(sql, positionId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Gets all unique departments
     * @return List of department names
     */
    public List<String> getAllDepartments() {
        String sql = "SELECT DISTINCT department FROM position ORDER BY department";
        List<String> departments = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                departments.add(rs.getString("department"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting departments: " + e.getMessage());
        }
        
        return departments;
    }
    
    // EMPLOYEE STATUS REFERENCE DATA
    
    /**
     * Gets all valid employee statuses
     * @return List of valid employee status values
     */
    public List<String> getEmployeeStatuses() {
        // These match your EmployeeModel.EmployeeStatus enum
        List<String> statuses = new ArrayList<>();
        statuses.add("Active");
        statuses.add("Inactive");
        statuses.add("Terminated");
        statuses.add("On Leave");
        return statuses;
    }
    
    // USER ROLE REFERENCE DATA
    
    /**
     * Gets all valid user roles
     * @return List of valid user role values
     */
    public List<String> getUserRoles() {
        List<String> roles = new ArrayList<>();
        roles.add("HR");
        roles.add("IT");
        roles.add("Accounting");
        roles.add("ImmediateSupervisor");
        roles.add("Employee");
        return roles;
    }
    
    // BENEFIT TYPE REFERENCE DATA
    
    /**
     * Gets all benefit types
     * @return List of benefit type maps with id and description
     */
    public List<Map<String, Object>> getAllBenefitTypes() {
        String sql = "SELECT benefitTypeId, benefitName, description FROM benefittype ORDER BY benefitName";
        return executeQuery(sql);
    }
    
    /**
     * Gets benefit type by ID
     * @param benefitTypeId The benefit type ID
     * @return Benefit type details or null if not found
     */
    public Map<String, Object> getBenefitTypeById(Integer benefitTypeId) {
        String sql = "SELECT benefitTypeId, benefitName, description FROM benefittype WHERE benefitTypeId = ?";
        List<Map<String, Object>> results = executeQuery(sql, benefitTypeId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    // LEAVE TYPE REFERENCE DATA
    
    /**
     * Gets all leave types
     * @return List of leave type maps with id and description
     */
    public List<Map<String, Object>> getAllLeaveTypes() {
        String sql = "SELECT leaveTypeId, leaveTypeName, description FROM leavetype ORDER BY leaveTypeName";
        return executeQuery(sql);
    }
    
    /**
     * Gets leave type by ID
     * @param leaveTypeId The leave type ID
     * @return Leave type details or null if not found
     */
    public Map<String, Object> getLeaveTypeById(Integer leaveTypeId) {
        String sql = "SELECT leaveTypeId, leaveTypeName, description FROM leavetype WHERE leaveTypeId = ?";
        List<Map<String, Object>> results = executeQuery(sql, leaveTypeId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    // PAYROLL PERIOD REFERENCE DATA
    
    /**
     * Gets all pay periods
     * @return List of pay period maps with period details
     */
    public List<Map<String, Object>> getAllPayPeriods() {
        String sql = "SELECT payPeriodId, startDate, endDate, payDate, payPeriodDescription FROM payperiod ORDER BY startDate DESC";
        return executeQuery(sql);
    }
    
    /**
     * Gets current active pay period
     * @return Current pay period details or null if none active
     */
    public Map<String, Object> getCurrentPayPeriod() {
        String sql = "SELECT payPeriodId, startDate, endDate, payDate, payPeriodDescription " +
                    "FROM payperiod WHERE CURDATE() BETWEEN startDate AND endDate LIMIT 1";
        List<Map<String, Object>> results = executeQuery(sql);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Gets most recent pay period
     * @return Most recent pay period details
     */
    public Map<String, Object> getLatestPayPeriod() {
        String sql = "SELECT payPeriodId, startDate, endDate, payDate, payPeriodDescription " +
                    "FROM payperiod ORDER BY endDate DESC LIMIT 1";
        List<Map<String, Object>> results = executeQuery(sql);
        return results.isEmpty() ? null : results.get(0);
    }
    
    // GOVERNMENT RATES AND CONSTANTS
    
    /**
     * Gets current SSS contribution rates
     * @return Map with SSS rate information
     */
    public Map<String, Object> getSSSRates() {
        Map<String, Object> rates = new HashMap<>();
        // These would typically come from a rates/constants table in your DB
        rates.put("employeeRate", 0.045); // 4.5%
        rates.put("employerRate", 0.095); // 9.5%
        rates.put("maxSalary", 25000.00); // Maximum salary for SSS
        rates.put("lastUpdated", "2024-01-01");
        return rates;
    }
    
    /**
     * Gets current PhilHealth contribution rates
     * @return Map with PhilHealth rate information
     */
    public Map<String, Object> getPhilHealthRates() {
        Map<String, Object> rates = new HashMap<>();
        rates.put("employeeRate", 0.0225); // 2.25%
        rates.put("employerRate", 0.0225); // 2.25%
        rates.put("maxSalary", 100000.00); // Maximum salary for PhilHealth
        rates.put("lastUpdated", "2024-01-01");
        return rates;
    }
    
    /**
     * Gets current Pag-IBIG contribution rates
     * @return Map with Pag-IBIG rate information
     */
    public Map<String, Object> getPagIBIGRates() {
        Map<String, Object> rates = new HashMap<>();
        rates.put("employeeRate", 0.02); // 2%
        rates.put("employerRate", 0.02); // 2%
        rates.put("maxContribution", 200.00); // Maximum monthly contribution
        rates.put("lastUpdated", "2024-01-01");
        return rates;
    }
    
    /**
     * Gets current tax brackets (BIR withholding tax)
     * @return List of tax bracket maps
     */
    public List<Map<String, Object>> getTaxBrackets() {
        List<Map<String, Object>> brackets = new ArrayList<>();
        
        // 2024 BIR tax brackets (simplified)
        Map<String, Object> bracket1 = new HashMap<>();
        bracket1.put("minAmount", 0.0);
        bracket1.put("maxAmount", 20833.0);
        bracket1.put("rate", 0.0);
        bracket1.put("fixedAmount", 0.0);
        brackets.add(bracket1);
        
        Map<String, Object> bracket2 = new HashMap<>();
        bracket2.put("minAmount", 20833.01);
        bracket2.put("maxAmount", 33333.0);
        bracket2.put("rate", 0.20);
        bracket2.put("fixedAmount", 0.0);
        brackets.add(bracket2);
        
        Map<String, Object> bracket3 = new HashMap<>();
        bracket3.put("minAmount", 33333.01);
        bracket3.put("maxAmount", 66667.0);
        bracket3.put("rate", 0.25);
        bracket3.put("fixedAmount", 2500.0);
        brackets.add(bracket3);
        
        Map<String, Object> bracket4 = new HashMap<>();
        bracket4.put("minAmount", 66667.01);
        bracket4.put("maxAmount", 166667.0);
        bracket4.put("rate", 0.30);
        bracket4.put("fixedAmount", 10833.33);
        brackets.add(bracket4);
        
        Map<String, Object> bracket5 = new HashMap<>();
        bracket5.put("minAmount", 166667.01);
        bracket5.put("maxAmount", 666667.0);
        bracket5.put("rate", 0.32);
        bracket5.put("fixedAmount", 40833.33);
        brackets.add(bracket5);
        
        Map<String, Object> bracket6 = new HashMap<>();
        bracket6.put("minAmount", 666667.01);
        bracket6.put("maxAmount", Double.MAX_VALUE);
        bracket6.put("rate", 0.35);
        bracket6.put("fixedAmount", 200833.33);
        brackets.add(bracket6);
        
        return brackets;
    }
    
    // SYSTEM CONFIGURATION
    
    /**
     * Gets system configuration values
     * @return Map with system configuration
     */
    public Map<String, Object> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("companyName", "MotorPH");
        config.put("payrollCutoff1", 15); // 15th of month
        config.put("payrollCutoff2", 30); // End of month
        config.put("hoursPerDay", 8.0);
        config.put("daysPerWeek", 5.0);
        config.put("overtimeMultiplier", 1.25);
        config.put("nightDifferentialRate", 0.10); // 10%
        config.put("holidayRate", 2.0); // Double pay
        return config;
    }
    
    // LOOKUP VALIDATION METHODS
    
    /**
     * Validates if a position ID exists
     * @param positionId The position ID to validate
     * @return true if position exists
     */
    public boolean isValidPositionId(Integer positionId) {
        String sql = "SELECT COUNT(*) FROM position WHERE positionId = ?";
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, positionId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error validating position ID: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates if a benefit type ID exists
     * @param benefitTypeId The benefit type ID to validate
     * @return true if benefit type exists
     */
    public boolean isValidBenefitTypeId(Integer benefitTypeId) {
        String sql = "SELECT COUNT(*) FROM benefittype WHERE benefitTypeId = ?";
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, benefitTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error validating benefit type ID: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates if a leave type ID exists
     * @param leaveTypeId The leave type ID to validate
     * @return true if leave type exists
     */
    public boolean isValidLeaveTypeId(Integer leaveTypeId) {
        String sql = "SELECT COUNT(*) FROM leavetype WHERE leaveTypeId = ?";
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, leaveTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error validating leave type ID: " + e.getMessage());
            return false;
        }
    }
    
    // UTILITY METHODS
    
    /**
     * Generic query execution method
     * @param sql The SQL query to execute
     * @param params Parameters for the query
     * @return List of result maps
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
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
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * Refreshes cached reference data (if you implement caching later)
     */
    public void refreshCache() {
        // Placeholder for cache refresh logic
        System.out.println("Reference data cache refreshed");
    }
}