package oop.classes.actors;

import Models.*;
import Services.*;
import Services.PayrollService.PayrollProcessingResult;
import Services.LeaveService.LeaveApprovalResult;
import Services.OvertimeService.OvertimeApprovalResult;
import Services.ReportService.*;
import DAOs.*;
import oop.classes.enums.ApprovalStatus;
import Utility.PasswordHasher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;


/**
 * Enhanced HR Role Class - integrates with Services layer
 * Handles all HR-specific operations including employee management, 
 * payroll processing, leave management, and reporting
 */
public class HR extends EmployeeModel {
    
    // Service layer dependencies
    private final PayrollService payrollService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final OvertimeService overtimeService;
    private final ReportService reportService;
    
    // DAO dependencies for direct operations
    private final EmployeeDAO employeeDAO;
    private final UserAuthenticationDAO userAuthDAO;
    private final PayPeriodDAO payPeriodDAO;
    
    // HR Role Permissions
    private static final String[] HR_PERMISSIONS = {
        "MANAGE_EMPLOYEES", "PROCESS_PAYROLL", "APPROVE_LEAVES", 
        "GENERATE_REPORTS", "MANAGE_BENEFITS", "VIEW_ALL_ATTENDANCE",
        "MANAGE_DEDUCTIONS", "SYSTEM_ADMINISTRATION"
    };

    /**
     * Constructor for login purposes
     */
    public HR(int employeeId, String firstName, String lastName, String email, String userRole) {
        super(employeeId, firstName, lastName, email, userRole);
        
        // Initialize services
        DatabaseConnection dbConnection = new DatabaseConnection();
        this.payrollService = new PayrollService(dbConnection);
        this.attendanceService = new AttendanceService(dbConnection);
        this.leaveService = new LeaveService(dbConnection);
        this.overtimeService = new OvertimeService(dbConnection);
        this.reportService = new ReportService(dbConnection);
        
        // Initialize DAOs (using default constructors like in your original code)
        this.employeeDAO = new EmployeeDAO();
        this.userAuthDAO = new UserAuthenticationDAO();
        this.payPeriodDAO = new PayPeriodDAO();
        
        System.out.println("HR user initialized: " + getFullName());
    }

    // ================================
    // EMPLOYEE MANAGEMENT OPERATIONS
    // ================================

    /**
     * Creates a new employee with all required validations
     */
    public HROperationResult createEmployee(EmployeeModel employee, String initialPassword) {
        HROperationResult result = new HROperationResult();
        
        try {
            // Validate HR permissions
            if (!hasPermission("MANAGE_EMPLOYEES")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to create employees");
                return result;
            }

            // Validate password requirements
            if (!PasswordHasher.isPasswordValid(initialPassword)) {
                result.setSuccess(false);
                result.setMessage("Password does not meet requirements: " + 
                    PasswordHasher.getPasswordRequirements());
                return result;
            }

            // Check if email already exists
            if (userAuthDAO.emailExists(employee.getEmail())) {
                result.setSuccess(false);
                result.setMessage("Email already exists in system: " + employee.getEmail());
                return result;
            }

            // Hash password and create employee
            String hashedPassword = PasswordHasher.hashPassword(initialPassword);
            employee.setPasswordHash(hashedPassword);

            boolean success = employeeDAO.addEmployee(employee);
            
            if (success) {
                // Initialize leave balances for the new employee
                leaveService.initializeEmployeeLeaveBalances(employee.getEmployeeId(), 
                    LocalDate.now().getYear());
                
                result.setSuccess(true);
                result.setMessage("Employee created successfully: " + employee.getFullName());
                result.setEmployeeId(employee.getEmployeeId());
                
                logHRActivity("EMPLOYEE_CREATED", "Created employee: " + employee.getFullName());
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to create employee in database");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error creating employee: " + e.getMessage());
            System.err.println("HR Error creating employee: " + e.getMessage());
        }

        return result;
    }

    /**
     * Updates employee information
     */
    public HROperationResult updateEmployee(EmployeeModel employee) {
        HROperationResult result = new HROperationResult();
        
        try {
            if (!hasPermission("MANAGE_EMPLOYEES")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to update employees");
                return result;
            }

            boolean success = employeeDAO.updateEmployee(employee);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("Employee updated successfully: " + employee.getEmployeeId());
                logHRActivity("EMPLOYEE_UPDATED", "Updated employee: " + employee.getEmployeeId());
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to update employee");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error updating employee: " + e.getMessage());
        }

