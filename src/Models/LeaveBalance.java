package Models;

import java.time.LocalDateTime;

/**
 * Represents an employee's leave balance record for a specific year and leave type.
 */
public class LeaveBalance {

    private Integer leaveBalanceId;
    private Integer employeeId;
    private Integer leaveTypeId;
    private Integer totalLeaveDays;
    private Integer usedLeaveDays;
    private Integer remainingLeaveDays;
    private Integer carryOverDays;
    private Integer balanceYear;
    private LocalDateTime lastUpdated;

    // Getters and Setters

    public Integer getLeaveBalanceId() {
        return leaveBalanceId;
    }

    public void setLeaveBalanceId(Integer leaveBalanceId) {
        this.leaveBalanceId = leaveBalanceId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Integer leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public Integer getTotalLeaveDays() {
        return totalLeaveDays;
    }

    public void setTotalLeaveDays(Integer totalLeaveDays) {
        this.totalLeaveDays = totalLeaveDays;
    }

    public Integer getUsedLeaveDays() {
        return usedLeaveDays;
    }

    public void setUsedLeaveDays(Integer usedLeaveDays) {
        this.usedLeaveDays = usedLeaveDays;
    }

    public Integer getRemainingLeaveDays() {
        return remainingLeaveDays;
    }

    public void setRemainingLeaveDays(Integer remainingLeaveDays) {
        this.remainingLeaveDays = remainingLeaveDays;
    }

    public Integer getCarryOverDays() {
        return carryOverDays;
    }

    public void setCarryOverDays(Integer carryOverDays) {
        this.carryOverDays = carryOverDays;
    }

    public Integer getBalanceYear() {
        return balanceYear;
    }

    public void setBalanceYear(Integer balanceYear) {
        this.balanceYear = balanceYear;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "LeaveBalance{" +
                "leaveBalanceId=" + leaveBalanceId +
                ", employeeId=" + employeeId +
                ", leaveTypeId=" + leaveTypeId +
                ", totalLeaveDays=" + totalLeaveDays +
                ", usedLeaveDays=" + usedLeaveDays +
                ", remainingLeaveDays=" + remainingLeaveDays +
                ", carryOverDays=" + carryOverDays +
                ", balanceYear=" + balanceYear +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
