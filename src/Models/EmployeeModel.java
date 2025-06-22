package Models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EmployeeModel class that matches the database table structure exactly.
 * This class represents an employee entity in the payroll system.
 * Each field corresponds to a column in the employee table.
 * @author User
 */
public class EmployeeModel {
    
    // Primary key - auto-increment in database
    private Integer employeeId;
    
    // Basic employee information
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String phoneNumber;
    private String email;
    
    // Salary information - using BigDecimal for precise money calculations
    private BigDecimal basicSalary;
    private BigDecimal hourlyRate;
    
    // System information
    private String userRole = "Employee"; // Default value matches database
    private String passwordHash;
    private EmployeeStatus status = EmployeeStatus.PROBATIONARY; // Default value
    
    // Timestamp fields - automatically managed by database
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    
    // Foreign key relationships
    private Integer positionId; // Links to position table
    private Integer supervisorId; // Links to another employee (self-reference)
    

    // CONSTRUCTORS
 
    
    /**
     * Default constructor
     * Initializes timestamps to current time
     */
    public EmployeeModel() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor with essential fields for creating a new employee
     * Use this when you have the minimum required information
     * @param firstName Employee's first name
     * @param lastName Employee's last name
     * @param birthDate Employee's birth date
     * @param email Employee's email (must be unique)
     * @param passwordHash Hashed password for login
     * @param positionId The position this employee will hold
     */
    public EmployeeModel(String firstName, String lastName, LocalDate birthDate, 
                   String email, String passwordHash, Integer positionId) {
        this(); // Call default constructor to set timestamps
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.email = email;
        this.passwordHash = passwordHash;
        this.positionId = positionId;
    }
    
