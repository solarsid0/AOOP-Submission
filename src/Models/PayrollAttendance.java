
package Models;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Models.EmployeeModel;
import oop.classes.management.AttendanceTracking;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * PayrollAttendance integrates attendance data with payroll calculations
 * Handles regular hours, overtime calculations, late deductions, and attendance-based pay
 * Links attendance tracking system to payroll processing
 * @author Chadley
 */

public class PayrollAttendance {
  private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    private final AttendanceTracking attendanceTracking;
    
    // Payroll calculation constants
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.25"); // 125% for overtime
    private static final BigDecimal NIGHT_DIFFERENTIAL_RATE = new BigDecimal("0.10"); // 10% night differential
    private static final BigDecimal HOLIDAY_MULTIPLIER = new BigDecimal("2.00"); // 200% for holidays
    private static final int STANDARD_HOURS_PER_DAY = 8;
    private static final int STANDARD_DAYS_PER_WEEK = 5;
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(18, 0); // 6:00 PM
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(6, 0); // 6:00 AM
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public PayrollAttendance(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.attendanceTracking = new AttendanceTracking(databaseConnection);
    }
    
    /**
     * Calculates payroll for an employee based on attendance data for a specific pay period
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return PayrollCalculationResult with all attendance-based calculations
     */
    public PayrollCalculationResult calculatePayrollFromAttendance(Integer employeeId, Integer payPeriodId) {
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
            
            // Get attendance data for the pay period
            List<Map<String, Object>> attendanceRecords = getAttendanceForPayPeriod(employeeId, startDate, endDate);
            
            // Initialize calculation result
            PayrollCalculationResult result = new PayrollCalculationResult();
            result.setEmployeeId(employeeId);
            result.setPayPeriodId(payPeriodId);
            result.setStartDate(startDate);
            result.setEndDate(endDate);
            
            // Calculate various attendance-based components
            calculateRegularHours(result, attendanceRecords, employee);
            calculateOvertimeHours(result, attendanceRecords, employee);
            calculateNightDifferential(result, attendanceRecords, employee);
            calculateHolidayPay(result, attendanceRecords, employee, startDate, endDate);
            calculateLateDeductions(result, attendanceRecords, employee);
            calculateAbsenceDeductions(result, attendanceRecords, employee, startDate, endDate);
            
            // Calculate total gross pay
            BigDecimal totalGrossPay = result.getRegularPay()
                .add(result.getOvertimePay())
                .add(result.getNightDifferentialPay())
                .add(result.getHolidayPay())
                .subtract(result.getLateDeductions())
                .subtract(result.getAbsenceDeductions());
            
            result.setGrossPay(totalGrossPay);
            
            System.out.println("Payroll calculated for employee " + employeeId + 
                             " for period " + startDate + " to " + endDate);
            System.out.println("Total gross pay: " + totalGrossPay);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error calculating payroll from attendance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Calculates regular working hours and pay
     */
    private void calculateRegularHours(PayrollCalculationResult result, 
                                     List<Map<String, Object>> attendanceRecords, 
                                     EmployeeModel employee) {
        
        BigDecimal totalRegularHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = employee.getHourlyRate() != null ? 
            employee.getHourlyRate() : calculateHourlyRateFromSalary(employee.getBasicSalary());
        
        for (Map<String, Object> record : attendanceRecords) {
            Double hoursWorked = (Double) record.get("hoursWorked");
            if (hoursWorked != null && hoursWorked > 0) {
                // Cap regular hours at standard hours per day
                double regularHoursForDay = Math.min(hoursWorked, STANDARD_HOURS_PER_DAY);
                totalRegularHours = totalRegularHours.add(new BigDecimal(regularHoursForDay));
            }
        }
        
        BigDecimal regularPay = totalRegularHours.multiply(hourlyRate);
        
        result.setRegularHours(totalRegularHours);
        result.setRegularPay(regularPay);
        result.setHourlyRate(hourlyRate);
    }
    
    /**
     * Calculates overtime hours and pay
     */
    private void calculateOvertimeHours(PayrollCalculationResult result, 
                                      List<Map<String, Object>> attendanceRecords, 
                                      EmployeeModel employee) {
        
        BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : attendanceRecords) {
            Double hoursWorked = (Double) record.get("hoursWorked");
            Double overtimeHours = (Double) record.get("overtimeHours");
            
            if (overtimeHours != null && overtimeHours > 0) {
                totalOvertimeHours = totalOvertimeHours.add(new BigDecimal(overtimeHours));
            } else if (hoursWorked != null && hoursWorked > STANDARD_HOURS_PER_DAY) {
                // Calculate overtime if not already stored
                double calculatedOvertime = hoursWorked - STANDARD_HOURS_PER_DAY;
                totalOvertimeHours = totalOvertimeHours.add(new BigDecimal(calculatedOvertime));
            }
        }
        
        BigDecimal overtimePay = totalOvertimeHours.multiply(hourlyRate).multiply(OVERTIME_MULTIPLIER);
        
        result.setOvertimeHours(totalOvertimeHours);
        result.setOvertimePay(overtimePay);
    }
    
