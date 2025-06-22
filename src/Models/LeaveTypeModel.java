/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LeaveTypeModel class that maps to the leavetype table
 * Fields: leaveTypeId, leaveTypeName, maxDays, description
 * Replaces deleted LeaveName.java
 * @author User
 */
public class LeaveTypeModel {
    
    private Integer leaveTypeId;
    private String leaveTypeName;
    private String leaveDescription;
    private Integer maxDaysPerYear;
    private LocalDateTime createdAt;
    
    // Constructors
    public LeaveTypeModel() {}
    
    public LeaveTypeModel(String leaveTypeName, String leaveDescription, Integer maxDaysPerYear) {
        this.leaveTypeName = leaveTypeName;
        this.leaveDescription = leaveDescription;
        this.maxDaysPerYear = maxDaysPerYear;
    }
    
    public LeaveTypeModel(Integer leaveTypeId, String leaveTypeName, String leaveDescription, 
                         Integer maxDaysPerYear, LocalDateTime createdAt) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.leaveDescription = leaveDescription;
        this.maxDaysPerYear = maxDaysPerYear;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Integer getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Integer leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    
    public String getLeaveDescription() { return leaveDescription; }
    public void setLeaveDescription(String leaveDescription) { this.leaveDescription = leaveDescription; }
    
    public Integer getMaxDaysPerYear() { return maxDaysPerYear; }
    public void setMaxDaysPerYear(Integer maxDaysPerYear) { this.maxDaysPerYear = maxDaysPerYear; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Business Methods
    
    /**
     * Check if leave type has unlimited days (null or negative maxDaysPerYear)
     * @return 
     */
    public boolean isUnlimited() {
        return maxDaysPerYear == null || maxDaysPerYear <= 0;
    }
    
    /**
     * Check if a requested number of days is within the annual limit
     * @param requestedDays
     * @param alreadyUsedDays
     * @return 
     */
    public boolean isWithinAnnualLimit(int requestedDays, int alreadyUsedDays) {
        if (isUnlimited()) {
            return true; // No limit
        }
        
        return (alreadyUsedDays + requestedDays) <= maxDaysPerYear;
    }
    
    /**
     * Calculate remaining days available for the year
     * @param alreadyUsedDays
     * @return 
     */
    public int getRemainingDays(int alreadyUsedDays) {
        if (isUnlimited()) {
            return Integer.MAX_VALUE; // Unlimited
        }
        
        int remaining = maxDaysPerYear - alreadyUsedDays;
        return Math.max(0, remaining); // Don't return negative
    }
    
    /**
     * Get formatted description with max days info
     * @return 
     */
    public String getFormattedDescription() {
        StringBuilder description = new StringBuilder();
        
        if (leaveDescription != null && !leaveDescription.trim().isEmpty()) {
            description.append(leaveDescription);
        }
        
        if (maxDaysPerYear != null && maxDaysPerYear > 0) {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("Max ").append(maxDaysPerYear).append(" days per year");
        } else {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("No annual limit");
        }
        
        return description.toString();
    }
    
    /**
     * Validate leave type data
     * @return 
     */
    public boolean isValid() {
        if (leaveTypeName == null || leaveTypeName.trim().isEmpty()) {
            return false;
        }
        
        if (leaveTypeName.length() > 50) { // Based on database constraint
            return false;
        }
        
        return !(leaveDescription != null && leaveDescription.length() > 255);
    }
    
    /**
     * Check if this leave type allows partial days
     * @return 
     */
    public boolean allowsPartialDays() {
        // This could be extended to include a flag in the database
        // For now, assume all leave types allow partial days
        return true;
    }
    
    /**
     * Get display name with limits
     * @return 
     */
    public String getDisplayNameWithLimits() {
        if (isUnlimited()) {
            return leaveTypeName + " (Unlimited)";
        } else {
            return leaveTypeName + " (Max: " + maxDaysPerYear + " days/year)";
        }
    }
    
    /**
     * Calculate percentage of annual allowance used
     * @param usedDays
     * @return 
     */
    public double getUsagePercentage(int usedDays) {
        if (isUnlimited() || maxDaysPerYear == 0) {
            return 0.0;
        }
        
        return (double) usedDays / maxDaysPerYear * 100.0;
    }
    
    /**
     * Check if usage is approaching the limit (within 80%)
     * @param usedDays
     * @return 
     */
    public boolean isApproachingLimit(int usedDays) {
        if (isUnlimited()) {
            return false;
        }
        
        return getUsagePercentage(usedDays) >= 80.0;
    }
    
    /**
     * Check if the annual limit has been exceeded
     * @param usedDays
     * @return 
     */
    public boolean isLimitExceeded(int usedDays) {
        if (isUnlimited()) {
            return false;
        }
        
        return usedDays > maxDaysPerYear;
    }
    
    @Override
    public String toString() {
        return String.format("LeaveTypeModel{leaveTypeId=%d, leaveTypeName='%s', leaveDescription='%s', maxDaysPerYear=%s, createdAt=%s}", 
                           leaveTypeId, leaveTypeName, leaveDescription, maxDaysPerYear, createdAt);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LeaveTypeModel leaveType = (LeaveTypeModel) obj;
        return Objects.equals(leaveTypeId, leaveType.leaveTypeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(leaveTypeId);
    }
}
