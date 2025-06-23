package Services;

import DAOs.*;
import Models.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService - Business logic for generating various reports
 * Handles payroll reports, attendance reports, leave reports, and compliance reports
 * @author User
 */
public class ReportService {
    
    // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final PayrollDAO payrollDAO;
    private final PayslipDAO payslipDAO;
    private final AttendanceDAO attendanceDAO;
    private final LeaveDAO leaveDAO;
    private final OvertimeRequestDAO overtimeDAO;
    private final PayPeriodDAO payPeriodDAO;
    
    // Service Dependencies
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;
    private final LeaveService leaveService;
    private final OvertimeService overtimeService;
    
    /**
     * Constructor - initializes required DAOs and services
     */
    public ReportService() {
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.leaveDAO = new LeaveDAO(databaseConnection);
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        
        // Initialize services
        this.attendanceService = new AttendanceService(databaseConnection);
        this.payrollService = new PayrollService(databaseConnection);
        this.leaveService = new LeaveService(databaseConnection);
        this.overtimeService = new OvertimeService(databaseConnection);
    }
    
    /**
     * Constructor with custom database connection
     */
    public ReportService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.leaveDAO = new LeaveDAO(databaseConnection);
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        
        this.attendanceService = new AttendanceService(databaseConnection);
        this.payrollService = new PayrollService(databaseConnection);
        this.leaveService = new LeaveService(databaseConnection);
        this.overtimeService = new OvertimeService(databaseConnection);
    }
    
    // ================================
    // PAYROLL REPORTS
    // ================================
    
    /**
     * Generates comprehensive payroll report for a pay period
     * @param payPeriodId Pay period ID
     * @return PayrollReport with detailed payroll information
     */
    public PayrollReport generatePayrollReport(Integer payPeriodId) {
        PayrollReport report = new PayrollReport();
        
        try {
            // Get pay period information
            PayPeriodModel payPeriod = payPeriodDAO.findById(payPeriodId);
            if (payPeriod == null) {
                report.setSuccess(false);
                report.setErrorMessage("Pay period not found: " + payPeriodId);
                return report;
            }
            
            report.setPayPeriodId(payPeriodId);
            report.setPeriodName(payPeriod.getPeriodName());
            report.setStartDate(payPeriod.getStartDate());
            report.setEndDate(payPeriod.getEndDate());
            report.setGeneratedDate(LocalDate.now());
            
            // Get payroll summary
            PayrollDAO.PayrollSummary summary = payrollDAO.getPayrollSummary(payPeriodId);
            report.setTotalEmployees(summary.getEmployeeCount());
            report.setTotalGrossIncome(summary.getTotalGrossIncome());
            report.setTotalNetSalary(summary.getTotalNetSalary());
            report.setTotalDeductions(summary.getTotalDeductions());
            report.setTotalBenefits(summary.getTotalBenefits());
            
            // Get individual payroll records
            List<PayrollModel> payrollRecords = payrollDAO.findByPayPeriod(payPeriodId);
            List<PayrollReportEntry> reportEntries = new ArrayList<>();
            
            for (PayrollModel payroll : payrollRecords) {
                EmployeeModel employee = employeeDAO.findById(payroll.getEmployeeId());
                if (employee != null) {
                    PayrollReportEntry entry = new PayrollReportEntry();
                    entry.setEmployeeId(employee.getEmployeeId());
                    entry.setEmployeeName(employee.getFullName());
                    entry.setPosition(getEmployeePosition(employee.getPositionId()));
                    entry.setBasicSalary(payroll.getBasicSalary());
                    entry.setGrossIncome(payroll.getGrossIncome());
                    entry.setTotalDeductions(payroll.getTotalDeduction());
                    entry.setNetSalary(payroll.getNetSalary());
                    
                    reportEntries.add(entry);
                }
            }
            
            report.setPayrollEntries(reportEntries);
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating payroll report: " + e.getMessage());
            System.err.println("Error generating payroll report: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Generates salary comparison report
     * @param startDate Start date for comparison
     * @param endDate End date for comparison
     * @return SalaryComparisonReport with salary analysis
     */
    public SalaryComparisonReport generateSalaryComparisonReport(LocalDate startDate, LocalDate endDate) {
        SalaryComparisonReport report = new SalaryComparisonReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedDate(LocalDate.now());
        
        try {
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();
            List<SalaryEntry> salaryEntries = new ArrayList<>();
            
            BigDecimal totalSalaries = BigDecimal.ZERO;
            BigDecimal highestSalary = BigDecimal.ZERO;
            BigDecimal lowestSalary = new BigDecimal("999999999");
            
            for (EmployeeModel employee : activeEmployees) {
                SalaryEntry entry = new SalaryEntry();
                entry.setEmployeeId(employee.getEmployeeId());
                entry.setEmployeeName(employee.getFullName());
                entry.setPosition(getEmployeePosition(employee.getPositionId()));
                entry.setBasicSalary(employee.getBasicSalary());
                entry.setHourlyRate(employee.getHourlyRate());
                
                // Calculate average net pay for the period
                List<PayrollModel> payrollHistory = payrollDAO.getPayrollHistory(employee.getEmployeeId(), 0);
                BigDecimal averageNetPay = payrollHistory.stream()
                        .map(PayrollModel::getNetSalary)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (!payrollHistory.isEmpty()) {
                    averageNetPay = averageNetPay.divide(new BigDecimal(payrollHistory.size()), 2, RoundingMode.HALF_UP);
                }
                
                entry.setAverageNetPay(averageNetPay);
                salaryEntries.add(entry);
                
                // Update statistics
                totalSalaries = totalSalaries.add(employee.getBasicSalary());
                if (employee.getBasicSalary().compareTo(highestSalary) > 0) {
                    highestSalary = employee.getBasicSalary();
                }
                if (employee.getBasicSalary().compareTo(lowestSalary) < 0) {
                    lowestSalary = employee.getBasicSalary();
                }
            }
            
            // Calculate averages
            if (!activeEmployees.isEmpty()) {
                BigDecimal averageSalary = totalSalaries.divide(new BigDecimal(activeEmployees.size()), 2, RoundingMode.HALF_UP);
                report.setAverageSalary(averageSalary);
            }
            
            report.setSalaryEntries(salaryEntries);
            report.setHighestSalary(highestSalary);
            report.setLowestSalary(lowestSalary);
            report.setTotalEmployees(activeEmployees.size());
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating salary comparison report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // ATTENDANCE REPORTS
    // ================================
    
    /**
     * Generates daily attendance report
     * @param date Date to generate report for
     * @return AttendanceReport with daily attendance data
     */
    public AttendanceReport generateDailyAttendanceReport(LocalDate date) {
        AttendanceReport report = new AttendanceReport();
        report.setReportDate(date);
        report.setGeneratedDate(LocalDate.now());
        report.setReportType("Daily Attendance");
        
        try {
            List<AttendanceService.DailyAttendanceRecord> attendanceRecords = 
                    attendanceService.getDailyAttendanceReport(date);
            
            report.setAttendanceRecords(attendanceRecords);
            
            // Calculate statistics
            long presentCount = attendanceRecords.stream().filter(r -> "Present".equals(r.getStatus())).count();
            long lateCount = attendanceRecords.stream().filter(r -> "Late".equals(r.getStatus())).count();
            long absentCount = attendanceRecords.stream().filter(r -> "Absent".equals(r.getStatus())).count();
            
            report.setPresentCount((int)presentCount);
            report.setLateCount((int)lateCount);
            report.setAbsentCount((int)absentCount);
            report.setTotalEmployees(attendanceRecords.size());
            
            if (attendanceRecords.size() > 0) {
                BigDecimal attendanceRate = new BigDecimal(presentCount + lateCount)
                        .divide(new BigDecimal(attendanceRecords.size()), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
                report.setAttendanceRate(attendanceRate);
            }
            
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating attendance report: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Generates monthly attendance summary
     * @param yearMonth Year and month
     * @return MonthlyAttendanceReport with attendance statistics
     */
    public MonthlyAttendanceReport generateMonthlyAttendanceReport(YearMonth yearMonth) {
        MonthlyAttendanceReport report = new MonthlyAttendanceReport();
        report.setYearMonth(yearMonth);
        report.setGeneratedDate(LocalDate.now());
        
        try {
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();
            List<EmployeeAttendanceSummary> employeeSummaries = new ArrayList<>();
            
            for (EmployeeModel employee : activeEmployees) {
                AttendanceService.AttendanceSummary summary = 
                        attendanceService.getMonthlyAttendanceSummary(employee.getEmployeeId(), yearMonth);
                
                EmployeeAttendanceSummary empSummary = new EmployeeAttendanceSummary();
                empSummary.setEmployeeId(employee.getEmployeeId());
                empSummary.setEmployeeName(employee.getFullName());
                empSummary.setTotalDays(summary.getTotalDays());
                empSummary.setCompleteDays(summary.getCompleteDays());
                empSummary.setTotalHours(summary.getTotalHours());
                empSummary.setAttendanceRate(summary.getAttendanceRate());
                empSummary.setLateInstances(summary.getLateInstances());
                
                employeeSummaries.add(empSummary);
            }
            
            report.setEmployeeSummaries(employeeSummaries);
            
            // Calculate overall statistics
            int totalCompleteDays = employeeSummaries.stream().mapToInt(EmployeeAttendanceSummary::getCompleteDays).sum();
            int totalPossibleDays = employeeSummaries.stream().mapToInt(EmployeeAttendanceSummary::getTotalDays).sum();
            
            if (totalPossibleDays > 0) {
                BigDecimal overallAttendanceRate = new BigDecimal(totalCompleteDays)
                        .divide(new BigDecimal(totalPossibleDays), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
                report.setOverallAttendanceRate(overallAttendanceRate);
            }
            
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating monthly attendance report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // LEAVE REPORTS
    // ================================
    
    /**
     * Generates leave summary report
     * @param year Year to generate report for
     * @return LeaveReport with leave statistics
     */
    public LeaveReport generateLeaveReport(Integer year) {
        LeaveReport report = new LeaveReport();
        report.setYear(year);
        report.setGeneratedDate(LocalDate.now());
        
        try {
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();
            List<EmployeeLeaveSummary> leaveSummaries = new ArrayList<>();
            
            for (EmployeeModel employee : activeEmployees) {
                LeaveService.LeaveSummary summary = leaveService.getEmployeeLeaveSummary(employee.getEmployeeId(), year);
                
                EmployeeLeaveSummary empSummary = new EmployeeLeaveSummary();
                empSummary.setEmployeeId(employee.getEmployeeId());
                empSummary.setEmployeeName(employee.getFullName());
                empSummary.setTotalAllocatedDays(summary.getTotalAllocatedDays());
                empSummary.setTotalUsedDays(summary.getTotalUsedDays());
                empSummary.setTotalRemainingDays(summary.getTotalRemainingDays());
                
                // Calculate usage percentage
                if (summary.getTotalAllocatedDays() > 0) {
                    BigDecimal usagePercentage = new BigDecimal(summary.getTotalUsedDays())
                            .divide(new BigDecimal(summary.getTotalAllocatedDays()), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100));
                    empSummary.setUsagePercentage(usagePercentage);
                }
                
                leaveSummaries.add(empSummary);
            }
            
            report.setLeaveSummaries(leaveSummaries);
            
            // Calculate overall statistics
            int totalAllocated = leaveSummaries.stream().mapToInt(EmployeeLeaveSummary::getTotalAllocatedDays).sum();
            int totalUsed = leaveSummaries.stream().mapToInt(EmployeeLeaveSummary::getTotalUsedDays).sum();
            
            report.setTotalAllocatedDays(totalAllocated);
            report.setTotalUsedDays(totalUsed);
            
            if (totalAllocated > 0) {
                BigDecimal overallUsageRate = new BigDecimal(totalUsed)
                        .divide(new BigDecimal(totalAllocated), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
                report.setOverallUsageRate(overallUsageRate);
            }
            
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating leave report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // OVERTIME REPORTS
    // ================================
    
    /**
     * Generates overtime summary report
     * @param startDate Start date
     * @param endDate End date
     * @return OvertimeReport with overtime statistics
     */
    public OvertimeReport generateOvertimeReport(LocalDate startDate, LocalDate endDate) {
        OvertimeReport report = new OvertimeReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedDate(LocalDate.now());
        
        try {
            List<OvertimeService.OvertimeRanking> rankings = 
                    overtimeService.getTopOvertimeEmployees(startDate, endDate, 0); // No limit
            
            report.setOvertimeRankings(rankings);
            
            // Calculate totals
            BigDecimal totalOvertimeHours = rankings.stream()
                    .map(OvertimeService.OvertimeRanking::getTotalOvertimeHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalOvertimePay = rankings.stream()
                    .map(OvertimeService.OvertimeRanking::getTotalOvertimePay)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            report.setTotalOvertimeHours(totalOvertimeHours);
            report.setTotalOvertimePay(totalOvertimePay);
            report.setTotalEmployeesWithOvertime(rankings.size());
            
            if (!rankings.isEmpty()) {
                BigDecimal averageHoursPerEmployee = totalOvertimeHours
                        .divide(new BigDecimal(rankings.size()), 2, RoundingMode.HALF_UP);
                report.setAverageOvertimeHoursPerEmployee(averageHoursPerEmployee);
            }
            
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating overtime report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // COMPLIANCE REPORTS
    // ================================
    
    /**
     * Generates government compliance report (BIR, SSS, PhilHealth, Pag-IBIG)
     * @param yearMonth Year and month for compliance
     * @return ComplianceReport with government contribution details
     */
    public ComplianceReport generateComplianceReport(YearMonth yearMonth) {
        ComplianceReport report = new ComplianceReport();
        report.setYearMonth(yearMonth);
        report.setGeneratedDate(LocalDate.now());
        
        try {
            // This would need to be implemented with proper deduction calculations
            // For now, providing a basic structure
            
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();
            BigDecimal totalSSS = BigDecimal.ZERO;
            BigDecimal totalPhilHealth = BigDecimal.ZERO;
            BigDecimal totalPagIbig = BigDecimal.ZERO;
            BigDecimal totalWithholdingTax = BigDecimal.ZERO;
            
            for (EmployeeModel employee : activeEmployees) {
                // Calculate government contributions based on basic salary
                BigDecimal basicSalary = employee.getBasicSalary();
                
                // SSS: 4.5% employee + 8.5% employer = 13%
                BigDecimal sssEmployee = basicSalary.multiply(new BigDecimal("0.045"));
                BigDecimal sssEmployer = basicSalary.multiply(new BigDecimal("0.085"));
                totalSSS = totalSSS.add(sssEmployee).add(sssEmployer);
                
                // PhilHealth: 2.75% employee + 2.75% employer = 5.5%
                BigDecimal philhealthEmployee = basicSalary.multiply(new BigDecimal("0.0275"));
                BigDecimal philhealthEmployer = basicSalary.multiply(new BigDecimal("0.0275"));
                totalPhilHealth = totalPhilHealth.add(philhealthEmployee).add(philhealthEmployer);
                
                // Pag-IBIG: 2% employee + 2% employer = 4%
                BigDecimal pagibigEmployee = basicSalary.multiply(new BigDecimal("0.02"));
                BigDecimal pagibigEmployer = basicSalary.multiply(new BigDecimal("0.02"));
                totalPagIbig = totalPagIbig.add(pagibigEmployee).add(pagibigEmployer);
                
                // Withholding Tax (simplified calculation)
                totalWithholdingTax = totalWithholdingTax.add(calculateSimpleWithholdingTax(basicSalary));
            }
            
            report.setTotalSSS(totalSSS);
            report.setTotalPhilHealth(totalPhilHealth);
            report.setTotalPagIbig(totalPagIbig);
            report.setTotalWithholdingTax(totalWithholdingTax);
            report.setTotalEmployees(activeEmployees.size());
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating compliance report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // HELPER METHODS
    // ================================
    
    /**
     * Gets employee position name (placeholder - would need Position DAO)
     */
    private String getEmployeePosition(Integer positionId) {
        // This would need a PositionDAO to get actual position names
        return positionId != null ? "Position " + positionId : "Unknown";
    }
    
    /**
     * Simplified withholding tax calculation
     */
    private BigDecimal calculateSimpleWithholdingTax(BigDecimal basicSalary) {
        if (basicSalary.compareTo(new BigDecimal("20833")) <= 0) {
            return BigDecimal.ZERO;
        } else {
            return basicSalary.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Formats currency for display
     */
    public String formatCurrency(BigDecimal amount) {
        return "₱" + String.format("%,.2f", amount);
    }
    
    /**
     * Formats date for display
     */
    public String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }
    
    /**
     * Formats percentage for display
     */
    public String formatPercentage(BigDecimal percentage) {
        return String.format("%.2f%%", percentage);
    }
    
    // ================================
    // INNER CLASSES - REPORT MODELS
    // ================================
    
    /**
     * Base report class
     */
    public static abstract class BaseReport {
        private boolean success = false;
        private String errorMessage = "";
        private LocalDate generatedDate;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
    }
    
    /**
     * Payroll report
     */
    public static class PayrollReport extends BaseReport {
        private Integer payPeriodId;
        private String periodName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalEmployees = 0;
        private BigDecimal totalGrossIncome = BigDecimal.ZERO;
        private BigDecimal totalNetSalary = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        private List<PayrollReportEntry> payrollEntries = new ArrayList<>();
        
        // Getters and setters
        public Integer getPayPeriodId() { return payPeriodId; }
        public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
        public String getPeriodName() { return periodName; }
        public void setPeriodName(String periodName) { this.periodName = periodName; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public void setTotalGrossIncome(BigDecimal totalGrossIncome) { this.totalGrossIncome = totalGrossIncome; }
        public BigDecimal getTotalNetSalary() { return totalNetSalary; }
        public void setTotalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; }
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { this.totalBenefits = totalBenefits; }
        public List<PayrollReportEntry> getPayrollEntries() { return payrollEntries; }
        public void setPayrollEntries(List<PayrollReportEntry> payrollEntries) { this.payrollEntries = payrollEntries; }
    }
    
    /**
     * Individual payroll entry for reports
     */
    public static class PayrollReportEntry {
        private Integer employeeId;
        private String employeeName;
        private String position;
        private BigDecimal basicSalary = BigDecimal.ZERO;
        private BigDecimal grossIncome = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal netSalary = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public BigDecimal getBasicSalary() { return basicSalary; }
        public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
        public BigDecimal getGrossIncome() { return grossIncome; }
        public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        public BigDecimal getNetSalary() { return netSalary; }
        public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    }
    
    /**
     * Salary comparison report
     */
    public static class SalaryComparisonReport extends BaseReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalEmployees = 0;
        private BigDecimal averageSalary = BigDecimal.ZERO;
        private BigDecimal highestSalary = BigDecimal.ZERO;
        private BigDecimal lowestSalary = BigDecimal.ZERO;
        private List<SalaryEntry> salaryEntries = new ArrayList<>();
        
        // Getters and setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public BigDecimal getAverageSalary() { return averageSalary; }
        public void setAverageSalary(BigDecimal averageSalary) { this.averageSalary = averageSalary; }
        public BigDecimal getHighestSalary() { return highestSalary; }
        public void setHighestSalary(BigDecimal highestSalary) { this.highestSalary = highestSalary; }
        public BigDecimal getLowestSalary() { return lowestSalary; }
        public void setLowestSalary(BigDecimal lowestSalary) { this.lowestSalary = lowestSalary; }
        public List<SalaryEntry> getSalaryEntries() { return salaryEntries; }
        public void setSalaryEntries(List<SalaryEntry> salaryEntries) { this.salaryEntries = salaryEntries; }
    }
    
    /**
     * Salary entry for comparison
     */
    public static class SalaryEntry {
        private Integer employeeId;
        private String employeeName;
        private String position;
        private BigDecimal basicSalary = BigDecimal.ZERO;
        private BigDecimal hourlyRate = BigDecimal.ZERO;
        private BigDecimal averageNetPay = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public BigDecimal getBasicSalary() { return basicSalary; }
        public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
        public BigDecimal getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
        public BigDecimal getAverageNetPay() { return averageNetPay; }
        public void setAverageNetPay(BigDecimal averageNetPay) { this.averageNetPay = averageNetPay; }
    }
    
    /**
     * Attendance report
     */
    public static class AttendanceReport extends BaseReport {
        private LocalDate reportDate;
        private String reportType;
        private int totalEmployees = 0;
        private int presentCount = 0;
        private int lateCount = 0;
        private int absentCount = 0;
        private BigDecimal attendanceRate = BigDecimal.ZERO;
        private List<AttendanceService.DailyAttendanceRecord> attendanceRecords = new ArrayList<>();
        
        // Getters and setters
        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public int getLateCount() { return lateCount; }
        public void setLateCount(int lateCount) { this.lateCount = lateCount; }
        public int getAbsentCount() { return absentCount; }
        public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }
        public BigDecimal getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(BigDecimal attendanceRate) { this.attendanceRate = attendanceRate; }
        public List<AttendanceService.DailyAttendanceRecord> getAttendanceRecords() { return attendanceRecords; }
        public void setAttendanceRecords(List<AttendanceService.DailyAttendanceRecord> attendanceRecords) { this.attendanceRecords = attendanceRecords; }
    }
    
    /**
     * Monthly attendance report
     */
    public static class MonthlyAttendanceReport extends BaseReport {
        private YearMonth yearMonth;
        private BigDecimal overallAttendanceRate = BigDecimal.ZERO;
        private List<EmployeeAttendanceSummary> employeeSummaries = new ArrayList<>();
        
        // Getters and setters
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public BigDecimal getOverallAttendanceRate() { return overallAttendanceRate; }
        public void setOverallAttendanceRate(BigDecimal overallAttendanceRate) { this.overallAttendanceRate = overallAttendanceRate; }
        public List<EmployeeAttendanceSummary> getEmployeeSummaries() { return employeeSummaries; }
        public void setEmployeeSummaries(List<EmployeeAttendanceSummary> employeeSummaries) { this.employeeSummaries = employeeSummaries; }
    }
    
    /**
     * Employee attendance summary
     */
    public static class EmployeeAttendanceSummary {
        private Integer employeeId;
        private String employeeName;
        private int totalDays = 0;
        private int completeDays = 0;
        private BigDecimal totalHours = BigDecimal.ZERO;
        private BigDecimal attendanceRate = BigDecimal.ZERO;
        private int lateInstances = 0;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public int getTotalDays() { return totalDays; }
        public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
        public int getCompleteDays() { return completeDays; }
        public void setCompleteDays(int completeDays) { this.completeDays = completeDays; }
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
        public BigDecimal getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(BigDecimal attendanceRate) { this.attendanceRate = attendanceRate; }
        public int getLateInstances() { return lateInstances; }
        public void setLateInstances(int lateInstances) { this.lateInstances = lateInstances; }
    }
    
    /**
     * Leave report
     */
    public static class LeaveReport extends BaseReport {
        private Integer year;
        private int totalAllocatedDays = 0;
        private int totalUsedDays = 0;
        private BigDecimal overallUsageRate = BigDecimal.ZERO;
        private List<EmployeeLeaveSummary> leaveSummaries = new ArrayList<>();
        
        // Getters and setters
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public int getTotalAllocatedDays() { return totalAllocatedDays; }
        public void setTotalAllocatedDays(int totalAllocatedDays) { this.totalAllocatedDays = totalAllocatedDays; }
        public int getTotalUsedDays() { return totalUsedDays; }
        public void setTotalUsedDays(int totalUsedDays) { this.totalUsedDays = totalUsedDays; }
        public BigDecimal getOverallUsageRate() { return overallUsageRate; }
        public void setOverallUsageRate(BigDecimal overallUsageRate) { this.overallUsageRate = overallUsageRate; }
        public List<EmployeeLeaveSummary> getLeaveSummaries() { return leaveSummaries; }
        public void setLeaveSummaries(List<EmployeeLeaveSummary> leaveSummaries) { this.leaveSummaries = leaveSummaries; }
    }
    
    /**
     * Employee leave summary
     */
    public static class EmployeeLeaveSummary {
        private Integer employeeId;
        private String employeeName;
        private int totalAllocatedDays = 0;
        private int totalUsedDays = 0;
        private int totalRemainingDays = 0;
        private BigDecimal usagePercentage = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public int getTotalAllocatedDays() { return totalAllocatedDays; }
        public void setTotalAllocatedDays(int totalAllocatedDays) { this.totalAllocatedDays = totalAllocatedDays; }
        public int getTotalUsedDays() { return totalUsedDays; }
        public void setTotalUsedDays(int totalUsedDays) { this.totalUsedDays = totalUsedDays; }
        public int getTotalRemainingDays() { return totalRemainingDays; }
        public void setTotalRemainingDays(int totalRemainingDays) { this.totalRemainingDays = totalRemainingDays; }
        public BigDecimal getUsagePercentage() { return usagePercentage; }
        public void setUsagePercentage(BigDecimal usagePercentage) { this.usagePercentage = usagePercentage; }
    }
    
    /**
     * Overtime report
     */
    public static class OvertimeReport extends BaseReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;
        private int totalEmployeesWithOvertime = 0;
        private BigDecimal averageOvertimeHoursPerEmployee = BigDecimal.ZERO;
        private List<OvertimeService.OvertimeRanking> overtimeRankings = new ArrayList<>();
        
        // Getters and setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        public int getTotalEmployeesWithOvertime() { return totalEmployeesWithOvertime; }
        public void setTotalEmployeesWithOvertime(int totalEmployeesWithOvertime) { this.totalEmployeesWithOvertime = totalEmployeesWithOvertime; }
        public BigDecimal getAverageOvertimeHoursPerEmployee() { return averageOvertimeHoursPerEmployee; }
        public void setAverageOvertimeHoursPerEmployee(BigDecimal averageOvertimeHoursPerEmployee) { this.averageOvertimeHoursPerEmployee = averageOvertimeHoursPerEmployee; }
        public List<OvertimeService.OvertimeRanking> getOvertimeRankings() { return overtimeRankings; }
        public void setOvertimeRankings(List<OvertimeService.OvertimeRanking> overtimeRankings) { this.overtimeRankings = overtimeRankings; }
    }
    
    /**
     * Compliance report for government contributions
     */
    public static class ComplianceReport extends BaseReport {
        private YearMonth yearMonth;
        private int totalEmployees = 0;
        private BigDecimal totalSSS = BigDecimal.ZERO;
        private BigDecimal totalPhilHealth = BigDecimal.ZERO;
        private BigDecimal totalPagIbig = BigDecimal.ZERO;
        private BigDecimal totalWithholdingTax = BigDecimal.ZERO;
        
        // Getters and setters
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public BigDecimal getTotalSSS() { return totalSSS; }
        public void setTotalSSS(BigDecimal totalSSS) { this.totalSSS = totalSSS; }
        public BigDecimal getTotalPhilHealth() { return totalPhilHealth; }
        public void setTotalPhilHealth(BigDecimal totalPhilHealth) { this.totalPhilHealth = totalPhilHealth; }
        public BigDecimal getTotalPagIbig() { return totalPagIbig; }
        public void setTotalPagIbig(BigDecimal totalPagIbig) { this.totalPagIbig = totalPagIbig; }
        public BigDecimal getTotalWithholdingTax() { return totalWithholdingTax; }
        public void setTotalWithholdingTax(BigDecimal totalWithholdingTax) { this.totalWithholdingTax = totalWithholdingTax; }
        
        public BigDecimal getTotalGovernmentContributions() {
            return totalSSS.add(totalPhilHealth).add(totalPagIbig).add(totalWithholdingTax);
        }
    }
}