        return result;
    }

    /**
     * Terminates an employee
     */
    public HROperationResult terminateEmployee(int employeeId, String reason) {
        HROperationResult result = new HROperationResult();
        
        try {
            if (!hasPermission("MANAGE_EMPLOYEES")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to terminate employees");
                return result;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            employee.setStatus("Terminated");
            boolean success = employeeDAO.update(employee);
            
            if (success) {
                // Deactivate user account
                userAuthDAO.deactivateUser(employeeId);
                
                result.setSuccess(true);
                result.setMessage("Employee terminated successfully");
                logHRActivity("EMPLOYEE_TERMINATED", 
                    "Terminated employee: " + employeeId + " - Reason: " + reason);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to terminate employee");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error terminating employee: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // PAYROLL MANAGEMENT OPERATIONS
    // ================================

    /**
     * Processes payroll for all employees in a pay period
     */
    public PayrollService.PayrollProcessingResult processPayrollForPeriod(Integer payPeriodId) {
        try {
            if (!hasPermission("PROCESS_PAYROLL")) {
                PayrollService.PayrollProcessingResult result = new PayrollService.PayrollProcessingResult();
                result.setSuccess(false);
                result.addError("Insufficient permissions to process payroll");
                return result;
            }

            PayrollService.PayrollProcessingResult result = payrollService.processPayrollForPeriod(payPeriodId);
            
            if (result.isSuccess()) {
                logHRActivity("PAYROLL_PROCESSED", 
                    "Processed payroll for period: " + payPeriodId + 
                    " - Employees: " + result.getProcessedEmployees());
            }

            return result;

        } catch (Exception e) {
            PayrollService.PayrollProcessingResult result = new PayrollService.PayrollProcessingResult();
            result.setSuccess(false);
            result.addError("Error processing payroll: " + e.getMessage());
            return result;
        }
    }

    /**
     * Processes payroll for a single employee
     */
    public HROperationResult processEmployeePayroll(int employeeId, int payPeriodId) {
        HROperationResult result = new HROperationResult();
        
        try {
            if (!hasPermission("PROCESS_PAYROLL")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to process payroll");
                return result;
            }

            boolean success = payrollService.processEmployeePayroll(employeeId, payPeriodId);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("Payroll processed successfully for employee: " + employeeId);
                logHRActivity("EMPLOYEE_PAYROLL_PROCESSED", 
                    "Processed payroll for employee: " + employeeId);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to process payroll for employee");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error processing payroll: " + e.getMessage());
        }

        return result;
    }

    /**
     * Gets payroll summary for a pay period
     */
    public PayrollDAO.PayrollSummary getPayrollSummary(Integer payPeriodId) {
        if (!hasPermission("PROCESS_PAYROLL")) {
            System.err.println("HR: Insufficient permissions to view payroll summary");
            return null;
        }
        
        return payrollService.getPayrollSummary(payPeriodId);
    }

    // ================================
    // LEAVE MANAGEMENT OPERATIONS
    // ================================

    /**
     * Approves a leave request
     */
    public LeaveApprovalResult approveLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        try {
            if (!hasPermission("APPROVE_LEAVES")) {
                LeaveApprovalResult result = new LeaveApprovalResult();
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to approve leave requests");
                return result;
            }

            LeaveApprovalResult result = leaveService.approveLeaveRequest(
                leaveRequestId, getEmployeeId(), supervisorNotes);
            
            if (result.isSuccess()) {
                logHRActivity("LEAVE_APPROVED", "Approved leave request: " + leaveRequestId);
            }

            return result;

        } catch (Exception e) {
            LeaveApprovalResult result = new LeaveApprovalResult();
            result.setSuccess(false);
            result.setMessage("Error approving leave request: " + e.getMessage());
            return result;
        }
    }

    /**
     * Rejects a leave request
     */
    public LeaveApprovalResult rejectLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        try {
            if (!hasPermission("APPROVE_LEAVES")) {
                LeaveApprovalResult result = new LeaveApprovalResult();
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to reject leave requests");
                return result;
            }

            LeaveApprovalResult result = leaveService.rejectLeaveRequest(
                leaveRequestId, getEmployeeId(), supervisorNotes);
            
            if (result.isSuccess()) {
                logHRActivity("LEAVE_REJECTED", "Rejected leave request: " + leaveRequestId);
            }

            return result;

        } catch (Exception e) {
            LeaveApprovalResult result = new LeaveApprovalResult();
            result.setSuccess(false);
            result.setMessage("Error rejecting leave request: " + e.getMessage());
            return result;
        }
    }

    /**
     * Gets all pending leave requests
     */
    public List<LeaveRequestModel> getPendingLeaveRequests() {
        if (!hasPermission("APPROVE_LEAVES")) {
            System.err.println("HR: Insufficient permissions to view leave requests");
            return List.of();
        }
        
        return leaveService.getPendingLeaveRequests();
    }

    /**
     * Gets leave summary for an employee
     */
    public LeaveService.LeaveSummary getEmployeeLeaveSummary(Integer employeeId, Integer year) {
        if (!hasPermission("APPROVE_LEAVES")) {
            System.err.println("HR: Insufficient permissions to view leave summaries");
            return null;
        }
        
        return leaveService.getEmployeeLeaveSummary(employeeId, year);
    }

    // ================================
    // ATTENDANCE MANAGEMENT OPERATIONS
    // ================================

    /**
     * Gets daily attendance report
     */
    public List<AttendanceService.DailyAttendanceRecord> getDailyAttendanceReport(LocalDate date) {
        if (!hasPermission("VIEW_ALL_ATTENDANCE")) {
            System.err.println("HR: Insufficient permissions to view attendance reports");
            return List.of();
        }
        
        return attendanceService.getDailyAttendanceReport(date);
    }

    /**
     * Gets monthly attendance summary for an employee
     */
    public AttendanceService.AttendanceSummary getMonthlyAttendanceSummary(
            Integer employeeId, YearMonth yearMonth) {
        if (!hasPermission("VIEW_ALL_ATTENDANCE")) {
            System.err.println("HR: Insufficient permissions to view attendance summaries");
            return null;
        }
        
        return attendanceService.getMonthlyAttendanceSummary(employeeId, yearMonth);
    }

    /**
     * Gets employees with perfect attendance
     */
    public List<Integer> getEmployeesWithPerfectAttendance(YearMonth yearMonth) {
        if (!hasPermission("VIEW_ALL_ATTENDANCE")) {
            System.err.println("HR: Insufficient permissions to view attendance data");
            return List.of();
        }
        
        return attendanceService.getEmployeesWithPerfectAttendance(yearMonth);
    }

    // ================================
    // OVERTIME MANAGEMENT OPERATIONS
    // ================================

    /**
     * Approves overtime request
     */
    public OvertimeService.OvertimeApprovalResult approveOvertimeRequest(
            Integer overtimeRequestId, String supervisorNotes) {
        try {
            if (!hasPermission("APPROVE_LEAVES")) { // Using same permission as leaves
                OvertimeService.OvertimeApprovalResult result = new OvertimeService.OvertimeApprovalResult();
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to approve overtime requests");
                return result;
            }

            OvertimeService.OvertimeApprovalResult result = overtimeService.approveOvertimeRequest(
                overtimeRequestId, getEmployeeId(), supervisorNotes);
            
            if (result.isSuccess()) {
                logHRActivity("OVERTIME_APPROVED", "Approved overtime request: " + overtimeRequestId);
            }

            return result;

        } catch (Exception e) {
            OvertimeService.OvertimeApprovalResult result = new OvertimeService.OvertimeApprovalResult();
            result.setSuccess(false);
            result.setMessage("Error approving overtime request: " + e.getMessage());
            return result;
        }
    }

    /**
     * Gets pending overtime requests
     */
    public List<OvertimeRequestModel> getPendingOvertimeRequests() {
        if (!hasPermission("APPROVE_LEAVES")) {
            System.err.println("HR: Insufficient permissions to view overtime requests");
            return List.of();
        }
        
        return overtimeService.getPendingOvertimeRequests();
    }

    // ================================
    // REPORTING OPERATIONS
    // ================================

    /**
     * Generates comprehensive payroll report
     */
    public ReportService.PayrollReport generatePayrollReport(Integer payPeriodId) {
        if (!hasPermission("GENERATE_REPORTS")) {
            ReportService.PayrollReport report = new ReportService.PayrollReport();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate reports");
            return report;
        }
        
        ReportService.PayrollReport report = reportService.generatePayrollReport(payPeriodId);
        
        if (report.isSuccess()) {
            logHRActivity("REPORT_GENERATED", "Generated payroll report for period: " + payPeriodId);
        }
        
        return report;
    }

    /**
     * Generates daily attendance report
     */
    public ReportService.AttendanceReport generateDailyAttendanceReport(LocalDate date) {
        if (!hasPermission("GENERATE_REPORTS")) {
            ReportService.AttendanceReport report = new ReportService.AttendanceReport();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate reports");
            return report;
        }
        
        return reportService.generateDailyAttendanceReport(date);
    }

    /**
     * Generates monthly attendance report
     */
    public ReportService.MonthlyAttendanceReport generateMonthlyAttendanceReport(YearMonth yearMonth) {
        if (!hasPermission("GENERATE_REPORTS")) {
            ReportService.MonthlyAttendanceReport report = new ReportService.MonthlyAttendanceReport();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate reports");
            return report;
        }
        
        return reportService.generateMonthlyAttendanceReport(yearMonth);
    }

    /**
     * Generates leave report
     */
    public ReportService.LeaveReport generateLeaveReport(Integer year) {
        if (!hasPermission("GENERATE_REPORTS")) {
            ReportService.LeaveReport report = new ReportService.LeaveReport();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate reports");
            return report;
        }
        
        return reportService.generateLeaveReport(year);
    }

    /**
     * Generates overtime report
     */
    public ReportService.OvertimeReport generateOvertimeReport(LocalDate startDate, LocalDate endDate) {
        if (!hasPermission("GENERATE_REPORTS")) {
            ReportService.OvertimeReport report = new ReportService.OvertimeReport();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate reports");
            return report;
        }
        
        return reportService.generateOvertimeReport(startDate, endDate);
    }

    /**
     * Generates compliance report
     */
    public ReportService.ComplianceReport generateComplianceReport(YearMonth yearMonth) {
        if (!hasPermission("GENERATE_REPORTS")) {
            ReportService.ComplianceReport report = new ReportService.ComplianceReport();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate reports");
            return report;
        }
        
        return reportService.generateComplianceReport(yearMonth);
    }

    // ================================
    // UTILITY AND HELPER METHODS
    // ================================

    /**
     * Checks if HR user has specific permission
     */
    private boolean hasPermission(String permission) {
        // HR role has all permissions in this system
        for (String hrPermission : HR_PERMISSIONS) {
            if (hrPermission.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all HR permissions
     */
    public String[] getHRPermissions() {
        return HR_PERMISSIONS.clone();
    }

    /**
     * Logs HR activities for audit purposes
     */
    private void logHRActivity(String action, String details) {
        try {
            String logMessage = String.format("[HR AUDIT] %s - %s: %s (Performed by: %s - ID: %d)",
                LocalDate.now(), action, details, getFullName(), getEmployeeId());
            System.out.println(logMessage);
            
            // In a real implementation, you'd save this to an audit log table
            // auditLogDAO.saveLog(getEmployeeId(), action, details, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("Error logging HR activity: " + e.getMessage());
        }
    }

    /**
     * Gets employee by ID (with permission check)
     */
    public EmployeeModel getEmployeeById(int employeeId) {
        if (!hasPermission("MANAGE_EMPLOYEES")) {
            System.err.println("HR: Insufficient permissions to view employee details");
            return null;
        }
        
        return employeeDAO.findById(employeeId);
    }

    /**
     * Gets all employees (with permission check)
     */
    public List<EmployeeModel> getAllEmployees() {
        if (!hasPermission("MANAGE_EMPLOYEES")) {
            System.err.println("HR: Insufficient permissions to view all employees");
            return List.of();
        }
        
        return employeeDAO.getActiveEmployees();
    }

    /**
     * Creates user account for employee
     */
    public HROperationResult createUserAccount(String email, String password, String userRole,
            String firstName, String lastName, int positionId) {
        HROperationResult result = new HROperationResult();
        
        try {
            if (!hasPermission("SYSTEM_ADMINISTRATION")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to create user accounts");
                return result;
            }

            if (userAuthDAO.emailExists(email)) {
                result.setSuccess(false);
                result.setMessage("Email already exists: " + email);
                return result;
            }

            boolean success = userAuthDAO.createUser(email, password, userRole, firstName, lastName, positionId);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("User account created successfully: " + email);
                logHRActivity("USER_CREATED", "Created user account: " + email + " (" + userRole + ")");
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to create user account");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error creating user account: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String toString() {
        return "HR{" +
                "employeeId=" + getEmployeeId() +
                ", name='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", permissions=" + java.util.Arrays.toString(HR_PERMISSIONS) +
                '}';
    }

    // ================================
    // INNER CLASSES
    // ================================

    /**
     * Result class for HR operations
     */
    public static class HROperationResult {
        private boolean success = false;
        private String message = "";
        private Integer employeeId;
        private Map<String, Object> additionalData;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

        @Override
        public String toString() {
            return "HROperationResult{success=" + success + ", message='" + message + "'}";
        }
    }
}