
package Services;

import DAOs.*;
import Models.*;
import Models.OvertimeRequestModel.ApprovalStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

/**
 * OvertimeService - Business logic for overtime management
 * Handles overtime requests, approvals, calculations, and overtime-related reporting
 * @author User
 */
public class OvertimeService {

    // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final OvertimeRequestDAO overtimeDAO;
    private final EmployeeDAO employeeDAO;
    private final AttendanceDAO attendanceDAO;

    // Business Rules Configuration
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5"); // Time and a half
    private static final BigDecimal NIGHT_SHIFT_MULTIPLIER = new BigDecimal("1.10"); // 10% night differential
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.30"); // 30% weekend premium
    private static final int MAX_DAILY_OVERTIME_HOURS = 4; // Maximum 4 hours overtime per day
    private static final int MAX_WEEKLY_OVERTIME_HOURS = 20; // Maximum 20 hours overtime per week
    private static final int MIN_OVERTIME_MINUTES = 30; // Minimum 30 minutes to qualify for overtime

    /**
     * Constructor - initializes required DAOs
     */
    public OvertimeService() {
        this.databaseConnection = new DatabaseConnection();
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
    }

    /**
     * Constructor with custom database connection
     */
    public OvertimeService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
    }

    // ================================
    // OVERTIME REQUEST OPERATIONS
    // ================================

    /**
     * Submits a new overtime request
     * @param employeeId Employee requesting overtime
     * @param overtimeStart Start date/time of overtime
     * @param overtimeEnd End date/time of overtime
     * @param reason Reason for overtime
     * @return OvertimeRequestResult with success status and details
     */
    public OvertimeRequestResult submitOvertimeRequest(Integer employeeId, LocalDateTime overtimeStart,
                                                       LocalDateTime overtimeEnd, String reason) {
        OvertimeRequestResult result = new OvertimeRequestResult();

        try {
            // Validate employee exists
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            // Validate overtime request
            OvertimeValidationResult validation = validateOvertimeRequest(employeeId, overtimeStart, overtimeEnd);
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setMessage(validation.getErrorMessage());
                return result;
            }

            // Create overtime request
            OvertimeRequestModel overtimeRequest = new OvertimeRequestModel(employeeId, overtimeStart, overtimeEnd, reason);
            boolean success = overtimeDAO.save(overtimeRequest);

            if (success) {
                result.setSuccess(true);
                result.setOvertimeRequestId(overtimeRequest.getOvertimeRequestId());
                result.setMessage("Overtime request submitted successfully for " + employee.getFullName());
                result.setOvertimeHours(overtimeRequest.getOvertimeHours());
                result.setEstimatedPay(calculateOvertimePay(overtimeRequest, employee.getHourlyRate()));

                System.out.println("✅ Overtime request submitted: " + employee.getFullName() +
                        " (" + overtimeStart + " to " + overtimeEnd + ", " +
                        overtimeRequest.getOvertimeHours() + " hours)");
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to submit overtime request for " + employee.getFullName());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error submitting overtime request: " + e.getMessage());
            System.err.println("❌ Error submitting overtime request for employee " + employeeId + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Approves an overtime request
     * @param overtimeRequestId Overtime request ID to approve
     * @param supervisorId ID of supervisor approving
     * @param supervisorNotes Optional notes from supervisor
     * @return OvertimeApprovalResult with success status
     */
    public OvertimeApprovalResult approveOvertimeRequest(Integer overtimeRequestId, Integer supervisorId, String supervisorNotes) {
        OvertimeApprovalResult result = new OvertimeApprovalResult();

        try {
            // Get overtime request
            OvertimeRequestModel overtimeRequest = overtimeDAO.findById(overtimeRequestId);
            if (overtimeRequest == null) {
                result.setSuccess(false);
                result.setMessage("Overtime request not found: " + overtimeRequestId);
                return result;
            }

            // Validate supervisor exists
            EmployeeModel supervisor = employeeDAO.findById(supervisorId);
            if (supervisor == null) {
                result.setSuccess(false);
                result.setMessage("Supervisor not found: " + supervisorId);
                return result;
            }

            // Check if already processed
            if (overtimeRequest.isProcessed()) {
                result.setSuccess(false);
                result.setMessage("Overtime request has already been " + overtimeRequest.getApprovalStatus().getValue().toLowerCase());
                return result;
            }

            // Approve the request
            boolean success = overtimeDAO.approveOvertime(overtimeRequestId, supervisorNotes);

            if (success) {
                result.setSuccess(true);
                result.setMessage("Overtime request approved successfully");

                // Calculate final pay
                EmployeeModel employee = employeeDAO.findById(overtimeRequest.getEmployeeId());
                if (employee != null) {
                    BigDecimal overtimePay = calculateOvertimePay(overtimeRequest, employee.getHourlyRate());
                    result.setOvertimePay(overtimePay);

                    System.out.println("✅ Overtime request approved: " + employee.getFullName() +
                            " by " + supervisor.getFullName() + " (Pay: ₱" + overtimePay + ")");
                }
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to approve overtime request");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error approving overtime request: " + e.getMessage());
            System.err.println("❌ Error approving overtime request " + overtimeRequestId + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Rejects an overtime request
     * @param overtimeRequestId Overtime request ID to reject
     * @param supervisorId ID of supervisor rejecting
     * @param supervisorNotes Required notes explaining rejection
     * @return OvertimeApprovalResult with success status
     */
    public OvertimeApprovalResult rejectOvertimeRequest(Integer overtimeRequestId, Integer supervisorId, String supervisorNotes) {
        OvertimeApprovalResult result = new OvertimeApprovalResult();

        try {
            // Validate supervisor notes are provided for rejection
            if (supervisorNotes == null || supervisorNotes.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Supervisor notes are required when rejecting an overtime request");
                return result;
            }

            // Get overtime request
            OvertimeRequestModel overtimeRequest = overtimeDAO.findById(overtimeRequestId);
            if (overtimeRequest == null) {
                result.setSuccess(false);
                result.setMessage("Overtime request not found: " + overtimeRequestId);
                return result;
            }

            // Check if already processed
            if (overtimeRequest.isProcessed()) {
                result.setSuccess(false);
                result.setMessage("Overtime request has already been " + overtimeRequest.getApprovalStatus().getValue().toLowerCase());
                return result;
            }

            // Reject the request
            boolean success = overtimeDAO.rejectOvertime(overtimeRequestId, supervisorNotes);

            if (success) {
                result.setSuccess(true);
                result.setMessage("Overtime request rejected successfully");

                EmployeeModel employee = employeeDAO.findById(overtimeRequest.getEmployeeId());
                EmployeeModel supervisor = employeeDAO.findById(supervisorId);

                System.out.println("❌ Overtime request rejected: " +
                        (employee != null ? employee.getFullName() : "Employee " + overtimeRequest.getEmployeeId()) +
                        " by " + (supervisor != null ? supervisor.getFullName() : "Supervisor " + supervisorId));
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to reject overtime request");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error rejecting overtime request: " + e.getMessage());
            System.err.println("❌ Error rejecting overtime request " + overtimeRequestId + ": " + e.getMessage());
        }

        return result;
    }

    // ================================
    // OVERTIME CALCULATIONS
    // ================================

    /**
     * Calculates overtime pay for a specific overtime request
     * @param overtimeRequest The overtime request
     * @param hourlyRate Employee's hourly rate
     * @return Calculated overtime pay
     */
    public BigDecimal calculateOvertimePay(OvertimeRequestModel overtimeRequest, BigDecimal hourlyRate) {
        if (hourlyRate == null || overtimeRequest.getOvertimeHours().equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        BigDecimal overtimeHours = overtimeRequest.getOvertimeHours();
        BigDecimal multiplier = OVERTIME_MULTIPLIER; // Start with base overtime rate

        // Apply night shift differential if applicable
        if (overtimeRequest.isNightShiftOvertime()) {
            multiplier = multiplier.add(NIGHT_SHIFT_MULTIPLIER.subtract(BigDecimal.ONE)); // Add night differential
        }

        // Apply weekend premium if applicable
        if (overtimeRequest.isWeekendOvertime()) {
            multiplier = multiplier.add(WEEKEND_MULTIPLIER.subtract(BigDecimal.ONE)); // Add weekend premium
        }

        return overtimeHours.multiply(hourlyRate).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates total overtime hours for an employee in a specific period
     * @param employeeId Employee ID
     * @param startDate Start date of period
     * @param endDate End date of period
     * @return Total approved overtime hours
     */
    public BigDecimal calculateTotalOvertimeHours(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<OvertimeRequestModel> overtimeRequests = overtimeDAO.findByDateRange(startDateTime, endDateTime);

            return overtimeRequests.stream()
                    .filter(req -> req.getEmployeeId().equals(employeeId))
                    .filter(OvertimeRequestModel::isApproved)
                    .map(OvertimeRequestModel::getOvertimeHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } catch (Exception e) {
            System.err.println("Error calculating total overtime hours: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculates total overtime pay for an employee in a specific month
     * @param employeeId Employee ID
     * @param yearMonth Year and month
     * @return Total overtime pay for the month
     */
    public BigDecimal calculateMonthlyOvertimePay(Integer employeeId, YearMonth yearMonth) {
        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null || employee.getHourlyRate() == null) {
                return BigDecimal.ZERO;
            }

            return overtimeDAO.getTotalOvertimePay(employeeId, yearMonth.getYear(),
                    yearMonth.getMonthValue(), OVERTIME_MULTIPLIER);

        } catch (Exception e) {
            System.err.println("Error calculating monthly overtime pay: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ================================
    // VALIDATION AND BUSINESS RULES
    // ================================

    /**
     * Validates an overtime request before submission
     */
    private OvertimeValidationResult validateOvertimeRequest(Integer employeeId, LocalDateTime overtimeStart, LocalDateTime overtimeEnd) {
        OvertimeValidationResult result = new OvertimeValidationResult();

        // Validate times
        if (overtimeStart == null || overtimeEnd == null) {
            result.setValid(false);
            result.setErrorMessage("Start time and end time are required");
            return result;
        }

        if (overtimeEnd.isBefore(overtimeStart) || overtimeEnd.isEqual(overtimeStart)) {
            result.setValid(false);
            result.setErrorMessage("End time must be after start time");
            return result;
        }

        // Check minimum overtime duration
        Duration duration = Duration.between(overtimeStart, overtimeEnd);
        if (duration.toMinutes() < MIN_OVERTIME_MINUTES) {
            result.setValid(false);
            result.setErrorMessage("Minimum overtime duration is " + MIN_OVERTIME_MINUTES + " minutes");
            return result;
        }

        // Check maximum daily overtime
        BigDecimal requestedHours = new BigDecimal(duration.toMinutes()).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
        if (requestedHours.compareTo(new BigDecimal(MAX_DAILY_OVERTIME_HOURS)) > 0) {
            result.setValid(false);
            result.setErrorMessage("Maximum daily overtime is " + MAX_DAILY_OVERTIME_HOURS + " hours");
            return result;
        }

        // Check if employee has regular attendance for the day
        LocalDate overtimeDate = overtimeStart.toLocalDate();
        AttendanceModel attendance = attendanceDAO.findByEmployeeAndDate(employeeId, overtimeDate);
        if (attendance == null || !attendance.isComplete()) {
            result.setValid(false);
            result.setErrorMessage("Employee must have complete regular attendance before requesting overtime");
            return result;
        }

        // Check weekly overtime limits
        BigDecimal weeklyOvertimeHours = calculateWeeklyOvertimeHours(employeeId, overtimeDate);
        if (weeklyOvertimeHours.add(requestedHours).compareTo(new BigDecimal(MAX_WEEKLY_OVERTIME_HOURS)) > 0) {
            result.setValid(false);
            result.setErrorMessage("Weekly overtime limit of " + MAX_WEEKLY_OVERTIME_HOURS + " hours would be exceeded");
            return result;
        }

        // Check for overlapping overtime requests
        List<OvertimeRequestModel> existingRequests = overtimeDAO.findByEmployee(employeeId);
        for (OvertimeRequestModel existing : existingRequests) {
            if (existing.getApprovalStatus() == ApprovalStatus.APPROVED || existing.getApprovalStatus() == ApprovalStatus.PENDING) {
                if (timesOverlap(overtimeStart, overtimeEnd, existing.getOvertimeStart(), existing.getOvertimeEnd())) {
                    result.setValid(false);
                    result.setErrorMessage("Overtime request overlaps with existing request");
                    return result;
                }
            }
        }

        result.setValid(true);
        return result;
    }

    /**
     * Checks if two time ranges overlap
     */
    private boolean timesOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }

    /**
     * Calculates weekly overtime hours for an employee
     */
    private BigDecimal calculateWeeklyOvertimeHours(Integer employeeId, LocalDate date) {
        // Get start of week (Monday)
        LocalDate startOfWeek = date.minusDays(date.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        return calculateTotalOvertimeHours(employeeId, startOfWeek, endOfWeek);
    }

    // ================================
    // REPORTING AND QUERIES
    // ================================

    /**
     * Gets pending overtime requests for approval
     */
    public List<OvertimeRequestModel> getPendingOvertimeRequests() {
        return overtimeDAO.findPendingOvertimeRequests();
    }

    /**
     * Gets overtime requests for a specific employee
     */
    public List<OvertimeRequestModel> getEmployeeOvertimeRequests(Integer employeeId) {
        return overtimeDAO.findByEmployee(employeeId);
    }

    /**
     * Gets overtime summary for an employee in a specific month
     */
    public OvertimeSummary getEmployeeOvertimeSummary(Integer employeeId, YearMonth yearMonth) {
        OvertimeSummary summary = new OvertimeSummary();
        summary.setEmployeeId(employeeId);
        summary.setYearMonth(yearMonth);

        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee != null) {
                summary.setEmployeeName(employee.getFullName());
                summary.setHourlyRate(employee.getHourlyRate());
            }

            // Get overtime requests for the month
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            List<OvertimeRequestModel> monthlyRequests = overtimeDAO.findByDateRange(startOfMonth, endOfMonth)
                    .stream()
                    .filter(req -> req.getEmployeeId().equals(employeeId))
                    .toList();

            summary.setOvertimeRequests(monthlyRequests);

            // Calculate totals
            BigDecimal totalHours = monthlyRequests.stream()
                    .filter(OvertimeRequestModel::isApproved)
                    .map(OvertimeRequestModel::getOvertimeHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            summary.setTotalOvertimeHours(totalHours);
            summary.setTotalOvertimePay(calculateMonthlyOvertimePay(employeeId, yearMonth));

            long approvedCount = monthlyRequests.stream().filter(OvertimeRequestModel::isApproved).count();
            long pendingCount = monthlyRequests.stream().filter(OvertimeRequestModel::isPending).count();
            long rejectedCount = monthlyRequests.stream().filter(OvertimeRequestModel::isRejected).count();

            summary.setApprovedCount((int)approvedCount);
            summary.setPendingCount((int)pendingCount);
            summary.setRejectedCount((int)rejectedCount);

        } catch (Exception e) {
            System.err.println("Error generating overtime summary: " + e.getMessage());
        }

        return summary;
    }

    /**
     * Gets employees with most overtime hours in a period
     */
    public List<OvertimeRanking> getTopOvertimeEmployees(LocalDate startDate, LocalDate endDate, int limit) {
        List<OvertimeRanking> rankings = new ArrayList<>();

        try {
            List<EmployeeModel> activeEmployees = employeeDAO.getActiveEmployees();

            for (EmployeeModel employee : activeEmployees) {
                BigDecimal totalHours = calculateTotalOvertimeHours(employee.getEmployeeId(), startDate, endDate);

                if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
                    OvertimeRanking ranking = new OvertimeRanking();
                    ranking.setEmployeeId(employee.getEmployeeId());
                    ranking.setEmployeeName(employee.getFullName());
                    ranking.setTotalOvertimeHours(totalHours);

                    if (employee.getHourlyRate() != null) {
                        BigDecimal totalPay = totalHours.multiply(employee.getHourlyRate()).multiply(OVERTIME_MULTIPLIER);
                        ranking.setTotalOvertimePay(totalPay);
                    }

                    rankings.add(ranking);
                }
            }

            // Sort by overtime hours (descending) and limit results
            rankings.sort((a, b) -> b.getTotalOvertimeHours().compareTo(a.getTotalOvertimeHours()));

            if (limit > 0 && rankings.size() > limit) {
                rankings = rankings.subList(0, limit);
            }

        } catch (Exception e) {
            System.err.println("Error getting top overtime employees: " + e.getMessage());
        }

        return rankings;
    }

    // ================================
    // INNER CLASSES
    // ================================

    /**
     * Result of overtime request operation
     */
    public static class OvertimeRequestResult {
        private boolean success = false;
        private String message = "";
        private Integer overtimeRequestId;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        private BigDecimal estimatedPay = BigDecimal.ZERO;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getOvertimeRequestId() { return overtimeRequestId; }
        public void setOvertimeRequestId(Integer overtimeRequestId) { this.overtimeRequestId = overtimeRequestId; }
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
        public BigDecimal getEstimatedPay() { return estimatedPay; }
        public void setEstimatedPay(BigDecimal estimatedPay) { this.estimatedPay = estimatedPay; }
    }

    /**
     * Result of overtime approval/rejection operation
     */
    public static class OvertimeApprovalResult {
        private boolean success = false;
        private String message = "";
        private BigDecimal overtimePay = BigDecimal.ZERO;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public BigDecimal getOvertimePay() { return overtimePay; }
        public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
    }

    /**
     * Result of overtime request validation
     */
    public static class OvertimeValidationResult {
        private boolean valid = false;
        private String errorMessage = "";

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Overtime summary for reporting
     */
    public static class OvertimeSummary {
        private Integer employeeId;
        private String employeeName;
        private YearMonth yearMonth;
        private BigDecimal hourlyRate = BigDecimal.ZERO;
        private List<OvertimeRequestModel> overtimeRequests = new ArrayList<>();
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;
        private int approvedCount = 0;
        private int pendingCount = 0;
        private int rejectedCount = 0;

        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public BigDecimal getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
        public List<OvertimeRequestModel> getOvertimeRequests() { return overtimeRequests; }
        public void setOvertimeRequests(List<OvertimeRequestModel> overtimeRequests) { this.overtimeRequests = overtimeRequests; }
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        public int getApprovedCount() { return approvedCount; }
        public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        public int getRejectedCount() { return rejectedCount; }
        public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

        public int getTotalRequests() {
            return approvedCount + pendingCount + rejectedCount;
        }

        public BigDecimal getAverageHoursPerRequest() {
            if (getTotalRequests() == 0) return BigDecimal.ZERO;
            return totalOvertimeHours.divide(new BigDecimal(getTotalRequests()), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Overtime ranking for top performers
     */
    public static class OvertimeRanking {
        private Integer employeeId;
        private String employeeName;
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;

        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }

        @Override
        public String toString() {
            return String.format("%s: %.2f hours, ₱%.2f", employeeName, totalOvertimeHours, totalOvertimePay);
        }
    }
}