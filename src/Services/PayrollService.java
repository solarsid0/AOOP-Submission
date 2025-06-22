
package Services;
import DAOs.*;
import Models.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;

/**
 * PayrollService - Core business logic for payroll processing
 * This service orchestrates payroll calculations using multiple DAOs
 * @author User
 */

public class PayrollService {
   // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final PayrollDAO payrollDAO;
    private final PayslipDAO payslipDAO;
    private final AttendanceDAO attendanceDAO;
    private final OvertimeRequestDAO overtimeDAO;
    private final DeductionDAO deductionDAO;
    private final BenefitTypeDAO benefitDAO;
    private final PayPeriodDAO payPeriodDAO;
    
    /**
     * Constructor - initializes all required DAOs
     */
    public PayrollService() {
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.deductionDAO = new DeductionDAO();
        this.benefitDAO = new BenefitTypeDAO();
        this.payPeriodDAO = new PayPeriodDAO();
    }
    
    /**
     * Constructor with custom database connection (for dependency injection)
     */
    public PayrollService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.deductionDAO = new DeductionDAO();
        this.benefitDAO = new BenefitTypeDAO();
        this.payPeriodDAO = new PayPeriodDAO();
    }
    
    // ================================
    // MAIN PAYROLL PROCESSING METHODS
    // ================================
    
    /**
     * Processes payroll for all active employees in a pay period
     * @param payPeriodId The pay period to process
     * @return PayrollProcessingResult with summary information
     */
    public PayrollProcessingResult processPayrollForPeriod(Integer payPeriodId) {
        PayrollProcessingResult result = new PayrollProcessingResult();
        result.setPayPeriodId(payPeriodId);
        result.setProcessedDate(LocalDate.now());
        
        try {
            // Validate pay period exists
            PayPeriodModel payPeriod = payPeriodDAO.findById(payPeriodId);
            if (payPeriod == null) {
                result.setSuccess(false);
                result.addError("Pay period not found: " + payPeriodId);
                return result;
            }
            
            // Get all active employees
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();
            result.setTotalEmployees(activeEmployees.size());
            
            System.out.println("🔄 Processing payroll for " + activeEmployees.size() + " employees in pay period " + payPeriodId);
            
            // Process each employee
            for (EmployeeModel employee : activeEmployees) {
                try {
                    boolean success = processEmployeePayroll(employee.getEmployeeId(), payPeriodId);
                    if (success) {
                        result.incrementProcessedEmployees();
                        System.out.println("✅ Processed payroll for: " + employee.getFullName());
                    } else {
                        result.incrementFailedEmployees();
                        result.addError("Failed to process payroll for employee: " + employee.getEmployeeId());
                        System.out.println("❌ Failed to process payroll for: " + employee.getFullName());
                    }
                } catch (Exception e) {
                    result.incrementFailedEmployees();
                    result.addError("Error processing employee " + employee.getEmployeeId() + ": " + e.getMessage());
                    System.err.println("❌ Error processing employee " + employee.getFullName() + ": " + e.getMessage());
                }
            }
            
            // Calculate summary totals
            calculatePayrollSummary(result, payPeriodId);
            
            result.setSuccess(result.getFailedEmployees() == 0);
            System.out.println("🏁 Payroll processing completed. Success: " + result.getProcessedEmployees() + ", Failed: " + result.getFailedEmployees());
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.addError("Fatal error during payroll processing: " + e.getMessage());
            System.err.println("💥 Fatal error during payroll processing: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Processes payroll for a single employee
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return true if successful, false otherwise
     */
    public boolean processEmployeePayroll(Integer employeeId, Integer payPeriodId) {
        try {
            // Check if payroll already exists
            if (payrollDAO.isPayrollGenerated(payPeriodId) && hasEmployeePayroll(employeeId, payPeriodId)) {
                System.out.println("⚠️ Payroll already exists for employee " + employeeId + " in period " + payPeriodId);
                return true; // Consider existing payroll as success
            }
            
            // Get employee information
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                System.err.println("❌ Employee not found: " + employeeId);
                return false;
            }
            
            // Get pay period information
            PayPeriodModel payPeriod = payPeriodDAO.findById(payPeriodId);
            if (payPeriod == null) {
                System.err.println("❌ Pay period not found: " + payPeriodId);
                return false;
            }
            
            // Calculate payroll components
            PayrollCalculation calculation = calculateEmployeePayroll(employee, payPeriod);
            
            // Create and save payroll record
            PayrollModel payroll = createPayrollRecord(employee, payPeriodId, calculation);
            boolean payrollSaved = payrollDAO.save(payroll);
            
            if (!payrollSaved) {
                System.err.println("❌ Failed to save payroll record for employee: " + employeeId);
                return false;
            }
            
            // Generate and save payslip
            PayslipModel payslip = payslipDAO.generatePayslip(employeeId, payPeriodId);
            if (payslip == null) {
                System.err.println("⚠️ Warning: Failed to generate payslip for employee: " + employeeId);
                // Don't fail the entire process for payslip generation issues
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error processing payroll for employee " + employeeId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculates all payroll components for an employee
     * @param employee Employee model
     * @param payPeriod Pay period model
     * @return PayrollCalculation with all calculated values
     */
    public PayrollCalculation calculateEmployeePayroll(EmployeeModel employee, PayPeriodModel payPeriod) {
        PayrollCalculation calc = new PayrollCalculation();
        calc.setEmployeeId(employee.getEmployeeId());
        calc.setPayPeriodId(payPeriod.getPayPeriodId());
        
        // Basic salary (semi-monthly)
        BigDecimal basicSalary = employee.getBasicSalary();
        BigDecimal semiMonthlyBasic = basicSalary.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        calc.setBasicSalary(semiMonthlyBasic);
        
        // Calculate attendance-based earnings
        BigDecimal attendanceEarnings = calculateAttendanceEarnings(employee, payPeriod);
        calc.setAttendanceEarnings(attendanceEarnings);
        
        // Calculate overtime pay
        BigDecimal overtimePay = calculateOvertimePay(employee, payPeriod);
        calc.setOvertimePay(overtimePay);
        
        // Calculate benefits
        BigDecimal totalBenefits = calculateBenefits(employee, payPeriod);
        calc.setTotalBenefits(totalBenefits);
        
        // Calculate gross income
        BigDecimal grossIncome = semiMonthlyBasic
            .add(attendanceEarnings)
            .add(overtimePay)
            .add(totalBenefits);
        calc.setGrossIncome(grossIncome);
        
        // Calculate deductions
        BigDecimal totalDeductions = calculateDeductions(employee, grossIncome);
        calc.setTotalDeductions(totalDeductions);
        
        // Calculate net salary
        BigDecimal netSalary = grossIncome.subtract(totalDeductions);
        calc.setNetSalary(netSalary);
        
        return calc;
    }
    
    // ===============================
    // CALCULATION HELPER METHODS
    // ===============================
    
    /**
     * Calculates earnings based on attendance records
     */
    private BigDecimal calculateAttendanceEarnings(EmployeeModel employee, PayPeriodModel payPeriod) {
        try {
            // Get attendance records for the pay period
            List<AttendanceModel> attendanceRecords = attendanceDAO.getAttendanceHistory(
                employee.getEmployeeId(), 
                payPeriod.getStartDate(), 
                payPeriod.getEndDate()
            );
            
            BigDecimal totalHours = BigDecimal.ZERO;
            for (AttendanceModel attendance : attendanceRecords) {
                if (attendance.isComplete()) {
                    totalHours = totalHours.add(attendance.getHoursWorked());
                }
            }
            
            // Calculate earnings based on hours worked
            return totalHours.multiply(employee.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            System.err.println("Error calculating attendance earnings: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculates overtime pay from approved overtime requests
     */
    private BigDecimal calculateOvertimePay(EmployeeModel employee, PayPeriodModel payPeriod) {
        try {
            YearMonth yearMonth = YearMonth.from(payPeriod.getStartDate());
            return overtimeDAO.getTotalOvertimePay(
                employee.getEmployeeId(), 
                yearMonth.getYear(), 
                yearMonth.getMonthValue(), 
                new BigDecimal("1.5") // Time and a half
            );
        } catch (Exception e) {
            System.err.println("Error calculating overtime pay: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculates total benefits for an employee
     */
    private BigDecimal calculateBenefits(EmployeeModel employee, PayPeriodModel payPeriod) {
        try {
            if (employee.getPositionId() == null) {
                return BigDecimal.ZERO;
            }
            
            // Get benefits for employee's position
            List<java.util.Map<String, Object>> positionBenefits = 
                benefitDAO.getBenefitsForPosition(employee.getPositionId());
            
            BigDecimal totalBenefits = BigDecimal.ZERO;
            for (java.util.Map<String, Object> benefit : positionBenefits) {
                BigDecimal benefitValue = (BigDecimal) benefit.get("benefitValue");
                if (benefitValue != null) {
                    totalBenefits = totalBenefits.add(benefitValue);
                }
            }
            
            return totalBenefits;
            
        } catch (Exception e) {
            System.err.println("Error calculating benefits: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculates total deductions (government mandated + others)
     */
    private BigDecimal calculateDeductions(EmployeeModel employee, BigDecimal grossIncome) {
        try {
            BigDecimal basicSalary = employee.getBasicSalary();
            
            // SSS: 4.5% of basic salary (employee share)
            BigDecimal sss = basicSalary.multiply(new BigDecimal("0.045")).setScale(2, RoundingMode.HALF_UP);
            
            // PhilHealth: 2.75% of basic salary (employee share) 
            BigDecimal philhealth = basicSalary.multiply(new BigDecimal("0.0275")).setScale(2, RoundingMode.HALF_UP);
            
            // Pag-IBIG: 2% of basic salary (employee share)
            BigDecimal pagibig = basicSalary.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            
            // Withholding Tax: Based on tax brackets
            BigDecimal withholdingTax = calculateWithholdingTax(grossIncome);
            
            return sss.add(philhealth).add(pagibig).add(withholdingTax);
            
        } catch (Exception e) {
            System.err.println("Error calculating deductions: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculates withholding tax based on BIR tax brackets
     */
    private BigDecimal calculateWithholdingTax(BigDecimal grossIncome) {
        BigDecimal monthlyGross = grossIncome;
        
        // Tax brackets (monthly basis)
        if (monthlyGross.compareTo(new BigDecimal("20833")) <= 0) {
            return BigDecimal.ZERO; // No tax for income ≤ 250,000 annually
        } else if (monthlyGross.compareTo(new BigDecimal("33333")) <= 0) {
            // 20% tax rate
            return monthlyGross.subtract(new BigDecimal("20833"))
                              .multiply(new BigDecimal("0.20"))
                              .setScale(2, RoundingMode.HALF_UP);
        } else {
            // Higher tax rates (simplified)
            return monthlyGross.multiply(new BigDecimal("0.25"))
                              .setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    // ===============================
    // UTILITY AND HELPER METHODS
    // ===============================
    
    /**
     * Creates a PayrollModel from calculation results
     */
    private PayrollModel createPayrollRecord(EmployeeModel employee, Integer payPeriodId, PayrollCalculation calc) {
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(employee.getEmployeeId());
        payroll.setPayPeriodId(payPeriodId);
        payroll.setBasicSalary(calc.getBasicSalary());
        payroll.setGrossIncome(calc.getGrossIncome());
        payroll.setTotalBenefit(calc.getTotalBenefits());
        payroll.setTotalDeduction(calc.getTotalDeductions());
        payroll.setNetSalary(calc.getNetSalary());
        return payroll;
    }
    
    /**
     * Checks if an employee already has payroll for a period
     */
    private boolean hasEmployeePayroll(Integer employeeId, Integer payPeriodId) {
        try {
            List<PayrollModel> employeePayrolls = payrollDAO.findByEmployee(employeeId);
            return employeePayrolls.stream()
                    .anyMatch(p -> p.getPayPeriodId().equals(payPeriodId));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculates summary totals for payroll processing result
     */
    private void calculatePayrollSummary(PayrollProcessingResult result, Integer payPeriodId) {
        try {
            PayrollDAO.PayrollSummary summary = payrollDAO.getPayrollSummary(payPeriodId);
            result.setTotalGrossIncome(summary.getTotalGrossIncome());
            result.setTotalNetSalary(summary.getTotalNetSalary());
            result.setTotalDeductions(summary.getTotalDeductions());
            result.setTotalBenefits(summary.getTotalBenefits());
        } catch (Exception e) {
            System.err.println("Error calculating payroll summary: " + e.getMessage());
        }
    }
    
    // ===============================
    // PUBLIC QUERY METHODS
    // ===============================
    
    /**
     * Gets payroll records for a specific pay period
     */
    public List<PayrollModel> getPayrollForPeriod(Integer payPeriodId) {
        return payrollDAO.findByPayPeriod(payPeriodId);
    }
    
    /**
     * Gets payroll history for an employee
     */
    public List<PayrollModel> getEmployeePayrollHistory(Integer employeeId, int limit) {
        return payrollDAO.getPayrollHistory(employeeId, limit);
    }
    
    /**
     * Gets payroll summary for a pay period
     */
    public PayrollDAO.PayrollSummary getPayrollSummary(Integer payPeriodId) {
        return payrollDAO.getPayrollSummary(payPeriodId);
    }
    
    /**
     * Deletes payroll for a pay period (use with caution)
     */
    public int deletePayrollForPeriod(Integer payPeriodId) {
        return payrollDAO.deletePayrollByPeriod(payPeriodId);
    }
    
    // ===============================
    // INNER CLASSES
    // ===============================
    
    /**
     * Holds calculation results for an employee's payroll
     */
    public static class PayrollCalculation {
        private Integer employeeId;
        private Integer payPeriodId;
        private BigDecimal basicSalary = BigDecimal.ZERO;
        private BigDecimal attendanceEarnings = BigDecimal.ZERO;
        private BigDecimal overtimePay = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        private BigDecimal grossIncome = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal netSalary = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public Integer getPayPeriodId() { return payPeriodId; }
        public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
        
        public BigDecimal getBasicSalary() { return basicSalary; }
        public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
        
        public BigDecimal getAttendanceEarnings() { return attendanceEarnings; }
        public void setAttendanceEarnings(BigDecimal attendanceEarnings) { this.attendanceEarnings = attendanceEarnings; }
        
        public BigDecimal getOvertimePay() { return overtimePay; }
        public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
        
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { this.totalBenefits = totalBenefits; }
        
        public BigDecimal getGrossIncome() { return grossIncome; }
        public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        
        public BigDecimal getNetSalary() { return netSalary; }
        public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    }
    
    /**
     * Holds results from payroll processing operation
     */
    public static class PayrollProcessingResult {
        private Integer payPeriodId;
        private boolean success = false;
        private LocalDate processedDate;
        private int totalEmployees = 0;
        private int processedEmployees = 0;
        private int failedEmployees = 0;
        private BigDecimal totalGrossIncome = BigDecimal.ZERO;
        private BigDecimal totalNetSalary = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        private List<String> errors = new ArrayList<>();
        
        // Getters and setters
        public Integer getPayPeriodId() { return payPeriodId; }
        public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public LocalDate getProcessedDate() { return processedDate; }
        public void setProcessedDate(LocalDate processedDate) { this.processedDate = processedDate; }
        
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        
        public int getProcessedEmployees() { return processedEmployees; }
        public void incrementProcessedEmployees() { this.processedEmployees++; }
        
        public int getFailedEmployees() { return failedEmployees; }
        public void incrementFailedEmployees() { this.failedEmployees++; }
        
        public BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public void setTotalGrossIncome(BigDecimal totalGrossIncome) { this.totalGrossIncome = totalGrossIncome; }
        
        public BigDecimal getTotalNetSalary() { return totalNetSalary; }
        public void setTotalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { this.totalBenefits = totalBenefits; }
        
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        
        @Override
        public String toString() {
            return String.format("PayrollProcessingResult{payPeriodId=%d, success=%s, processed=%d/%d, totalNet=%s}",
                    payPeriodId, success, processedEmployees, totalEmployees, totalNetSalary);
        }
    }
} 
