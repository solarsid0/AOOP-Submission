/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oop.classes.actors;

import java.util.List;
import oop.classes.calculations.LeaveBalance;
import oop.classes.empselfservice.LeaveRequest;
import oop.classes.empselfservice.Payslip;
import oop.classes.management.AttendanceTracking;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

public class Employee extends User {
    // defining attributes that are protected so subclasses can access them
    protected LocalDate birthday;
    protected String address;
    protected String phoneNo;
    protected String sssNo;
    protected String philhealthNo;
    protected String pagibigNo;
    protected String tinNo;
    protected String position;
    protected String department;
    protected String supervisorName; //stores supervisor's name
    protected Employee supervisor; // stores actual supervisor object
    protected String empStatus;
    protected double basicSalary;
    protected double riceSubsidy;
    protected double phoneAllowance;
    protected double clothingAllowance;
    protected double grossSemiMthlyRate;
    protected double hourlyRate;
    protected List<Employee> subordinates;

        // Simplified constructor for login
    public Employee(int employeeID, String firstName, String lastName, String email, String password, String role) {
        super(employeeID, firstName, lastName, email, password, role);
    }
    
    // full constructor to initialize all the properties of the employee object
    public Employee(int employeeID, String firstName, String lastName, String email, String password, String role, LocalDate birthday, String address, String phoneNo, String sssNo, String philhealthNo, String position, String pagibigNo, String empStatus, double basicSalary, double riceSubsidy, double phoneAllowance, double clothingAllowance, double grossSemiMthlyRate, double hourlyRate) {
        super(employeeID, firstName, lastName, email, password, role); // calling the parent constructor for common user info
        this.birthday = birthday;
        this.address = address;
        this.phoneNo = phoneNo;
        this.sssNo = sssNo;
        this.philhealthNo = philhealthNo;
        this.pagibigNo = pagibigNo;
        this.tinNo = tinNo;
        this.position = position;
        this.supervisor = supervisor;
        this.empStatus = empStatus;
        this.basicSalary = basicSalary;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        this.grossSemiMthlyRate = grossSemiMthlyRate;
        this.hourlyRate = hourlyRate;
        this.subordinates = new ArrayList<>();  // Initialize subordinates list; for Immediate Sipervisors
    }
    

    // Static factory method to create Employee from CSV record
    public static Employee createEmployeeFromCSVRecord(Map<String, String> record) {
        int employeeID = Integer.parseInt(record.get("Employee ID"));
        String firstName = record.get("First Name");
        String lastName = record.get("Last Name");
        String email = record.get("Email");
        String password = record.get("Password");
        String role = String.valueOf(record.get("Role"));

        LocalDate birthday = LocalDate.parse(record.get("Birthday"), java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String address = record.get("Address");
        String phoneNo = record.get("Phone Number");
        String sssNo = record.get("SSS #");
        String philhealthNo = record.get("Philhealth #");
        String pagibigNo = record.get("Pag-ibig #");
        String tinNo = record.get("TIN #");
        String position = String.valueOf(record.get("Position"));
        String empStatus = String.valueOf(record.get("Status"));

        double basicSalary = Double.parseDouble(record.get("Basic Salary").replace(",", ""));
        double riceSubsidy = Double.parseDouble(record.get("Rice Subsidy").replace(",", ""));
        double phoneAllowance = Double.parseDouble(record.get("Phone Allowance").replace(",", ""));
        double clothingAllowance = Double.parseDouble(record.get("Clothing Allowance").replace(",", ""));
        double grossSemiMthlyRate = Double.parseDouble(record.get("Gross Semi-monthly Rate").replace(",", ""));
        double hourlyRate = Double.parseDouble(record.get("Hourly Rate").replace(",", ""));

        Employee employee = new Employee(employeeID, firstName, lastName, email, password, role, birthday, address, phoneNo, sssNo, philhealthNo, position, pagibigNo, empStatus, basicSalary, riceSubsidy, phoneAllowance, clothingAllowance, grossSemiMthlyRate, hourlyRate);

        // Set supervisor name (if available)
        String supervisorName = record.get("Immediate Supervisor");
        if (supervisorName != null && !supervisorName.isEmpty() && !supervisorName.equals("N/A")) {
            employee.setSupervisorName(supervisorName);
        }

        return employee;
    }

    // getters for all the attributes to return respective values
    public LocalDate getBirthday() {
        return birthday;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public String getSssNo() {
        return sssNo;
    }

    public String getPhilhealthNo() {
        return philhealthNo;
    }

    public String getPagibigNo() {
        return pagibigNo;
    }

    public String getTinNo() {
        return tinNo;
    }

    public String getPosition() {
        return position;
    }

    public String getEmpStatus() {
        return empStatus;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public double getGrossSemiMthlyRate() {
        return grossSemiMthlyRate;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

        //Getters and Setters to handle supervisor names and link employees to them
        public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public void setSupervisor(Employee supervisor) {
        this.supervisor = supervisor;
    }

    public Employee getSupervisor() {
        return supervisor;
    }

    public List<Employee> getSubordinates() {
        return subordinates;
    }

    public void addSubordinate(Employee subordinate) {
        if (subordinates == null) {
            subordinates = new ArrayList<>();
        }
        subordinates.add(subordinate);
    }
    
    // this method returns the full name of the employee by combining first and last name
    public String getFullName() {
        return getFirstName() + " " + getLastName(); // using firstName and lastName inherited from User class
    }
    
    /**
     * Allows system to update employee basic monthly salary
     * @param basicSalary 
     */
    public void setBasicSalary(double basicSalary) {
    this.basicSalary = basicSalary;
}
    /**
     * setting employee hourly pay rate
     * @param hourlyRate 
     * 
     */
    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    /**
     *
     * @return
     */
    @Override
    public Employee viewEmployeeDetails() {
        return this; // return the current employee object
    }

    @Override
    public void logout() {
        super.logout();
    }

    @Override
    public String toString() {
        return "Employee: " + getFullName() + ", Department: " + department + ", Position: " + position;
    }

    // this method is for viewing leave requests (empty logic for now)
    public List<LeaveRequest> viewLeaveRequest() {
        return null;
    }

    // this method returns the leave balance (empty logic for now)
    public List<LeaveBalance> viewLeaveBalance() {
        return null;
    }

    public boolean submitLeaveRequest(LeaveRequest leaveRequest) {
        return true;
    }

    public List<AttendanceTracking> viewAttendanceTracking() {
        return null;
    }

    public Payslip viewPayrollDetails(LocalDate payPeriod) {
        return null;
    }

    public Payslip viewPayrollDetails() {
        return viewPayrollDetails(LocalDate.now());
    }

    public void downloadPayrollDetails(int payslipID) {
        // placeholder logic for downloading payroll details
    }

    public String getStatus() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}