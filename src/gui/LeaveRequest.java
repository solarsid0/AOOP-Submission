package gui;

import CSV.CSVDatabaseProcessor;
import com.toedter.calendar.JDateChooser; // JCalendar import
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import oop.classes.actors.User;
import oop.classes.actors.Employee;
import oop.classes.enums.LeaveName;
import oop.classes.enums.ApprovalStatus;

/**
 * This screen allows employees to submit leave requests
 */
public class LeaveRequest extends javax.swing.JFrame {

    private CSVDatabaseProcessor csvProcessor;
    private String employeeId;
    private User loggedInUser;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private boolean justSubmitted = false;
    
    // Declare JDateChooser components
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;

    /**
     * NEW CONSTRUCTOR: Creates LeaveRequest form with a User object
     * This constructor should be used when navigating from another screen
     * @param user
     */
    public LeaveRequest(User user) {
        initComponents();
        
        // Store the user
        this.loggedInUser = user;

        // Debug logs to verify user data
        System.out.println("LeaveRequest initialized with user: " + 
                       (user != null ? user.getEmployeeID() + " - " + user.getFirstName() + " " + user.getLastName() : "null"));
    
        // Check if user is properly set
        if (user == null) {
        JOptionPane.showMessageDialog(this, 
                                    "User session not found. Please log in again.", 
                                    "Session Error", 
                                    JOptionPane.ERROR_MESSAGE);
        dispose();
        new Login().setVisible(true);
        return;
    }
        // If the user is an Employee, cast it to access employee-specific methods
        if (user instanceof Employee) {
        }
        
        // Set employee ID from the user
        this.employeeId = String.valueOf(user.getEmployeeID());
        
        // Center the form on screen
        this.setLocationRelativeTo(null);
        
        // Initialize CSV processor
        csvProcessor = new CSVDatabaseProcessor();
        
        // Load employee data from CSV
        csvProcessor.loadEmployeeCSVData();
        csvProcessor.loadLeaveRequestData();
        
        // Set current date in the submission date field
        jTextField7.setText(LocalDate.now().format(dateFormatter));
        
        //Gets employee data 
        Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
        
        if (employeeData != null) {
            // Set employee info in the UI
            String firstName = employeeData.get("First Name");
            String lastName = employeeData.get("Last Name");
            String position = employeeData.get("Position");
            String status = employeeData.get("Status");
            String supervisor = employeeData.get("Immediate Supervisor");
            
            
            // Fill UI elements with employee data
            jTextField2.setText(employeeId);
            jTextField1.setText(firstName);
            jTextField4.setText(lastName);
            jTextField5.setText(position);
            jTextField6.setText(status);
            
            // Load existing leave requests for this employee
            loadLeaveRequests();
        } else {
            JOptionPane.showMessageDialog(this, "Employee data not found for ID: " + employeeId,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        // Replace text fields with date choosers
        setupDateChoosers();
        
        // Add action listener for back button
        backattndncbttn.addActionListener((e) -> {
            goBack();
        });
        
        // Add action listener for submit button
        jButton1.addActionListener((e) -> {
            submitLeaveRequest();
        });
        
    }

        // Helper method to organize code
        private void setupEmployeeData(Map<String, String> employeeData) {
        // Set employee info in the GUI
        String firstName = employeeData.get("First Name");
        String lastName = employeeData.get("Last Name");
        String position = employeeData.get("Position");
        String status = employeeData.get("Status");
        String supervisor = employeeData.get("Immediate Supervisor");


        // Fill UI elements with employee data
        jTextField2.setText(employeeId);
        jTextField1.setText(firstName);
        jTextField4.setText(lastName);
        jTextField5.setText(position);
        jTextField6.setText(status);

        // Load existing leave requests for this employee
        loadLeaveRequests();
    }

    /**
     * Original constructor for testing purposes
     */
    public LeaveRequest() {
        initComponents();
        
        // Center the form on screen
        this.setLocationRelativeTo(null);
        
        // For testing purposes, use a default employee ID
        this.employeeId = "10001";
        
        // Initialize CSV processor
        csvProcessor = new CSVDatabaseProcessor();
        
        // Load employee data from CSV
        csvProcessor.loadEmployeeCSVData();
        csvProcessor.loadLeaveRequestData();
        
        // Set current date in the submission date field
        jTextField7.setText(LocalDate.now().format(dateFormatter));
        
        Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
        
        if (employeeData != null) {
            // Set employee info in the UI
            String firstName = employeeData.get("First Name");
            String lastName = employeeData.get("Last Name");
            String position = employeeData.get("Position");
            String status = employeeData.get("Status");
            String supervisor = employeeData.get("Immediate Supervisor");
            
            
            // Fill UI elements with employee data
            jTextField2.setText(employeeId);
            jTextField1.setText(firstName);
            jTextField4.setText(lastName);
            jTextField5.setText(position);
            jTextField6.setText(status);
            
            // Load existing leave requests for this employee
            loadLeaveRequests();
        } else {
            JOptionPane.showMessageDialog(this, "Employee data not found for ID: " + employeeId,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        // Replace text fields with date choosers
        setupDateChoosers();
        
        // Add action listener for back button
        backattndncbttn.addActionListener((e) -> {
            goBack();
        });
        
        // Add action listener for submit button
        jButton1.addActionListener((e) -> {
            submitLeaveRequest();
        });
    }
    
    /**
     * Setup date choosers to replace the text fields
     */
    private void setupDateChoosers() {
        // Create start date chooser
        startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("MM/dd/yyyy");
        startDateChooser.setDate(new Date()); // Set current date
        
        // Create end date chooser
        endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("MM/dd/yyyy");
        
        // Set end date to a week from current date
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        endDateChooser.setDate(cal.getTime());
        
        // Get the bounds of the text fields to replace
        java.awt.Rectangle startBounds = jTextField9.getBounds();
        java.awt.Rectangle endBounds = jTextField11.getBounds();
        
        // Remove the original text fields
        jPanel5.remove(jTextField9);
        jPanel5.remove(jTextField11);
        
        // Add date choosers with the same bounds
        startDateChooser.setBounds(startBounds);
        endDateChooser.setBounds(endBounds);
        
        jPanel5.add(startDateChooser);
        jPanel5.add(endDateChooser);
        
        // Refresh the panel
        jPanel5.revalidate();
        jPanel5.repaint();
    }
    
    /**
     * Load and display existing leave requests for this employee
     */
    private void loadLeaveRequests() {
    // Get the table model
    DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
    model.setRowCount(0); // Clear existing data

    System.out.println("Loading leave requests for employee ID: " + employeeId);

    boolean foundRecords = false;
    List<Map<String, String>> requests = csvProcessor.getAllLeaveRequestRecords();
    
    // Debug log
    System.out.println("Total leave requests in system: " + requests.size());
    
    for (Map<String, String> request : requests) {
        String requestEmpId = request.get("Employee ID");
        
        // Check if this request belongs to the current employee
        if (requestEmpId != null && requestEmpId.equals(employeeId)) {
            foundRecords = true;
            
            // Add this record to the table
            model.addRow(new Object[]{
                request.get("Date of Submission"),
                request.get("Employee ID"),
                request.get("First Name"),
                request.get("Last Name"),
                request.get("Position"),
                request.get("Status"),
                request.get("Immediate Supervisor"),
                request.get("Type of Leave"),
                request.get("Note"),
                request.get("Start"),
                request.get("End"),
                request.get("Leave Status"),
                request.get("Remaining Vacation Leave"),
                request.get("Remaining Sick Leave")
            });
            
            System.out.println("Added leave request from " + request.get("Start") + 
                              " to " + request.get("End") + 
                              " (Status: " + request.get("Leave Status") + ")");
        }
    }
    
    // If no records found, add a placeholder or sample record
    if (!foundRecords) {
        System.out.println("No leave requests found for employee ID: " + employeeId);
        model.addRow(new Object[]{
            LocalDate.now().minusDays(10).format(dateFormatter),
            employeeId,
            jTextField1.getText(),
            jTextField4.getText(),
            jTextField5.getText(),
            jTextField6.getText(),
            "Team Manager",
            "Vacation Leave",
            "Annual vacation",
            LocalDate.now().plusDays(5).format(dateFormatter),
            LocalDate.now().plusDays(10).format(dateFormatter),
            "Pending",
            "5",
            "5"
        });
    }

    // Set up table formatting for better display
    formatTable();
}

/**
 * Format the table for better display
 */
private void formatTable() {
    // Enable auto resize mode to allow horizontal scrolling
    jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // Set preferred column widths
    for (int i = 0; i < jTable1.getColumnCount(); i++) {
        switch (i) {
            case 0: // Date
                jTable1.getColumnModel().getColumn(i).setPreferredWidth(100);
                break;
            case 8: // Note
                jTable1.getColumnModel().getColumn(i).setPreferredWidth(150);
                break;
            case 6: // Supervisor
                jTable1.getColumnModel().getColumn(i).setPreferredWidth(150);
                break;
            default:
                jTable1.getColumnModel().getColumn(i).setPreferredWidth(100);
                break;
        }
    }

    // Enable horizontal scrolling
    jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    // Always show vertical scrollbar
    jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
}
    
    /**
     * Submit a new leave request
     */
    private void submitLeaveRequest() {
        
        // If the form was just reset after submission, clear the flag and exit
        if (justSubmitted) {
            justSubmitted = false;
            return;
        }
    
        try {
            // Get the selected leave type
            String leaveTypeStr = (String) jComboBox1.getSelectedItem();
            if (leaveTypeStr.contains("Select")) {
                JOptionPane.showMessageDialog(this, 
                        "Please select a leave type", 
                        "Missing Information", 
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Parse the leave type
            LeaveName leaveType = null;
            if (leaveTypeStr.contains("Sick")) {
                leaveType = LeaveName.SICK;
            } else if (leaveTypeStr.contains("Vacation")) {
                leaveType = LeaveName.VACATION;
            } else if (leaveTypeStr.contains("Emergency")) {
                leaveType = LeaveName.EMERGENCY;
            }

            // Get leave reason
            String reason = jTextField10.getText().trim();
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                        "Please provide a reason for the leave", 
                        "Missing Information", 
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Get dates from date choosers
            LocalDate startDate = null;
            LocalDate endDate = null;

            try {
                // Convert java.util.Date to LocalDate
                startDate = startDateChooser.getDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                endDate = endDateChooser.getDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                        "Please select valid dates", 
                        "Invalid Date", 
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate dates
            if (startDate.isAfter(endDate)) {
                JOptionPane.showMessageDialog(this, 
                        "End date must be after start date", 
                        "Invalid Date Range", 
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (startDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, 
                        "Start date cannot be in the past", 
                        "Invalid Date", 
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get supervisor name from employee data using the existing method
            Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
            String supervisorName = "Team Manager"; // Default fallback

            if (employeeData != null) {
                String supervisor = employeeData.get("Immediate Supervisor");
                if (supervisor != null && !supervisor.trim().isEmpty()) {
                    supervisorName = supervisor;
                }
            }

            // Create the leave request
            oop.classes.empselfservice.LeaveRequest newRequest = 
                new oop.classes.empselfservice.LeaveRequest(
                    employeeId, 
                    leaveType, 
                    startDate, 
                    endDate, 
                    reason
                );

            // Set initial status
            newRequest.setStatus(ApprovalStatus.PENDING);

            // Add to the table for display
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.addRow(new Object[]{
                LocalDate.now().format(dateFormatter),
                employeeId,
                jTextField1.getText(),
                jTextField4.getText(),
                jTextField5.getText(),
                jTextField6.getText(),
                supervisorName,  // Use the supervisor name from employee data
                leaveTypeStr,
                reason,
                startDate.format(dateFormatter),
                endDate.format(dateFormatter),
                "Pending",
                "5",
                "5"
            });

            // Create a new leave request record map
            Map<String, String> leaveRequestRecord = new HashMap<>();
            leaveRequestRecord.put("Date of Submission", LocalDate.now().format(dateFormatter));
            leaveRequestRecord.put("Employee ID", employeeId);
            leaveRequestRecord.put("First Name", jTextField1.getText());
            leaveRequestRecord.put("Last Name", jTextField4.getText());
            leaveRequestRecord.put("Position", jTextField5.getText());
            leaveRequestRecord.put("Status", jTextField6.getText());
            leaveRequestRecord.put("Immediate Supervisor", supervisorName);
            leaveRequestRecord.put("Type of Leave", leaveTypeStr);
            leaveRequestRecord.put("Note", reason);
            leaveRequestRecord.put("Start", startDate.format(dateFormatter));
            leaveRequestRecord.put("End", endDate.format(dateFormatter));
            leaveRequestRecord.put("Leave Status", "Pending");
            leaveRequestRecord.put("Remaining Vacation Leave", "5");
            leaveRequestRecord.put("Remaining Sick Leave", "5");

            // To save to CSV file, add the record to the existing records list
            try {
                // Add to the leave request records
                List<Map<String, String>> allRecords = csvProcessor.getAllLeaveRequestRecords();
                allRecords.add(leaveRequestRecord);

                // Save the updated records
                System.out.println("Leave request added to records. Needs CSV file saving implementation.");
            } catch (Exception ex) {
                System.err.println("Error saving leave request to CSV: " + ex.getMessage());
                ex.printStackTrace();
            }

            // Show success message
            JOptionPane.showMessageDialog(this, 
                    "Leave request submitted successfully!\nRequest ID: " + newRequest.getRequestId(), 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);

            // Clear form fields
            jComboBox1.setSelectedIndex(0);
            jTextField10.setText("");

            // Reset date choosers
            startDateChooser.setDate(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 7);
            endDateChooser.setDate(cal.getTime());

            // Set the flag to prevent validation on the next click
            justSubmitted = true;
        
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                    "Error submitting leave request: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
        /**
         * Navigate back to the previous screen based on user role
         */
        private void goBack() {
            // First, check if the user object is available
            if (loggedInUser == null) {
                System.err.println("Cannot navigate back: User is null");
                // Fallback to login page
                this.dispose();
                new Login().setVisible(true);
                return;
            }

            // Always dispose the current window FIRST before creating any new ones
            this.setVisible(false); // Hide this window first

            try {
                // Add debugging info
                System.out.println("Back button clicked - Debug info:");
                System.out.println("  User ID: " + loggedInUser.getEmployeeID());
                System.out.println("  User role: " + loggedInUser.getRole());
                System.out.println("  User name: " + loggedInUser.getFirstName() + " " + loggedInUser.getLastName());

                // Navigate back based on user role
                switch (loggedInUser.getRole()) {
                    case "IT":
                        new AdminIT(loggedInUser).setVisible(true);
                        break;
                    case "HR":
                        new AdminHR(loggedInUser).setVisible(true);
                        break;
                    case "ACCOUNTING":
                        new AdminAccounting(loggedInUser).setVisible(true);
                        break;
                    case "IMMEDIATE SUPERVISOR":
                        new AdminSupervisor(loggedInUser).setVisible(true);
                        break;
                    case "EMPLOYEE":
                        new EmployeeSelfService(loggedInUser).setVisible(true);
                        break;
                    default:
                        // Fall back to login if role is unknown
                        System.err.println("Unknown role: " + loggedInUser.getRole() + ", returning to login");
                        new Login().setVisible(true);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error navigating back: " + e.getMessage());
                e.printStackTrace();
                // If navigation fails, go to login as a fallback
                new Login().setVisible(true);
            } finally {
                this.dispose(); // Always dispose the current window
            }
        }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        backattndncbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jTextField11 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("LEAVE REQUEST PAGE");

        backattndncbttn.setBackground(new java.awt.Color(207, 10, 10));
        backattndncbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backattndncbttn.setForeground(new java.awt.Color(255, 255, 255));
        backattndncbttn.setText("Back");
        backattndncbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backattndncbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backattndncbttn)
                .addGap(39, 39, 39)
                .addComponent(jLabel1)
                .addContainerGap(902, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backattndncbttn))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jPanel3.setBackground(new java.awt.Color(220, 95, 0));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("PERSONAL DETAILS");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel2)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Employee ID:");

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("First Name:");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Last Name:");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("Position:");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setText("Employment Status:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField5, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(jTextField1)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5)
                    .addComponent(jTextField2)
                    .addComponent(jLabel7))
                .addGap(33, 33, 33)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel8)
                    .addComponent(jLabel6)
                    .addComponent(jTextField4)
                    .addComponent(jTextField6, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                .addContainerGap(36, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(jTextField4))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField5, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(jTextField6))
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("Date of Submission");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("Type of Leave:");

        jTextField7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField7ActionPerformed(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setText("Start of Leave");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setText("Reason:");

        jTextField9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField9ActionPerformed(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setText("End of Leave:");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select", "Sick Leave", "Vacation Leave", "Emergency Leave" }));

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton1.setText("Submit");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jTextField11, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                        .addComponent(jLabel9)
                        .addComponent(jLabel11)
                        .addComponent(jTextField7)
                        .addComponent(jLabel13))
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel12)
                    .addComponent(jTextField10)
                    .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(53, 53, 53))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField7, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(jComboBox1))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(14, 14, 14))
        );

        jPanel6.setBackground(new java.awt.Color(220, 95, 0));

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("LEAVE DETAILS");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jLabel15)
                .addContainerGap(358, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel15)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Date", "ID #", "First Name", "Last Name", "Position", "Status", "Supervisor", "Leave Type", "Note", "Start", "End", "Leave Status", "VL Remaining", "SL Remaining"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(87, 87, 87)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1171, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(31, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1227, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField7ActionPerformed

    private void jTextField9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField9ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField9ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        submitLeaveRequest();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void backattndncbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattndncbttnActionPerformed
    System.out.println("Back button clicked - Debug info:");
    System.out.println("  loggedInUser is " + (loggedInUser == null ? "NULL" : "NOT NULL"));
    if (loggedInUser != null) {
        System.out.println("  User ID: " + loggedInUser.getEmployeeID());
        System.out.println("  User role: " + loggedInUser.getRole());
        System.out.println("  User name: " + loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
    }
    }//GEN-LAST:event_backattndncbttnActionPerformed

    /**
     * @param args the command line arguments
     */
     public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new gui.LeaveRequest().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backattndncbttn;
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField9;
    // End of variables declaration//GEN-END:variables
}
