
package Models;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Models.EmployeeModel;
import oop.classes.management.LeaveRequestManagement;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * PayrollLeave integrates leave data with payroll calculations
 * Handles paid leave, unpaid leave, leave deductions, and leave-based pay adjustments
 * Links leave management system to payroll processing
 * @author Chadley
 */

public class PayrollLeave {
  private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    private final LeaveRequestManagement leaveManagement;
    
    // Leave calculation constants
    private static final int STANDARD_HOURS_PER_DAY = 8;
    private static final int STANDARD_DAYS_PER_WEEK = 5;
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public PayrollLeave(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.leaveManagement = new LeaveRequestManagement(databaseConnection);
    }
    
    /**
     * Calculates leave-related payroll adjustments for an employee in a specific pay period
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return PayrollLeaveResult with all leave-based calculations
     */
    public PayrollLeaveResult calculateLeavePayroll(Integer employeeId, Integer payPeriodId) {
        try {
            // Get employee details
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Employee not found: " + employeeId);
            }
            
            // Get pay period dates
            Map<String, Object> payPeriod = getPayPeriodDetails(payPeriodId);
            if (payPeriod == null) {
                throw new IllegalArgumentException("Pay period not found: " + payPeriodId);
            }
            
            LocalDate startDate = ((java.sql.Date) payPeriod.get("startDate")).toLocalDate();
            LocalDate endDate = ((java.sql.Date) payPeriod.get("endDate")).toLocalDate();
            
            // Get leave records for the pay period
            List<Map<String, Object>> leaveRecords = getApprovedLeaveInPeriod(employeeId, startDate, endDate);
            
            // Initialize calculation result
            PayrollLeaveResult result = new PayrollLeaveResult();
            result.setEmployeeId(employeeId);
            result.setPayPeriodId(payPeriodId);
            result.setStartDate(startDate);
            result.setEndDate(endDate);
            
            // Calculate hourly rate
            BigDecimal hourlyRate = calculateHourlyRate(employee);
            result.setHourlyRate(hourlyRate);
            
            // Calculate different leave components
            calculatePaidLeave(result, leaveRecords, employee);
            calculateUnpaidLeave(result, leaveRecords, employee);
            calculateLeaveDeductions(result, leaveRecords, employee);
            calculateLeaveCredits(result, leaveRecords, employee);
            calculateMaternityPaternityLeave(result, leaveRecords, employee);
            calculateSickLeaveAdjustments(result, leaveRecords, employee);
            
            // Calculate total leave pay adjustments
            BigDecimal totalLeavePayments = result.getPaidLeavePay()
                .add(result.getMaternityPaternityPay())
                .add(result.getSickLeavePay());
            
            BigDecimal totalLeaveDeductions = result.getUnpaidLeaveDeductions()
                .add(result.getExcessLeaveDeductions());
            
            BigDecimal netLeaveAdjustment = totalLeavePayments.subtract(totalLeaveDeductions);
            
            result.setTotalLeavePayments(totalLeavePayments);
            result.setTotalLeaveDeductions(totalLeaveDeductions);
            result.setNetLeaveAdjustment(netLeaveAdjustment);
            
            System.out.println("Leave payroll calculated for employee " + employeeId + 
                             " for period " + startDate + " to " + endDate);
            System.out.println("Net leave adjustment: " + netLeaveAdjustment);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error calculating leave payroll: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Calculates paid leave amounts
     */
    private void calculatePaidLeave(PayrollLeaveResult result, 
                                  List<Map<String, Object>> leaveRecords, 
                                  EmployeeModel employee) {
        
        BigDecimal totalPaidLeaveDays = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : leaveRecords) {
            String leaveTypeName = (String) record.get("leaveTypeName");
            Long leaveDays = (Long) record.get("leaveDays");
            
            if (leaveDays != null && isPaidLeaveType(leaveTypeName)) {
                totalPaidLeaveDays = totalPaidLeaveDays.add(new BigDecimal(leaveDays));
            }
        }
        
        BigDecimal paidLeavePay = totalPaidLeaveDays
            .multiply(new BigDecimal(STANDARD_HOURS_PER_DAY))
            .multiply(hourlyRate);
        
        result.setPaidLeaveDays(totalPaidLeaveDays);
        result.setPaidLeavePay(paidLeavePay);
    }
    
