package oop.classes.actors;

import Models.EmployeeModel;
import Models.PayrollModel;
import Models.UserAuthenticationModel;
import DAOs.*;
import oop.classes.enums.ApprovalStatus;
import Utility.PasswordHasher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * HR actor class - handles employee management, payroll processing, and report generation
 * Inherits from Employee and adds HR-specific database operations
 */
public class HR extends EmployeeModel {
    
    private EmployeeDAO employeeDAO;
    private PayrollDAO payrollDAO;
    private UserAuthenticationDAO userAuthDAO;
    private LeaveRequestDAO leaveRequestDAO;
    private AttendanceDAO attendanceDAO;
    private DeductionDAO deductionDAO;
    private PositionDAO positionDAO;
    
    public HR() {
        super();
        initializeDAOs();
    }
    
    public HR(int employeeId, String firstName, String lastName, String email, String userRole) {
        super(employeeId, firstName, lastName, email, userRole);
        initializeDAOs();
    }
    
    private void initializeDAOs() {
        this.employeeDAO = new EmployeeDAO();
        this.payrollDAO = new PayrollDAO();
        this.userAuthDAO = new UserAuthenticationDAO();
        this.leaveRequestDAO = new LeaveRequestDAO();
        this.attendanceDAO = new AttendanceDAO();
        this.deductionDAO = new DeductionDAO();
        this.positionDAO = new PositionDAO();
    }
    
    // Employee Management Methods
    
