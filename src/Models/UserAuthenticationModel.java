/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

/**
 *
 * @author USER
 */
import java.time.LocalDateTime;

/**
 * Model class for user authentication and session management
 */
public class UserAuthenticationModel {
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
    
    // Constructors
    public UserAuthenticationModel() {}
    
    public UserAuthenticationModel(int employeeId, String email, String userRole) {
        this.employeeId = employeeId;
        this.email = email;
        this.userRole = userRole;
        this.isLoggedIn = false;
        this.loginAttempts = 0;
    }
    
    // Getters and Setters
    public int getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public LocalDateTime getSessionExpiry() {
        return sessionExpiry;
    }
    
    public void setSessionExpiry(LocalDateTime sessionExpiry) {
        this.sessionExpiry = sessionExpiry;
    }
    
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    
    public void setLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
    }
    
    public int getLoginAttempts() {
        return loginAttempts;
    }
    
    public void setLoginAttempts(int loginAttempts) {
        this.loginAttempts = loginAttempts;
    }
    
    public LocalDateTime getLastLoginAttempt() {
        return lastLoginAttempt;
    }
    
    public void setLastLoginAttempt(LocalDateTime lastLoginAttempt) {
        this.lastLoginAttempt = lastLoginAttempt;
    }
    
    // Business Logic Methods
    
    /**
     * Checks if the session is still valid
     * @return true if session is valid, false otherwise
     */
    public boolean isSessionValid() {
        if (sessionToken == null || sessionExpiry == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(sessionExpiry) && isLoggedIn;
    }
    
    /**
     * Checks if the account is locked due to too many failed login attempts
     * @return true if account is locked, false otherwise
     */
    public boolean isAccountLocked() {
        if (loginAttempts >= 5) {
            if (lastLoginAttempt != null) {
                // Account locked for 30 minutes after 5 failed attempts
                return LocalDateTime.now().isBefore(lastLoginAttempt.plusMinutes(30));
            }
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the user account is active
     * @return true if account is active, false otherwise
     */
    public boolean isAccountActive() {
        return "Regular".equals(status) || "Probationary".equals(status);
    }
    
    /**
     * Increments login attempts counter
     */
    public void incrementLoginAttempts() {
        this.loginAttempts++;
        this.lastLoginAttempt = LocalDateTime.now();
    }
    
    /**
     * Resets login attempts counter (called on successful login)
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lastLoginAttempt = null;
    }
    
    /**
     * Starts a new session
     * @param sessionToken The session token
     * @param durationMinutes Session duration in minutes
     */
    public void startSession(String sessionToken, int durationMinutes) {
        this.sessionToken = sessionToken;
        this.sessionExpiry = LocalDateTime.now().plusMinutes(durationMinutes);
        this.isLoggedIn = true;
        this.lastLogin = LocalDateTime.now();
        resetLoginAttempts();
    }
    
    /**
     * Ends the current session
     */
    public void endSession() {
        this.sessionToken = null;
        this.sessionExpiry = null;
        this.isLoggedIn = false;
    }
    
    /**
     * Extends the current session
     * @param additionalMinutes Additional minutes to extend the session
     */
    public void extendSession(int additionalMinutes) {
        if (sessionExpiry != null) {
            this.sessionExpiry = this.sessionExpiry.plusMinutes(additionalMinutes);
        }
    }
    
    /**
     * Checks if user has administrative privileges
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin() {
        return "HR".equals(userRole) || "IT".equals(userRole);
    }
    
    /**
     * Checks if user has supervisor privileges
     * @return true if user is supervisor, false otherwise
     */
    public boolean isSupervisor() {
        return "ImmediateSupervisor".equals(userRole) || isAdmin();
    }
    
    /**
     * Gets the time until session expires
     * @return minutes until session expires, or 0 if session is invalid
     */
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
    
    @Override
    public String toString() {
        return "UserAuthenticationModel{" +
                "employeeId=" + employeeId +
                ", email='" + email + '\'' +
                ", userRole='" + userRole + '\'' +
                ", status='" + status + '\'' +
                ", isLoggedIn=" + isLoggedIn +
                ", loginAttempts=" + loginAttempts +
                '}';
    }
}