    /**
     * Full constructor with all fields
     * Use this when loading employees from the database
     * @param employeeId
     * @param firstName
     * @param lastName
     * @param birthDate
     * @param phoneNumber
     * @param email
     * @param basicSalary
     * @param hourlyRate
     * @param userRole
     * @param passwordHash
     * @param status
     * @param createdAt
     * @param updatedAt
     * @param lastLogin
     * @param positionId
     * @param supervisorId
     */
    public EmployeeModel(Integer employeeId, String firstName, String lastName, 
                   LocalDate birthDate, String phoneNumber, String email,
                   BigDecimal basicSalary, BigDecimal hourlyRate, String userRole,
                   String passwordHash, EmployeeStatus status, LocalDateTime createdAt,
                   LocalDateTime updatedAt, LocalDateTime lastLogin, 
                   Integer positionId, Integer supervisorId) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.basicSalary = basicSalary;
        this.hourlyRate = hourlyRate;
        this.userRole = userRole;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLogin = lastLogin;
        this.positionId = positionId;
        this.supervisorId = supervisorId;
    }
    

    // GETTERS AND SETTERS

    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    /**
     * Sets the first name and updates timestamp only if employee already exists in database
     * @param firstName The new first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        // Only update timestamp if object already exists (has an ID)
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public String getLastName() {
        return lastName;
    }
    
    /**
     * Sets the last name and updates timestamp only if employee already exists in database
     * @param lastName The new last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
        // Only update timestamp if object already exists (has an ID)
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    /**
     * Sets the birth date and updates timestamp only if employee already exists in database
     * @param birthDate The new birth date
     */
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        // Only update timestamp if object already exists (has an ID)
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    /**
     * Sets the phone number and updates timestamp only if employee already exists in database
     * @param phoneNumber The new phone number
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        // Only update timestamp if object already exists (has an ID)
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public String getEmail() {
        return email;
    }
    
    /**
     * Sets the email and updates timestamp only if employee already exists in database
     * @param email The new email address
     */
    public void setEmail(String email) {
        this.email = email;
        // Only update timestamp if object already exists (has an ID)
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public BigDecimal getBasicSalary() {
        return basicSalary;
    }
    
    /**
     * Sets the basic salary and always updates the timestamp
     * Salary changes should always be tracked regardless of object state
     * @param basicSalary The new basic salary
     */
    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
        this.updatedAt = LocalDateTime.now(); // Always update timestamp for salary changes
    }
    
    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }
    
    /**
     * Sets the hourly rate and always updates the timestamp
     * Salary changes should always be tracked regardless of object state
     * @param hourlyRate The new hourly rate
     */
    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.updatedAt = LocalDateTime.now(); // Always update timestamp for salary changes
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    /**
     * Sets the user role and updates timestamp only if employee already exists in database
     * @param userRole The new user role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    /**
     * Sets the password hash and always updates the timestamp
     * Password changes should always be tracked for security
     * @param passwordHash The new hashed password
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = LocalDateTime.now(); // Always update timestamp for security changes
    }
    
    public EmployeeStatus getStatus() {
        return status;
    }
    
    /**
     * Sets the employee status and always updates the timestamp
     * Status changes are important and should always be tracked
     * @param status The new employee status
     */
    public void setStatus(EmployeeStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now(); // Always update timestamp for status changes
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Integer getPositionId() {
        return positionId;
    }
    
    /**
     * Sets the position ID and updates timestamp only if employee already exists in database
     * @param positionId The new position ID
     */
    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public Integer getSupervisorId() {
        return supervisorId;
    }
    
    /**
     * Sets the supervisor ID and updates timestamp only if employee already exists in database
     * @param supervisorId The new supervisor ID
     */
    public void setSupervisorId(Integer supervisorId) {
        this.supervisorId = supervisorId;
        if (this.employeeId != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    

    // BUSINESS METHODS - Useful methods for working with employees

    
    /**
     * Gets the employee's full name by combining first and last name
     * @return firstName + " " + lastName
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Checks if the employee is currently active (not terminated)
     * @return true if employee is not terminated
     */
    public boolean isActive() {
        return status != EmployeeStatus.TERMINATED;
    }
    
    /**
     * Checks if the employee has regular status (not probationary)
     * @return true if employee status is regular
     */
    public boolean isRegular() {
        return status == EmployeeStatus.REGULAR;
    }
    
    /**
     * Updates the last login timestamp to the current time
     * Call this when an employee successfully logs in
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }
    
    /**
     * Checks if the employee has a supervisor assigned
     * @return true if supervisorId is not null
     */
    public boolean hasSupervisor() {
        return supervisorId != null;
    }
    
    /**
     * Checks if this employee is a supervisor (has subordinates)
     * Note: This would need to be implemented with a DAO query to check subordinates
     * @return true if employee has supervisorId that matches other employees
     */
    public boolean isSupervisor() {
        // This is a placeholder - would need DAO to check if any employees have this ID as supervisorId
        return false;
    }
    
    /**
     * Calculates years of service based on createdAt date
     * @return number of years since employee was created
     */
    public long getYearsOfService() {
        if (createdAt == null) return 0;
        return java.time.temporal.ChronoUnit.YEARS.between(createdAt.toLocalDate(), LocalDate.now());
    }
    
    /**
     * Checks if employee can be promoted (is probationary and has served minimum time)
     * @return true if employee is probationary and has served at least 6 months
     */
    public boolean canBePromoted() {
        return status == EmployeeStatus.PROBATIONARY && getYearsOfService() >= 0.5; // 6 months
    }
    

    // UTILITY METHODS - Standard object methods

    
    /**
     * Returns a string representation of the employee
     * Useful for debugging and logging
     */
    @Override
    public String toString() {
        return "EmployeeModel{" +
                "employeeId=" + employeeId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", userRole='" + userRole + '\'' +
                ", status=" + status +
                ", positionId=" + positionId +
                ", supervisorId=" + supervisorId +
                ", active=" + isActive() +
                '}';
    }
    
    /**
     * Checks if two EmployeeModel objects are equal
     * Two employees are equal if they have the same employeeId
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        EmployeeModel employee = (EmployeeModel) obj;
        return employeeId != null && employeeId.equals(employee.employeeId);
    }
    
    /**
     * Generates a hash code for the employee
     * Uses employeeId as the basis for the hash code
     */
    @Override
    public int hashCode() {
        return employeeId != null ? employeeId.hashCode() : 0;
    }
    

    // EMPLOYEE STATUS ENUM - Matches database enum values

    
    /**
     * Employee status enum that matches the database enum values exactly
     * These are the only valid status values in the database
     */
    public enum EmployeeStatus {
        PROBATIONARY("Probationary"),
        REGULAR("Regular"),
        TERMINATED("Terminated");
        
        
        // The actual string value stored in the database
        private final String value;
        
        EmployeeStatus(String value) {
            this.value = value;
        }
        
        /**
         * Gets the database string value for this status
         * @return The string value stored in the database
         */
        public String getValue() {
            return value;
        }
        
        /**
         * Converts a database string value back to the enum
         * This is used when loading employees from the database
         * @param value The string value from the database
         * @return The corresponding enum value
         * @throws IllegalArgumentException if the value is not recognized
         */
        public static EmployeeStatus fromString(String value) {
            if (value == null) {
                return PROBATIONARY; // Default value
            }
            
            for (EmployeeStatus status : EmployeeStatus.values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown employee status: " + value);
        }
        
        /**
         * Gets all possible status values as strings
         * Useful for validation or UI dropdowns
         * @return Array of all status values
         */
        public static String[] getAllValues() {
            EmployeeStatus[] statuses = values();
            String[] values = new String[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
                values[i] = statuses[i].getValue();
            }
            return values;
        }
    }
}