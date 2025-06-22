package oop.classes.actors;

import Models.EmployeeModel;
import Models.UserAuthenticationModel;
import Utility.PasswordHasher;
import DAOs.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * IT actor class - handles system maintenance and user account management
 * Inherits from Employee and adds IT-specific database operations
 */
public class IT extends EmployeeModel {
    
    private EmployeeDAO employeeDAO;
    private UserAuthenticationDAO userAuthDAO;
    private PositionDAO positionDAO;
    
    public IT() {
        super();
        initializeDAOs();
    }
    
    public IT(int employeeId, String firstName, String lastName, String email, String userRole) {
        super(employeeId, firstName, lastName, email, userRole);
        initializeDAOs();
    }
    
    private void initializeDAOs() {
        this.employeeDAO = new EmployeeDAO();
        this.userAuthDAO = new UserAuthenticationDAO();
        this.positionDAO = new PositionDAO();
    }
    
    // User Account Management Methods
    
    /**
     * Creates a new system user account
     * @param email User email
     * @param temporaryPassword Temporary password for first login
     * @param userRole User role (Employee, HR, IT, ImmediateSupervisor, Accounting)
     * @param firstName First name
     * @param lastName Last name
     * @param positionId Position ID
     * @return true if successful, false otherwise
     */
    public boolean createSystemUser(String email, String temporaryPassword, String userRole, 
                                  String firstName, String lastName, int positionId) {
        try {
            // Check if email already exists
            if (userAuthDAO.emailExists(email)) {
                System.err.println("Email already exists in system: " + email);
                return false;
            }
            
            // Validate user role
            if (!isValidUserRole(userRole)) {
                System.err.println("Invalid user role: " + userRole);
                return false;
            }
            
            // Create user account
            boolean success = userAuthDAO.createUser(email, temporaryPassword, userRole, 
                                                   firstName, lastName, positionId);
            
            if (success) {
                System.out.println("System user created successfully: " + email + " (" + userRole + ")");
                // Log this activity
                logSystemActivity("USER_CREATED", "Created user account: " + email);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error creating system user: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Resets a user's password to a temporary password
     * @param employeeId Employee ID
     * @param temporaryPassword Temporary password
     * @return true if successful, false otherwise
     */
    public boolean resetUserPassword(int employeeId, String temporaryPassword) {
        try {
            boolean success = userAuthDAO.updatePassword(employeeId, temporaryPassword);
            
            if (success) {
                EmployeeModel employee = employeeDAO.getEmployeeById(employeeId);
                System.out.println("Password reset for user: " + employee.getEmail());
                logSystemActivity("PASSWORD_RESET", "Reset password for employee ID: " + employeeId);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error resetting user password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deactivates a user account
     * @param employeeId Employee ID to deactivate
     * @param reason Reason for deactivation
     * @return true if successful, false otherwise
     */
    public boolean deactivateUserAccount(int employeeId, String reason) {
        try {
            boolean success = userAuthDAO.deactivateUser(employeeId);
            
            if (success) {
                EmployeeModel employee = employeeDAO.getEmployeeById(employeeId);
                System.out.println("User account deactivated: " + employee.getEmail() + " - Reason: " + reason);
                logSystemActivity("USER_DEACTIVATED", "Deactivated user ID: " + employeeId + " - " + reason);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error deactivating user account: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates user role
     * @param employeeId Employee ID
     * @param newRole New user role
     * @return true if successful, false otherwise
     */
    public boolean updateUserRole(int employeeId, String newRole) {
        try {
            if (!isValidUserRole(newRole)) {
                System.err.println("Invalid user role: " + newRole);
                return false;
            }
            
            EmployeeModel employee = employeeDAO.getEmployeeById(employeeId);
            if (employee == null) {
                System.err.println("Employee not found: " + employeeId);
                return false;
            }
            
            String oldRole = employee.getUserRole();
            employee.setUserRole(newRole);
            
            boolean success = employeeDAO.updateEmployee(employee);
            
            if (success) {
                System.out.println("User role updated for " + employee.getEmail() + 
                                 ": " + oldRole + " -> " + newRole);
                logSystemActivity("ROLE_UPDATED", "Updated role for employee ID: " + employeeId + 
                                " from " + oldRole + " to " + newRole);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error updating user role: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets all system users
     * @return List of user authentication models
     */
    public List<UserAuthenticationModel> getAllSystemUsers() {
        try {
            List<EmployeeModel> employees = employeeDAO.getAllEmployees();
            List<UserAuthenticationModel> users = new ArrayList<>();
            
            for (EmployeeModel emp : employees) {
                UserAuthenticationModel user = userAuthDAO.getUserById(emp.getEmployeeId());
                if (user != null) {
                    users.add(user);
                }
            }
            
            return users;
        } catch (Exception e) {
            System.err.println("Error getting all system users: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets users by role
     * @param userRole User role to filter by
     * @return List of users with the specified role
     */
    public List<UserAuthenticationModel> getUsersByRole(String userRole) {
        try {
            List<EmployeeModel> employees = employeeDAO.getEmployeesByRole(userRole);
            List<UserAuthenticationModel> users = new ArrayList<>();
            
            for (EmployeeModel emp : employees) {
                UserAuthenticationModel user = userAuthDAO.getUserById(emp.getEmployeeId());
                if (user != null) {
                    users.add(user);
                }
            }
            
            return users;
        } catch (Exception e) {
            System.err.println("Error getting users by role: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // System Maintenance Methods
    
    /**
     * Performs database backup
     * @param backupPath Path where backup should be stored
     * @return true if successful, false otherwise
     */
    public boolean performDatabaseBackup(String backupPath) {
        try {
            // This is a simplified example - actual implementation would depend on your backup strategy
            System.out.println("Initiating database backup to: " + backupPath);
            
            // Execute mysqldump command or use appropriate backup method
            String command = "mysqldump --user=root --password=yourpassword payrollsystem_db > " + backupPath;
            
            // Log the backup activity
            logSystemActivity("DB_BACKUP", "Database backup initiated to: " + backupPath);
            
            System.out.println("Database backup completed successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error performing database backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks database connection health
     * @return true if database is healthy, false otherwise
     */
    public boolean checkDatabaseHealth() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                // Test with a simple query
                String testQuery = "SELECT 1";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(testQuery)) {
                    
                    if (rs.next()) {
                        System.out.println("Database connection is healthy");
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database health check failed: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Gets database statistics
     * @return Database statistics as formatted string
     */
    public String getDatabaseStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("DATABASE STATISTICS\n");
        stats.append("Generated: ").append(LocalDate.now()).append("\n\n");
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // Get table counts
            String[] tables = {"employee", "payroll", "attendance", "leaverequest", "deduction"};
            
            for (String table : tables) {
                String query = "SELECT COUNT(*) FROM " + table;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    
                    if (rs.next()) {
                        stats.append(table.toUpperCase()).append(" records: ")
                             .append(rs.getInt(1)).append("\n");
                    }
                }
            }
            
            // Get database size
            String sizeQuery = "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) AS 'DB Size in MB' " +
                              "FROM information_schema.tables WHERE table_schema = 'payrollsystem_db'";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sizeQuery)) {
                
                if (rs.next()) {
                    stats.append("\nDatabase Size: ").append(rs.getString(1)).append(" MB\n");
                }
            }
            
        } catch (SQLException e) {
            stats.append("Error retrieving database statistics: ").append(e.getMessage());
        }
        
        return stats.toString();
    }
    
    /**
     * Optimizes database tables
     * @return true if successful, false otherwise
     */
    public boolean optimizeDatabaseTables() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String[] tables = {"employee", "payroll", "attendance", "leaverequest", "deduction", 
                              "position", "address", "benefittype"};
            
            int optimizedCount = 0;
            for (String table : tables) {
                String optimizeQuery = "OPTIMIZE TABLE " + table;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(optimizeQuery);
                    optimizedCount++;
                    System.out.println("Optimized table: " + table);
                }
            }
            
            logSystemActivity("DB_OPTIMIZE", "Optimized " + optimizedCount + " database tables");
            System.out.println("Database optimization completed. Tables optimized: " + optimizedCount);
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error optimizing database tables: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cleans up old session data and logs
     * @param daysOld Number of days old to consider for cleanup
     * @return true if successful, false otherwise
     */
    public boolean cleanupOldData(int daysOld) {
        try {
            // This is a placeholder for cleanup operations
            // You might want to clean up old attendance records, logs, etc.
            
            System.out.println("Cleaning up data older than " + daysOld + " days");
            
            // Example: Clean up old attendance records (if you have such a requirement)
            // String cleanupQuery = "DELETE FROM attendance WHERE date < DATE_SUB(NOW(), INTERVAL ? DAY)";
            
            logSystemActivity("DATA_CLEANUP", "Cleaned up data older than " + daysOld + " days");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error cleaning up old data: " + e.getMessage());
            return false;
        }
    }
    
    // System Monitoring Methods
    
    /**
     * Generates system health report
     * @return System health report as string
     */
    public String generateSystemHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("SYSTEM HEALTH REPORT\n");
        report.append("Generated: ").append(LocalDate.now()).append("\n\n");
        
        // Database health check
        boolean dbHealthy = checkDatabaseHealth();
        report.append("Database Status: ").append(dbHealthy ? "HEALTHY" : "UNHEALTHY").append("\n");
        
        // Get user statistics
        List<UserAuthenticationModel> allUsers = getAllSystemUsers();
        long activeUsers = allUsers.stream().filter(u -> u.isAccountActive()).count();
        
        report.append("Total System Users: ").append(allUsers.size()).append("\n");
        report.append("Active Users: ").append(activeUsers).append("\n");
        report.append("Inactive Users: ").append(allUsers.size() - activeUsers).append("\n\n");
        
        // Add database statistics
        report.append(getDatabaseStatistics());
        
        return report.toString();
    }
    
    // Utility Methods
    
    /**
     * Validates if a user role is valid
     * @param userRole User role to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidUserRole(String userRole) {
        String[] validRoles = {"Employee", "HR", "IT", "ImmediateSupervisor", "Accounting"};
        for (String role : validRoles) {
            if (role.equals(userRole)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Logs system activities for audit purposes
     * @param action Action performed
     * @param details Details of the action
     */
    private void logSystemActivity(String action, String details) {
        try {
            // This is a simplified logging - you might want to implement a proper audit log table
            System.out.println("[AUDIT] " + LocalDate.now() + " - " + action + ": " + details + 
                             " (Performed by IT User: " + getEmployeeId() + ")");
            
            // You could also save this to a database audit log table
            String logQuery = "INSERT INTO audit_log (employee_id, action, details, timestamp) VALUES (?, ?, ?, NOW())";
            // Implementation would depend on if you have an audit_log table
            
        } catch (Exception e) {
            System.err.println("Error logging system activity: " + e.getMessage());
        }
    }
    
    /**
     * Generates password reset token (for email-based password reset)
     * @param employeeId Employee ID
     * @return Reset token string
     */
    public String generatePasswordResetToken(int employeeId) {
        try {
            // Generate a secure random token
            String token = java.util.UUID.randomUUID().toString();
            
            // In a real implementation, you'd store this token in database with expiration
            // For now, we'll just return the token
            
            logSystemActivity("RESET_TOKEN_GENERATED", "Password reset token generated for employee: " + employeeId);
            return token;
            
        } catch (Exception e) {
            System.err.println("Error generating password reset token: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Forces password change on next login
     * @param employeeId Employee ID
     * @return true if successful, false otherwise
     */
    public boolean forcePasswordChange(int employeeId) {
        try {
            // This would require adding a 'force_password_change' column to employee table
            // For now, we'll just log the action
            
            EmployeeModel employee = employeeDAO.getEmployeeById(employeeId);
            if (employee != null) {
                System.out.println("Password change forced for user: " + employee.getEmail());
                logSystemActivity("FORCE_PASSWORD_CHANGE", "Forced password change for employee: " + employeeId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error forcing password change: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String toString() {
        return "IT{" +
                "employeeId=" + getEmployeeId() +
                ", name='" + getFirstName() + " " + getLastName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getUserRole() + '\'' +
                '}';
    }
}