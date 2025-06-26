package Models;

import DAOs.EmployeeDAO;
import DAOs.DatabaseConnection;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * UserAuthenticationModel class that handles both data model and authentication logic
 * Works with your database schema (employee table for authentication)
 * Supports all user roles: Employee, HR, IT, Accounting, ImmediateSupervisor
 * @author chadley
 */
public class UserAuthenticationModel {
    
    // Data fields (session management - in-memory only)
    private int employeeId;
    private String email;
    private String passwordHash;
    private String userRole;
    private String status;
    private LocalDateTime lastLogin;
    private String sessionToken;
    private LocalDateTime sessionExpiry;
    private boolean isLoggedIn;
    private int loginAttempts;
    private LocalDateTime lastLoginAttempt;
    
    // Additional fields for user details
    private String firstName;
    private String lastName;
    private Object userObject; // Will hold the specific Model type (HRModel, ITModel, EmployeeModel, etc.)
    
    // Database components (using only EmployeeDAO since authentication is in employee table)
    private final EmployeeDAO employeeDAO;
    private final DatabaseConnection databaseConnection;
    
    // Constructors
    public UserAuthenticationModel() {
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.isLoggedIn = false;
        this.loginAttempts = 0;
    }
    
    public UserAuthenticationModel(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.isLoggedIn = false;
        this.loginAttempts = 0;
    }
    
    // =========================
    // GUI INTERACTION METHODS
    // =========================
    