    /**
     * Calculates unpaid leave deductions
     */
    private void calculateUnpaidLeave(PayrollLeaveResult result, 
                                    List<Map<String, Object>> leaveRecords, 
                                    EmployeeModel employee) {
        
        BigDecimal totalUnpaidLeaveDays = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : leaveRecords) {
            String leaveTypeName = (String) record.get("leaveTypeName");
            Long leaveDays = (Long) record.get("leaveDays");
            
            if (leaveDays != null && isUnpaidLeaveType(leaveTypeName)) {
                totalUnpaidLeaveDays = totalUnpaidLeaveDays.add(new BigDecimal(leaveDays));
            }
        }
        
        BigDecimal unpaidLeaveDeductions = totalUnpaidLeaveDays
            .multiply(new BigDecimal(STANDARD_HOURS_PER_DAY))
            .multiply(hourlyRate);
        
        result.setUnpaidLeaveDays(totalUnpaidLeaveDays);
        result.setUnpaidLeaveDeductions(unpaidLeaveDeductions);
    }
    
    /**
     * Calculates leave deductions for excess leave usage
     */
    private void calculateLeaveDeductions(PayrollLeaveResult result, 
                                        List<Map<String, Object>> leaveRecords, 
                                        EmployeeModel employee) {
        
        BigDecimal totalExcessLeaveDays = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        // Get current leave balances
        List<Map<String, Object>> leaveBalances = leaveManagement.getLeaveBalance(
            employee.getEmployeeId(), null);
        
        // Check for negative balances (excess leave usage)
        for (Map<String, Object> balance : leaveBalances) {
            Integer remaining = (Integer) balance.get("remaining");
            if (remaining != null && remaining < 0) {
                // Employee has used more leave than entitled
                totalExcessLeaveDays = totalExcessLeaveDays.add(new BigDecimal(Math.abs(remaining)));
            }
        }
        
        BigDecimal excessLeaveDeductions = totalExcessLeaveDays
            .multiply(new BigDecimal(STANDARD_HOURS_PER_DAY))
            .multiply(hourlyRate);
        
        result.setExcessLeaveDays(totalExcessLeaveDays);
        result.setExcessLeaveDeductions(excessLeaveDeductions);
    }
    
    /**
     * Calculates leave credits and accruals
     */
    private void calculateLeaveCredits(PayrollLeaveResult result, 
                                     List<Map<String, Object>> leaveRecords, 
                                     EmployeeModel employee) {
        
        // Calculate monthly leave accrual (placeholder - customize based on company policy)
        BigDecimal monthlyVacationAccrual = new BigDecimal("1.25"); // 15 days per year / 12 months
        BigDecimal monthlySickAccrual = new BigDecimal("0.83");     // 10 days per year / 12 months
        
        result.setVacationLeaveAccrual(monthlyVacationAccrual);
        result.setSickLeaveAccrual(monthlySickAccrual);
        
        // Note: You might want to update leave balances here based on accruals
        System.out.println("Leave accruals calculated - Vacation: " + monthlyVacationAccrual + 
                          ", Sick: " + monthlySickAccrual);
    }
    
    /**
     * Calculates maternity/paternity leave payments
     */
    private void calculateMaternityPaternityLeave(PayrollLeaveResult result, 
                                                List<Map<String, Object>> leaveRecords, 
                                                EmployeeModel employee) {
        
        BigDecimal totalMaternityPaternityDays = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : leaveRecords) {
            String leaveTypeName = (String) record.get("leaveTypeName");
            Long leaveDays = (Long) record.get("leaveDays");
            
            if (leaveDays != null && isMaternityPaternityLeave(leaveTypeName)) {
                totalMaternityPaternityDays = totalMaternityPaternityDays.add(new BigDecimal(leaveDays));
            }
        }
        
        // Maternity/Paternity leave might have different pay rates or government benefits
        BigDecimal maternityPaternityPay = totalMaternityPaternityDays
            .multiply(new BigDecimal(STANDARD_HOURS_PER_DAY))
            .multiply(hourlyRate)
            .multiply(new BigDecimal("0.60")); // Assume 60% pay rate for maternity leave
        
        result.setMaternityPaternityDays(totalMaternityPaternityDays);
        result.setMaternityPaternityPay(maternityPaternityPay);
    }
    
    /**
     * Calculates sick leave pay adjustments
     */
    private void calculateSickLeaveAdjustments(PayrollLeaveResult result, 
                                             List<Map<String, Object>> leaveRecords, 
                                             EmployeeModel employee) {
        
        BigDecimal totalSickLeaveDays = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : leaveRecords) {
            String leaveTypeName = (String) record.get("leaveTypeName");
            Long leaveDays = (Long) record.get("leaveDays");
            
            if (leaveDays != null && isSickLeave(leaveTypeName)) {
                totalSickLeaveDays = totalSickLeaveDays.add(new BigDecimal(leaveDays));
            }
        }
        
        BigDecimal sickLeavePay = totalSickLeaveDays
            .multiply(new BigDecimal(STANDARD_HOURS_PER_DAY))
            .multiply(hourlyRate);
        
        result.setSickLeaveDays(totalSickLeaveDays);
        result.setSickLeavePay(sickLeavePay);
    }
    
    /**
     * Gets approved leave records for a specific pay period
     */
    private List<Map<String, Object>> getApprovedLeaveInPeriod(Integer employeeId, 
                                                              LocalDate startDate, 
                                                              LocalDate endDate) {
        String sql = "SELECT lr.*, lt.leaveTypeName " +
                    "FROM leaverequest lr " +
                    "LEFT JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId " +
                    "WHERE lr.employeeId = ? AND lr.status = 'Approved' " +
                    "AND ((lr.startDate <= ? AND lr.endDate >= ?) OR " +
                    "(lr.startDate <= ? AND lr.endDate >= ?)) " +
                    "ORDER BY lr.startDate";
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            stmt.setDate(3, java.sql.Date.valueOf(startDate));
            stmt.setDate(4, java.sql.Date.valueOf(startDate));
            stmt.setDate(5, java.sql.Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        record.put(columnName, value);
                    }
                    
                    results.add(record);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting approved leave records: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Gets pay period details by ID
     */
    private Map<String, Object> getPayPeriodDetails(Integer payPeriodId) {
        String sql = "SELECT payPeriodId, startDate, endDate, payDate, payPeriodDescription " +
                    "FROM payperiod WHERE payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> payPeriod = new HashMap<>();
                    payPeriod.put("payPeriodId", rs.getInt("payPeriodId"));
                    payPeriod.put("startDate", rs.getDate("startDate"));
                    payPeriod.put("endDate", rs.getDate("endDate"));
                    payPeriod.put("payDate", rs.getDate("payDate"));
                    payPeriod.put("payPeriodDescription", rs.getString("payPeriodDescription"));
                    return payPeriod;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting pay period details: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Calculates hourly rate from employee data
     */
    private BigDecimal calculateHourlyRate(EmployeeModel employee) {
        if (employee.getHourlyRate() != null) {
            return employee.getHourlyRate();
        }
        
        if (employee.getBasicSalary() != null) {
            // Convert monthly salary to hourly rate
            // Assume 22 working days per month, 8 hours per day
            BigDecimal monthlyHours = new BigDecimal("176"); // 22 * 8
            return employee.getBasicSalary().divide(monthlyHours, 4, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    // UTILITY METHODS FOR LEAVE TYPE CLASSIFICATION
    
    /**
     * Determines if a leave type is paid
     */
    private boolean isPaidLeaveType(String leaveTypeName) {
        if (leaveTypeName == null) return false;
        
        String lowerCaseType = leaveTypeName.toLowerCase();
        return lowerCaseType.contains("vacation") || 
               lowerCaseType.contains("annual") ||
               lowerCaseType.contains("personal") ||
               lowerCaseType.contains("sick");
    }
    
    /**
     * Determines if a leave type is unpaid
     */
    private boolean isUnpaidLeaveType(String leaveTypeName) {
        if (leaveTypeName == null) return false;
        
        String lowerCaseType = leaveTypeName.toLowerCase();
        return lowerCaseType.contains("unpaid") || 
               lowerCaseType.contains("leave without pay") ||
               lowerCaseType.contains("lwop");
    }
    
    /**
     * Determines if a leave type is maternity/paternity leave
     */
    private boolean isMaternityPaternityLeave(String leaveTypeName) {
        if (leaveTypeName == null) return false;
        
        String lowerCaseType = leaveTypeName.toLowerCase();
        return lowerCaseType.contains("maternity") || 
               lowerCaseType.contains("paternity") ||
               lowerCaseType.contains("parental");
    }
    
    /**
     * Determines if a leave type is sick leave
     */
    private boolean isSickLeave(String leaveTypeName) {
        if (leaveTypeName == null) return false;
        
        String lowerCaseType = leaveTypeName.toLowerCase();
        return lowerCaseType.contains("sick") || 
               lowerCaseType.contains("medical");
    }
    
    /**
     * Saves leave payroll calculation result to database
     * @param result PayrollLeaveResult to save
     * @return true if successful
     */
    public boolean saveLeavePayrollCalculation(PayrollLeaveResult result) {
        String sql = "INSERT INTO payroll_leave " +
                    "(employeeId, payPeriodId, paidLeaveDays, paidLeavePay, " +
                    "unpaidLeaveDays, unpaidLeaveDeductions, excessLeaveDays, excessLeaveDeductions, " +
                    "maternityPaternityDays, maternityPaternityPay, sickLeaveDays, sickLeavePay, " +
                    "vacationLeaveAccrual, sickLeaveAccrual, totalLeavePayments, totalLeaveDeductions, " +
                    "netLeaveAdjustment, calculatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "paidLeaveDays = VALUES(paidLeaveDays), paidLeavePay = VALUES(paidLeavePay), " +
                    "unpaidLeaveDays = VALUES(unpaidLeaveDays), unpaidLeaveDeductions = VALUES(unpaidLeaveDeductions), " +
                    "excessLeaveDays = VALUES(excessLeaveDays), excessLeaveDeductions = VALUES(excessLeaveDeductions), " +
                    "maternityPaternityDays = VALUES(maternityPaternityDays), maternityPaternityPay = VALUES(maternityPaternityPay), " +
                    "sickLeaveDays = VALUES(sickLeaveDays), sickLeavePay = VALUES(sickLeavePay), " +
                    "vacationLeaveAccrual = VALUES(vacationLeaveAccrual), sickLeaveAccrual = VALUES(sickLeaveAccrual), " +
                    "totalLeavePayments = VALUES(totalLeavePayments), totalLeaveDeductions = VALUES(totalLeaveDeductions), " +
                    "netLeaveAdjustment = VALUES(netLeaveAdjustment), calculatedDate = CURRENT_TIMESTAMP";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, result.getEmployeeId());
            stmt.setInt(2, result.getPayPeriodId());
            stmt.setBigDecimal(3, result.getPaidLeaveDays());
            stmt.setBigDecimal(4, result.getPaidLeavePay());
            stmt.setBigDecimal(5, result.getUnpaidLeaveDays());
            stmt.setBigDecimal(6, result.getUnpaidLeaveDeductions());
            stmt.setBigDecimal(7, result.getExcessLeaveDays());
            stmt.setBigDecimal(8, result.getExcessLeaveDeductions());
            stmt.setBigDecimal(9, result.getMaternityPaternityDays());
            stmt.setBigDecimal(10, result.getMaternityPaternityPay());
            stmt.setBigDecimal(11, result.getSickLeaveDays());
            stmt.setBigDecimal(12, result.getSickLeavePay());
            stmt.setBigDecimal(13, result.getVacationLeaveAccrual());
            stmt.setBigDecimal(14, result.getSickLeaveAccrual());
            stmt.setBigDecimal(15, result.getTotalLeavePayments());
            stmt.setBigDecimal(16, result.getTotalLeaveDeductions());
            stmt.setBigDecimal(17, result.getNetLeaveAdjustment());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Leave payroll calculation saved for employee " + result.getEmployeeId() + 
                                 " pay period " + result.getPayPeriodId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error saving leave payroll calculation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Inner class to hold leave payroll calculation results
     */
    public static class PayrollLeaveResult {
        private Integer employeeId;
        private Integer payPeriodId;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal hourlyRate = BigDecimal.ZERO;
        
        // Paid leave
        private BigDecimal paidLeaveDays = BigDecimal.ZERO;
        private BigDecimal paidLeavePay = BigDecimal.ZERO;
        
        // Unpaid leave
        private BigDecimal unpaidLeaveDays = BigDecimal.ZERO;
        private BigDecimal unpaidLeaveDeductions = BigDecimal.ZERO;
        
        // Excess leave (over entitlement)
        private BigDecimal excessLeaveDays = BigDecimal.ZERO;
        private BigDecimal excessLeaveDeductions = BigDecimal.ZERO;
        
        // Maternity/Paternity leave
        private BigDecimal maternityPaternityDays = BigDecimal.ZERO;
        private BigDecimal maternityPaternityPay = BigDecimal.ZERO;
        
        // Sick leave
        private BigDecimal sickLeaveDays = BigDecimal.ZERO;
        private BigDecimal sickLeavePay = BigDecimal.ZERO;
        
        // Leave accruals
        private BigDecimal vacationLeaveAccrual = BigDecimal.ZERO;
        private BigDecimal sickLeaveAccrual = BigDecimal.ZERO;
        
        // Totals
        private BigDecimal totalLeavePayments = BigDecimal.ZERO;
        private BigDecimal totalLeaveDeductions = BigDecimal.ZERO;
        private BigDecimal netLeaveAdjustment = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public Integer getPayPeriodId() { return payPeriodId; }
        public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public BigDecimal getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
        
        public BigDecimal getPaidLeaveDays() { return paidLeaveDays; }
        public void setPaidLeaveDays(BigDecimal paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }
        
        public BigDecimal getPaidLeavePay() { return paidLeavePay; }
        public void setPaidLeavePay(BigDecimal paidLeavePay) { this.paidLeavePay = paidLeavePay; }
        
        public BigDecimal getUnpaidLeaveDays() { return unpaidLeaveDays; }
        public void setUnpaidLeaveDays(BigDecimal unpaidLeaveDays) { this.unpaidLeaveDays = unpaidLeaveDays; }
        
        public BigDecimal getUnpaidLeaveDeductions() { return unpaidLeaveDeductions; }
        public void setUnpaidLeaveDeductions(BigDecimal unpaidLeaveDeductions) { this.unpaidLeaveDeductions = unpaidLeaveDeductions; }
        
        public BigDecimal getExcessLeaveDays() { return excessLeaveDays; }
        public void setExcessLeaveDays(BigDecimal excessLeaveDays) { this.excessLeaveDays = excessLeaveDays; }
        
        public BigDecimal getExcessLeaveDeductions() { return excessLeaveDeductions; }
        public void setExcessLeaveDeductions(BigDecimal excessLeaveDeductions) { this.excessLeaveDeductions = excessLeaveDeductions; }
        
        public BigDecimal getMaternityPaternityDays() { return maternityPaternityDays; }
        public void setMaternityPaternityDays(BigDecimal maternityPaternityDays) { this.maternityPaternityDays = maternityPaternityDays; }
        
        public BigDecimal getMaternityPaternityPay() { return maternityPaternityPay; }
        public void setMaternityPaternityPay(BigDecimal maternityPaternityPay) { this.maternityPaternityPay = maternityPaternityPay; }
        
        public BigDecimal getSickLeaveDays() { return sickLeaveDays; }
        public void setSickLeaveDays(BigDecimal sickLeaveDays) { this.sickLeaveDays = sickLeaveDays; }
        
        public BigDecimal getSickLeavePay() { return sickLeavePay; }
        public void setSickLeavePay(BigDecimal sickLeavePay) { this.sickLeavePay = sickLeavePay; }
        
        public BigDecimal getVacationLeaveAccrual() { return vacationLeaveAccrual; }
        public void setVacationLeaveAccrual(BigDecimal vacationLeaveAccrual) { this.vacationLeaveAccrual = vacationLeaveAccrual; }
        
        public BigDecimal getSickLeaveAccrual() { return sickLeaveAccrual; }
        public void setSickLeaveAccrual(BigDecimal sickLeaveAccrual) { this.sickLeaveAccrual = sickLeaveAccrual; }
        
        public BigDecimal getTotalLeavePayments() { return totalLeavePayments; }
        public void setTotalLeavePayments(BigDecimal totalLeavePayments) { this.totalLeavePayments = totalLeavePayments; }
        
        public BigDecimal getTotalLeaveDeductions() { return totalLeaveDeductions; }
        public void setTotalLeaveDeductions(BigDecimal totalLeaveDeductions) { this.totalLeaveDeductions = totalLeaveDeductions; }
        
        public BigDecimal getNetLeaveAdjustment() { return netLeaveAdjustment; }
        public void setNetLeaveAdjustment(BigDecimal netLeaveAdjustment) { this.netLeaveAdjustment = netLeaveAdjustment; }
    }
}