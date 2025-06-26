package oop.classes.actors;

import java.time.LocalDate;


public class IT extends Employee {
    
    //contructor with basic parameters for login
    public IT (int employeeID, String firstName, String lastName, String email, String password, String role) {
        super(employeeID, firstName, lastName, email, password, role);
    }
    
    // Constructor for IT class, extends Employee class
    public IT(int employeeID, String firstName, String lastName, String address, String phoneNo,
              String email, String password, String role, LocalDate birthday,
              String sssNo, String philhealthNo, String pagibigNo,
              String tinNo, String position, String department,
              Employee supervisor, String empStatus,
              double basicSalary, double riceSubsidy, double phoneAllowance,
              double clothingAllowance, double grossSemiMthlyRate,
              double hourlyRate, double vacationLeave, double sickLeave, double emergencyLeave,
              double maternityLeave, double paternityLeave, double specialLeave) {
        super(employeeID, firstName, lastName, email, password, role,
                birthday, address, phoneNo, sssNo, philhealthNo, position,
                pagibigNo, empStatus, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMthlyRate, hourlyRate);
    }

    // Method to create user credentials (username and password) for an employee
    public boolean createUserCredentials(int employeeID, String email, String password, String role) {
        return true; // Assuming credentials are created successfully
    }

    // Method to edit user credentials (username and password) for an employee
    public boolean editUserCredentials(int employeeID, String newEmail, String newPassword) {
        return true; // Assuming credentials are edited successfully
    }

    // Method to delete a user by employeeID
    public boolean deleteUser(int employeeID) {
        return true; // Assuming the user is deleted successfully
    }

    // Method to set a new username for a user (private method)
    @Override
    public void setEmail(String email) {
        super.setEmail(email); // Setting the username
    }

    // Method to set a new password for a user (private method)

    /**
     *
     * @param password
     */
    @Override
    public void setPassword(String password) {
        super.setPassword(password); // Setting the password
    }
}