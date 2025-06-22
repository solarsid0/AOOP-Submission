package Models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

/**
 * AttendanceModel class that matches the database table structure exactly.
 * This class represents an attendance record entity in the payroll system.
 * Each field corresponds to a column in the attendance table.
 * @author User
 */
public class AttendanceModel {
    
    // Primary key - auto-increment in database
    private Integer attendanceId;
    
    // Required fields
    private LocalDate date;      // Date of attendance
    private Integer employeeId;  // Foreign key to employee table
    
    // Optional time fields
    private LocalTime timeIn;    // Time employee clocked in
    private LocalTime timeOut;   // Time employee clocked out
    

    // CONSTRUCTORS

    
    /**
     * Default constructor
     */
    public AttendanceModel() {
        // Empty constructor
    }
    
    /**
     * Constructor with essential fields for creating a new attendance record
     * @param employeeId The employee ID
     * @param date The date of attendance
     */
    public AttendanceModel(Integer employeeId, LocalDate date) {
        this.employeeId = employeeId;
        this.date = date;
    }
    
    /**
     * Constructor with time in for marking arrival
     * @param employeeId The employee ID
     * @param date The date of attendance
     * @param timeIn The time employee clocked in
     */
    public AttendanceModel(Integer employeeId, LocalDate date, LocalTime timeIn) {
        this.employeeId = employeeId;
        this.date = date;
        this.timeIn = timeIn;
    }
    
    /**
     * Full constructor with all fields
     * Use this when loading attendance records from the database
     * @param attendanceId
     * @param employeeId
     * @param date
     * @param timeIn
     * @param timeOut
     */
    public AttendanceModel(Integer attendanceId, Integer employeeId, LocalDate date, 
                          LocalTime timeIn, LocalTime timeOut) {
        this.attendanceId = attendanceId;
        this.employeeId = employeeId;
        this.date = date;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
    }
    

    // GETTERS AND SETTERS

    
    public Integer getAttendanceId() {
        return attendanceId;
    }
    
    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public LocalTime getTimeIn() {
        return timeIn;
    }
    
    public void setTimeIn(LocalTime timeIn) {
        this.timeIn = timeIn;
    }
    
    public LocalTime getTimeOut() {
        return timeOut;
    }
    
    public void setTimeOut(LocalTime timeOut) {
        this.timeOut = timeOut;
    }
    
    // ===============================
    // BUSINESS METHODS
    // ===============================
    
