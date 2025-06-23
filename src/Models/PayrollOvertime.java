
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
 * PayrollOvertime integrates overtime data with payroll calculations
 * Handles regular overtime, holiday overtime, night differential overtime, and special overtime rates
 * Links overtime management system to payroll processing
 * @author Chadley
 */


public class PayrollOvertime {
 private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    private final AttendanceTracking attendanceTracking;
    
    // Overtime calculation constants
    private static final BigDecimal REGULAR_OVERTIME_RATE = new BigDecimal("1.25"); // 125% for regular overtime
    private static final BigDecimal HOLIDAY_OVERTIME_RATE = new BigDecimal("2.60"); // 260% for holiday overtime
    private static final BigDecimal SPECIAL_HOLIDAY_OVERTIME_RATE = new BigDecimal("1.69"); // 169% for special holiday overtime
    private static final BigDecimal NIGHT_DIFFERENTIAL_RATE = new BigDecimal("0.10"); // 10% night differential
    private static final BigDecimal SUNDAY_OVERTIME_RATE = new BigDecimal("1.30"); // 130% for Sunday overtime
    private static final int STANDARD_HOURS_PER_DAY = 8;
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(22, 0); // 10:00 PM
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(6, 0); // 6:00 AM
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public PayrollOvertime(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.attendanceTracking = new AttendanceTracking(databaseConnection);
    }
    
    /**
     * Calculates overtime-related payroll adjustments for an employee in a specific pay period
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return PayrollOvertimeResult with all overtime-based calculations
     */
    public PayrollOvertimeResult calculateOvertimePayroll(Integer employeeId, Integer payPeriodId) {
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
            
            // Get overtime records for the pay period
            List<Map<String, Object>> overtimeRecords = getOvertimeRecordsInPeriod(employeeId, startDate, endDate);
            List<Map<String, Object>> attendanceRecords = attendanceTracking.getAttendanceHistory(employeeId, startDate, endDate);
            
            // Initialize calculation result
            PayrollOvertimeResult result = new PayrollOvertimeResult();
            result.setEmployeeId(employeeId);
            result.setPayPeriodId(payPeriodId);
            result.setStartDate(startDate);
            result.setEndDate(endDate);
            
            // Calculate hourly rate
            BigDecimal hourlyRate = calculateHourlyRate(employee);
            result.setHourlyRate(hourlyRate);
            
            // Calculate different overtime components
            calculateRegularOvertime(result, overtimeRecords, attendanceRecords, employee);
            calculateHolidayOvertime(result, overtimeRecords, startDate, endDate, employee);
            calculateNightDifferentialOvertime(result, overtimeRecords, attendanceRecords, employee);
            calculateWeekendOvertime(result, overtimeRecords, attendanceRecords, employee);
            calculateSpecialOvertime(result, overtimeRecords, employee);
            calculateOvertimeAllowances(result, overtimeRecords, employee);
            
            // Calculate totals
            BigDecimal totalOvertimeHours = result.getRegularOvertimeHours()
                .add(result.getHolidayOvertimeHours())
                .add(result.getWeekendOvertimeHours())
                .add(result.getSpecialOvertimeHours());
            
            BigDecimal totalOvertimePay = result.getRegularOvertimePay()
                .add(result.getHolidayOvertimePay())
                .add(result.getNightDifferentialPay())
                .add(result.getWeekendOvertimePay())
                .add(result.getSpecialOvertimePay())
                .add(result.getOvertimeAllowances());
            
            result.setTotalOvertimeHours(totalOvertimeHours);
            result.setTotalOvertimePay(totalOvertimePay);
            
            System.out.println("Overtime payroll calculated for employee " + employeeId + 
                             " for period " + startDate + " to " + endDate);
            System.out.println("Total overtime hours: " + totalOvertimeHours);
            System.out.println("Total overtime pay: " + totalOvertimePay);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error calculating overtime payroll: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Calculates regular overtime (weekday overtime)
     */
    private void calculateRegularOvertime(PayrollOvertimeResult result, 
                                        List<Map<String, Object>> overtimeRecords,
                                        List<Map<String, Object>> attendanceRecords,
                                        EmployeeModel employee) {
        
        BigDecimal totalRegularOvertimeHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        // Calculate from attendance records (automatic overtime)
        for (Map<String, Object> record : attendanceRecords) {
            java.sql.Date attendanceDate = (java.sql.Date) record.get("attendanceDate");
            Double hoursWorked = (Double) record.get("hoursWorked");
            
            if (attendanceDate != null && hoursWorked != null) {
                LocalDate workDate = attendanceDate.toLocalDate();
                DayOfWeek dayOfWeek = workDate.getDayOfWeek();
                
                // Only count weekday overtime (Monday to Friday)
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    if (hoursWorked > STANDARD_HOURS_PER_DAY) {
                        double overtimeHours = hoursWorked - STANDARD_HOURS_PER_DAY;
                        
                        // Check if it's not a holiday
                        if (!isHoliday(workDate)) {
                            totalRegularOvertimeHours = totalRegularOvertimeHours.add(new BigDecimal(overtimeHours));
                        }
                    }
                }
            }
        }
        
        // Add from approved overtime requests
        for (Map<String, Object> record : overtimeRecords) {
            String overtimeType = (String) record.get("overtimeType");
            Double requestedHours = (Double) record.get("overtimeHours");
            java.sql.Date overtimeDate = (java.sql.Date) record.get("overtimeDate");
            
            if ("Regular".equalsIgnoreCase(overtimeType) && requestedHours != null && overtimeDate != null) {
                LocalDate workDate = overtimeDate.toLocalDate();
                DayOfWeek dayOfWeek = workDate.getDayOfWeek();
                
                // Only weekday overtime
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !isHoliday(workDate)) {
                    totalRegularOvertimeHours = totalRegularOvertimeHours.add(new BigDecimal(requestedHours));
                }
            }
        }
        