    /**
     * Creates a new employee record
     * @param employee The employee to create
     * @param initialPassword Initial password for the employee
     * @return true if successful, false otherwise
     */
    public boolean createEmployee(EmployeeModel employee, String initialPassword) {
        try {
            // Validate password requirements
            if (!PasswordHasher.isPasswordValid(initialPassword)) {
                System.err.println("Password does not meet requirements: " + PasswordHasher.getPasswordRequirements());
                return false;
            }
            
            // Hash the password
            String hashedPassword = PasswordHasher.hashPassword(initialPassword);
            employee.setPasswordHash(hashedPassword);
            
            // Create employee record
            boolean success = employeeDAO.addEmployee(employee);
            
            if (success) {
                System.out.println("Employee created successfully: " + employee.getFirstName() + " " + employee.getLastName());
                // Could add audit logging here
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error creating employee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates an existing employee record
     * @param employee The employee with updated information
     * @return true if successful, false otherwise
     */
    public boolean updateEmployee(EmployeeModel employee) {
        try {
            boolean success = employeeDAO.updateEmployee(employee);
            if (success) {
                System.out.println("Employee updated successfully: " + employee.getEmployeeId());
            }
            return success;
        } catch (Exception e) {
            System.err.println("Error updating employee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Terminates an employee
     * @param employeeId The ID of the employee to terminate
     * @param reason Reason for termination
     * @return true if successful, false otherwise
     */
    public boolean terminateEmployee(int employeeId, String reason) {
        try {
            EmployeeModel employee = employeeDAO.getEmployeeById(employeeId);
            if (employee == null) {
                System.err.println("Employee not found: " + employeeId);
                return false;
            }
            
            employee.setStatus("Terminated");
            boolean success = employeeDAO.updateEmployee(employee);
            
            if (success) {
                // Also deactivate user account
                userAuthDAO.deactivateUser(employeeId);
                System.out.println("Employee terminated: " + employeeId + " - Reason: " + reason);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error terminating employee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets all employees
     * @return List of all employees
     */
    public List<EmployeeModel> getAllEmployees() {
        return employeeDAO.getAllEmployees();
    }
    
    /**
     * Gets employees by department
     * @param department Department name
     * @return List of employees in the department
     */
    public List<EmployeeModel> getEmployeesByDepartment(String department) {
        return employeeDAO.getEmployeesByDepartment(department);
    }
    
    /**
     * Gets employees by status
     * @param status Employee status (Regular, Probationary, Terminated)
     * @return List of employees with the specified status
     */
    public List<EmployeeModel> getEmployeesByStatus(String status) {
        return employeeDAO.getEmployeesByStatus(status);
    }
    
    // Payroll Management Methods
    
    /**
     * Processes payroll for all employees for a specific pay period
     * @param payPeriodId The pay period ID
     * @return true if successful, false otherwise
     */
    public boolean processPayrollForPeriod(int payPeriodId) {
        try {
            List<EmployeeModel> activeEmployees = employeeDAO.getEmployeesByStatus("Regular");
            activeEmployees.addAll(employeeDAO.getEmployeesByStatus("Probationary"));
            
            int processedCount = 0;
            for (EmployeeModel employee : activeEmployees) {
                if (processEmployeePayroll(employee.getEmployeeId(), payPeriodId)) {
                    processedCount++;
                }
            }
            
            System.out.println("Payroll processed for " + processedCount + " employees in pay period " + payPeriodId);
            return processedCount > 0;
            
        } catch (Exception e) {
            System.err.println("Error processing payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Processes payroll for a specific employee
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return true if successful, false otherwise
     */
    public boolean processEmployeePayroll(int employeeId, int payPeriodId) {
        try {
            EmployeeModel employee = employeeDAO.getEmployeeById(employeeId);
            if (employee == null) {
                return false;
            }
            
            // Calculate payroll components
            PayrollModel payroll = calculatePayroll(employee, payPeriodId);
            
            // Save payroll record
            return payrollDAO.addPayroll(payroll);
            
        } catch (Exception e) {
            System.err.println("Error processing employee payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculates payroll for an employee
     * @param employee The employee
     * @param payPeriodId Pay period ID
     * @return PayrollModel with calculated values
     */
    private PayrollModel calculatePayroll(EmployeeModel employee, int payPeriodId) {
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(employee.getEmployeeId());
        payroll.setPayPeriodId(payPeriodId);
        payroll.setBasicSalary(employee.getBasicSalary());
        
        // Calculate gross income (this is simplified - you'd need actual attendance/overtime data)
        BigDecimal grossIncome = employee.getBasicSalary();
        payroll.setGrossIncome(grossIncome);
        
        // Calculate deductions (simplified)
        BigDecimal totalDeductions = calculateTotalDeductions(employee, grossIncome);
        payroll.setTotalDeduction(totalDeductions);
        
        // Calculate benefits (simplified)
        BigDecimal totalBenefits = calculateTotalBenefits(employee);
        payroll.setTotalBenefit(totalBenefits);
        
        // Calculate net salary
        BigDecimal netSalary = grossIncome.add(totalBenefits).subtract(totalDeductions);
        payroll.setNetSalary(netSalary);
        
        return payroll;
    }
    
    /**
     * Calculates total deductions for an employee
     * @param employee The employee
     * @param grossIncome Gross income for the period
     * @return Total deductions amount
     */
    private BigDecimal calculateTotalDeductions(EmployeeModel employee, BigDecimal grossIncome) {
        // This is a simplified calculation - implement actual deduction logic
        BigDecimal sss = grossIncome.multiply(new BigDecimal("0.045")); // 4.5% SSS
        BigDecimal philhealth = grossIncome.multiply(new BigDecimal("0.0175")); // 1.75% PhilHealth
        BigDecimal pagibig = new BigDecimal("100.00"); // Fixed Pag-IBIG
        
        return sss.add(philhealth).add(pagibig);
    }
    
    /**
     * Calculates total benefits for an employee
     * @param employee The employee
     * @return Total benefits amount
     */
    private BigDecimal calculateTotalBenefits(EmployeeModel employee) {
        // This is simplified - implement actual benefit calculation based on position
        return new BigDecimal("2000.00"); // Fixed benefits for now
    }
    
    // Leave Management Methods
    
    /**
     * Approves a leave request
     * @param leaveRequestId Leave request ID
     * @param supervisorNotes Notes from supervisor
     * @return true if successful, false otherwise
     */
    public boolean approveLeaveRequest(int leaveRequestId, String supervisorNotes) {
        try {
            return leaveRequestDAO.updateLeaveRequestStatus(leaveRequestId, 
                    ApprovalStatus.APPROVED.getValue(), supervisorNotes);
        } catch (Exception e) {
            System.err.println("Error approving leave request: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Rejects a leave request
     * @param leaveRequestId Leave request ID
     * @param supervisorNotes Notes from supervisor
     * @return true if successful, false otherwise
     */
    public boolean rejectLeaveRequest(int leaveRequestId, String supervisorNotes) {
        try {
            return leaveRequestDAO.updateLeaveRequestStatus(leaveRequestId, 
                    ApprovalStatus.REJECTED.getValue(), supervisorNotes);
        } catch (Exception e) {
            System.err.println("Error rejecting leave request: " + e.getMessage());
            return false;
        }
    }
    
    // User Management Methods
    
    /**
     * Creates a new user account
     * @param email User email
     * @param password User password
     * @param userRole User role
     * @param firstName First name
     * @param lastName Last name
     * @param positionId Position ID
     * @return true if successful, false otherwise
     */
    public boolean createUserAccount(String email, String password, String userRole, 
                                   String firstName, String lastName, int positionId) {
        try {
            if (userAuthDAO.emailExists(email)) {
                System.err.println("Email already exists: " + email);
                return false;
            }
            
            return userAuthDAO.createUser(email, password, userRole, firstName, lastName, positionId);
        } catch (Exception e) {
            System.err.println("Error creating user account: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Resets a user's password
     * @param employeeId Employee ID
     * @param newPassword New password
     * @return true if successful, false otherwise
     */
    public boolean resetUserPassword(int employeeId, String newPassword) {
        try {
            return userAuthDAO.updatePassword(employeeId, newPassword);
        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
    }
    
    // Report Generation Methods
    
    /**
     * Generates employee summary report
     * @return Report data as string
     */
    public String generateEmployeeReport() {
        try {
            List<EmployeeModel> employees = getAllEmployees();
            StringBuilder report = new StringBuilder();
            report.append("EMPLOYEE SUMMARY REPORT\n");
            report.append("Generated: ").append(LocalDate.now()).append("\n\n");
            
            int regular = 0, probationary = 0, terminated = 0;
            
            for (EmployeeModel emp : employees) {
                switch (emp.getStatus()) {
                    case "Regular": regular++; break;
                    case "Probationary": probationary++; break;
                    case "Terminated": terminated++; break;
                }
            }
            
            report.append("Total Employees: ").append(employees.size()).append("\n");
            report.append("Regular: ").append(regular).append("\n");
            report.append("Probationary: ").append(probationary).append("\n");
            report.append("Terminated: ").append(terminated).append("\n");
            
            return report.toString();
        } catch (Exception e) {
            System.err.println("Error generating employee report: " + e.getMessage());
            return "Error generating report";
        }
    }
    
    /**
     * Generates payroll summary report for a pay period
     * @param payPeriodId Pay period ID
     * @return Report data as string
     */
    public String generatePayrollReport(int payPeriodId) {
        try {
            List<PayrollModel> payrolls = payrollDAO.getPayrollsByPeriod(payPeriodId);
            StringBuilder report = new StringBuilder();
            report.append("PAYROLL SUMMARY REPORT\n");
            report.append("Pay Period: ").append(payPeriodId).append("\n");
            report.append("Generated: ").append(LocalDate.now()).append("\n\n");
            
            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            BigDecimal totalDeductions = BigDecimal.ZERO;
            
            for (PayrollModel payroll : payrolls) {
                totalGross = totalGross.add(payroll.getGrossIncome());
                totalNet = totalNet.add(payroll.getNetSalary());
                totalDeductions = totalDeductions.add(payroll.getTotalDeduction());
            }
            
            report.append("Employees Processed: ").append(payrolls.size()).append("\n");
            report.append("Total Gross Pay: ").append(totalGross).append("\n");
            report.append("Total Deductions: ").append(totalDeductions).append("\n");
            report.append("Total Net Pay: ").append(totalNet).append("\n");
            
            return report.toString();
        } catch (Exception e) {
            System.err.println("Error generating payroll report: " + e.getMessage());
            return "Error generating report";
        }
    }
    
    @Override
    public String toString() {
        return "HR{" +
                "employeeId=" + getEmployeeId() +
                ", name='" + getFirstName() + " " + getLastName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getUserRole() + '\'' +
                '}';
    }
}