    /**
     * Calculates night differential pay
     */
    private void calculateNightDifferential(PayrollCalculationResult result, 
                                          List<Map<String, Object>> attendanceRecords, 
                                          EmployeeModel employee) {
        
        BigDecimal totalNightHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : attendanceRecords) {
            Time timeIn = (Time) record.get("timeIn");
            Time timeOut = (Time) record.get("timeOut");
            
            if (timeIn != null && timeOut != null) {
                LocalTime clockIn = timeIn.toLocalTime();
                LocalTime clockOut = timeOut.toLocalTime();
                
                // Calculate night shift hours
                double nightHours = calculateNightShiftHours(clockIn, clockOut);
                if (nightHours > 0) {
                    totalNightHours = totalNightHours.add(new BigDecimal(nightHours));
                }
            }
        }
        
        BigDecimal nightDifferentialPay = totalNightHours.multiply(hourlyRate).multiply(NIGHT_DIFFERENTIAL_RATE);
        
        result.setNightDifferentialHours(totalNightHours);
        result.setNightDifferentialPay(nightDifferentialPay);
    }
    
    /**
     * Calculates holiday pay
     */
    private void calculateHolidayPay(PayrollCalculationResult result, 
                                   List<Map<String, Object>> attendanceRecords, 
                                   EmployeeModel employee,
                                   LocalDate startDate, LocalDate endDate) {
        
        BigDecimal totalHolidayHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        // Get holidays in the pay period
        List<LocalDate> holidays = getHolidaysInPeriod(startDate, endDate);
        
        for (Map<String, Object> record : attendanceRecords) {
            java.sql.Date attendanceDate = (java.sql.Date) record.get("attendanceDate");
            if (attendanceDate != null) {
                LocalDate workDate = attendanceDate.toLocalDate();
                
                if (holidays.contains(workDate)) {
                    Double hoursWorked = (Double) record.get("hoursWorked");
                    if (hoursWorked != null && hoursWorked > 0) {
                        totalHolidayHours = totalHolidayHours.add(new BigDecimal(hoursWorked));
                    }
                }
            }
        }
        
        // Holiday pay is regular rate + 100% premium (total 200%)
        BigDecimal holidayPay = totalHolidayHours.multiply(hourlyRate).multiply(HOLIDAY_MULTIPLIER.subtract(BigDecimal.ONE));
        
        result.setHolidayHours(totalHolidayHours);
        result.setHolidayPay(holidayPay);
    }
    
    /**
     * Calculates late deductions
     */
    private void calculateLateDeductions(PayrollCalculationResult result, 
                                       List<Map<String, Object>> attendanceRecords, 
                                       EmployeeModel employee) {
        
        BigDecimal totalLateMinutes = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : attendanceRecords) {
            Boolean isLate = (Boolean) record.get("isLate");
            Integer lateMinutes = (Integer) record.get("lateMinutes");
            
            if (isLate != null && isLate && lateMinutes != null && lateMinutes > 0) {
                totalLateMinutes = totalLateMinutes.add(new BigDecimal(lateMinutes));
            }
        }
        
        // Convert minutes to hours and calculate deduction
        BigDecimal lateHours = totalLateMinutes.divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
        BigDecimal lateDeductions = lateHours.multiply(hourlyRate);
        
        result.setTotalLateMinutes(totalLateMinutes.intValue());
        result.setLateDeductions(lateDeductions);
    }
    
    /**
     * Calculates absence deductions
     */
    private void calculateAbsenceDeductions(PayrollCalculationResult result, 
                                          List<Map<String, Object>> attendanceRecords, 
                                          EmployeeModel employee,
                                          LocalDate startDate, LocalDate endDate) {
        
        int workingDays = calculateWorkingDays(startDate, endDate);
        int presentDays = 0;
        
        for (Map<String, Object> record : attendanceRecords) {
            String status = (String) record.get("status");
            if ("Present".equals(status) || "On Leave".equals(status)) {
                presentDays++;
            }
        }
        
        int absentDays = workingDays - presentDays;
        BigDecimal dailyRate = result.getHourlyRate().multiply(new BigDecimal(STANDARD_HOURS_PER_DAY));
        BigDecimal absenceDeductions = dailyRate.multiply(new BigDecimal(absentDays));
        
        result.setAbsentDays(absentDays);
        result.setAbsenceDeductions(absenceDeductions);
    }
    
    /**
     * Gets attendance records for a specific pay period
     */
    private List<Map<String, Object>> getAttendanceForPayPeriod(Integer employeeId, 
                                                               LocalDate startDate, 
                                                               LocalDate endDate) {
        return attendanceTracking.getAttendanceHistory(employeeId, startDate, endDate);
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
     * Calculates hourly rate from monthly salary
     */
    private BigDecimal calculateHourlyRateFromSalary(BigDecimal monthlySalary) {
        if (monthlySalary == null) {
            return BigDecimal.ZERO;
        }
        
        // Assume 22 working days per month, 8 hours per day
        BigDecimal monthlyHours = new BigDecimal("176"); // 22 * 8
        return monthlySalary.divide(monthlyHours, 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates night shift hours between two times
     */
    private double calculateNightShiftHours(LocalTime clockIn, LocalTime clockOut) {
        double nightHours = 0.0;
        
        // Handle overnight shifts
        if (clockOut.isBefore(clockIn)) {
            // Shift spans midnight
            if (clockIn.isAfter(NIGHT_SHIFT_START) || clockIn.equals(NIGHT_SHIFT_START)) {
                // From clock in to midnight
                nightHours += Duration.between(clockIn, LocalTime.MAX).toMinutes() / 60.0;
            }
            if (clockOut.isBefore(NIGHT_SHIFT_END) || clockOut.equals(NIGHT_SHIFT_END)) {
                // From midnight to clock out
                nightHours += Duration.between(LocalTime.MIN, clockOut).toMinutes() / 60.0;
            }
        } else {
            // Regular shift within same day
            LocalTime nightStart = clockIn.isAfter(NIGHT_SHIFT_START) ? clockIn : NIGHT_SHIFT_START;
            LocalTime nightEnd = clockOut.isBefore(NIGHT_SHIFT_END) ? clockOut : NIGHT_SHIFT_END;
            
            if (nightStart.isBefore(nightEnd)) {
                nightHours = Duration.between(nightStart, nightEnd).toMinutes() / 60.0;
            }
        }
        
        return nightHours;
    }
    
    /**
     * Gets holidays within a date range
     */
    private List<LocalDate> getHolidaysInPeriod(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> holidays = new ArrayList<>();
        
        String sql = "SELECT holidayDate FROM holidays " +
                    "WHERE holidayDate BETWEEN ? AND ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    holidays.add(rs.getDate("holidayDate").toLocalDate());
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting holidays: " + e.getMessage());
        }
        
        return holidays;
    }
    
    /**
     * Calculates working days between two dates (excluding weekends)
     */
    private int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    /**
     * Saves payroll calculation result to database
     * @param result PayrollCalculationResult to save
     * @return true if successful
     */
    public boolean savePayrollCalculation(PayrollCalculationResult result) {
        String sql = "INSERT INTO payroll " +
                    "(employeeId, payPeriodId, regularHours, regularPay, overtimeHours, overtimePay, " +
                    "nightDifferentialHours, nightDifferentialPay, holidayHours, holidayPay, " +
                    "lateDeductions, absenceDeductions, grossPay, calculatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "regularHours = VALUES(regularHours), regularPay = VALUES(regularPay), " +
                    "overtimeHours = VALUES(overtimeHours), overtimePay = VALUES(overtimePay), " +
                    "nightDifferentialHours = VALUES(nightDifferentialHours), " +
                    "nightDifferentialPay = VALUES(nightDifferentialPay), " +
                    "holidayHours = VALUES(holidayHours), holidayPay = VALUES(holidayPay), " +
                    "lateDeductions = VALUES(lateDeductions), absenceDeductions = VALUES(absenceDeductions), " +
                    "grossPay = VALUES(grossPay), calculatedDate = CURRENT_TIMESTAMP";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, result.getEmployeeId());
            stmt.setInt(2, result.getPayPeriodId());
            stmt.setBigDecimal(3, result.getRegularHours());
            stmt.setBigDecimal(4, result.getRegularPay());
            stmt.setBigDecimal(5, result.getOvertimeHours());
            stmt.setBigDecimal(6, result.getOvertimePay());
            stmt.setBigDecimal(7, result.getNightDifferentialHours());
            stmt.setBigDecimal(8, result.getNightDifferentialPay());
            stmt.setBigDecimal(9, result.getHolidayHours());
            stmt.setBigDecimal(10, result.getHolidayPay());
            stmt.setBigDecimal(11, result.getLateDeductions());
            stmt.setBigDecimal(12, result.getAbsenceDeductions());
            stmt.setBigDecimal(13, result.getGrossPay());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Payroll calculation saved for employee " + result.getEmployeeId() + 
                                 " pay period " + result.getPayPeriodId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error saving payroll calculation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Inner class to hold payroll calculation results
     */
    public static class PayrollCalculationResult {
        private Integer employeeId;
        private Integer payPeriodId;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal hourlyRate = BigDecimal.ZERO;
        private BigDecimal regularHours = BigDecimal.ZERO;
        private BigDecimal regularPay = BigDecimal.ZERO;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        private BigDecimal overtimePay = BigDecimal.ZERO;
        private BigDecimal nightDifferentialHours = BigDecimal.ZERO;
        private BigDecimal nightDifferentialPay = BigDecimal.ZERO;
        private BigDecimal holidayHours = BigDecimal.ZERO;
        private BigDecimal holidayPay = BigDecimal.ZERO;
        private Integer totalLateMinutes = 0;
        private BigDecimal lateDeductions = BigDecimal.ZERO;
        private Integer absentDays = 0;
        private BigDecimal absenceDeductions = BigDecimal.ZERO;
        private BigDecimal grossPay = BigDecimal.ZERO;
        
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
        
        public BigDecimal getRegularHours() { return regularHours; }
        public void setRegularHours(BigDecimal regularHours) { this.regularHours = regularHours; }
        
        public BigDecimal getRegularPay() { return regularPay; }
        public void setRegularPay(BigDecimal regularPay) { this.regularPay = regularPay; }
        
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
        
        public BigDecimal getOvertimePay() { return overtimePay; }
        public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
        
        public BigDecimal getNightDifferentialHours() { return nightDifferentialHours; }
        public void setNightDifferentialHours(BigDecimal nightDifferentialHours) { this.nightDifferentialHours = nightDifferentialHours; }
        
        public BigDecimal getNightDifferentialPay() { return nightDifferentialPay; }
        public void setNightDifferentialPay(BigDecimal nightDifferentialPay) { this.nightDifferentialPay = nightDifferentialPay; }
        
        public BigDecimal getHolidayHours() { return holidayHours; }
        public void setHolidayHours(BigDecimal holidayHours) { this.holidayHours = holidayHours; }
        
        public BigDecimal getHolidayPay() { return holidayPay; }
        public void setHolidayPay(BigDecimal holidayPay) { this.holidayPay = holidayPay; }
        
        public Integer getTotalLateMinutes() { return totalLateMinutes; }
        public void setTotalLateMinutes(Integer totalLateMinutes) { this.totalLateMinutes = totalLateMinutes; }
        
        public BigDecimal getLateDeductions() { return lateDeductions; }
        public void setLateDeductions(BigDecimal lateDeductions) { this.lateDeductions = lateDeductions; }
        
        public Integer getAbsentDays() { return absentDays; }
        public void setAbsentDays(Integer absentDays) { this.absentDays = absentDays; }
        
        public BigDecimal getAbsenceDeductions() { return absenceDeductions; }
        public void setAbsenceDeductions(BigDecimal absenceDeductions) { this.absenceDeductions = absenceDeductions; }
        
        public BigDecimal getGrossPay() { return grossPay; }
        public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }
    }
}