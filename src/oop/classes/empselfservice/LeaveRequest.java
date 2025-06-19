package oop.classes.empselfservice;

import java.time.LocalDate;
import oop.classes.enums.LeaveName;
import oop.classes.enums.ApprovalStatus;

/**
 * Represents a leave request made by an employee.
 */
public class LeaveRequest {
    private static int counter = 1000; // Auto-incrementing request ID
    private final int requestId;
    private final String employeeId;
    private final LeaveName leaveType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String reason;
    private ApprovalStatus approvalStatus;

    /**
     * Constructor for creating a leave request.
     * @param employeeId Employee ID making the request.
     * @param leaveType Type of leave requested.
     * @param startDate Start date of leave.
     * @param endDate End date of leave.
     * @param reason Reason for leave.
     */
    public LeaveRequest(String employeeId, LeaveName leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        this.requestId = counter++;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.approvalStatus = ApprovalStatus.PENDING; // Default status
    }

    // Getters
    public int getRequestId() {
        return requestId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LeaveName getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    // Setter for approval status
    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
    
    public ApprovalStatus getStatus() {
    return approvalStatus;
}

public void setStatus(ApprovalStatus status) {
    this.approvalStatus = status;
}
    @Override
    public String toString() {
        return "LeaveRequest{" +
                "requestId=" + requestId +
                ", employeeId='" + employeeId + '\'' +
                ", leaveType=" + leaveType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", reason='" + reason + '\'' +
                ", approvalStatus=" + approvalStatus +
                '}';
    }
}