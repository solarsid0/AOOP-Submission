
package Services;

import DAOs.*;
import Models.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * AttendanceService - Business logic for attendance tracking and management
 * Handles time in/out, attendance reporting, and attendance-based calculations
 */

public class AttendanceService {
    
    // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final AttendanceDAO attendanceDAO;
    private final EmployeeDAO employeeDAO;
    private final TardinessRecordDAO tardinessDAO;
    
    // Business Rules Configuration
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0); // 8:00 AM
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0);  // 5:00 PM
    private static final int STANDARD_WORK_HOURS = 8;
    private static final int GRACE_PERIOD_MINUTES = 15; // 15-minute grace period for late
    
    /**
     * Constructor - initializes required DAOs
     */
    public AttendanceService() {
        this.databaseConnection = new DatabaseConnection();
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.tardinessDAO = new TardinessRecordDAO();
    }
    
    /**
     * Constructor with custom database connection
     */
    public AttendanceService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.tardinessDAO = new TardinessRecordDAO();
    }
    
    // ================================
    // TIME IN/OUT OPERATIONS
    // ================================
    
    /**
     * Records time in for an employee
     * @param employeeId Employee ID
     * @param timeIn Time in (null for current time)
     * @return AttendanceResult with success status and details
     */
    public AttendanceResult recordTimeIn(Integer employeeId, LocalTime timeIn) {
        AttendanceResult result = new AttendanceResult();
        LocalDate today = LocalDate.now();
        LocalTime actualTimeIn = timeIn != null ? timeIn : LocalTime.now();
        
        try {
            // Validate employee exists
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }
            
            // Check if already timed in today
            if (attendanceDAO.hasMarkedTimeInToday(employeeId)) {
                result.setSuccess(false);
                result.setMessage("Employee " + employee.getFullName() + " has already timed in today");
                return result;
            }
            
            // Record time in
            boolean success = attendanceDAO.markTimeIn(employeeId, today, actualTimeIn);
            
            if (success) {
                result.setSuccess(true);
                result.setMessage("Time in recorded successfully for " + employee.getFullName() + " at " + actualTimeIn);
                
                // Check for tardiness
                if (isLate(actualTimeIn)) {
                    long lateMinutes = calculateLateMinutes(actualTimeIn);
                    result.setLateMinutes(lateMinutes);
                    result.setMessage(result.getMessage() + " (Late by " + lateMinutes + " minutes)");
                    
                    // Record tardiness if beyond grace period
                    if (lateMinutes > GRACE_PERIOD_MINUTES) {
                        recordTardiness(employeeId, today, lateMinutes, TardinessRecordModel.TardinessType.LATE);
                    }
                }
                
                System.out.println("✅ Time in recorded: " + employee.getFullName() + " at " + actualTimeIn);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to record time in for " + employee.getFullName());
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error recording time in: " + e.getMessage());
            System.err.println("❌ Error recording time in for employee " + employeeId + ": " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Records time out for an employee
     * @param employeeId Employee ID
     * @param timeOut Time out (null for current time)
     * @return AttendanceResult with success status and details
     */
    public AttendanceResult recordTimeOut(Integer employeeId, LocalTime timeOut) {
        AttendanceResult result = new AttendanceResult();
        LocalDate today = LocalDate.now();
        LocalTime actualTimeOut = timeOut != null ? timeOut : LocalTime.now();
        
        try {
            // Validate employee exists
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }
            
            // Check if already timed out today
            if (attendanceDAO.hasMarkedTimeOutToday(employeeId)) {
                result.setSuccess(false);
                result.setMessage("Employee " + employee.getFullName() + " has already timed out today");
                return result;
            }
            
            // Check if employee has timed in today
            if (!attendanceDAO.hasMarkedTimeInToday(employeeId)) {
                result.setSuccess(false);
                result.setMessage("Employee " + employee.getFullName() + " has not timed in today");
                return result;
            }
            
            // Record time out
            boolean success = attendanceDAO.markTimeOut(employeeId, today, actualTimeOut);
            
            if (success) {
                // Get the attendance record to calculate hours worked
                AttendanceModel attendance = attendanceDAO.findByEmployeeAndDate(employeeId, today);
                
                result.setSuccess(true);
                result.setMessage("Time out recorded successfully for " + employee.getFullName() + " at " + actualTimeOut);
                
                if (attendance != null && attendance.isComplete()) {
                    BigDecimal hoursWorked = attendance.getHoursWorked();
                    result.setHoursWorked(hoursWorked);
                    result.setMessage(result.getMessage() + " (Hours worked: " + hoursWorked + ")");
                    
                    // Check for undertime
                    if (isEarlyLeave(actualTimeOut)) {
                        long undertimeMinutes = calculateUndertimeMinutes(actualTimeOut);
                        result.setUndertimeMinutes(undertimeMinutes);
                        result.setMessage(result.getMessage() + " (Undertime: " + undertimeMinutes + " minutes)");
                        
                        // Record undertime
                        recordTardiness(employeeId, today, undertimeMinutes, TardinessRecordModel.TardinessType.UNDERTIME);
                    }
                }
                
                System.out.println("✅ Time out recorded: " + employee.getFullName() + " at " + actualTimeOut);
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to record time out for " + employee.getFullName());
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error recording time out: " + e.getMessage());
            System.err.println("❌ Error recording time out for employee " + employeeId + ": " + e.getMessage());
        }
        
        return result;
    }
    
    // ================================
    // ATTENDANCE REPORTING
    // ================================
    
    /**
     * Gets attendance summary for an employee in a specific month
     * @param employeeId Employee ID
     * @param yearMonth Year and month
     * @return AttendanceSummary with detailed statistics
     */
    public AttendanceSummary getMonthlyAttendanceSummary(Integer employeeId, YearMonth yearMonth) {
        AttendanceSummary summary = new AttendanceSummary();
        summary.setEmployeeId(employeeId);
        summary.setYearMonth(yearMonth);
        
        try {
            // Get employee info
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee != null) {
                summary.setEmployeeName(employee.getFullName());
            }
            
            // Get attendance statistics from DAO
            AttendanceDAO.AttendanceStatistics stats = attendanceDAO.getMonthlyAttendanceStatistics(employeeId, yearMonth);
            
            summary.setTotalDays(stats.getTotalDays());
            summary.setCompleteDays(stats.getCompleteDays());
            summary.setIncompleteDays(stats.getIncompleteDays());
            summary.setTotalHours(stats.getTotalHours());
            
            // Calculate additional metrics
            summary.setAttendanceRate(calculateAttendanceRate(stats.getCompleteDays(), getWorkingDaysInMonth(yearMonth)));
            summary.setAverageHoursPerDay(calculateAverageHoursPerDay(stats.getTotalHours(), stats.getCompleteDays()));
            
            // Get tardiness information
            List<TardinessRecordModel> tardinessRecords = getTardinessRecordsForMonth(employeeId, yearMonth);
            summary.setLateInstances(countLateInstances(tardinessRecords));
            summary.setUndertimeInstances(countUndertimeInstances(tardinessRecords));
            summary.setTotalLateHours(calculateTotalLateHours(tardinessRecords));
            
        } catch (Exception e) {
            System.err.println("Error generating attendance summary: " + e.getMessage());
        }
        
        return summary;
    }
    
    /**
     * Gets daily attendance report for a specific date
     * @param date The date to get attendance for
     * @return List of daily attendance records
     */
    public List<DailyAttendanceRecord> getDailyAttendanceReport(LocalDate date) {
        List<DailyAttendanceRecord> records = new ArrayList<>();
        
        try {
            // Get all attendance records for the date
            List<AttendanceModel> attendanceList = attendanceDAO.getAttendanceByDate(date);
            
            for (AttendanceModel attendance : attendanceList) {
                EmployeeModel employee = employeeDAO.findById(attendance.getEmployeeId());
                if (employee != null) {
                    DailyAttendanceRecord record = new DailyAttendanceRecord();
                    record.setEmployeeId(attendance.getEmployeeId());
                    record.setEmployeeName(employee.getFullName());
                    record.setDate(date);
                    record.setTimeIn(attendance.getTimeIn());
                    record.setTimeOut(attendance.getTimeOut());
                    record.setHoursWorked(attendance.getHoursWorked());
                    record.setIsComplete(attendance.isComplete());
                    
                    // Calculate status
                    if (attendance.getTimeIn() != null && isLate(attendance.getTimeIn())) {
                        record.setStatus("Late");
                        record.setLateMinutes(calculateLateMinutes(attendance.getTimeIn()));
                    } else if (attendance.getTimeOut() != null && isEarlyLeave(attendance.getTimeOut())) {
                        record.setStatus("Early Leave");
                        record.setUndertimeMinutes(calculateUndertimeMinutes(attendance.getTimeOut()));
                    } else if (attendance.isComplete()) {
                        record.setStatus("Present");
                    } else if (attendance.getTimeIn() != null) {
                        record.setStatus("Incomplete");
                    } else {
                        record.setStatus("Absent");
                    }
                    
                    records.add(record);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error generating daily attendance report: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * Gets employees who are currently clocked in (have time in but no time out today)
     * @return List of employees currently at work
     */
    public List<DailyAttendanceRecord> getCurrentlyPresentEmployees() {
        List<DailyAttendanceRecord> currentlyPresent = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        try {
            List<AttendanceModel> incompleteAttendance = attendanceDAO.getIncompleteAttendance(today);
            
            for (AttendanceModel attendance : incompleteAttendance) {
                EmployeeModel employee = employeeDAO.findById(attendance.getEmployeeId());
                if (employee != null) {
                    DailyAttendanceRecord record = new DailyAttendanceRecord();
                    record.setEmployeeId(attendance.getEmployeeId());
                    record.setEmployeeName(employee.getFullName());
                    record.setDate(today);
                    record.setTimeIn(attendance.getTimeIn());
                    record.setStatus("Currently Present");
                    record.setIsComplete(false);
                    
                    // Calculate current hours worked
                    if (attendance.getTimeIn() != null) {
                        Duration duration = Duration.between(attendance.getTimeIn(), LocalTime.now());
                        BigDecimal currentHours = new BigDecimal(duration.toMinutes())
                                .divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
                        record.setHoursWorked(currentHours);
                    }
                    
                    currentlyPresent.add(record);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error getting currently present employees: " + e.getMessage());
        }
        
        return currentlyPresent;
    }
    
    // ================================
    // TARDINESS AND COMPLIANCE
    // ================================
    
    /**
     * Records tardiness for an employee
     */
    private void recordTardiness(Integer employeeId, LocalDate date, long minutes, TardinessRecordModel.TardinessType type) {
        try {
            // First get the attendance record
            AttendanceModel attendance = attendanceDAO.findByEmployeeAndDate(employeeId, date);
            if (attendance != null) {
                TardinessRecordModel tardiness = new TardinessRecordModel();
                tardiness.setAttendanceId(attendance.getAttendanceId());
                tardiness.setTardinessHours(TardinessRecordModel.minutesToHours((int) minutes));
                tardiness.setTardinessType(type);
                tardiness.setSupervisorNotes("Auto-generated " + type.getDisplayName().toLowerCase() + " record");
                
                tardinessDAO.save(tardiness);
                System.out.println("📝 Recorded " + type.getDisplayName().toLowerCase() + " for employee " + employeeId + ": " + minutes + " minutes");
            }
        } catch (Exception e) {
            System.err.println("Error recording tardiness: " + e.getMessage());
        }
    }
    
    /**
     * Gets employees with perfect attendance for a month
     * @param yearMonth Year and month
     * @return List of employee IDs with perfect attendance
     */
    public List<Integer> getEmployeesWithPerfectAttendance(YearMonth yearMonth) {
        List<Integer> perfectAttendance = new ArrayList<>();
        
        try {
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();
            int workingDays = getWorkingDaysInMonth(yearMonth);
            
            for (EmployeeModel employee : activeEmployees) {
                AttendanceDAO.AttendanceStatistics stats = attendanceDAO.getMonthlyAttendanceStatistics(employee.getEmployeeId(), yearMonth);
                
                // Perfect attendance: complete days equals working days and no tardiness
                if (stats.getCompleteDays() == workingDays) {
                    List<TardinessRecordModel> tardinessRecords = getTardinessRecordsForMonth(employee.getEmployeeId(), yearMonth);
                    if (tardinessRecords.isEmpty()) {
                        perfectAttendance.add(employee.getEmployeeId());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error finding employees with perfect attendance: " + e.getMessage());
        }
        
        return perfectAttendance;
    }
    
    // ================================
    // UTILITY AND CALCULATION METHODS
    // ================================
    
    /**
     * Checks if a time in is considered late
     */
    private boolean isLate(LocalTime timeIn) {
        return timeIn.isAfter(STANDARD_START_TIME);
    }
    
    /**
     * Checks if a time out is considered early leave
     */
    private boolean isEarlyLeave(LocalTime timeOut) {
        return timeOut.isBefore(STANDARD_END_TIME);
    }
    
    /**
     * Calculates minutes late from standard start time
     */
    private long calculateLateMinutes(LocalTime timeIn) {
        if (!isLate(timeIn)) return 0;
        return Duration.between(STANDARD_START_TIME, timeIn).toMinutes();
    }
    
    /**
     * Calculates undertime minutes from standard end time
     */
    private long calculateUndertimeMinutes(LocalTime timeOut) {
        if (!isEarlyLeave(timeOut)) return 0;
        return Duration.between(timeOut, STANDARD_END_TIME).toMinutes();
    }
    
    /**
     * Calculates attendance rate as percentage
     */
    private BigDecimal calculateAttendanceRate(int completeDays, int workingDays) {
        if (workingDays == 0) return BigDecimal.ZERO;
        return new BigDecimal(completeDays).divide(new BigDecimal(workingDays), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates average hours per day
     */
    private BigDecimal calculateAverageHoursPerDay(BigDecimal totalHours, int completeDays) {
        if (completeDays == 0) return BigDecimal.ZERO;
        return totalHours.divide(new BigDecimal(completeDays), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Gets working days in a month (excludes weekends)
     */
    private int getWorkingDaysInMonth(YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        int workingDays = 0;
        
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() <= 5) { // Monday to Friday
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    /**
     * Gets tardiness records for a month (placeholder - needs TardinessRecordDAO implementation)
     */
    private List<TardinessRecordModel> getTardinessRecordsForMonth(Integer employeeId, YearMonth yearMonth) {
        // This would need implementation in TardinessRecordDAO
        return new ArrayList<>();
    }
    
    /**
     * Counts late instances from tardiness records
     */
    private int countLateInstances(List<TardinessRecordModel> records) {
        return (int) records.stream()
                .filter(r -> r.getTardinessType() == TardinessRecordModel.TardinessType.LATE)
                .count();
    }
    
    /**
     * Counts undertime instances from tardiness records
     */
    private int countUndertimeInstances(List<TardinessRecordModel> records) {
        return (int) records.stream()
                .filter(r -> r.getTardinessType() == TardinessRecordModel.TardinessType.UNDERTIME)
                .count();
    }
    
    /**
     * Calculates total late hours from tardiness records
     */
    private BigDecimal calculateTotalLateHours(List<TardinessRecordModel> records) {
        return records.stream()
                .filter(r -> r.getTardinessType() == TardinessRecordModel.TardinessType.LATE)
                .map(TardinessRecordModel::getTardinessHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // ================================
    // PUBLIC QUERY METHODS
    // ================================
    
    /**
     * Gets attendance history for an employee
     */
    public List<AttendanceModel> getEmployeeAttendanceHistory(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceDAO.getAttendanceHistory(employeeId, startDate, endDate);
    }
    
    /**
     * Gets attendance record for a specific employee and date
     */
    public AttendanceModel getAttendanceRecord(Integer employeeId, LocalDate date) {
        return attendanceDAO.findByEmployeeAndDate(employeeId, date);
    }
    
    // ================================
    // INNER CLASSES
    // ================================
    
    /**
     * Result of attendance operations (time in/out)
     */
    public static class AttendanceResult {
        private boolean success = false;
        private String message = "";
        private long lateMinutes = 0;
        private long undertimeMinutes = 0;
        private BigDecimal hoursWorked = BigDecimal.ZERO;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getLateMinutes() { return lateMinutes; }
        public void setLateMinutes(long lateMinutes) { this.lateMinutes = lateMinutes; }
        
        public long getUndertimeMinutes() { return undertimeMinutes; }
        public void setUndertimeMinutes(long undertimeMinutes) { this.undertimeMinutes = undertimeMinutes; }
        
        public BigDecimal getHoursWorked() { return hoursWorked; }
        public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }
    }
    
    /**
     * Monthly attendance summary for an employee
     */
    public static class AttendanceSummary {
        private Integer employeeId;
        private String employeeName;
        private YearMonth yearMonth;
        private int totalDays = 0;
        private int completeDays = 0;
        private int incompleteDays = 0;
        private BigDecimal totalHours = BigDecimal.ZERO;
        private BigDecimal attendanceRate = BigDecimal.ZERO;
        private BigDecimal averageHoursPerDay = BigDecimal.ZERO;
        private int lateInstances = 0;
        private int undertimeInstances = 0;
        private BigDecimal totalLateHours = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        
        public int getTotalDays() { return totalDays; }
        public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
        
        public int getCompleteDays() { return completeDays; }
        public void setCompleteDays(int completeDays) { this.completeDays = completeDays; }
        
        public int getIncompleteDays() { return incompleteDays; }
        public void setIncompleteDays(int incompleteDays) { this.incompleteDays = incompleteDays; }
        
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
        
        public BigDecimal getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(BigDecimal attendanceRate) { this.attendanceRate = attendanceRate; }
        
        public BigDecimal getAverageHoursPerDay() { return averageHoursPerDay; }
        public void setAverageHoursPerDay(BigDecimal averageHoursPerDay) { this.averageHoursPerDay = averageHoursPerDay; }
        
        public int getLateInstances() { return lateInstances; }
        public void setLateInstances(int lateInstances) { this.lateInstances = lateInstances; }
        
        public int getUndertimeInstances() { return undertimeInstances; }
        public void setUndertimeInstances(int undertimeInstances) { this.undertimeInstances = undertimeInstances; }
        
        public BigDecimal getTotalLateHours() { return totalLateHours; }
        public void setTotalLateHours(BigDecimal totalLateHours) { this.totalLateHours = totalLateHours; }
    }
    
    /**
     * Daily attendance record for reporting
     */
    public static class DailyAttendanceRecord {
        private Integer employeeId;
        private String employeeName;
        private LocalDate date;
        private LocalTime timeIn;
        private LocalTime timeOut;
        private BigDecimal hoursWorked = BigDecimal.ZERO;
        private boolean isComplete = false;
        private String status = "Absent";
        private long lateMinutes = 0;
        private long undertimeMinutes = 0;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public LocalTime getTimeIn() { return timeIn; }
        public void setTimeIn(LocalTime timeIn) { this.timeIn = timeIn; }
        
        public LocalTime getTimeOut() { return timeOut; }
        public void setTimeOut(LocalTime timeOut) { this.timeOut = timeOut; }
        
        public BigDecimal getHoursWorked() { return hoursWorked; }
        public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }
        
        public boolean getIsComplete() { return isComplete; }
        public void setIsComplete(boolean isComplete) { this.isComplete = isComplete; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getLateMinutes() { return lateMinutes; }
        public void setLateMinutes(long lateMinutes) { this.lateMinutes = lateMinutes; }
        
        public long getUndertimeMinutes() { return undertimeMinutes; }
        public void setUndertimeMinutes(long undertimeMinutes) { this.undertimeMinutes = undertimeMinutes; }
    }
}
