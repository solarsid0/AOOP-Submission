package oop.classes.management;

import CSV.CSVDatabaseProcessor;
import java.util.List;
import java.util.Map;
import oop.classes.actors.Accounting;
import oop.classes.actors.Employee;
import oop.classes.actors.HR;
import oop.classes.actors.IT;
import oop.classes.actors.ImmediateSupervisor;
import oop.classes.actors.User;


/**
 * This class authenticates the user credentials inputted in the log in
 * @author Admin
 */

/**
 * Handles user authentication by validating credentials from a CSV database.
 * It determines the user's role and creates the appropriate user object.
 */
public class UserAuthentication {
   private final CSVDatabaseProcessor databaseProcessor;

    /**
     * Initializes the authentication system with a database processor.
     * @param databaseProcessor The CSV database handler.
     */
    public UserAuthentication(CSVDatabaseProcessor databaseProcessor) {
        this.databaseProcessor = databaseProcessor;
    }

     /**
     * Validates the given email and password against stored credentials.
     * @param email User's email.
     * @param password User's password.
     * @return A User object if authentication is successful; otherwise, null.
     */
    public User validateCredentials(String email, String password) {
        
        // Get all stored user credentials from the database
        List<Map<String, String>> userCredentialRecords = databaseProcessor.getAllUserCredentialRecords();
        
        // If no credentials exist, return null
        if (userCredentialRecords == null || userCredentialRecords.isEmpty()) {
            System.out.println("No user credentials found. Please check if the CSV file is loaded.");
            return null;
        }
        
        

        // Loop through the credentials and check if the input matches any stored record        
        for (Map<String, String> record : userCredentialRecords) {
            String storedEmail = record.get("Email");
            String storedPassword = record.get("Password");
        
            // Credentials match, retrieve employee details
            if (email.equals(storedEmail) && password.equals(storedPassword)) {
                try {
                    int employeeID = Integer.parseInt(record.get("Employee ID"));
                    return getUserByID(employeeID); // Retrieve the User object
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Employee ID format: " + record.get("Employee ID"));
                    return null;
                }
            }
        }
        return null; // Return null if no matching credentials are found
    }

     /**
     * Retrieves an employee's details by their ID.
     * @param employeeID The employee's unique identifier.
     * @return A User object if the employee exists; otherwise, null.
     */
    private User getUserByID(int employeeID) {
        // Fetch the employee's record from the database
        Map<String, String> employeeRecord = databaseProcessor.getEmployeeRecordsByEmployeeId(String.valueOf(employeeID));

        if (employeeRecord != null) {
            try {
                return createUserFromRecord(employeeRecord); // Create the appropriate User object
            } catch (Exception e) {
                System.err.println("Error creating user from record: " + e.getMessage());
                // Debug information
                System.err.println("Employee ID: " + employeeID);
                if (employeeRecord != null) {
                    for (Map.Entry<String, String> entry : employeeRecord.entrySet()) {
                        System.err.println(entry.getKey() + ": " + entry.getValue());
                    }
                }
                return null;
            }
        }
        return null; // Employee not found
    }

     /**
     * Creates a User object based on the employee record.
     * @param record The employee's data retrieved from the database.
     * @return A specific User object depending on their role.
     */
    private User createUserFromRecord(Map<String, String> record) {
        // Validating the record has all required fields
        String[] requiredFields = {"Employee ID", "First Name", "Last Name", "Position"};
        for (String field : requiredFields) {
            if (record.get(field) == null || record.get(field).isEmpty()) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
        }
        
        int employeeID = Integer.parseInt(record.get("Employee ID"));
        String firstName = record.get("First Name");
        String lastName = record.get("Last Name");
        String email = record.get("Email");
        String password = record.get("Password");
        String position = record.get("Position");
        
        // Debug information
        System.out.println("Creating user with position: " + position);

        // Determine the role based on the position
        String role = determineUserRole(position);

        // Create the appropriate User object based on the role
        switch (role) {
            case "HR":
                return new HR(employeeID, firstName, lastName, email, password, role);
            case "EMPLOYEE":
                return new Employee(employeeID, firstName, lastName, email, password, role);
            case "IT":
                return new IT(employeeID, firstName, lastName, email, password, role);
            case "IMMEDIATE SUPERVISOR":
                return new ImmediateSupervisor(employeeID, firstName, lastName, email, password, role);
            case "ACCOUNTING":
                return new Accounting(employeeID, firstName, lastName, email, password, role);
            
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

     /**
     * Determines the user's role based on their job position.
     * @param position The job title of the user.
     * @return The role category (e.g., "HR", "EMPLOYEE", "IT", etc.).
     */
    private String determineUserRole(String position) {
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
            return "IMMEDIATE SUPERVISOR";
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
            position = position.toLowerCase();
            
            if (position.contains("hr") || position.contains("human resource")) {
                return "HR";
            }
            else if (position.contains("it") || position.contains("information tech") || position.contains("system")) {
                return "IT";
            }
            else if (position.contains("account") || position.contains("payroll") || position.contains("financ")) {
                return "ACCOUNTING";
            }
            else if (position.contains("manager") || position.contains("supervisor") || position.contains("lead")) {
                return "IMMEDIATE SUPERVISOR";
            }
            else {
                System.out.println("Unknown position detected: '" + position + "'. Defaulting to EMPLOYEE role.");
                return "EMPLOYEE"; // Default to employee
            }
        }
    }
}