/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oop.classes.empselfservice;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import CSV.CSVDatabaseProcessor;

/**
 * This class handles an employee's daily attendance record.
 * Tracks login/logout times, calculates total hours worked,and determines late hours based on a grace period.
 * There's a minus 1 hour in total hours worked cause of the 1 hr. unpaid break every work day.
 */
public class AttendanceDetails {
    // Defining attributes ; Employee details
    private final String employeeId;
    private final LocalDate date;
    private final LocalTime logIn;
    private final LocalTime logOut;

    private final double lateHours;
    private final double hoursWorked;
    private final double overtimeHours;

    // Fixed values in the code 
    private static final LocalTime GRACE_PERIOD_END = LocalTime.of(8, 10); // 8:10 AM grace period based on MPH website
    private static final double STANDARD_WORK_HOURS = 8.0; // Standard company work hours
    private static final double LUNCH_BREAK_HOURS = 1.0; // Unpaid Lunch break

    /**
     * Constructor to initialize attendance details with login and logout times.
     * @param employeeId
     * @param date
     * @param logIn
     * @param logOut
     */
    public AttendanceDetails(String employeeId, LocalDate date, LocalTime logIn, LocalTime logOut) {
        this.employeeId = employeeId;
        this.date = date;
        this.logIn = logIn;
        this.logOut = logOut;
        this.lateHours = calculateLateHours();
        this.hoursWorked = calculateHoursWorked();
        this.overtimeHours = calculateOvertimeHours();
    }

    /**
     * Constructor for cases where attendance data is unavailable (ex: absent employee).
     * @param employeeId
     */
    public AttendanceDetails(String employeeId) {
        this.employeeId = employeeId;
        this.date = null;
        this.logIn = null;
        this.logOut = null;
        this.lateHours = 0.0;
        this.hoursWorked = 0.0;
        this.overtimeHours = 0.0;
    }

    /**
     * Determines if employee clocked in within the grace period (on or before 8:10 AM).
     * @return true if the employee clocked in within the grace period, otherwise false.
     */
    public boolean isWithinGracePeriod() {
        return logIn != null && !logIn.isAfter(GRACE_PERIOD_END);
    }

    /**
     * Calculates number of late hours based on the grace period.
     */
    private double calculateLateHours() {
        if (logIn == null || !logIn.isAfter(GRACE_PERIOD_END)) {
            return 0.0; // No late hours if login is before or within grace period
        }
        return Duration.between(GRACE_PERIOD_END, logIn).toMinutes() / 60.0;
    }

    /**
     * Calculates the total hours worked based on login and logout times.
     */
    private double calculateHoursWorked() {
        if (logIn == null || logOut == null) {
            return 0.0; // No work hours if no login or logout
        }

        double totalHours;

        // Handles cases where logout is past midnight
        if (logOut.isBefore(logIn)) {
            totalHours = (Duration.between(logIn, LocalTime.of(23, 59)).toMinutes() +
                    Duration.between(LocalTime.of(0, 0), logOut).toMinutes()) / 60.0;
        } else {
            totalHours = Duration.between(logIn, logOut).toMinutes() / 60.0;
        }

        // Subtract lunch break only if totalHours is greater than zero
        if (totalHours > 0) {
            totalHours = Math.max(0, totalHours - LUNCH_BREAK_HOURS);
        }

        return totalHours;
    }

    /**
     * Calculates overtime hours (any hours worked beyond the standard work hours).
     */
    private double calculateOvertimeHours() {
        return Math.max(0, hoursWorked - STANDARD_WORK_HOURS);
    }

     /**
     * Calculates the number of days an employee has worked within a given date range.
     * @param csvProcessor The CSV database processor to retrieve attendance records.
     * @param employeeId The employee's ID.
     * @param startDate The start date of the period.
     * @param endDate The end date of the period.
     * @return The number of days worked within the period.
     */
    public static int calculateDaysWorked(CSVDatabaseProcessor csvProcessor, String employeeId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> attendanceRecords = csvProcessor.getAttendanceRecordsByEmployeeId(employeeId);
        int daysWorked = 0;

        for (Map<String, Object> record : attendanceRecords) {
            LocalDate recordDate = (LocalDate) record.get("Date");
            if (recordDate != null && !recordDate.isBefore(startDate) && !recordDate.isAfter(endDate)) {
                LocalTime logIn = (LocalTime) record.get("Log In");
                LocalTime logOut = (LocalTime) record.get("Log Out");

                // Will count only if login and logout is valid
                if (logIn != null && logOut != null) {
                    daysWorked++;
                }
            }
        }
        return daysWorked;
    }

    // GETTERS 
    public String getEmployeeId() { return employeeId; }
    public LocalDate getDate() { return date; }
    public LocalTime getLogIn() { return logIn; }
    public LocalTime getLogOut() { return logOut; }
    public double getLateHours() { return lateHours; }
    public double getHoursWorked() { return hoursWorked; }
    public double getOvertimeHours() { return overtimeHours; }

    /**
     * Retrieves an employee's attendance details for a specific date from the CSV database.
     * @param csvProcessor
     * @param employeeId
     * @param date
     * @return 
     */
    public static AttendanceDetails getAttendanceDetailsByEmployeeIdAndDate(CSVDatabaseProcessor csvProcessor, String employeeId, LocalDate date) {
        List<Map<String, Object>> attendanceRecords = csvProcessor.getAttendanceRecordsByEmployeeId(employeeId);

        for (Map<String, Object> record : attendanceRecords) {
            LocalDate recordDate = (LocalDate) record.get("Date");
            if (recordDate != null && recordDate.equals(date)) {
                LocalTime logIn = (LocalTime) record.get("Log In");
                LocalTime logOut = (LocalTime) record.get("Log Out");
                return new AttendanceDetails(employeeId, recordDate, logIn, logOut);
            }
        }
        return new AttendanceDetails(employeeId); // Return empty attendance if no record is found
    }

}