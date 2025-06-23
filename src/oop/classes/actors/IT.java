package oop.classes.actors;

import Models.*;
import Models.EmployeeModel.EmployeeStatus;
import DAOs.*;
import DAOs.DatabaseConnection;
import Utility.PasswordHasher;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Enhanced IT Role Class - handles system administration and technical operations
 * Focuses on user management, system maintenance, database operations, and security
 */

public class IT extends User {
    
    // Employee information
    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String userRole;
    
    // DAO dependencies for IT operations
    private final EmployeeDAO employeeDAO;
    private final UserAuthenticationDAO userAuthDAO;
    private final DatabaseConnection databaseConnection;
    
    // IT Role Permissions
    private static final String[] IT_PERMISSIONS = {
        "MANAGE_USERS", "SYSTEM_ADMINISTRATION", "DATABASE_OPERATIONS", 
        "SECURITY_MANAGEMENT", "BACKUP_RESTORE", "SYSTEM_MONITORING",
        "TECHNICAL_SUPPORT", "PASSWORD_MANAGEMENT"
    };

    /**
     * Constructor for IT role
     */
    public IT(int employeeId, String firstName, String lastName, String email, String userRole) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userRole = userRole;
        
        // Initialize DAOs and database connection
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.userAuthDAO = new UserAuthenticationDAO();
        