    /**
     * Main login method for GUI - validates credentials and sets up user session
     * Works with employee table authentication
     * @param email User's email
     * @param password User's password (plain text)
     * @return true if login successful, false otherwise
     */
    public boolean login(String email, String password) {
        try {
            // Check if account is locked (in-memory tracking)
            if (isAccountLocked()) {
                System.out.println("Account is locked. Please try again later.");
                return false;
            }
            
            // Find employee by email (authentication is in employee table)
            EmployeeModel foundEmployee = employeeDAO.findByEmail(email);
            
            if (foundEmployee == null) {
                incrementLoginAttempts();
                System.out.println("Invalid credentials for email: " + email);
                return false;
            }
            
            // Verify password hash
            String hashedInputPassword = hashPassword(password);
            if (!hashedInputPassword.equals(foundEmployee.getPasswordHash())) {
                incrementLoginAttempts();
                System.out.println("Invalid password for email: " + email);
                return false;
            }
            
            // Check if account is active (using employee status)
            if (!isEmployeeActive(foundEmployee)) {
                System.out.println("Account is deactivated for email: " + email);
                return false;
            }
            
            // Set all user data in this object from employee table
            this.employeeId = foundEmployee.getEmployeeId();
            this.email = foundEmployee.getEmail();
            this.passwordHash = foundEmployee.getPasswordHash();
            this.userRole = normalizeUserRole(foundEmployee.getUserRole());
            this.status = foundEmployee.getStatus().getValue(); // Convert enum to string
            this.firstName = foundEmployee.getFirstName();
            this.lastName = foundEmployee.getLastName();
            
            // Create appropriate Model object based on role
            this.userObject = createUserFromEmployee(foundEmployee, this.userRole);
            
            // Start session (in-memory only)
            String newSessionToken = generateSessionToken();
            startSession(newSessionToken, 480); // 8 hours session
            
            // Update last login in database
            foundEmployee.updateLastLogin();
            employeeDAO.update(foundEmployee);
            
            System.out.println("Login successful for: " + firstName + " " + lastName);
            return true;
            
        } catch (Exception e) {
            incrementLoginAttempts();
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Logout method for GUI
     */
    public void logout() {
        endSession();
        clearUserData();
        System.out.println("User logged out successfully");
    }
    
    /**
     * Check if user is currently logged in (for GUI state management)
     * @return true if user is logged in with valid session
     */
    public boolean isUserLoggedIn() {
        return isLoggedIn && isSessionValid();
    }
    
    /**
     * Get user's full name for GUI display
     * @return Full name or empty string if not logged in
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return "";
    }
    
    /**
     * Get user's display role for GUI
     * @return User role in readable format
     */
    public String getDisplayRole() {
        if (userRole == null) return "Guest";
        
        switch (userRole.toUpperCase()) {
            case "HR": return "Human Resources";
            case "IT": return "Information Technology";
            case "ACCOUNTING": return "Accounting";
            case "IMMEDIATESUPERVISOR": return "Supervisor";
            default: return "Employee";
        }
    }
    
    /**
     * Check if user has permission for specific actions (enhanced for all roles)
     * @param permission Required permission level
     * @return true if user has permission
     */
    public boolean hasPermission(String permission) {
        if (!isUserLoggedIn()) return false;
        
        switch (permission.toUpperCase()) {
            // Admin-level permissions
            case "ADMIN":
            case "SYSTEM_ADMINISTRATION":
                return isAdmin();
                
            // Supervisor-level permissions  
            case "SUPERVISOR":
            case "MANAGE_TEAM":
            case "APPROVE_TEAM_LEAVE":
            case "APPROVE_TEAM_OVERTIME":
                return isSupervisor();
                
            // Role-specific permissions
            case "HR_ACCESS":
            case "MANAGE_EMPLOYEES":
            case "PROCESS_PAYROLL":
            case "APPROVE_LEAVES":
            case "GENERATE_REPORTS":
                return "HR".equals(userRole);
                
            case "IT_ACCESS":
            case "MANAGE_USERS":
            case "DATABASE_OPERATIONS":
            case "SECURITY_MANAGEMENT":
                return "IT".equals(userRole);
                
            case "ACCOUNTING_ACCESS":
            case "VERIFY_PAYROLL":
            case "AUDIT_FINANCIAL_DATA":
            case "VIEW_PAYROLL_DATA":
                return "ACCOUNTING".equals(userRole);
                
            case "EMPLOYEE_ACCESS":
            case "VIEW_OWN_PAYROLL":
            case "REQUEST_LEAVE":
            case "VIEW_OWN_ATTENDANCE":
                return true; // All logged-in users have basic employee permissions
                
            default:
                return true; // Basic employee permissions
        }
    }
    
    /**
     * Get all available permissions for current user role
     * @return Array of permissions available to the current user
     */
    public String[] getAvailablePermissions() {
        if (!isUserLoggedIn()) return new String[0];
        
        switch (userRole.toUpperCase()) {
            case "HR":
                return new String[]{
                    "ADMIN", "MANAGE_EMPLOYEES", "PROCESS_PAYROLL", "APPROVE_LEAVES", 
                    "GENERATE_REPORTS", "HR_ACCESS", "EMPLOYEE_ACCESS"
                };
            case "IT":
                return new String[]{
                    "ADMIN", "MANAGE_USERS", "DATABASE_OPERATIONS", "SECURITY_MANAGEMENT",
                    "IT_ACCESS", "EMPLOYEE_ACCESS"
                };
            case "ACCOUNTING":
                return new String[]{
                    "VERIFY_PAYROLL", "AUDIT_FINANCIAL_DATA", "VIEW_PAYROLL_DATA",
                    "ACCOUNTING_ACCESS", "EMPLOYEE_ACCESS"
                };
            case "IMMEDIATESUPERVISOR":
                return new String[]{
                    "SUPERVISOR", "MANAGE_TEAM", "APPROVE_TEAM_LEAVE", "APPROVE_TEAM_OVERTIME",
                    "EMPLOYEE_ACCESS"
                };
            case "EMPLOYEE":
            default:
                return new String[]{
                    "EMPLOYEE_ACCESS", "VIEW_OWN_PAYROLL", "REQUEST_LEAVE", "VIEW_OWN_ATTENDANCE"
                };
        }
    }
    
    /**
     * Check if current user can perform action on target employee
     * @param targetEmployeeId The employee ID to perform action on
     * @param action The action to perform
     * @return true if action is allowed
     */
    public boolean canPerformActionOn(Integer targetEmployeeId, String action) {
        if (!isUserLoggedIn() || targetEmployeeId == null) return false;
        
        // Users can always perform actions on themselves
        if (targetEmployeeId.equals(this.employeeId)) {
            return true;
        }
        
        // Admin roles can perform actions on anyone
        if (isAdmin()) {
            return true;
        }
        
        // Supervisors can perform certain actions on their team members
        if (isSupervisor() && action != null) {
            switch (action.toUpperCase()) {
                case "APPROVE_LEAVE":
                case "APPROVE_OVERTIME":
                case "VIEW_ATTENDANCE":
                case "MANAGE_PERFORMANCE":
                    // In a real implementation, you'd check if targetEmployeeId is supervised by this user
                    return true; // Simplified for now
                default:
                    return false;
            }
        }
        
        return false;
    }
    
    // =========================
    // GETTERS AND SETTERS
    // =========================
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    
    public LocalDateTime getSessionExpiry() { return sessionExpiry; }
    public void setSessionExpiry(LocalDateTime sessionExpiry) { this.sessionExpiry = sessionExpiry; }
    
    public boolean isLoggedIn() { return isLoggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.isLoggedIn = loggedIn; }
    
    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }
    
    public LocalDateTime getLastLoginAttempt() { return lastLoginAttempt; }
    public void setLastLoginAttempt(LocalDateTime lastLoginAttempt) { this.lastLoginAttempt = lastLoginAttempt; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public Object getUserObject() { return userObject; }
    public void setUserObject(Object userObject) { this.userObject = userObject; }
    
    // =========================
    // BUSINESS LOGIC METHODS
    // =========================
    
    public boolean isSessionValid() {
        if (sessionToken == null || sessionExpiry == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(sessionExpiry) && isLoggedIn;
    }
    
    public boolean isAccountLocked() {
        if (loginAttempts >= 5) {
            if (lastLoginAttempt != null) {
                return LocalDateTime.now().isBefore(lastLoginAttempt.plusMinutes(30));
            }
            return true;
        }
        return false;
    }
    
    public boolean isAccountActive() {
        // Since we're working with employee status directly, check if status allows login
        return "Regular".equals(status) || "Probationary".equals(status);
    }
    
    public void incrementLoginAttempts() {
        this.loginAttempts++;
        this.lastLoginAttempt = LocalDateTime.now();
    }
    
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lastLoginAttempt = null;
    }
    
    public void startSession(String sessionToken, int durationMinutes) {
        this.sessionToken = sessionToken;
        this.sessionExpiry = LocalDateTime.now().plusMinutes(durationMinutes);
        this.isLoggedIn = true;
        this.lastLogin = LocalDateTime.now();
        resetLoginAttempts();
    }
    
    public void endSession() {
        this.sessionToken = null;
        this.sessionExpiry = null;
        this.isLoggedIn = false;
    }
    
    public void extendSession(int additionalMinutes) {
        if (sessionExpiry != null) {
            this.sessionExpiry = this.sessionExpiry.plusMinutes(additionalMinutes);
        }
    }
    
    public boolean isAdmin() {
        return "HR".equals(userRole) || "IT".equals(userRole);
    }
    
    public boolean isSupervisor() {
        return "IMMEDIATESUPERVISOR".equals(userRole) || isAdmin();
    }
    
    public long getMinutesUntilExpiry() {
        if (sessionExpiry == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(sessionExpiry)) {
            return 0;
        }
        return java.time.Duration.between(now, sessionExpiry).toMinutes();
    }
    
    // =========================
    // HELPER METHODS FOR MODEL ACCESS
    // =========================
    
    /**
     * Get the specific user model object with proper type casting
     * @param <T> The expected model type
     * @param modelClass The class type to cast to
     * @return The user model object cast to the specified type, or null if not that type
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserModel(Class<T> modelClass) {
        if (userObject != null && modelClass.isInstance(userObject)) {
            return (T) userObject;
        }
        return null;
    }
    
    /**
     * Get the user as HRModel (convenience method)
     * @return HRModel if user is HR, null otherwise
     */
    public HRModel getAsHRModel() {
        return getUserModel(HRModel.class);
    }
    
    /**
     * Get the user as ITModel (convenience method)
     * @return ITModel if user is IT, null otherwise
     */
    public ITModel getAsITModel() {
        return getUserModel(ITModel.class);
    }
    
    /**
     * Get the user as AccountingModel (convenience method)
     * @return AccountingModel if user is Accounting, null otherwise
     */
    public AccountingModel getAsAccountingModel() {
        return getUserModel(AccountingModel.class);
    }
    
    /**
     * Get the user as ImmediateSupervisorModel (convenience method)
     * @return ImmediateSupervisorModel if user is supervisor, null otherwise
     */
    public ImmediateSupervisorModel getAsSupervisorModel() {
        return getUserModel(ImmediateSupervisorModel.class);
    }
    
    /**
     * Get the user as EmployeeModel (convenience method)
     * @return EmployeeModel - works for any role since all models extend EmployeeModel
     */
    public EmployeeModel getAsEmployeeModel() {
        if (userObject instanceof EmployeeModel) {
            return (EmployeeModel) userObject;
        }
        return null;
    }
    
    /**
     * Get the specific type of user model currently stored
     * @return String representation of the model type
     */
    public String getUserModelType() {
        if (userObject == null) return "None";
        return userObject.getClass().getSimpleName();
    }
    
    /**
     * Check if the current user has specific model capabilities
     * @param modelType The model type to check for ("HRModel", "ITModel", etc.)
     * @return true if user object is of the specified type
     */
    public boolean hasModelType(String modelType) {
        if (userObject == null) return false;
        return userObject.getClass().getSimpleName().equals(modelType);
    }
    
    // =========================
    // HELPER METHODS (Original UserAuthentication Logic)
    // =========================
    
    private Object createUserFromEmployee(EmployeeModel employee, String userRole) {
        if (employee.getEmployeeId() == null || 
            employee.getFirstName() == null || employee.getFirstName().isEmpty() ||
            employee.getLastName() == null || employee.getLastName().isEmpty() ||
            employee.getEmail() == null || employee.getEmail().isEmpty()) {
            
            throw new IllegalArgumentException("Missing required employee fields");
        }

        int empId = employee.getEmployeeId();
        String empFirstName = employee.getFirstName();
        String empLastName = employee.getLastName();
        String empEmail = employee.getEmail();
        
        try {
            switch (userRole.toUpperCase()) {
                case "HR":
                    // Option 1: Use constructor with parameters
                    return new HRModel(empId, empFirstName, empLastName, empEmail, userRole);
                    // Option 2: Use constructor with EmployeeModel (uncomment if preferred)
                    // return new HRModel(employee);
                    
                case "IT":
                    // Option 1: Use constructor with parameters
                    return new ITModel(empId, empFirstName, empLastName, empEmail, userRole);
                    // Option 2: Use constructor with EmployeeModel (uncomment if preferred)
                    // return new ITModel(employee);
                    
                case "ACCOUNTING":
                    // Option 1: Use constructor with parameters
                    return new AccountingModel(empId, empFirstName, empLastName, empEmail, userRole);
                    // Option 2: Use constructor with EmployeeModel (uncomment if preferred)
                    // return new AccountingModel(employee);
                    
                case "IMMEDIATESUPERVISOR":
                    // Option 1: Use constructor with parameters (without department)
                    return new ImmediateSupervisorModel(empId, empFirstName, empLastName, empEmail, userRole);
                    // Option 2: Use constructor with EmployeeModel (uncomment if preferred)
                    // return new ImmediateSupervisorModel(employee);
                    
                case "EMPLOYEE":
                default:
                    // Return the original EmployeeModel object
                    return employee;
            }
        } catch (Exception e) {
            System.err.println("Error creating user model for role: " + userRole + ". " + e.getMessage());
            e.printStackTrace();
            // Fallback to the original employee object
            return employee;
        }
    }
    
    private String normalizeUserRole(String userRole) {
        if (userRole == null || userRole.trim().isEmpty()) {
            return "EMPLOYEE";
        }
        
        userRole = userRole.trim().toUpperCase();
        
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
    
    private String generateSessionToken() {
        return java.util.UUID.randomUUID().toString();
    }
    
    private void clearUserData() {
        this.employeeId = 0;
        this.email = null;
        this.passwordHash = null;
        this.userRole = null;
        this.status = null;
        this.firstName = null;
        this.lastName = null;
        this.userObject = null;
    }
    
    /**
     * Simple password hashing (you should use a proper password hashing library like BCrypt)
     * @param password Plain text password
     * @return Hashed password
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Check if employee account is active based on status
     * @param employeeToCheck Employee to check
     * @return true if employee can log in
     */
    private boolean isEmployeeActive(EmployeeModel employeeToCheck) {
        return employeeToCheck.getStatus() == EmployeeModel.EmployeeStatus.REGULAR || 
               employeeToCheck.getStatus() == EmployeeModel.EmployeeStatus.PROBATIONARY;
    }
    
    // =========================
    // ORIGINAL BUSINESS LOGIC METHODS
    // =========================
    
    /**
     * Alternative login method that matches original validateCredentials signature
     * Works with employee table
     * @param email User's email
     * @param password User's password
     * @return User object if authentication successful, null otherwise
     */
    public Object validateCredentials(String email, String password) {
        if (login(email, password)) {
            return this.userObject;
        }
        return null;
    }
    
    /**
     * Gets user by employee ID (for system operations)
     * Works with employee table
     * @param employeeId Employee ID
     * @return User object if found, null otherwise
     */
    public Object getUserByEmployeeId(Integer employeeId) {
        try {
            EmployeeModel foundEmployee = employeeDAO.findById(employeeId);
            if (foundEmployee == null) {
                return null;
            }
            
            // Create appropriate user object based on role from employee table
            String foundUserRole = foundEmployee.getUserRole() != null ? foundEmployee.getUserRole() : "EMPLOYEE";
            return createUserFromEmployee(foundEmployee, foundUserRole);
            
        } catch (Exception e) {
            System.err.println("Error getting user by employee ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if email exists in the system
     * Works with employee table
     * @param email Email to check
     * @return true if email exists
     */
    public boolean emailExists(String email) {
        try {
            EmployeeModel foundEmployee = employeeDAO.findByEmail(email);
            return foundEmployee != null;
        } catch (Exception e) {
            System.err.println("Error checking if email exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates user password
     * Works with employee table
     * @param employeeId Employee ID
     * @param newPassword New password (plain text - will be hashed)
     * @return true if update successful
     */
    public boolean updatePassword(Integer employeeId, String newPassword) {
        try {
            EmployeeModel foundEmployee = employeeDAO.findById(employeeId);
            if (foundEmployee == null) {
                return false;
            }
            
            // Hash the new password and update
            String hashedPassword = hashPassword(newPassword);
            foundEmployee.setPasswordHash(hashedPassword);
            
            return employeeDAO.update(foundEmployee);
        } catch (Exception e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates current user's password
     * @param newPassword New password
     * @return true if update successful
     */
    public boolean updatePassword(String newPassword) {
        if (!isUserLoggedIn()) {
            return false;
        }
        return updatePassword(this.employeeId, newPassword);
    }

    /**
     * Deactivates user account by setting status to TERMINATED
     * Works with employee table
     * @param employeeId Employee ID
     * @return true if deactivation successful
     */
    public boolean deactivateUser(Integer employeeId) {
        try {
            EmployeeModel foundEmployee = employeeDAO.findById(employeeId);
            if (foundEmployee == null) {
                return false;
            }
            
            // Set status to TERMINATED to deactivate
            foundEmployee.setStatus(EmployeeModel.EmployeeStatus.TERMINATED);
            
            return employeeDAO.update(foundEmployee);
        } catch (Exception e) {
            System.err.println("Error deactivating user: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Determines user role based on job position (enhanced version with exact matching)
     * @param position Job position/title
     * @return Appropriate user role
     */
    public String determineUserRoleFromPosition(String position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        
        // Normalize the position by trimming whitespace
        position = position.trim();
        
        // Check for numeric values which would indicate an error
        if (position.matches("\\d+")) {
            throw new IllegalArgumentException("Position appears to be a numeric ID instead of a job title: " + position);
        }
        
        // Check for executive positions (immediate supervisors)
        if (position.equals("Chief Executive Officer") ||
            position.equals("Chief Operating Officer") ||
            position.equals("Chief Finance Officer") ||
            position.equals("Chief Marketing Officer") ||
            position.equals("Account Manager") ||
            position.equals("Account Team Leader")) {
            return "IMMEDIATESUPERVISOR";
        }
        
        // Check for IT position
        else if (position.equals("IT Operations and Systems")) {
            return "IT";
        }
        
        // Check for HR positions
        else if (position.equals("HR Manager") ||
                 position.equals("HR Team Leader") ||
                 position.equals("HR Rank and File")) {
            return "HR";
        }
        
        // Check for Accounting positions
        else if (position.equals("Accounting Head") ||
                 position.equals("Payroll Manager") ||
                 position.equals("Payroll Team Leader") ||
                 position.equals("Payroll Rank and File")) {
            return "ACCOUNTING";
        }
        
        // Check for regular employee positions
        else if (position.equals("Account Rank and File") ||
                 position.equals("Sales & Marketing") ||
                 position.equals("Supply Chain and Logistics") ||
                 position.equals("Customer Service and Relations")) {
            return "EMPLOYEE";
        }
        
        // If position doesn't match any known roles, use a default role based on keywords
        else {
            System.out.println("Position not directly matched: '" + position + "'. Attempting to infer role.");
            
            // Try to infer the role from the position name
            String lowerPosition = position.toLowerCase();
            
            if (lowerPosition.contains("hr") || lowerPosition.contains("human resource")) {
                return "HR";
            }
            else if (lowerPosition.contains("it") || lowerPosition.contains("information tech") || lowerPosition.contains("system")) {
                return "IT";
            }
            else if (lowerPosition.contains("account") || lowerPosition.contains("payroll") || lowerPosition.contains("finance")) {
                return "ACCOUNTING";
            }
            else if (lowerPosition.contains("manager") || lowerPosition.contains("supervisor") || lowerPosition.contains("lead")) {
                return "IMMEDIATESUPERVISOR";
            }
            else {
                System.out.println("Unknown position detected: '" + position + "'. Defaulting to EMPLOYEE role.");
                return "EMPLOYEE"; // Default to employee
            }
        }
    }

    /**
     * Validates if user has permission to access specific role features (original method)
     * @param user User object to check (can be the userObject or an EmployeeModel)
     * @param requiredRole Required role for access
     * @return true if user has required permissions
     */
    public boolean hasRolePermission(Object user, String requiredRole) {
        if (user == null || requiredRole == null) {
            return false;
        }
        
        String extractedUserRole = null;
        
        // Extract role from user object
        if (user instanceof EmployeeModel) {
            EmployeeModel employeeModel = (EmployeeModel) user;
            extractedUserRole = employeeModel.getUserRole();
        } else if (user == this.userObject && this.userRole != null) {
            extractedUserRole = this.userRole;
        }
        
        if (extractedUserRole == null) {
            return false;
        }
        
        // Normalize roles for comparison
        extractedUserRole = normalizeUserRole(extractedUserRole);
        String normalizedRequiredRole = normalizeUserRole(requiredRole);
        
        return extractedUserRole.equals(normalizedRequiredRole);
    }

    /**
     * Creates a new employee account (for admin use)
     * Works with employee table
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param password Plain text password
     * @param userRole User role
     * @param positionId Position ID
     * @return true if creation successful
     */
    public boolean createUserAccount(String firstName, String lastName, String email, 
                                   String password, String userRole, Integer positionId) {
        try {
            // Check if email already exists
            if (emailExists(email)) {
                System.err.println("Email already exists: " + email);
                return false;
            }
            
            // Create new employee model
            EmployeeModel newEmployee = new EmployeeModel();
            newEmployee.setFirstName(firstName);
            newEmployee.setLastName(lastName);
            newEmployee.setEmail(email);
            newEmployee.setPasswordHash(hashPassword(password));
            newEmployee.setUserRole(userRole);
            newEmployee.setPositionId(positionId);
            newEmployee.setStatus(EmployeeModel.EmployeeStatus.PROBATIONARY);
            newEmployee.setBirthDate(java.time.LocalDate.now().minusYears(25)); // Default age
            newEmployee.setBasicSalary(new java.math.BigDecimal("25000.00")); // Default salary
            
            return employeeDAO.save(newEmployee);
            
        } catch (Exception e) {
            System.err.println("Error creating user account: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all employees with a specific role
     * Works with employee table
     * @param userRole Role to filter by
     * @return List of employees with that role
     */
    public java.util.List<EmployeeModel> getUsersByRole(String userRole) {
        try {
            return employeeDAO.getEmployeesByRole(userRole);
        } catch (Exception e) {
            System.err.println("Error getting users by role: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Logs authentication attempt for audit purposes
     * @param email Email used for login
     * @param success Whether login was successful
     * @param ipAddress Optional IP address
     */
    public void logAuthenticationAttempt(String email, boolean success, String ipAddress) {
        try {
            String logMessage = String.format("[AUTH] %s - Email: %s, Success: %s, IP: %s",
                java.time.LocalDateTime.now(), email, success, ipAddress != null ? ipAddress : "Unknown");
            System.out.println(logMessage);
            
            // In a real implementation, you'd save this to an audit log table
            // You could add an audit_log table to your schema for this
            
        } catch (Exception e) {
            System.err.println("Error logging authentication attempt: " + e.getMessage());
        }
    }
    
    // =========================
    // UTILITY METHODS
    // =========================
    
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
    
    @Override
    public String toString() {
        return "UserAuthenticationModel{" +
                "employeeId=" + employeeId +
                ", email='" + email + '\'' +
                ", userRole='" + userRole + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", isLoggedIn=" + isLoggedIn +
                '}';
    }
}