    /**
     * Calculates the total hours worked for this attendance record
     * @return Total hours worked as BigDecimal, or 0 if incomplete record
     */
    public BigDecimal getHoursWorked() {
        if (timeIn == null || timeOut == null) {
            return BigDecimal.ZERO;
        }
        
        // Handle cases where time out is after midnight (next day)
        Duration duration = Duration.between(timeIn, timeOut);
        if (duration.isNegative()) {
            // Add 24 hours if timeOut is on the next day
            duration = duration.plusDays(1);
        }
        
        // Convert to hours with 2 decimal places
        long totalMinutes = duration.toMinutes();
        return new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculates the total minutes worked for this attendance record
     * @return Total minutes worked, or 0 if incomplete record
     */
    public long getMinutesWorked() {
        if (timeIn == null || timeOut == null) {
            return 0;
        }
        
        Duration duration = Duration.between(timeIn, timeOut);
        if (duration.isNegative()) {
            duration = duration.plusDays(1);
        }
        
        return duration.toMinutes();
    }
    
    /**
     * Checks if this attendance record is complete (has both time in and time out)
     * @return true if both timeIn and timeOut are set
     */
    public boolean isComplete() {
        return timeIn != null && timeOut != null;
    }
    
    /**
     * Checks if employee has clocked in but not yet clocked out
     * @return true if timeIn is set but timeOut is null
     */
    public boolean isClockedIn() {
        return timeIn != null && timeOut == null;
    }
    
    /**
     * Checks if this attendance record is for today
     * @return true if date is today's date
     */
    public boolean isToday() {
        return date != null && date.equals(LocalDate.now());
    }
    
    /**
     * Checks if employee was late based on a standard start time
     * @param standardStartTime The expected start time
     * @return true if timeIn is after standardStartTime
     */
    public boolean isLate(LocalTime standardStartTime) {
        return timeIn != null && timeIn.isAfter(standardStartTime);
    }
    
    /**
     * Checks if employee left early based on a standard end time
     * @param standardEndTime The expected end time
     * @return true if timeOut is before standardEndTime
     */
    public boolean isEarlyLeave(LocalTime standardEndTime) {
        return timeOut != null && timeOut.isBefore(standardEndTime);
    }
    
    /**
     * Calculates overtime hours based on a standard work day
     * @param standardHours The standard number of hours per day (e.g., 8 hours)
     * @return Overtime hours worked, or 0 if no overtime
     */
    public BigDecimal getOvertimeHours(BigDecimal standardHours) {
        BigDecimal hoursWorked = getHoursWorked();
        if (hoursWorked.compareTo(standardHours) > 0) {
            return hoursWorked.subtract(standardHours);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculates late minutes based on a standard start time
     * @param standardStartTime The expected start time
     * @return Minutes late, or 0 if on time or early
     */
    public long getLateMinutes(LocalTime standardStartTime) {
        if (timeIn == null || !timeIn.isAfter(standardStartTime)) {
            return 0;
        }
        return Duration.between(standardStartTime, timeIn).toMinutes();
    }
    
    /**
     * Calculates undertime (early leave) minutes based on a standard end time
     * @param standardEndTime The expected end time
     * @return Minutes of undertime, or 0 if stayed until or past standard time
     */
    public long getUndertimeMinutes(LocalTime standardEndTime) {
        if (timeOut == null || !timeOut.isBefore(standardEndTime)) {
            return 0;
        }
        return Duration.between(timeOut, standardEndTime).toMinutes();
    }
    
    /**
     * Marks time in for this attendance record
     * @param timeIn The time to mark as time in
     */
    public void clockIn(LocalTime timeIn) {
        this.timeIn = timeIn;
    }
    
    /**
     * Marks time out for this attendance record
     * @param timeOut The time to mark as time out
     */
    public void clockOut(LocalTime timeOut) {
        this.timeOut = timeOut;
    }
    
    /**
     * Marks time in with current time
     */
    public void clockInNow() {
        this.timeIn = LocalTime.now();
    }
    
    /**
     * Marks time out with current time
     */
    public void clockOutNow() {
        this.timeOut = LocalTime.now();
    }
    

    // UTILITY METHODS

    
    /**
     * Returns a string representation of the attendance record
     */
    @Override
    public String toString() {
        return "AttendanceModel{" +
                "attendanceId=" + attendanceId +
                ", employeeId=" + employeeId +
                ", date=" + date +
                ", timeIn=" + timeIn +
                ", timeOut=" + timeOut +
                ", hoursWorked=" + getHoursWorked() +
                ", isComplete=" + isComplete() +
                '}';
    }
    
    /**
     * Checks if two AttendanceModel objects are equal
     * Two attendance records are equal if they have the same attendanceId
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AttendanceModel that = (AttendanceModel) obj;
        return attendanceId != null && attendanceId.equals(that.attendanceId);
    }
    
    /**
     * Generates a hash code for the attendance record
     */
    @Override
    public int hashCode() {
        return attendanceId != null ? attendanceId.hashCode() : 0;
    }
    
    /**
     * Returns a formatted display string for this attendance record
     * @return Human-readable string with date, times, and hours worked
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(date);
        
        if (timeIn != null) {
            sb.append(", Time In: ").append(timeIn);
        } else {
            sb.append(", Time In: Not marked");
        }
        
        if (timeOut != null) {
            sb.append(", Time Out: ").append(timeOut);
        } else {
            sb.append(", Time Out: Not marked");
        }
        
        if (isComplete()) {
            sb.append(", Hours Worked: ").append(getHoursWorked());
        } else {
            sb.append(", Hours Worked: Incomplete");
        }
        
        return sb.toString();
    }
}