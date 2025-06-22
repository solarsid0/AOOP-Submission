package oop.classes.calculations;

import java.util.HashMap;
import java.util.Map;
import oop.classes.enums.LeaveName;

/**
 * Manages leave balance calculations for employees.
 * Each employee has a leave balance for different leave types.
 */
public class LeaveBalance {
    private final Map<LeaveName, Integer> leaveBalances;

    /**
     * Initializes leave balances with default values.
     */
    public LeaveBalance() {
        leaveBalances = new HashMap<>();
        for (LeaveName leaveType : LeaveName.values()) {
            leaveBalances.put(leaveType, 5); // Default 5 days per leave type
        }
    }

    /**
     * Retrieves the available leave balance for a specific leave type.
     * @param leaveType The type of leave.
     * @return The remaining leave balance.
     */
    public int getLeaveBalance(LeaveName leaveType) {
        return leaveBalances.getOrDefault(leaveType, 0);
    }

    /**
     * Deducts leave from the balance, ensuring it does not go negative.
     * @param leaveType The type of leave.
     * @param days The number of days to deduct.
     * @return True if leave was successfully deducted, false if insufficient balance.
     */
    public boolean deductLeave(LeaveName leaveType, int days) {
        int currentBalance = leaveBalances.getOrDefault(leaveType, 0);
        if (currentBalance < days) {
            return false; // Deny deduction if insufficient balance
        }
        leaveBalances.put(leaveType, currentBalance - days);
        return true; // Successfully deducted
    }

    /**
     * Adds leave back to the balance (e.g., when leave is canceled).
     * @param leaveType The type of leave.
     * @param days The number of days to add.
     */
    public void addLeave(LeaveName leaveType, int days) {
        leaveBalances.put(leaveType, getLeaveBalance(leaveType) + days);
    }
}