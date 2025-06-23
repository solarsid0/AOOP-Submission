package oop.classes.actors;

import Models.*;
import Models.EmployeeModel.EmployeeStatus;
import Services.*;
import DAOs.*;
import DAOs.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Enhanced ImmediateSupervisor Role Class - handles team management and approvals
 * Focuses on supervising direct reports, approving leave/overtime requests,
 * and monitoring team performance
 */

public class ImmediateSupervisor 
{
    // Employee information
    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String userRole;
    private String department;
    
    // Service layer dependencies
    private final LeaveService leaveService;
    private final OvertimeService overtimeService;
    private final AttendanceService attendanceService;
    
    // DAO dependencies
    private final EmployeeDAO employeeDAO;
    private final LeaveRequestDAO leaveRequestDAO;
    private final OvertimeRequestDAO overtimeDAO;
    private final AttendanceDAO attendanceDAO;
    
    // Supervisor Role Permissions
    private static final String[] SUPERVISOR_PERMISSIONS = {
        "APPROVE_TEAM_LEAVE", "APPROVE_TEAM_OVERTIME", "VIEW_TEAM_ATTENDANCE", 
        "MANAGE_TEAM_PERFORMANCE", "VIEW_TEAM_REPORTS", "CORRECT_TEAM_ATTENDANCE",
        "APPROVE_TEAM_REQUESTS", "MONITOR_TEAM_PRODUCTIVITY"
    };

    /**
     * Constructor for ImmediateSupervisor role
     */
    public ImmediateSupervisor(int employeeId, String firstName, String lastName, 
            String email, String userRole, String department) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userRole = userRole;
        this.department = department != null ? department : determineDepartmentFromRole();
        
        // Initialize services
        DatabaseConnection dbConnection = new DatabaseConnection();
        this.leaveService = new LeaveService(dbConnection);
        this.overtimeService = new OvertimeService(dbConnection);
        this.attendanceService = new AttendanceService(dbConnection);
        
        // Initialize DAOs
        this.employeeDAO = new EmployeeDAO(dbConnection);
        this.leaveRequestDAO = new LeaveRequestDAO(dbConnection);
        this.overtimeDAO = new OvertimeRequestDAO(dbConnection);
        this.attendanceDAO = new AttendanceDAO(dbConnection);
        
