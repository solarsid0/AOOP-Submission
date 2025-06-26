package Models;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * PositionModel class that maps to the position table
 * Fields: positionId, position, positionDescription, department
 * Handles job positions and organizational structure with automatic department assignment
 * 
 * Departments in MPH:
 * - Leadership (C-Level executives)
 * - HR (Human Resources)
 * - IT (Information Technology)
 * - Accounting (Payroll and Accounting)
 * - Accounts (Account Management)
 * - Sales and Marketing
 * - Supply Chain and Logistics
 * - Customer Service
 * - Other (fallback)
 * 
 * @author User
 */
public class PositionModel {
    
    private Integer positionId;
    private String position;           // Job title/position name
    private String positionDescription;
    private String department;
    
    // Constants for common departments (updated to match your exact naming)
    public static final String DEPT_LEADERSHIP = "Leadership";
    public static final String DEPT_HR = "HR";
    public static final String DEPT_IT = "IT";
    public static final String DEPT_ACCOUNTING = "Accounting";
    public static final String DEPT_ACCOUNTS = "Accounts";
    public static final String DEPT_SALES_MARKETING = "Sales and Marketing";
    public static final String DEPT_SUPPLY_CHAIN = "Supply Chain and Logistics";
    public static final String DEPT_CUSTOMER_SERVICE = "Customer Service";
    public static final String DEPT_OTHER = "Other";
    
    // Constants for common position types
    public static final String TYPE_EXECUTIVE = "Executive";
    public static final String TYPE_MANAGER = "Manager";
    public static final String TYPE_SUPERVISOR = "Supervisor";
    public static final String TYPE_SPECIALIST = "Specialist";
    public static final String TYPE_ASSOCIATE = "Associate";
    public static final String TYPE_ENTRY_LEVEL = "Entry Level";
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor
     */
    public PositionModel() {}
    
    /**
     * Constructor with essential fields - auto-assigns department
     * @param position Job title/position name
     */
    public PositionModel(String position) {
        this.position = position;
        this.department = getDepartmentForPosition(position); // Auto-assign department
    }
    
    /**
     * Constructor with position and description - auto-assigns department
     * @param position Job title/position name
     * @param positionDescription Description of the position
     */
    public PositionModel(String position, String positionDescription) {
        this.position = position;
        this.positionDescription = positionDescription;
        this.department = getDepartmentForPosition(position);
    }
    
    /**
     * Full constructor with all fields
     * @param positionId Position ID (from database)
     * @param position Job title/position name
     * @param positionDescription Description of the position
     * @param department Department name
     */
    public PositionModel(Integer positionId, String position, String positionDescription, String department) {
        this.positionId = positionId;
        this.position = position;
        this.positionDescription = positionDescription;
        this.department = department;
    }
    
    // ===============================
    // STATIC FACTORY METHODS
    // ===============================
    
    /**
     * Creates a position with explicit department (doesn't auto-assign)
     * @param position Job title/position name
     * @param department Department name
     * @return PositionModel instance
     */
    public static PositionModel withDepartment(String position, String department) {
        PositionModel model = new PositionModel();
        model.position = position;
        model.department = department;
        return model;
    }
    
    /**
     * Creates a position with description and auto-assigned department
     * @param position Job title/position name
     * @param positionDescription Description of the position
     * @return PositionModel instance
     */
    public static PositionModel withDescription(String position, String positionDescription) {
        PositionModel model = new PositionModel();
        model.position = position;
        model.positionDescription = positionDescription;
        model.department = getDepartmentForPosition(position);
        return model;
    }
    
    // ===============================
    // GETTERS AND SETTERS
    // ===============================
    
