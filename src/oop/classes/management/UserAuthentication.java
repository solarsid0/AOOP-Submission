package oop.classes.management;

import DAOs.UserAuthenticationDAO;
import DAOs.EmployeeDAO;
import DAOs.DatabaseConnection;
import Models.UserAuthenticationModel;
import Models.EmployeeModel;
import oop.classes.actors.*;

/** Enhanced UserAuthentication class for MySQL/JDBC system
 * Handles user authentication by validating credentials from the database
 * and creates appropriate user role objects
 * @author chadley
 */

public class UserAuthentication {
  
    private final UserAuthenticationDAO userAuthDAO;
    private final EmployeeDAO employeeDAO;
    private final DatabaseConnection databaseConnection;

    /**
     * Constructor for UserAuthentication
     */
    public UserAuthentication() {
        this.databaseConnection = new DatabaseConnection();
        this.userAuthDAO = new UserAuthenticationDAO();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
    }

    /**
     * Constructor with custom database connection
     * @param databaseConnection Custom database connection
     */
    public UserAuthentication(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.userAuthDAO = new UserAuthenticationDAO();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
    }

    /**
     * Validates user credentials and returns appropriate User object
     * @param email User's email address
     * @param password User's password
     * @return User object if authentication successful, null otherwise
     */
    public Object validateCredentials(String email, String password) {
        try {
            // Use your existing UserAuthenticationDAO.authenticateUser method
            UserAuthenticationModel authUser = userAuthDAO.authenticateUser(email, password);
            
            if (authUser == null) {
                System.out.println("Invalid credentials for email: " + email);
                return null;
            }

            // Check if account is active (already done in authenticateUser, but double-check)
            if (!authUser.isAccountActive()) {
                System.out.println("Account is deactivated for email: " + email);
                return null;
            }

            // Get employee details
            EmployeeModel employee = employeeDAO.findById(authUser.getEmployeeId());
            
            if (employee == null) {
                System.err.println("Employee record not found for ID: " + authUser.getEmployeeId());
                return null;
            }

            // Create appropriate user object based on role
            return createUserFromEmployee(employee, authUser.getUserRole());

        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates the appropriate User object based on employee data and role
     * @param employee Employee model with employee details
     * @param userRole User's system role
     * @return Specific User object based on role
     */
    private Object createUserFromEmployee(EmployeeModel employee, String userRole) {
        
        // Validate required fields
        if (employee.getEmployeeId() == null || 
            employee.getFirstName() == null || employee.getFirstName().isEmpty() ||
            employee.getLastName() == null || employee.getLastName().isEmpty() ||
            employee.getEmail() == null || employee.getEmail().isEmpty()) {
            
            throw new IllegalArgumentException("Missing required employee fields");
        }

        int employeeId = employee.getEmployeeId();
        String firstName = employee.getFirstName();
        String lastName = employee.getLastName();
        String email = employee.getEmail();
        
        // Normalize user role
        userRole = normalizeUserRole(userRole);
        
        System.out.println("Creating user object for: " + firstName + " " + lastName + 
                          " with role: " + userRole);

        // Create appropriate User object based on role
        switch (userRole.toUpperCase()) {
            case "HR":
                return new HR(employeeId, firstName, lastName, email, userRole);
                
            case "IT":
                return new IT(employeeId, firstName, lastName, email, userRole);
                
            case "ACCOUNTING":
                return new Accounting(employeeId, firstName, lastName, email, userRole);
                
            case "IMMEDIATESUPERVISOR":
            case "IMMEDIATE_SUPERVISOR":
            case "SUPERVISOR":
                // Create supervisor without department for now (you can add department later)
                return new ImmediateSupervisor(employeeId, firstName, lastName, email, userRole);
                
            case "EMPLOYEE":
            default:
                // For regular employees or unrecognized roles, create Employee object
                return new Employee(employeeId, firstName, lastName, email, "defaultPassword", "Employee");
        }
    }

    /**
     * Normalizes user role string to standard format
     * @param userRole Raw user role from database
     * @return Normalized user role
     */
    private String normalizeUserRole(String userRole) {
        if (userRole == null || userRole.trim().isEmpty()) {
            return "EMPLOYEE"; // Default role
        }
        
        // Clean up the role string
        userRole = userRole.trim().toUpperCase();
        
        // Handle common variations
        switch (userRole) {
            case "IMMEDIATE SUPERVISOR":
            case "IMMEDIATE_SUPERVISOR":
            case "IMMEDIATESUPERVISOR":
            case "SUPERVISOR":
                return "IMMEDIATESUPERVISOR";
                
            case "HUMAN RESOURCES":
            case "HR":
                return "HR";
                
            case "INFORMATION TECHNOLOGY":
            case "IT":
                return "IT";
                
            case "ACCOUNTING":
            case "PAYROLL":
                return "ACCOUNTING";
                
            default:
                return "EMPLOYEE";
        }
    }

    /**
     * Determines user role based on job position (for legacy compatibility)
     * @param position Job position/title
     * @return Appropriate user role
     */
    public String determineUserRoleFromPosition(String position) {
        if (position == null || position.trim().isEmpty()) {
            return "EMPLOYEE";
        }
        
        position = position.trim().toLowerCase();
        
        // Executive positions - Immediate Supervisors
        if (position.contains("chief") || position.contains("ceo") || position.contains("coo") ||
            position.contains("cfo") || position.contains("cmo") || position.contains("manager") ||
            position.contains("team leader") || position.contains("supervisor")) {
            return "IMMEDIATESUPERVISOR";
        }
        
        // IT positions
        else if (position.contains("it") || position.contains("information tech") || 
                position.contains("system") || position.contains("developer") ||
                position.contains("programmer")) {
            return "IT";
        }
        
        // HR positions
        else if (position.contains("hr") || position.contains("human resource")) {
            return "HR";
        }
        
        // Accounting positions
        else if (position.contains("accounting") || position.contains("payroll") || 
                position.contains("finance") || position.contains("bookkeep")) {
            return "ACCOUNTING";
        }
        
        // Default to employee
        else {
            return "EMPLOYEE";
        }
    }

    /**
     * Validates if user has permission to access specific role features
     * @param user User object to check
     * @param requiredRole Required role for access
     * @return true if user has required permissions
     */
    public boolean hasRolePermission(User user, String requiredRole) {
        if (user == null || requiredRole == null) {
            return false;
        }
        
        String userRole = user.getRole();
        if (userRole == null) {
            return false;
        }
        
        // Normalize roles for comparison
        userRole = normalizeUserRole(userRole);
        requiredRole = normalizeUserRole(requiredRole);
        
        return userRole.equals(requiredRole);
    }

    /**
     * Gets user by employee ID (for system operations)
     * @param employeeId Employee ID
     * @return User object if found, null otherwise
     */
    public Object getUserByEmployeeId(Integer employeeId) {
        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                return null;
            }
            
            // Get user role from authentication table using your existing method
            UserAuthenticationModel authUser = userAuthDAO.getUserById(employeeId);
            if (authUser == null) {
                // Fallback to employee model role if auth user not found
                String userRole = employee.getUserRole() != null ? employee.getUserRole() : "EMPLOYEE";
                return createUserFromEmployee(employee, userRole);
            }
            
            return createUserFromEmployee(employee, authUser.getUserRole());
            
        } catch (Exception e) {
            System.err.println("Error getting user by employee ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if email exists in the system
     * @param email Email to check
     * @return true if email exists
     */
    public boolean emailExists(String email) {
        return userAuthDAO.emailExists(email);
    }

    /**
     * Updates user password
     * @param employeeId Employee ID
     * @param newPassword New password
     * @return true if update successful
     */
    public boolean updatePassword(Integer employeeId, String newPassword) {
        return userAuthDAO.updatePassword(employeeId, newPassword);
    }

    /**
     * Deactivates user account
     * @param employeeId Employee ID
     * @return true if deactivation successful
     */
    public boolean deactivateUser(Integer employeeId) {
        return userAuthDAO.deactivateUser(employeeId);
    }

    /**
     * Logs authentication attempt for audit purposes
     * @param email Email used for login
     * @param success Whether login was successful
     * @param ipAddress Optional IP address
     */
    private void logAuthenticationAttempt(String email, boolean success, String ipAddress) {
        try {
            String logMessage = String.format("[AUTH] %s - Email: %s, Success: %s, IP: %s",
                java.time.LocalDateTime.now(), email, success, ipAddress != null ? ipAddress : "Unknown");
            System.out.println(logMessage);
            
            // In a real implementation, you'd save this to an audit log table
            // auditLogDAO.saveAuthLog(email, success, ipAddress, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("Error logging authentication attempt: " + e.getMessage());
        }
    }

    /**
     * Closes database connections
     */
    public void close() {
        try {
            if (databaseConnection != null) {
                // Close any open connections if needed
            }
        } catch (Exception e) {
            System.err.println("Error closing authentication resources: " + e.getMessage());
        }
    }
}