        System.out.println("ImmediateSupervisor initialized: " + getFullName() + 
            " - Department: " + this.department);
    }

    /**
     * Simplified constructor without department
     */
    public ImmediateSupervisor(int employeeId, String firstName, String lastName, String email, String userRole) {
        this(employeeId, firstName, lastName, email, userRole, null);
    }

    // ================================
    // GETTER METHODS
    // ================================
    
    public int getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getUserRole() { return userRole; }
    public String getDepartment() { return department; }
    public String getFullName() { return firstName + " " + lastName; }

    public void setDepartment(String department) { this.department = department; }

    // ================================
    // TEAM MANAGEMENT OPERATIONS
    // ================================

    /**
     * Gets all employees supervised by this supervisor
     */
    public List<EmployeeModel> getTeamMembers() {
        if (!hasPermission("VIEW_TEAM_REPORTS")) {
            System.err.println("Supervisor: Insufficient permissions to view team members");
            return new ArrayList<>();
        }
        
        try {
            List<EmployeeModel> teamMembers = employeeDAO.getEmployeesBySupervisor(getEmployeeId());
            
            // If no direct reports found by supervisor ID, try department-based lookup
            if (teamMembers.isEmpty() && department != null) {
                teamMembers = employeeDAO.getEmployeesByDepartment(department);
                // Remove self from the list
                teamMembers = teamMembers.stream()
                    .filter(emp -> emp.getEmployeeId() != getEmployeeId())
                    .collect(Collectors.toList());
            }
            
            return teamMembers;
        } catch (Exception e) {
            System.err.println("Error getting team members: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets team performance summary
     */
    public SupervisorResult getTeamPerformanceSummary(YearMonth yearMonth) {
        SupervisorResult result = new SupervisorResult();
        
        try {
            if (!hasPermission("MANAGE_TEAM_PERFORMANCE")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to view team performance");
                return result;
            }

            List<EmployeeModel> teamMembers = getTeamMembers();
            if (teamMembers.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No team members found");
                return result;
            }

            TeamPerformanceSummary summary = new TeamPerformanceSummary();
            summary.setSupervisorId(getEmployeeId());
            summary.setSupervisorName(getFullName());
            summary.setDepartment(department);
            summary.setReportMonth(yearMonth);
            summary.setTotalTeamMembers(teamMembers.size());

            int activeMembers = 0;
            BigDecimal totalAttendanceRate = BigDecimal.ZERO;
            int perfectAttendanceCount = 0;

            for (EmployeeModel member : teamMembers) {
                if (member.getStatus() == EmployeeStatus.REGULAR || 
                    member.getStatus() == EmployeeStatus.PROBATIONARY) {
                    activeMembers++;
                    
                    // Get attendance summary for each team member
                    AttendanceService.AttendanceSummary attendanceSummary = 
                        attendanceService.getMonthlyAttendanceSummary(member.getEmployeeId(), yearMonth);
                    
                    if (attendanceSummary != null) {
                        totalAttendanceRate = totalAttendanceRate.add(attendanceSummary.getAttendanceRate());
                        
                        if (attendanceSummary.getLateInstances() == 0 && 
                            attendanceSummary.getCompleteDays() > 0) {
                            perfectAttendanceCount++;
                        }
                    }
                }
            }

            summary.setActiveTeamMembers(activeMembers);
            
            if (activeMembers > 0) {
                BigDecimal avgAttendanceRate = totalAttendanceRate.divide(
                    new BigDecimal(activeMembers), 2, BigDecimal.ROUND_HALF_UP);
                summary.setAverageAttendanceRate(avgAttendanceRate);
            }
            
            summary.setPerfectAttendanceCount(perfectAttendanceCount);

            result.setSuccess(true);
            result.setMessage("Team performance summary generated for " + yearMonth);
            result.setTeamPerformance(summary);
            
            logSupervisorActivity("TEAM_PERFORMANCE_REVIEWED", 
                "Generated team performance summary for " + yearMonth);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error generating team performance summary: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // LEAVE REQUEST MANAGEMENT
    // ================================

    /**
     * Gets pending leave requests for team members
     */
    public List<LeaveRequestModel> getPendingTeamLeaveRequests() {
        if (!hasPermission("APPROVE_TEAM_LEAVE")) {
            System.err.println("Supervisor: Insufficient permissions to view team leave requests");
            return new ArrayList<>();
        }
        
        try {
            List<EmployeeModel> teamMembers = getTeamMembers();
            List<LeaveRequestModel> pendingRequests = new ArrayList<>();
            
            for (EmployeeModel member : teamMembers) {
                List<LeaveRequestModel> memberRequests = leaveService.getEmployeeLeaveRequests(member.getEmployeeId());
                pendingRequests.addAll(memberRequests.stream()
                    .filter(req -> req.getApprovalStatus().toString().equals("PENDING"))
                    .collect(Collectors.toList()));
            }
            
            return pendingRequests;
        } catch (Exception e) {
            System.err.println("Error getting pending team leave requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Approves a team member's leave request
     */
    public SupervisorResult approveTeamLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        SupervisorResult result = new SupervisorResult();
        
        try {
            if (!hasPermission("APPROVE_TEAM_LEAVE")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to approve leave requests");
                return result;
            }

            // Verify this leave request is for a team member
            if (!isTeamMemberLeaveRequest(leaveRequestId)) {
                result.setSuccess(false);
                result.setMessage("Leave request does not belong to your team");
                return result;
            }

            LeaveService.LeaveApprovalResult approvalResult = 
                leaveService.approveLeaveRequest(leaveRequestId, getEmployeeId(), supervisorNotes);
            
            result.setSuccess(approvalResult.isSuccess());
            result.setMessage(approvalResult.getMessage());
            
            if (approvalResult.isSuccess()) {
                logSupervisorActivity("LEAVE_APPROVED", 
                    "Approved leave request: " + leaveRequestId);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error approving leave request: " + e.getMessage());
        }

        return result;
    }

    /**
     * Rejects a team member's leave request
     */
    public SupervisorResult rejectTeamLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        SupervisorResult result = new SupervisorResult();
        
        try {
            if (!hasPermission("APPROVE_TEAM_LEAVE")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to reject leave requests");
                return result;
            }

            if (supervisorNotes == null || supervisorNotes.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Supervisor notes are required when rejecting leave requests");
                return result;
            }

            // Verify this leave request is for a team member
            if (!isTeamMemberLeaveRequest(leaveRequestId)) {
                result.setSuccess(false);
                result.setMessage("Leave request does not belong to your team");
                return result;
            }

            LeaveService.LeaveApprovalResult approvalResult = 
                leaveService.rejectLeaveRequest(leaveRequestId, getEmployeeId(), supervisorNotes);
            
            result.setSuccess(approvalResult.isSuccess());
            result.setMessage(approvalResult.getMessage());
            
            if (approvalResult.isSuccess()) {
                logSupervisorActivity("LEAVE_REJECTED", 
                    "Rejected leave request: " + leaveRequestId + " - Reason: " + supervisorNotes);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error rejecting leave request: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // OVERTIME REQUEST MANAGEMENT
    // ================================

    /**
     * Gets pending overtime requests for team members
     */
    public List<OvertimeRequestModel> getPendingTeamOvertimeRequests() {
        if (!hasPermission("APPROVE_TEAM_OVERTIME")) {
            System.err.println("Supervisor: Insufficient permissions to view team overtime requests");
            return new ArrayList<>();
        }
        
        try {
            List<EmployeeModel> teamMembers = getTeamMembers();
            List<OvertimeRequestModel> pendingRequests = new ArrayList<>();
            
            for (EmployeeModel member : teamMembers) {
                List<OvertimeRequestModel> memberRequests = overtimeService.getEmployeeOvertimeRequests(member.getEmployeeId());
                pendingRequests.addAll(memberRequests.stream()
                    .filter(req -> req.getApprovalStatus().toString().equals("PENDING"))
                    .collect(Collectors.toList()));
            }
            
            return pendingRequests;
        } catch (Exception e) {
            System.err.println("Error getting pending team overtime requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Approves a team member's overtime request
     */
    public SupervisorResult approveTeamOvertimeRequest(Integer overtimeRequestId, String supervisorNotes) {
        SupervisorResult result = new SupervisorResult();
        
        try {
            if (!hasPermission("APPROVE_TEAM_OVERTIME")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to approve overtime requests");
                return result;
            }

            // Verify this overtime request is for a team member
            if (!isTeamMemberOvertimeRequest(overtimeRequestId)) {
                result.setSuccess(false);
                result.setMessage("Overtime request does not belong to your team");
                return result;
            }

            OvertimeService.OvertimeApprovalResult approvalResult = 
                overtimeService.approveOvertimeRequest(overtimeRequestId, getEmployeeId(), supervisorNotes);
            
            result.setSuccess(approvalResult.isSuccess());
            result.setMessage(approvalResult.getMessage());
            
            if (approvalResult.isSuccess()) {
                logSupervisorActivity("OVERTIME_APPROVED", 
                    "Approved overtime request: " + overtimeRequestId);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error approving overtime request: " + e.getMessage());
        }

        return result;
    }

    /**
     * Rejects a team member's overtime request
     */
    public SupervisorResult rejectTeamOvertimeRequest(Integer overtimeRequestId, String supervisorNotes) {
        SupervisorResult result = new SupervisorResult();
        
        try {
            if (!hasPermission("APPROVE_TEAM_OVERTIME")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to reject overtime requests");
                return result;
            }

            if (supervisorNotes == null || supervisorNotes.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Supervisor notes are required when rejecting overtime requests");
                return result;
            }

            // Verify this overtime request is for a team member
            if (!isTeamMemberOvertimeRequest(overtimeRequestId)) {
                result.setSuccess(false);
                result.setMessage("Overtime request does not belong to your team");
                return result;
            }

            OvertimeService.OvertimeApprovalResult approvalResult = 
                overtimeService.rejectOvertimeRequest(overtimeRequestId, getEmployeeId(), supervisorNotes);
            
            result.setSuccess(approvalResult.isSuccess());
            result.setMessage(approvalResult.getMessage());
            
            if (approvalResult.isSuccess()) {
                logSupervisorActivity("OVERTIME_REJECTED", 
                    "Rejected overtime request: " + overtimeRequestId + " - Reason: " + supervisorNotes);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error rejecting overtime request: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // ATTENDANCE MANAGEMENT
    // ================================

    /**
     * Gets team attendance for a specific date
     */
    public List<AttendanceService.DailyAttendanceRecord> getTeamAttendance(LocalDate date) {
        if (!hasPermission("VIEW_TEAM_ATTENDANCE")) {
            System.err.println("Supervisor: Insufficient permissions to view team attendance");
            return new ArrayList<>();
        }
        
        try {
            List<AttendanceService.DailyAttendanceRecord> allAttendance = attendanceService.getDailyAttendanceReport(date);
            List<EmployeeModel> teamMembers = getTeamMembers();
            
            // Filter to only show team members' attendance
            return allAttendance.stream()
                .filter(record -> teamMembers.stream()
                    .anyMatch(member -> member.getEmployeeId().equals(record.getEmployeeId())))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Error getting team attendance: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets team members with attendance issues
     */
    public SupervisorResult getTeamAttendanceIssues(YearMonth yearMonth) {
        SupervisorResult result = new SupervisorResult();
        
        try {
            if (!hasPermission("VIEW_TEAM_ATTENDANCE")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to view team attendance");
                return result;
            }

            List<EmployeeModel> teamMembers = getTeamMembers();
            List<AttendanceIssue> attendanceIssues = new ArrayList<>();
            
            for (EmployeeModel member : teamMembers) {
                AttendanceService.AttendanceSummary summary = attendanceService.getMonthlyAttendanceSummary(
                    member.getEmployeeId(), yearMonth);
                
                if (summary != null) {
                    // Check for attendance issues
                    if (summary.getLateInstances() > 3) {
                        attendanceIssues.add(new AttendanceIssue(
                            member.getEmployeeId(), 
                            member.getFullName(), 
                            "Excessive tardiness: " + summary.getLateInstances() + " instances",
                            "TARDINESS"
                        ));
                    }
                    
                    if (summary.getAttendanceRate().compareTo(new BigDecimal("85")) < 0) {
                        attendanceIssues.add(new AttendanceIssue(
                            member.getEmployeeId(), 
                            member.getFullName(), 
                            "Low attendance rate: " + summary.getAttendanceRate() + "%",
                            "LOW_ATTENDANCE"
                        ));
                    }
                }
            }

            result.setSuccess(true);
            result.setMessage("Found " + attendanceIssues.size() + " attendance issues for " + yearMonth);
            result.setAttendanceIssues(attendanceIssues);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error checking team attendance issues: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Determines department from role if not provided
     */
    private String determineDepartmentFromRole() {
        // Default department assignment based on common patterns
        return "General"; // Can be customized based on business logic
    }

    /**
     * Checks if a leave request belongs to a team member
     */
    private boolean isTeamMemberLeaveRequest(Integer leaveRequestId) {
        try {
            LeaveRequestModel leaveRequest = leaveRequestDAO.findById(leaveRequestId);
            if (leaveRequest == null) return false;
            
            List<EmployeeModel> teamMembers = getTeamMembers();
            return teamMembers.stream()
                .anyMatch(member -> member.getEmployeeId().equals(leaveRequest.getEmployeeId()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if an overtime request belongs to a team member
     */
    private boolean isTeamMemberOvertimeRequest(Integer overtimeRequestId) {
        try {
            OvertimeRequestModel overtimeRequest = overtimeDAO.findById(overtimeRequestId);
            if (overtimeRequest == null) return false;
            
            List<EmployeeModel> teamMembers = getTeamMembers();
            return teamMembers.stream()
                .anyMatch(member -> member.getEmployeeId().equals(overtimeRequest.getEmployeeId()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if supervisor has specific permission
     */
    private boolean hasPermission(String permission) {
        for (String supervisorPermission : SUPERVISOR_PERMISSIONS) {
            if (supervisorPermission.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all supervisor permissions
     */
    public String[] getSupervisorPermissions() {
        return SUPERVISOR_PERMISSIONS.clone();
    }

    /**
     * Logs supervisor activities for audit purposes
     */
    private void logSupervisorActivity(String action, String details) {
        try {
            String logMessage = String.format("[SUPERVISOR AUDIT] %s - %s: %s (Performed by: %s - ID: %d)",
                LocalDate.now(), action, details, getFullName(), getEmployeeId());
            System.out.println(logMessage);
            
        } catch (Exception e) {
            System.err.println("Error logging supervisor activity: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "ImmediateSupervisor{" +
                "employeeId=" + getEmployeeId() +
                ", name='" + getFullName() + '\'' +
                ", department='" + getDepartment() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", permissions=" + java.util.Arrays.toString(SUPERVISOR_PERMISSIONS) +
                '}';
    }

    // ================================
    // INNER CLASSES
    // ================================

    /**
     * Result class for supervisor operations
     */
    public static class SupervisorResult {
        private boolean success = false;
        private String message = "";
        private TeamPerformanceSummary teamPerformance;
        private List<AttendanceIssue> attendanceIssues = new ArrayList<>();

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public TeamPerformanceSummary getTeamPerformance() { return teamPerformance; }
        public void setTeamPerformance(TeamPerformanceSummary teamPerformance) { this.teamPerformance = teamPerformance; }
        
        public List<AttendanceIssue> getAttendanceIssues() { return attendanceIssues; }
        public void setAttendanceIssues(List<AttendanceIssue> attendanceIssues) { this.attendanceIssues = attendanceIssues; }

        @Override
        public String toString() {
            return "SupervisorResult{success=" + success + ", message='" + message + "'}";
        }
    }

    /**
     * Team performance summary
     */
    public static class TeamPerformanceSummary {
        private Integer supervisorId;
        private String supervisorName;
        private String department;
        private YearMonth reportMonth;
        private int totalTeamMembers = 0;
        private int activeTeamMembers = 0;
        private BigDecimal averageAttendanceRate = BigDecimal.ZERO;
        private int perfectAttendanceCount = 0;

        // Getters and setters
        public Integer getSupervisorId() { return supervisorId; }
        public void setSupervisorId(Integer supervisorId) { this.supervisorId = supervisorId; }
        
        public String getSupervisorName() { return supervisorName; }
        public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        
        public YearMonth getReportMonth() { return reportMonth; }
        public void setReportMonth(YearMonth reportMonth) { this.reportMonth = reportMonth; }
        
        public int getTotalTeamMembers() { return totalTeamMembers; }
        public void setTotalTeamMembers(int totalTeamMembers) { this.totalTeamMembers = totalTeamMembers; }
        
        public int getActiveTeamMembers() { return activeTeamMembers; }
        public void setActiveTeamMembers(int activeTeamMembers) { this.activeTeamMembers = activeTeamMembers; }
        
        public BigDecimal getAverageAttendanceRate() { return averageAttendanceRate; }
        public void setAverageAttendanceRate(BigDecimal averageAttendanceRate) { this.averageAttendanceRate = averageAttendanceRate; }
        
        public int getPerfectAttendanceCount() { return perfectAttendanceCount; }
        public void setPerfectAttendanceCount(int perfectAttendanceCount) { this.perfectAttendanceCount = perfectAttendanceCount; }
    }

    /**
     * Attendance issue tracking
     */
    public static class AttendanceIssue {
        private Integer employeeId;
        private String employeeName;
        private String issueDescription;
        private String issueType;

        public AttendanceIssue(Integer employeeId, String employeeName, String issueDescription, String issueType) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.issueDescription = issueDescription;
            this.issueType = issueType;
        }

        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public String getIssueDescription() { return issueDescription; }
        public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
        
        public String getIssueType() { return issueType; }
        public void setIssueType(String issueType) { this.issueType = issueType; }

        @Override
        public String toString() {
            return "AttendanceIssue{" +
                   "employeeId=" + employeeId +
                   ", employeeName='" + employeeName + '\'' +
                   ", issueType='" + issueType + '\'' +
                   ", description='" + issueDescription + '\'' +
                   '}';
        }
    }
}