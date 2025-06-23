
package Models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TardinessRecordModel class that maps to the tardinessrecord table
 * Fields: tardinessId, employeeId, date, tardinessType, minutesLate, deductionAmount
 * @author User
 */
public class TardinessRecordModel {
    
    // Enum for tardiness types
    public enum TardinessType {
        LATE("Late"),
        UNDERTIME("Undertime");
        
        private final String displayName;
        
        TardinessType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static TardinessType fromString(String text) {
            for (TardinessType t : TardinessType.values()) {
                if (t.displayName.equalsIgnoreCase(text)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    private Integer tardinessId;
    private Integer attendanceId;
    private BigDecimal tardinessHours;
    private TardinessType tardinessType;
    private String supervisorNotes;
    private LocalDateTime createdAt;
    
    // Constructors
    public TardinessRecordModel() {}
    
    public TardinessRecordModel(Integer attendanceId, BigDecimal tardinessHours, TardinessType tardinessType, String supervisorNotes) {
        this.attendanceId = attendanceId;
        this.tardinessHours = tardinessHours;
        this.tardinessType = tardinessType;
        this.supervisorNotes = supervisorNotes;
    }
    
    public TardinessRecordModel(Integer tardinessId, Integer attendanceId, BigDecimal tardinessHours, 
                              TardinessType tardinessType, String supervisorNotes, LocalDateTime createdAt) {
        this.tardinessId = tardinessId;
        this.attendanceId = attendanceId;
        this.tardinessHours = tardinessHours;
        this.tardinessType = tardinessType;
        this.supervisorNotes = supervisorNotes;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Integer getTardinessId() { return tardinessId; }
    public void setTardinessId(Integer tardinessId) { this.tardinessId = tardinessId; }
    
    public Integer getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Integer attendanceId) { this.attendanceId = attendanceId; }
    
    public BigDecimal getTardinessHours() { return tardinessHours; }
    public void setTardinessHours(BigDecimal tardinessHours) { this.tardinessHours = tardinessHours; }
    
    public TardinessType getTardinessType() { return tardinessType; }
    public void setTardinessType(TardinessType tardinessType) { this.tardinessType = tardinessType; }
    
    public String getSupervisorNotes() { return supervisorNotes; }
    public void setSupervisorNotes(String supervisorNotes) { this.supervisorNotes = supervisorNotes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Business Methods
    
    /**
     * Calculate deduction amount based on hourly rate
     * @param hourlyRate
     * @return 
     */
    public BigDecimal calculateDeductionAmount(BigDecimal hourlyRate) {
        if (hourlyRate == null || tardinessHours == null) {
            return BigDecimal.ZERO;
        }
        
        return hourlyRate.multiply(tardinessHours).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Convert minutes to hours (for storage)
     * @param minutes
     * @return 
     */
    public static BigDecimal minutesToHours(int minutes) {
        return new BigDecimal(minutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Convert hours to minutes (for display)
     * @return 
     */
    public int getMinutesLate() {
        if (tardinessHours == null) {
            return 0;
        }
        
        return tardinessHours.multiply(new BigDecimal(60)).intValue();
    }
    
    /**
     * Format tardiness hours for display (H:MM format)
     * @return 
     */
    public String getFormattedTardinessTime() {
        if (tardinessHours == null) {
            return "0:00";
        }
        
        int totalMinutes = getMinutesLate();
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        
        return String.format("%d:%02d", hours, minutes);
    }
    
    /**
     * Check if this is a significant tardiness (more than threshold)
     * @param thresholdHours
     * @return 
     */
    public boolean isSignificantTardiness(BigDecimal thresholdHours) {
        if (tardinessHours == null || thresholdHours == null) {
            return false;
        }
        
        return tardinessHours.compareTo(thresholdHours) > 0;
    }
    
    /**
     * Get severity level based on tardiness hours
     * @return 
     */
    public String getSeverityLevel() {
        if (tardinessHours == null) {
            return "None";
        }
        
        BigDecimal hours = tardinessHours;
        
        if (hours.compareTo(new BigDecimal("0.25")) <= 0) { // 15 minutes or less
            return "Minor";
        } else if (hours.compareTo(new BigDecimal("1.00")) <= 0) { // 1 hour or less
            return "Moderate";
        } else if (hours.compareTo(new BigDecimal("2.00")) <= 0) { // 2 hours or less
            return "Serious";
        } else {
            return "Critical";
        }
    }
    
    
    /**
     * Validate tardiness record data
     * @return 
     */
    public boolean isValid() {
        if (attendanceId == null || attendanceId <= 0) {
            return false;
        }
        
        if (tardinessHours == null || tardinessHours.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (tardinessType == null) {
            return false;
        }
        // Check if tardiness hours is reasonable (not more than 24 hours)
        
        return tardinessHours.compareTo(new BigDecimal("24.00")) <= 0;
    }
    
    /**
     * Check if supervisor notes are present
     * @return 
     */
    public boolean hasNotes() {
        return supervisorNotes != null && !supervisorNotes.trim().isEmpty();
    }
    
    /**
     * Get display description of the tardiness
     * @return 
     */
    public String getDisplayDescription() {
        StringBuilder description = new StringBuilder();
        
        description.append(tardinessType.getDisplayName());
        description.append(": ").append(getFormattedTardinessTime());
        description.append(" (").append(getSeverityLevel()).append(")");
        
        return description.toString();
    }
    
    /**
     * Calculate tardiness as percentage of standard work day (8 hours)
     * @return 
     */
    public double getTardinessPercentage() {
        if (tardinessHours == null) {
            return 0.0;
        }
        
        BigDecimal standardWorkDay = new BigDecimal("8.00"); // 8 hours
        return tardinessHours.divide(standardWorkDay, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal(100))
                            .doubleValue();
    }
    
    /**
     * Check if this tardiness occurred today
     * @return 
     */
    public boolean isToday() {
        if (createdAt == null) {
            return false;
        }
        
        return createdAt.toLocalDate().equals(LocalDate.now());
    }
    
    /**
     * Set tardiness from minutes
     * @param minutes
     */
    public void setTardinessFromMinutes(int minutes) {
        this.tardinessHours = minutesToHours(minutes);
    }
    
    /**
     * Add supervisor note with timestamp
     * @param note
     * @param supervisorName
     */
    public void addSupervisorNote(String note, String supervisorName) {
        if (note == null || note.trim().isEmpty()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String formattedNote = String.format("[%s by %s] %s", timestamp, supervisorName, note.trim());
        
        if (this.supervisorNotes == null || this.supervisorNotes.trim().isEmpty()) {
            this.supervisorNotes = formattedNote;
        } else {
            this.supervisorNotes += "\n" + formattedNote;
        }
    }
    
    @Override
    public String toString() {
        return String.format("TardinessRecordModel{tardinessId=%d, attendanceId=%d, tardinessHours=%s, tardinessType=%s, supervisorNotes='%s', createdAt=%s}", 
                           tardinessId, attendanceId, tardinessHours, tardinessType, supervisorNotes, createdAt);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TardinessRecordModel that = (TardinessRecordModel) obj;
        return Objects.equals(tardinessId, that.tardinessId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tardinessId);
    }
}
