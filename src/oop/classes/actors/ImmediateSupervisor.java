package oop.classes.actors;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import oop.classes.empselfservice.LeaveRequest;
import oop.classes.enums.ApprovalStatus;
import oop.classes.management.AttendanceTracking;
import oop.classes.management.LeaveRequestManagement;

public class ImmediateSupervisor extends Employee implements AttendanceTracking, LeaveRequestManagement {
    private Map<Integer, LeaveRequest> leaveRequests = new HashMap<>();
    private String department; // Add department field
    
    // Constructor for login
    public ImmediateSupervisor(int employeeID, String firstName, String lastName, String email, String password, String role) {
        super(employeeID, firstName, lastName, email, password, role);
        this.department = determineDepartmentFromPosition(super.getPosition()); // Default initialization
    }
    
    // Constructor with all parameters
    public ImmediateSupervisor(int employeeID, String firstName, String lastName, String address, String phoneNo,
                               String email, String password, String role, LocalDate birthday,
                               String sssNo, String philhealthNo, String pagibigNo,
                               String tinNo, String position, String department,
                               Employee supervisor, String empStatus,
                               double basicSalary, double riceSubsidy, double phoneAllowance,
                               double clothingAllowance, double grossSemiMthlyRate,
                               double hourlyRate, double vacationLeave, double sickLeave, double emergencyLeave,
                               double maternityLeave, double paternityLeave, double specialLeave) {
        super(employeeID, firstName, lastName, email, password, role,
                birthday, address, phoneNo, sssNo, philhealthNo, position,
                pagibigNo, empStatus, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMthlyRate, hourlyRate);
        this.department = department; // Initialize department directly
    }
    
    /**
     * Get the department of this supervisor
     * @return The department name
     */
    public String getDepartment() {
        // If department is not explicitly set, determine it from position
        if (department == null || department.isEmpty()) {
            department = determineDepartmentFromPosition(super.getPosition());
        }
        return department;
    }
    
    /**
     * Set the department for this supervisor
     * @param department The department name
     */
    public void setDepartment(String department) {
        this.department = department;
    }
    
    /**
     * Determines the department based on the position
     * Similar to the logic in EmployeeManagement class
     * @param position The job title
     * @return The department name
     */
    private String determineDepartmentFromPosition(String position) {
        if (position == null) {
            return "Other";
        }

        position = position.trim();

        // Leadership positions
        if (position.equals("Chief Executive Officer") || 
            position.equals("Chief Operating Officer") || 
            position.equals("Chief Finance Officer") || 
            position.equals("Chief Marketing Officer")) {
            return "Leadership";
        }

        // HR Department
        else if (position.equals("HR Manager") || 
                 position.equals("HR Team Leader") || 
                 position.equals("HR Rank and File")) {
            return "HR";
        }

        // IT Department
        else if (position.equals("IT Operations and Systems") ||
                 position.toLowerCase().contains("it ")) {
            return "IT";
        }

        // Accounting Department
        else if (position.equals("Accounting Head") || 
                 position.equals("Payroll Manager") || 
                 position.equals("Payroll Team Leader") || 
                 position.equals("Payroll Rank and File")) {
            return "Accounting";
        }

        // Accounts Department
        else if (position.equals("Account Manager") || 
                 position.equals("Account Team Leader") || 
                 position.equals("Account Rank and File")) {
            return "Accounts";
        }

        // Sales and Marketing Department
        else if (position.equals("Sales & Marketing")) {
            return "Sales and Marketing";
        }

        // Supply Chain and Logistics Department
        else if (position.equals("Supply Chain and Logistics")) {
            return "Supply Chain and Logistics";
        }

        // Customer Service Department
        else if (position.equals("Customer Service and Relations")) {
            return "Customer Service";
        }

        // If no specific match is found
        return "Other";
    }
    
    // Rest of your existing methods...
    
    //View leave requests of employees under supervisor
    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequests.values().stream()
                .filter(request -> request.getStatus() == ApprovalStatus.PENDING) // ✅ Use PENDING instead
                .collect(Collectors.toList());
    }
    
    //Approve leave request
    @Override
    public boolean approveLeaveRequest(int leaveID) {
        System.out.println("Supervisor approved leave ID: " + leaveID);
        return true;
    }
    
    //Deny leave request
    @Override
    public boolean rejectLeaveRequest(int leaveID) {
    // Don't check the leaveRequests HashMap
    System.out.println("Supervisor rejected leave request ID: " + leaveID);
    return true;
}
    
    //Approve attendance
    @Override
    public boolean approveAttendance(int attendanceID) {
        System.out.println("Supervisor approved attendance ID: " + attendanceID);
        return true;
    }
    
    //Deny attendance
    @Override
    public boolean denyAttendance(int attendanceID, String reason) {
        System.out.println("Supervisor denied attendance ID: " + attendanceID + " - Reason: " + reason);
        return true; // Changed to true to be consistent with return type
    }
    
    public void addLeaveRequest(LeaveRequest request) {
        leaveRequests.put(request.getRequestId(), request); // Supervisor tracks requests
    }
}