    public Integer getPositionId() {
        return positionId;
    }
    
    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
        // Auto-assign department when position changes
        this.assignDepartmentByPosition();
    }
    
    public String getPositionDescription() {
        return positionDescription;
    }
    
    public void setPositionDescription(String positionDescription) {
        this.positionDescription = positionDescription;
    }
    
    /**
     * Gets the assigned department (auto-assigns if null)
     * @return Department name
     */
    public String getDepartment() {
        if (department == null) {
            assignDepartmentByPosition();
        }
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    // ===============================
    // BUSINESS METHODS
    // ===============================
    
    /**
     * Validates the position data
     * @return true if position data is valid
     */
    public boolean isValid() {
        if (position == null || position.trim().isEmpty()) {
            return false;
        }
        
        if (position.length() > 50) {
            return false;
        }
        
        if (positionDescription != null && positionDescription.length() > 255) {
            return false;
        }
        
        if (department != null && department.length() > 50) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Determines the position level based on actual job titles
     * @return Position level (Executive, Manager, Supervisor, etc.)
     */
    public String getPositionLevel() {
        if (position == null) {
            return TYPE_ENTRY_LEVEL;
        }
        
        // Check exact matches for your actual positions
        switch (position.trim()) {
            case "Chief Executive Officer":
            case "Chief Operating Officer":
            case "Chief Finance Officer":
            case "Chief Marketing Officer":
                return TYPE_EXECUTIVE;
            
            case "HR Manager":
            case "Payroll Manager":
            case "Account Manager":
            case "Accounting Head":
                return TYPE_MANAGER;
            
            case "HR Team Leader":
            case "Payroll Team Leader":
            case "Account Team Leader":
                return TYPE_SUPERVISOR;
            
            case "IT Operations and Systems":
                return TYPE_SPECIALIST;
            
            case "HR Rank and File":
            case "Payroll Rank and File":
            case "Account Rank and File":
            case "Sales & Marketing":
            case "Supply Chain and Logistics":
            case "Customer Service and Relations":
                return TYPE_ASSOCIATE;
            
            default:
                // Fallback logic
                String lowerPosition = position.toLowerCase();
                if (lowerPosition.contains("chief")) {
                    return TYPE_EXECUTIVE;
                } else if (lowerPosition.contains("manager") || lowerPosition.contains("head")) {
                    return TYPE_MANAGER;
                } else if (lowerPosition.contains("leader") || lowerPosition.contains("supervisor")) {
                    return TYPE_SUPERVISOR;
                } else if (lowerPosition.contains("specialist") || lowerPosition.contains("operations")) {
                    return TYPE_SPECIALIST;
                } else {
                    return TYPE_ASSOCIATE;
                }
        }
    }
    
    /**
     * Determines user role based on position (matches UserAuthenticationModel logic exactly)
     * @return User role for authentication system
     */
    public String determineUserRole() {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        
        // Normalize the position by trimming whitespace  
        String normalizedPosition = position.trim();
        
        // Check for numeric values which would indicate an error
        if (normalizedPosition.matches("\\d+")) {
            throw new IllegalArgumentException("Position appears to be a numeric ID instead of a job title: " + normalizedPosition);
        }
        
        // Check for executive positions (immediate supervisors)
        if (normalizedPosition.equals("Chief Executive Officer") ||
            normalizedPosition.equals("Chief Operating Officer") ||
            normalizedPosition.equals("Chief Finance Officer") ||
            normalizedPosition.equals("Chief Marketing Officer") ||
            normalizedPosition.equals("Account Manager") ||
            normalizedPosition.equals("Account Team Leader")) {
            return "IMMEDIATESUPERVISOR";
        }
        
        // Check for IT position
        else if (normalizedPosition.equals("IT Operations and Systems")) {
            return "IT";
        }
        
        // Check for HR positions
        else if (normalizedPosition.equals("HR Manager") ||
                 normalizedPosition.equals("HR Team Leader") ||
                 normalizedPosition.equals("HR Rank and File")) {
            return "HR";
        }
        
        // Check for Accounting positions
        else if (normalizedPosition.equals("Accounting Head") ||
                 normalizedPosition.equals("Payroll Manager") ||
                 normalizedPosition.equals("Payroll Team Leader") ||
                 normalizedPosition.equals("Payroll Rank and File")) {
            return "ACCOUNTING";
        }
        
        // Check for regular employee positions
        else if (normalizedPosition.equals("Account Rank and File") ||
                 normalizedPosition.equals("Sales & Marketing") ||
                 normalizedPosition.equals("Supply Chain and Logistics") ||
                 normalizedPosition.equals("Customer Service and Relations")) {
            return "EMPLOYEE";
        }
        
        // If position doesn't match any known roles, use a default role based on keywords
        else {
            System.out.println("Position not directly matched: '" + normalizedPosition + "'. Attempting to infer role.");
            
            // Try to infer the role from the position name
            String lowerPosition = normalizedPosition.toLowerCase();
            
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
                System.out.println("Unknown position detected: '" + normalizedPosition + "'. Defaulting to EMPLOYEE role.");
                return "EMPLOYEE"; // Default to employee
            }
        }
    }
    
    /**
     * Checks if this is a management position
     * @return true if position involves managing others
     */
    public boolean isManagementPosition() {
        String level = getPositionLevel();
        return TYPE_EXECUTIVE.equals(level) || TYPE_MANAGER.equals(level) || TYPE_SUPERVISOR.equals(level);
    }
    
    /**
     * Checks if this is an executive position
     * @return true if position is executive level
     */
    public boolean isExecutivePosition() {
        return TYPE_EXECUTIVE.equals(getPositionLevel());
    }
    
    /**
     * Gets the position display name with department
     * @return Formatted position name with department
     */
    public String getDisplayName() {
        StringBuilder display = new StringBuilder();
        
        if (position != null) {
            display.append(position);
        }
        
        if (department != null && !department.trim().isEmpty()) {
            display.append(" (").append(department).append(")");
        }
        
        return display.toString();
    }
    
    /**
     * Gets the full position information
     * @return Full position details
     */
    public String getFullPositionInfo() {
        StringBuilder info = new StringBuilder();
        
        if (position != null) {
            info.append("Position: ").append(position).append("\n");
        }
        
        if (department != null) {
            info.append("Department: ").append(department).append("\n");
        }
        
        info.append("Level: ").append(getPositionLevel()).append("\n");
        
        if (positionDescription != null && !positionDescription.trim().isEmpty()) {
            info.append("Description: ").append(positionDescription);
        }
        
        return info.toString();
    }
    
    /**
     * Checks if this position belongs to a specific department
     * @param departmentName Department to check
     * @return true if position belongs to the department
     */
    public boolean belongsToDepartment(String departmentName) {
        if (department == null || departmentName == null) {
            return false;
        }
        
        return department.equalsIgnoreCase(departmentName);
    }
    
    /**
     * Automatically assigns department based on position title
     * Call this method when creating or updating position
     */
    public void assignDepartmentByPosition() {
        this.department = getDepartmentForPosition(this.position);
    }
    
    /**
     * Gets the correct department for a specific position title
     * @param positionTitle The position title
     * @return Department name for the position
     */
    public static String getDepartmentForPosition(String positionTitle) {
        if (positionTitle == null) {
            return "Other";
        }
        
        String position = positionTitle.trim();
        
        // Leadership positions
        if (position.equals("Chief Executive Officer") || 
            position.equals("Chief Operating Officer") || 
            position.equals("Chief Finance Officer") || 
            position.equals("Chief Marketing Officer")) {
            return "Leadership";
        }
        // HR Department
        else if (position.equals("HR Manager") || 
                 position.equals("HR Team Leader") || 
                 position.equals("HR Rank and File")) {
            return "HR";
        }
        // IT Department
        else if (position.equals("IT Operations and Systems") ||
                 position.toLowerCase().contains("it ")) {
            return "IT";
        }
        // Accounting Department
        else if (position.equals("Accounting Head") || 
                 position.equals("Payroll Manager") || 
                 position.equals("Payroll Team Leader") || 
                 position.equals("Payroll Rank and File")) {
            return "Accounting";
        }
        // Accounts Department
        else if (position.equals("Account Manager") || 
                 position.equals("Account Team Leader") || 
                 position.equals("Account Rank and File")) {
            return "Accounts";
        }
        // Sales and Marketing Department
        else if (position.equals("Sales & Marketing")) {
            return "Sales and Marketing";
        }
        // Supply Chain and Logistics Department
        else if (position.equals("Supply Chain and Logistics")) {
            return "Supply Chain and Logistics";
        }
        // Customer Service Department
        else if (position.equals("Customer Service and Relations")) {
            return "Customer Service";
        }
        // If no specific match is found
        return "Other";
    }
    
    /**
     * Gets actual positions available in your organization by department
     * @param departmentName Department name
     * @return List of actual positions for the department
     */
    public static List<String> getActualPositionsForDepartment(String departmentName) {
        List<String> positions = new ArrayList<>();
        
        if (departmentName == null) {
            return positions;
        }
        
        switch (departmentName.toUpperCase()) {
            case "HUMAN RESOURCES":
            case "HR":
                positions.add("HR Manager");
                positions.add("HR Team Leader");
                positions.add("HR Rank and File");
                break;
                
            case "INFORMATION TECHNOLOGY":
            case "IT":
                positions.add("IT Operations and Systems");
                break;
                
            case "ACCOUNTING":
            case "FINANCE":
                positions.add("Accounting Head");
                positions.add("Payroll Manager");
                positions.add("Payroll Team Leader");
                positions.add("Payroll Rank and File");
                break;
                
            case "ACCOUNT MANAGEMENT":
            case "ACCOUNTS":
                positions.add("Account Manager");
                positions.add("Account Team Leader");
                positions.add("Account Rank and File");
                break;
                
            case "SALES & MARKETING":
            case "SALES":
                positions.add("Sales & Marketing");
                break;
                
            case "OPERATIONS":
                positions.add("Supply Chain and Logistics");
                break;
                
            case "CUSTOMER SERVICE":
                positions.add("Customer Service and Relations");
                break;
                
            case "EXECUTIVE":
                positions.add("Chief Executive Officer");
                positions.add("Chief Operating Officer");
                positions.add("Chief Finance Officer");
                positions.add("Chief Marketing Officer");
                break;
                
            default:
                // Return all positions if department not specified
                positions.addAll(getAllActualPositions());
                break;
        }
        
        return positions;
    }
    
    /**
     * Creates a position hierarchy key for sorting
     * @return Hierarchy key for organizational sorting
     */
    public String getHierarchyKey() {
        String level = getPositionLevel();
        String dept = getDepartment(); // Use getDepartment() which auto-assigns if needed
        
        // Create sorting key: Level priority + Department + Position
        String levelPriority;
        switch (level) {
            case TYPE_EXECUTIVE: levelPriority = "1"; break;
            case TYPE_MANAGER: levelPriority = "2"; break;
            case TYPE_SUPERVISOR: levelPriority = "3"; break;
            case TYPE_SPECIALIST: levelPriority = "4"; break;
            case TYPE_ASSOCIATE: levelPriority = "5"; break;
            default: levelPriority = "6"; break;
        }
        
        return levelPriority + "_" + dept + "_" + (position != null ? position : "");
    }
    
    /**
     * Gets all actual positions available in your organization
     * @return Complete list of all position titles
     */
    public static List<String> getAllActualPositions() {
        List<String> allPositions = new ArrayList<>();
        
        // Executive positions
        allPositions.add("Chief Executive Officer");
        allPositions.add("Chief Operating Officer");
        allPositions.add("Chief Finance Officer");
        allPositions.add("Chief Marketing Officer");
        
        // IT position
        allPositions.add("IT Operations and Systems");
        
        // HR positions
        allPositions.add("HR Manager");
        allPositions.add("HR Team Leader");
        allPositions.add("HR Rank and File");
        
        // Accounting positions
        allPositions.add("Accounting Head");
        allPositions.add("Payroll Manager");
        allPositions.add("Payroll Team Leader");
        allPositions.add("Payroll Rank and File");
        
        // Account management positions
        allPositions.add("Account Manager");
        allPositions.add("Account Team Leader");
        allPositions.add("Account Rank and File");
        
        // Other department positions
        allPositions.add("Sales & Marketing");
        allPositions.add("Supply Chain and Logistics");
        allPositions.add("Customer Service and Relations");
        
        return allPositions;
    }
    
    /**
     * Gets all actual departments in your organization based on positions
     * @return List of department names
     */
    public static List<String> getAllDepartments() {
        List<String> departments = new ArrayList<>();
        departments.add("Leadership");
        departments.add("HR");
        departments.add("IT");
        departments.add("Accounting");
        departments.add("Accounts");
        departments.add("Sales and Marketing");
        departments.add("Supply Chain and Logistics");
        departments.add("Customer Service");
        departments.add("Other");
        return departments;
    }
    
    /**
     * Gets all positions for a specific department
     * @param departmentName Department name
     * @return List of positions in that department
     */
    public static List<String> getPositionsInDepartment(String departmentName) {
        List<String> positions = new ArrayList<>();
        
        if (departmentName == null) {
            return positions;
        }
        
        // Get all actual positions and filter by department
        List<String> allPositions = getAllActualPositions();
        for (String position : allPositions) {
            if (departmentName.equals(getDepartmentForPosition(position))) {
                positions.add(position);
            }
        }
        
        return positions;
    }
    
    /**
     * Validates if department name exists in your organization
     * @param departmentName Department name to validate
     * @return true if department exists
     */
    public static boolean isValidDepartment(String departmentName) {
        return departmentName != null && getAllDepartments().contains(departmentName);
    }
    
    /**
     * Gets complete position-department mapping for reference
     * @return Map of position title to department
     */
    public static Map<String, String> getCompletePositionDepartmentMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        for (String position : getAllActualPositions()) {
            mapping.put(position, getDepartmentForPosition(position));
        }
        
        return mapping;
    }
    
    /**
     * Checks if this position's current department matches the expected department
     * @return true if department is correctly assigned
     */
    public boolean hasDepartmentCorrectlyAssigned() {
        if (position == null) {
            return true; // No position to validate
        }
        
        String expectedDepartment = getDepartmentForPosition(position);
        return expectedDepartment.equals(this.department);
    }
    
    /**
     * Fixes department assignment if it's incorrect
     * @return true if department was corrected, false if it was already correct
     */
    public boolean correctDepartmentAssignment() {
        String expectedDepartment = getDepartmentForPosition(position);
        if (!expectedDepartment.equals(this.department)) {
            this.department = expectedDepartment;
            return true;
        }
        return false;
    }
    
    /**
     * Gets department statistics - count of positions per department
     * @return Map of department name to position count
     */
    public static Map<String, Integer> getDepartmentStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        // Initialize all departments with 0
        for (String dept : getAllDepartments()) {
            stats.put(dept, 0);
        }
        
        // Count positions per department
        for (String position : getAllActualPositions()) {
            String department = getDepartmentForPosition(position);
            stats.put(department, stats.getOrDefault(department, 0) + 1);
        }
        
        return stats;
    }
    
    /**
     * Validates if position title exists in your actual organization
     * @param positionTitle Position title to validate
     * @return true if position exists in your organization
     */
    public static boolean isValidPositionTitle(String positionTitle) {
        if (positionTitle == null) {
            return false;
        }
        
        return getAllActualPositions().contains(positionTitle.trim());
    }
    
    /**
     * Gets positions by user role for testing purposes
     * @param userRole User role (HR, IT, ACCOUNTING, etc.)
     * @return List of positions that map to this user role
     */
    public static List<String> getPositionsByUserRole(String userRole) {
        List<String> positions = new ArrayList<>();
        
        if (userRole == null) {
            return positions;
        }
        
        switch (userRole.toUpperCase()) {
            case "IMMEDIATESUPERVISOR":
                positions.add("Chief Executive Officer");
                positions.add("Chief Operating Officer");
                positions.add("Chief Finance Officer");
                positions.add("Chief Marketing Officer");
                positions.add("Account Manager");
                positions.add("Account Team Leader");
                break;
                
            case "IT":
                positions.add("IT Operations and Systems");
                break;
                
            case "HR":
                positions.add("HR Manager");
                positions.add("HR Team Leader");
                positions.add("HR Rank and File");
                break;
                
            case "ACCOUNTING":
                positions.add("Accounting Head");
                positions.add("Payroll Manager");
                positions.add("Payroll Team Leader");
                positions.add("Payroll Rank and File");
                break;
                
            case "EMPLOYEE":
                positions.add("Account Rank and File");
                positions.add("Sales & Marketing");
                positions.add("Supply Chain and Logistics");
                positions.add("Customer Service and Relations");
                break;
        }
        
        return positions;
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    @Override
    public String toString() {
        return "PositionModel{" +
                "positionId=" + positionId +
                ", position='" + position + '\'' +
                ", department='" + getDepartment() + '\'' +
                ", level='" + getPositionLevel() + '\'' +
                ", userRole='" + (position != null ? determineUserRole() : "N/A") + '\'' +
                ", departmentCorrect=" + hasDepartmentCorrectlyAssigned() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PositionModel that = (PositionModel) obj;
        return Objects.equals(positionId, that.positionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(positionId);
    }
    
    /**
     * Returns a formatted display string for this position
     * @return Human-readable string with position details
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        
        if (position != null) {
            sb.append(position);
        }
        
        if (getDepartment() != null && !getDepartment().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(getDepartment());
        }
        
        sb.append(" [").append(getPositionLevel()).append("]");
        
        if (positionDescription != null && !positionDescription.trim().isEmpty()) {
            sb.append("\nDescription: ").append(positionDescription);
        }
        
        return sb.toString();
    }
    
}