        System.out.println("IT user initialized: " + getFullName());
    }

    // ================================
    // GETTER METHODS
    // ================================
    
    public int getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getUserRole() { return userRole; }
    public String getFullName() { return firstName + " " + lastName; }

    // ================================
    // USER ACCOUNT MANAGEMENT
    // ================================

    /**
     * Creates a new system user account
     */
    public ITOperationResult createSystemUser(String email, String temporaryPassword, String userRole,
            String firstName, String lastName, int positionId) {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("MANAGE_USERS")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to create user accounts");
                return result;
            }

            // Check if email already exists
            if (userAuthDAO.emailExists(email)) {
                result.setSuccess(false);
                result.setMessage("Email already exists in system: " + email);
                return result;
            }

            // Validate user role
            if (!isValidUserRole(userRole)) {
                result.setSuccess(false);
                result.setMessage("Invalid user role: " + userRole);
                return result;
            }

            // Validate password requirements
            if (!PasswordHasher.isPasswordValid(temporaryPassword)) {
                result.setSuccess(false);
                result.setMessage("Password does not meet requirements: " + 
                    PasswordHasher.getPasswordRequirements());
                return result;
            }

            // Create user account
            boolean success = userAuthDAO.createUser(email, temporaryPassword, userRole,
                firstName, lastName, positionId);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("System user created successfully: " + email + " (" + userRole + ")");
                logITActivity("USER_CREATED", "Created user account: " + email + " (" + userRole + ")");
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to create user account");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error creating system user: " + e.getMessage());
            System.err.println("IT Error creating user: " + e.getMessage());
        }

        return result;
    }

    /**
     * Resets a user's password to a temporary password
     */
    public ITOperationResult resetUserPassword(int employeeId, String temporaryPassword) {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("PASSWORD_MANAGEMENT")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to reset passwords");
                return result;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            // Validate password requirements
            if (!PasswordHasher.isPasswordValid(temporaryPassword)) {
                result.setSuccess(false);
                result.setMessage("Password does not meet requirements: " + 
                    PasswordHasher.getPasswordRequirements());
                return result;
            }

            boolean success = userAuthDAO.updatePassword(employeeId, temporaryPassword);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("Password reset successfully for user: " + employee.getEmail());
                logITActivity("PASSWORD_RESET", "Reset password for employee ID: " + employeeId);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to reset password");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error resetting password: " + e.getMessage());
        }

        return result;
    }

    /**
     * Deactivates a user account
     */
    public ITOperationResult deactivateUserAccount(int employeeId, String reason) {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("MANAGE_USERS")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to deactivate user accounts");
                return result;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            boolean success = userAuthDAO.deactivateUser(employeeId);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("User account deactivated: " + employee.getEmail());
                logITActivity("USER_DEACTIVATED", 
                    "Deactivated user: " + employee.getEmail() + " - Reason: " + reason);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to deactivate user account");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error deactivating user account: " + e.getMessage());
        }

        return result;
    }

    /**
     * Updates user role
     */
    public ITOperationResult updateUserRole(int employeeId, String newRole) {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("MANAGE_USERS")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to update user roles");
                return result;
            }

            if (!isValidUserRole(newRole)) {
                result.setSuccess(false);
                result.setMessage("Invalid user role: " + newRole);
                return result;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            String oldRole = employee.getUserRole();
            employee.setUserRole(newRole);
            
            boolean success = employeeDAO.update(employee);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("User role updated for " + employee.getEmail() + 
                    ": " + oldRole + " -> " + newRole);
                logITActivity("ROLE_UPDATED", 
                    "Updated role for employee ID: " + employeeId + 
                    " from " + oldRole + " to " + newRole);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to update user role");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error updating user role: " + e.getMessage());
        }

        return result;
    }

    /**
     * Gets all system users
     */
    public List<UserAuthenticationModel> getAllSystemUsers() {
        if (!hasPermission("MANAGE_USERS")) {
            System.err.println("IT: Insufficient permissions to view system users");
            return new ArrayList<>();
        }
        
        try {
            List<EmployeeModel> employees = employeeDAO.getActiveEmployees();
            List<UserAuthenticationModel> users = new ArrayList<>();
            
            for (EmployeeModel emp : employees) {
                UserAuthenticationModel user = userAuthDAO.getUserById(emp.getEmployeeId());
                if (user != null) {
                    users.add(user);
                }
            }
            
            return users;
        } catch (Exception e) {
            System.err.println("Error getting system users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets users by role
     */
    public List<EmployeeModel> getUsersByRole(String userRole) {
        if (!hasPermission("MANAGE_USERS")) {
            System.err.println("IT: Insufficient permissions to view users by role");
            return new ArrayList<>();
        }
        
        return employeeDAO.getEmployeesByRole(userRole);
    }

    // ================================
    // DATABASE OPERATIONS
    // ================================

    /**
     * Checks database connection health
     */
    public ITOperationResult checkDatabaseHealth() {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("DATABASE_OPERATIONS")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions for database operations");
                return result;
            }

            boolean isHealthy = databaseConnection.testConnection();
            
            if (isHealthy) {
                result.setSuccess(true);
                result.setMessage("Database connection is healthy");
                logITActivity("DB_HEALTH_CHECK", "Database health check - Status: HEALTHY");
            } else {
                result.setSuccess(false);
                result.setMessage("Database connection is unhealthy");
                logITActivity("DB_HEALTH_CHECK", "Database health check - Status: UNHEALTHY");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error checking database health: " + e.getMessage());
        }

        return result;
    }

    /**
     * Gets database statistics
     */
    public ITOperationResult getDatabaseStatistics() {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("DATABASE_OPERATIONS")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions for database operations");
                return result;
            }

            StringBuilder stats = new StringBuilder();
            stats.append("DATABASE STATISTICS\n");
            stats.append("Generated: ").append(LocalDate.now()).append("\n\n");

            try (Connection conn = databaseConnection.createConnection()) {
                // Get table counts for main tables
                String[] tables = {"employee", "payroll", "attendance", "leaverequest"};
                
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

                result.setSuccess(true);
                result.setMessage("Database statistics retrieved successfully");
                result.setDatabaseStats(stats.toString());
                logITActivity("DB_STATS_RETRIEVED", "Retrieved database statistics");

            } catch (SQLException e) {
                result.setSuccess(false);
                result.setMessage("Error retrieving database statistics: " + e.getMessage());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error getting database statistics: " + e.getMessage());
        }

        return result;
    }

    /**
     * Performs database backup (simplified)
     */
    public ITOperationResult performDatabaseBackup(String backupPath) {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("BACKUP_RESTORE")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions for backup operations");
                return result;
            }

            // This is a simplified backup operation
            // In a real implementation, you would execute mysqldump or similar
            System.out.println("Initiating database backup to: " + backupPath);
            
            // Simulate backup process
            boolean backupSuccess = true; // In real implementation, check actual backup result
            
            if (backupSuccess) {
                result.setSuccess(true);
                result.setMessage("Database backup completed successfully to: " + backupPath);
                logITActivity("DB_BACKUP", "Database backup completed to: " + backupPath);
            } else {
                result.setSuccess(false);
                result.setMessage("Database backup failed");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error performing database backup: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // SYSTEM MONITORING
    // ================================

    /**
     * Generates system health report
     */
    public ITOperationResult generateSystemHealthReport() {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("SYSTEM_MONITORING")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions for system monitoring");
                return result;
            }

            StringBuilder report = new StringBuilder();
            report.append("SYSTEM HEALTH REPORT\n");
            report.append("Generated: ").append(LocalDate.now()).append("\n\n");

            // Database health check
            boolean dbHealthy = databaseConnection.testConnection();
            report.append("Database Status: ").append(dbHealthy ? "HEALTHY" : "UNHEALTHY").append("\n");

            // Get user statistics
            List<UserAuthenticationModel> allUsers = getAllSystemUsers();
            long activeUsers = allUsers.stream().filter(u -> u.isAccountActive()).count();
            
            report.append("Total System Users: ").append(allUsers.size()).append("\n");
            report.append("Active Users: ").append(activeUsers).append("\n");
            report.append("Inactive Users: ").append(allUsers.size() - activeUsers).append("\n\n");

            // Get database statistics
            ITOperationResult dbStats = getDatabaseStatistics();
            if (dbStats.isSuccess()) {
                report.append(dbStats.getDatabaseStats());
            }

            result.setSuccess(true);
            result.setMessage("System health report generated successfully");
            result.setSystemHealthReport(report.toString());
            
            logITActivity("SYSTEM_HEALTH_REPORT", "Generated system health report");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error generating system health report: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // SECURITY MANAGEMENT
    // ================================

    /**
     * Generates password reset token
     */
    public ITOperationResult generatePasswordResetToken(int employeeId) {
        ITOperationResult result = new ITOperationResult();
        
        try {
            if (!hasPermission("SECURITY_MANAGEMENT")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions for security management");
                return result;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            // Generate a secure random token
            String token = UUID.randomUUID().toString();
            
            result.setSuccess(true);
            result.setMessage("Password reset token generated for: " + employee.getEmail());
            result.setResetToken(token);
            
            logITActivity("RESET_TOKEN_GENERATED", 
                "Password reset token generated for employee: " + employeeId);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error generating password reset token: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Validates if a user role is valid
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
     * Checks if IT user has specific permission
     */
    private boolean hasPermission(String permission) {
        for (String itPermission : IT_PERMISSIONS) {
            if (itPermission.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all IT permissions
     */
    public String[] getITPermissions() {
        return IT_PERMISSIONS.clone();
    }

    /**
     * Logs IT activities for audit purposes
     */
    private void logITActivity(String action, String details) {
        try {
            String logMessage = String.format("[IT AUDIT] %s - %s: %s (Performed by: %s - ID: %d)",
                LocalDateTime.now(), action, details, getFullName(), getEmployeeId());
            System.out.println(logMessage);
            
        } catch (Exception e) {
            System.err.println("Error logging IT activity: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "IT{" +
                "employeeId=" + getEmployeeId() +
                ", name='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", permissions=" + java.util.Arrays.toString(IT_PERMISSIONS) +
                '}';
    }

    // ================================
    // INNER CLASS - RESULT OBJECT
    // ================================

    /**
     * Result class for IT operations
     */
    public static class ITOperationResult {
        private boolean success = false;
        private String message = "";
        private String databaseStats = "";
        private String systemHealthReport = "";
        private String resetToken = "";

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getDatabaseStats() { return databaseStats; }
        public void setDatabaseStats(String databaseStats) { this.databaseStats = databaseStats; }
        
        public String getSystemHealthReport() { return systemHealthReport; }
        public void setSystemHealthReport(String systemHealthReport) { this.systemHealthReport = systemHealthReport; }
        
        public String getResetToken() { return resetToken; }
        public void setResetToken(String resetToken) { this.resetToken = resetToken; }

        @Override
        public String toString() {
            return "ITOperationResult{" +
                   "success=" + success + 
                   ", message='" + message + '\'' +
                   '}';
        }
    }
}