        BigDecimal regularOvertimePay = totalRegularOvertimeHours.multiply(hourlyRate).multiply(REGULAR_OVERTIME_RATE);
        
        result.setRegularOvertimeHours(totalRegularOvertimeHours);
        result.setRegularOvertimePay(regularOvertimePay);
    }
    
    /**
     * Calculates holiday overtime
     */
    private void calculateHolidayOvertime(PayrollOvertimeResult result,
                                        List<Map<String, Object>> overtimeRecords,
                                        LocalDate startDate, LocalDate endDate,
                                        EmployeeModel employee) {
        
        BigDecimal totalHolidayOvertimeHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        // Get holidays in the pay period
        List<LocalDate> holidays = getHolidaysInPeriod(startDate, endDate);
        List<LocalDate> specialHolidays = getSpecialHolidaysInPeriod(startDate, endDate);
        
        for (Map<String, Object> record : overtimeRecords) {
            java.sql.Date overtimeDate = (java.sql.Date) record.get("overtimeDate");
            Double overtimeHours = (Double) record.get("overtimeHours");
            
            if (overtimeDate != null && overtimeHours != null) {
                LocalDate workDate = overtimeDate.toLocalDate();
                
                if (holidays.contains(workDate)) {
                    // Regular holiday overtime
                    totalHolidayOvertimeHours = totalHolidayOvertimeHours.add(new BigDecimal(overtimeHours));
                }
            }
        }
        
        BigDecimal holidayOvertimePay = totalHolidayOvertimeHours.multiply(hourlyRate).multiply(HOLIDAY_OVERTIME_RATE);
        
        result.setHolidayOvertimeHours(totalHolidayOvertimeHours);
        result.setHolidayOvertimePay(holidayOvertimePay);
    }
    
    /**
     * Calculates night differential overtime
     */
    private void calculateNightDifferentialOvertime(PayrollOvertimeResult result,
                                                  List<Map<String, Object>> overtimeRecords,
                                                  List<Map<String, Object>> attendanceRecords,
                                                  EmployeeModel employee) {
        
        BigDecimal totalNightDifferentialHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        // Calculate night differential from attendance records
        for (Map<String, Object> record : attendanceRecords) {
            Time timeIn = (Time) record.get("timeIn");
            Time timeOut = (Time) record.get("timeOut");
            Double hoursWorked = (Double) record.get("hoursWorked");
            
            if (timeIn != null && timeOut != null && hoursWorked != null && hoursWorked > STANDARD_HOURS_PER_DAY) {
                LocalTime clockIn = timeIn.toLocalTime();
                LocalTime clockOut = timeOut.toLocalTime();
                
                // Calculate night shift hours during overtime period
                double nightHours = calculateNightShiftOvertimeHours(clockIn, clockOut, hoursWorked);
                if (nightHours > 0) {
                    totalNightDifferentialHours = totalNightDifferentialHours.add(new BigDecimal(nightHours));
                }
            }
        }
        
        BigDecimal nightDifferentialPay = totalNightDifferentialHours
            .multiply(hourlyRate)
            .multiply(NIGHT_DIFFERENTIAL_RATE);
        
        result.setNightDifferentialHours(totalNightDifferentialHours);
        result.setNightDifferentialPay(nightDifferentialPay);
    }
    
    /**
     * Calculates weekend overtime (Saturday/Sunday)
     */
    private void calculateWeekendOvertime(PayrollOvertimeResult result,
                                        List<Map<String, Object>> overtimeRecords,
                                        List<Map<String, Object>> attendanceRecords,
                                        EmployeeModel employee) {
        
        BigDecimal totalWeekendOvertimeHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        // Calculate from attendance records
        for (Map<String, Object> record : attendanceRecords) {
            java.sql.Date attendanceDate = (java.sql.Date) record.get("attendanceDate");
            Double hoursWorked = (Double) record.get("hoursWorked");
            
            if (attendanceDate != null && hoursWorked != null) {
                LocalDate workDate = attendanceDate.toLocalDate();
                DayOfWeek dayOfWeek = workDate.getDayOfWeek();
                
                // Weekend work (Saturday/Sunday)
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    totalWeekendOvertimeHours = totalWeekendOvertimeHours.add(new BigDecimal(hoursWorked));
                }
            }
        }
        
        // Add from overtime requests
        for (Map<String, Object> record : overtimeRecords) {
            java.sql.Date overtimeDate = (java.sql.Date) record.get("overtimeDate");
            Double overtimeHours = (Double) record.get("overtimeHours");
            
            if (overtimeDate != null && overtimeHours != null) {
                LocalDate workDate = overtimeDate.toLocalDate();
                DayOfWeek dayOfWeek = workDate.getDayOfWeek();
                
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    totalWeekendOvertimeHours = totalWeekendOvertimeHours.add(new BigDecimal(overtimeHours));
                }
            }
        }
        
        BigDecimal weekendOvertimePay = totalWeekendOvertimeHours.multiply(hourlyRate).multiply(SUNDAY_OVERTIME_RATE);
        
        result.setWeekendOvertimeHours(totalWeekendOvertimeHours);
        result.setWeekendOvertimePay(weekendOvertimePay);
    }
    
    /**
     * Calculates special overtime (emergency, project-based)
     */
    private void calculateSpecialOvertime(PayrollOvertimeResult result,
                                        List<Map<String, Object>> overtimeRecords,
                                        EmployeeModel employee) {
        
        BigDecimal totalSpecialOvertimeHours = BigDecimal.ZERO;
        BigDecimal hourlyRate = result.getHourlyRate();
        
        for (Map<String, Object> record : overtimeRecords) {
            String overtimeType = (String) record.get("overtimeType");
            Double overtimeHours = (Double) record.get("overtimeHours");
            BigDecimal specialRate = (BigDecimal) record.get("specialRate");
            
            if (overtimeHours != null && 
                ("Special".equalsIgnoreCase(overtimeType) || 
                 "Emergency".equalsIgnoreCase(overtimeType) ||
                 "Project".equalsIgnoreCase(overtimeType))) {
                
                totalSpecialOvertimeHours = totalSpecialOvertimeHours.add(new BigDecimal(overtimeHours));
            }
        }
        
        // Use special rate if available, otherwise use regular overtime rate
        BigDecimal rate = REGULAR_OVERTIME_RATE; // Default
        BigDecimal specialOvertimePay = totalSpecialOvertimeHours.multiply(hourlyRate).multiply(rate);
        
        result.setSpecialOvertimeHours(totalSpecialOvertimeHours);
        result.setSpecialOvertimePay(specialOvertimePay);
    }
    
    /**
     * Calculates overtime allowances and bonuses
     */
    private void calculateOvertimeAllowances(PayrollOvertimeResult result,
                                           List<Map<String, Object>> overtimeRecords,
                                           EmployeeModel employee) {
        
        BigDecimal totalOvertimeAllowances = BigDecimal.ZERO;
        
        for (Map<String, Object> record : overtimeRecords) {
            BigDecimal allowanceAmount = (BigDecimal) record.get("allowanceAmount");
            if (allowanceAmount != null) {
                totalOvertimeAllowances = totalOvertimeAllowances.add(allowanceAmount);
            }
        }
        
        result.setOvertimeAllowances(totalOvertimeAllowances);
    }
    
    // DATA RETRIEVAL METHODS
    
    /**
     * Gets overtime records for a specific pay period
     */
    private List<Map<String, Object>> getOvertimeRecordsInPeriod(Integer employeeId, 
                                                               LocalDate startDate, 
                                                               LocalDate endDate) {
        String sql = "SELECT or.*, ot.overtimeTypeName " +
                    "FROM overtimerequest or " +
                    "LEFT JOIN overtimetype ot ON or.overtimeTypeId = ot.overtimeTypeId " +
                    "WHERE or.employeeId = ? AND or.status = 'Approved' " +
                    "AND or.overtimeDate BETWEEN ? AND ? " +
                    "ORDER BY or.overtimeDate";
        
        return executeQuery(sql, employeeId, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
    }
    
    /**
     * Gets pay period details by ID
     */
    private Map<String, Object> getPayPeriodDetails(Integer payPeriodId) {
        String sql = "SELECT payPeriodId, startDate, endDate, payDate, payPeriodDescription " +
                    "FROM payperiod WHERE payPeriodId = ?";
        
        List<Map<String, Object>> results = executeQuery(sql, payPeriodId);
        return results.isEmpty() ? null : results.get(0);
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
    
    /**
     * Calculates night shift hours during overtime period
     */
    private double calculateNightShiftOvertimeHours(LocalTime clockIn, LocalTime clockOut, double totalHours) {
        if (totalHours <= STANDARD_HOURS_PER_DAY) {
            return 0.0; // No overtime
        }
        
        // Calculate overtime portion
        double overtimeHours = totalHours - STANDARD_HOURS_PER_DAY;
        
        // Determine if overtime falls during night shift
        LocalTime overtimeStart = clockIn.plusHours(STANDARD_HOURS_PER_DAY);
        
        double nightOvertimeHours = 0.0;
        
        // Check if overtime period overlaps with night shift (10 PM to 6 AM)
        if ((overtimeStart.isAfter(NIGHT_SHIFT_START) || overtimeStart.equals(NIGHT_SHIFT_START)) ||
            (clockOut.isBefore(NIGHT_SHIFT_END) || clockOut.equals(NIGHT_SHIFT_END))) {
            
            // Simplified calculation - can be made more precise
            if (overtimeStart.isAfter(NIGHT_SHIFT_START) || overtimeStart.isBefore(NIGHT_SHIFT_END)) {
                nightOvertimeHours = Math.min(overtimeHours, 8.0); // Max 8 hours night differential
            }
        }
        
        return nightOvertimeHours;
    }
    
    /**
     * Checks if a date is a holiday
     */
    private boolean isHoliday(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM holidays WHERE holidayDate = ? AND isRegularHoliday = true";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, java.sql.Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking holiday: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets holidays within a date range
     */
    private List<LocalDate> getHolidaysInPeriod(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> holidays = new ArrayList<>();
        
        String sql = "SELECT holidayDate FROM holidays " +
                    "WHERE holidayDate BETWEEN ? AND ? AND isRegularHoliday = true";
        
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
     * Gets special holidays within a date range
     */
    private List<LocalDate> getSpecialHolidaysInPeriod(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> specialHolidays = new ArrayList<>();
        
        String sql = "SELECT holidayDate FROM holidays " +
                    "WHERE holidayDate BETWEEN ? AND ? AND isRegularHoliday = false";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    specialHolidays.add(rs.getDate("holidayDate").toLocalDate());
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting special holidays: " + e.getMessage());
        }
        
        return specialHolidays;
    }
    
    /**
     * Generic query execution method
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Saves overtime payroll calculation result to database
     * @param result PayrollOvertimeResult to save
     * @return true if successful
     */
    public boolean saveOvertimePayrollCalculation(PayrollOvertimeResult result) {
        String sql = "INSERT INTO payroll_overtime " +
                    "(employeeId, payPeriodId, regularOvertimeHours, regularOvertimePay, " +
                    "holidayOvertimeHours, holidayOvertimePay, nightDifferentialHours, nightDifferentialPay, " +
                    "weekendOvertimeHours, weekendOvertimePay, specialOvertimeHours, specialOvertimePay, " +
                    "overtimeAllowances, totalOvertimeHours, totalOvertimePay, calculatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "regularOvertimeHours = VALUES(regularOvertimeHours), regularOvertimePay = VALUES(regularOvertimePay), " +
                    "holidayOvertimeHours = VALUES(holidayOvertimeHours), holidayOvertimePay = VALUES(holidayOvertimePay), " +
                    "nightDifferentialHours = VALUES(nightDifferentialHours), nightDifferentialPay = VALUES(nightDifferentialPay), " +
                    "weekendOvertimeHours = VALUES(weekendOvertimeHours), weekendOvertimePay = VALUES(weekendOvertimePay), " +
                    "specialOvertimeHours = VALUES(specialOvertimeHours), specialOvertimePay = VALUES(specialOvertimePay), " +
                    "overtimeAllowances = VALUES(overtimeAllowances), totalOvertimeHours = VALUES(totalOvertimeHours), " +
                    "totalOvertimePay = VALUES(totalOvertimePay), calculatedDate = CURRENT_TIMESTAMP";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, result.getEmployeeId());
            stmt.setInt(2, result.getPayPeriodId());
            stmt.setBigDecimal(3, result.getRegularOvertimeHours());
            stmt.setBigDecimal(4, result.getRegularOvertimePay());
            stmt.setBigDecimal(5, result.getHolidayOvertimeHours());
            stmt.setBigDecimal(6, result.getHolidayOvertimePay());
            stmt.setBigDecimal(7, result.getNightDifferentialHours());
            stmt.setBigDecimal(8, result.getNightDifferentialPay());
            stmt.setBigDecimal(9, result.getWeekendOvertimeHours());
            stmt.setBigDecimal(10, result.getWeekendOvertimePay());
            stmt.setBigDecimal(11, result.getSpecialOvertimeHours());
            stmt.setBigDecimal(12, result.getSpecialOvertimePay());
            stmt.setBigDecimal(13, result.getOvertimeAllowances());
            stmt.setBigDecimal(14, result.getTotalOvertimeHours());
            stmt.setBigDecimal(15, result.getTotalOvertimePay());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Overtime payroll calculation saved for employee " + result.getEmployeeId() + 
                                 " pay period " + result.getPayPeriodId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error saving overtime payroll calculation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Inner class to hold overtime payroll calculation results
     */
    public static class PayrollOvertimeResult {
        private Integer employeeId;
        private Integer payPeriodId;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal hourlyRate = BigDecimal.ZERO;
        
        // Regular overtime (weekday overtime)
        private BigDecimal regularOvertimeHours = BigDecimal.ZERO;
        private BigDecimal regularOvertimePay = BigDecimal.ZERO;
        
        // Holiday overtime
        private BigDecimal holidayOvertimeHours = BigDecimal.ZERO;
        private BigDecimal holidayOvertimePay = BigDecimal.ZERO;
        
        // Night differential
        private BigDecimal nightDifferentialHours = BigDecimal.ZERO;
        private BigDecimal nightDifferentialPay = BigDecimal.ZERO;
        
        // Weekend overtime
        private BigDecimal weekendOvertimeHours = BigDecimal.ZERO;
        private BigDecimal weekendOvertimePay = BigDecimal.ZERO;
        
        // Special overtime
        private BigDecimal specialOvertimeHours = BigDecimal.ZERO;
        private BigDecimal specialOvertimePay = BigDecimal.ZERO;
        
        // Overtime allowances
        private BigDecimal overtimeAllowances = BigDecimal.ZERO;
        
        // Totals
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;
        
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
        
        public BigDecimal getRegularOvertimeHours() { return regularOvertimeHours; }
        public void setRegularOvertimeHours(BigDecimal regularOvertimeHours) { this.regularOvertimeHours = regularOvertimeHours; }
        
        public BigDecimal getRegularOvertimePay() { return regularOvertimePay; }
        public void setRegularOvertimePay(BigDecimal regularOvertimePay) { this.regularOvertimePay = regularOvertimePay; }
        
        public BigDecimal getHolidayOvertimeHours() { return holidayOvertimeHours; }
        public void setHolidayOvertimeHours(BigDecimal holidayOvertimeHours) { this.holidayOvertimeHours = holidayOvertimeHours; }
        
        public BigDecimal getHolidayOvertimePay() { return holidayOvertimePay; }
        public void setHolidayOvertimePay(BigDecimal holidayOvertimePay) { this.holidayOvertimePay = holidayOvertimePay; }
        
        public BigDecimal getNightDifferentialHours() { return nightDifferentialHours; }
        public void setNightDifferentialHours(BigDecimal nightDifferentialHours) { this.nightDifferentialHours = nightDifferentialHours; }
        
        public BigDecimal getNightDifferentialPay() { return nightDifferentialPay; }
        public void setNightDifferentialPay(BigDecimal nightDifferentialPay) { this.nightDifferentialPay = nightDifferentialPay; }
        
        public BigDecimal getWeekendOvertimeHours() { return weekendOvertimeHours; }
        public void setWeekendOvertimeHours(BigDecimal weekendOvertimeHours) { this.weekendOvertimeHours = weekendOvertimeHours; }
        
        public BigDecimal getWeekendOvertimePay() { return weekendOvertimePay; }
        public void setWeekendOvertimePay(BigDecimal weekendOvertimePay) { this.weekendOvertimePay = weekendOvertimePay; }
        
        public BigDecimal getSpecialOvertimeHours() { return specialOvertimeHours; }
        public void setSpecialOvertimeHours(BigDecimal specialOvertimeHours) { this.specialOvertimeHours = specialOvertimeHours; }
        
        public BigDecimal getSpecialOvertimePay() { return specialOvertimePay; }
        public void setSpecialOvertimePay(BigDecimal specialOvertimePay) { this.specialOvertimePay = specialOvertimePay; }
        
        public BigDecimal getOvertimeAllowances() { return overtimeAllowances; }
        public void setOvertimeAllowances(BigDecimal overtimeAllowances) { this.overtimeAllowances = overtimeAllowances; }
        
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
    }
}
