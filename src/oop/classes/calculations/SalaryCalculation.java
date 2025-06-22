package oop.classes.calculations;

import CSV.CSVDatabaseProcessor;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import oop.classes.empselfservice.AttendanceDetails;

/**
 * This class handles the gross pay calculation for the month per employee.
 * @author Admin
 */
public class SalaryCalculation {

    // Overtime pay multiplier for regular employees (Rank and File)
    private static final double REGULAR_OVERTIME_MULTIPLIER = 1.25;

    /**
     * Calculates the gross monthly salary for an employee based on their attendance records.
     * 
     * @param employeeId   The ID of the employee.
     * @param payrollMonth The payroll month and year.
     * @param csvProcessor The CSV processor to fetch employee and attendance records.
     * @return The calculated gross monthly salary.
     * @throws IllegalArgumentException If any required data is missing or invalid.
     */
    public double calculateGrossMonthlySalary(String employeeId, YearMonth payrollMonth, CSVDatabaseProcessor csvProcessor) {
        // Validate that none of the required parameters are null
        Objects.requireNonNull(employeeId, "Employee ID cannot be null.");
        Objects.requireNonNull(payrollMonth, "Payroll Month cannot be null.");
        Objects.requireNonNull(csvProcessor, "CSV Processor cannot be null.");

        // Retrieve employee details from CSV database
        Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
        if (employeeData == null) {
            throw new IllegalArgumentException("Employee data not found for ID: " + employeeId);
        }

        // Extract the employee's position (used to determine overtime eligibility)
        String position = employeeData.get("Position");
        if (position == null) {
            throw new IllegalArgumentException("Position is missing in employee data for ID: " + employeeId);
        }

        // Get the employee's hourly rate
        double hourlyRate = getHourlyRate(employeeData);
        
        // Check if the employee is Rank and File (eligible for overtime pay)
        boolean isRankAndFile = position.toLowerCase().contains("rank and file");

        // Fetch the attendance records for the employee
        List<Map<String, Object>> attendanceRecords = csvProcessor.getAttendanceRecordsByEmployeeId(employeeId);
        if (attendanceRecords == null || attendanceRecords.isEmpty()) {
            System.out.println("No attendance records found for employee ID: " + employeeId);
            return 0.0; // Return zero salary if no attendance records exist
        }

        double totalGrossPay = 0.0; // Initialize gross pay accumulator

        // Loop through the employee's attendance records
        for (Map<String, Object> record : attendanceRecords) {
            LocalDate recordDate = (LocalDate) record.get("Date");
            
            // Process only the records that fall within the given payroll month
            if (recordDate != null && recordDate.getYear() == payrollMonth.getYear() 
                && recordDate.getMonth() == payrollMonth.getMonth()) {
                LocalTime logIn = (LocalTime) record.get("Log In");
                LocalTime logOut = (LocalTime) record.get("Log Out");

                // Ensure both login and logout times exist and are valid
                if (logIn != null && logOut != null && !logOut.isBefore(logIn)) {
                    AttendanceDetails attendanceDetails = new AttendanceDetails(employeeId, recordDate, logIn, logOut);
                    
                    double hoursWorked = attendanceDetails.getHoursWorked();
                    double overtimeHours = attendanceDetails.getOvertimeHours();
                    
                    // Overtime pay applies only to Rank and File employees
                    double overtimePay = isRankAndFile ? calculateOvertimePay(overtimeHours, hourlyRate) : 0;
                    
                    // Calculate daily earnings and add to total gross pay
                    totalGrossPay += (hoursWorked * hourlyRate) + overtimePay;
                } else {
                    System.out.println("Invalid log in/out times for employee ID: " + employeeId + " on date: " + recordDate);
                }
            }
        }

        return totalGrossPay; // Return the total computed gross salary for the month
    }

    /**
     * Calculates the overtime pay based on the given overtime hours and hourly rate.
     * 
     * @param overtimeHours Number of overtime hours worked.
     * @param hourlyRate    Employee's hourly rate.
     * @return The total overtime pay for the given period.
     */
    private double calculateOvertimePay(double overtimeHours, double hourlyRate) {
        return overtimeHours * hourlyRate * REGULAR_OVERTIME_MULTIPLIER;
    }

    /**
     * Retrieves and parses the hourly rate from employee data.
     * 
     * @param employeeData Employee record containing salary details.
     * @return The parsed hourly rate.
     * @throws IllegalArgumentException If the hourly rate is missing or invalid.
     */
    private double getHourlyRate(Map<String, String> employeeData) {
        String hourlyRateString = employeeData.get("Hourly Rate");
        if (hourlyRateString == null || hourlyRateString.isEmpty()) {
            throw new IllegalArgumentException("Hourly Rate is missing in employee data.");
        }
        try {
            double hourlyRate = Double.parseDouble(hourlyRateString);
            if (hourlyRate <= 0) {
                throw new IllegalArgumentException("Hourly Rate must be a positive value.");
            }
            return hourlyRate;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Hourly Rate format: " + hourlyRateString, e);
        }
    }
}