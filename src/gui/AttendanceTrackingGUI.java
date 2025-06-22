/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

import CSV.CSVDatabaseProcessor;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import oop.classes.actors.User;
import oop.classes.actors.HR;
import oop.classes.actors.ImmediateSupervisor;
import oop.classes.management.AttendanceTracking;

/**
 * This class is the implementation of the Attendance Tracking interface functionality.
 * Implements polymorphic behavior for HR and Immediate Supervisor roles.
 * 
 * @author USER
 */
public class AttendanceTrackingGUI extends javax.swing.JFrame {
    
    // Fields for polymorphic implementation
    private User loggedInUser;
    private AttendanceTracking attendanceTracker;
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();
    private int selectedAttendanceID = -1;
    
    /**
     * Inner class to represent an attendance record
     */
    private static class AttendanceRecord {
        private final int id;
        private final int employeeID;
        private final String date;
        private final String logIn;
        private final String logOut;
        private String status;
        private final String employeeName;
        private final String department;
        
        public AttendanceRecord(int id, int employeeID, String employeeName, String department, 
                                String date, String logIn, String logOut, String status) {
            this.id = id;
            this.employeeID = employeeID;
            this.employeeName = employeeName;
            this.department = department;
            this.date = date;
            this.logIn = logIn;
            this.logOut = logOut;
            this.status = status;
        }
        
        // Getters
        public int getId() { return id; }
        public int getEmployeeID() { return employeeID; }
        public String getDate() { return date; }
        public String getLogIn() { return logIn; }
        public String getLogOut() { return logOut; }
        public String getStatus() { return status; }
        public String getEmployeeName() { return employeeName; }
        public String getDepartment() { return department; }
        
        // Setter for status
        public void setStatus(String status) { this.status = status; }
        
    }

    /**
     * Default constructor for NetBeans GUI builder
     */
    public AttendanceTrackingGUI() {
        initComponents();
        // Makes table uneditable
        AttendanceTrckrHRTbl.setDefaultEditor(Object.class, null); // This disables editing for all cell types
        AttendanceTrckrHRTbl.setCellSelectionEnabled(false);
        AttendanceTrckrHRTbl.setRowSelectionAllowed(true);
    }
 
    
    /**
     * Constructor that initializes the GUI with the logged-in user.
     * It implements polymorphism by accepting any type of User that implements AttendanceTracking.
     * 
     * @param user The logged-in user (either HR or Immediate Supervisor)
     */
    public AttendanceTrackingGUI(User user) {
        initComponents();
        
        // Makes table uneditable
        AttendanceTrckrHRTbl.setDefaultEditor(Object.class, null); // This disables editing for all cell types
        AttendanceTrckrHRTbl.setCellSelectionEnabled(false);
        AttendanceTrckrHRTbl.setRowSelectionAllowed(true);
        
        //Store user
        this.loggedInUser = user;
        
        // Polymorphic assignment - cast to AttendanceTracking interface
        if (user instanceof AttendanceTracking) {
            this.attendanceTracker = (AttendanceTracking) user;
        } else {
            throw new IllegalArgumentException("User must implement AttendanceTracking interface");
        }
        
        
        // Configure the back button
        configureBackButton();
        
        // Configure table selection listener
        configureTableSelection();
        
        // Load attendance data from CSV
        loadAttendanceData();
    }
    
    /**
     * Configure the back button to return to the appropriate admin page
     */
    private void configureBackButton() {
        // Remove existing action listeners if any
        for (ActionListener al : backattndncbttn.getActionListeners()) {
            backattndncbttn.removeActionListener(al);
        }
        
        // Add custom action listener
        backattndncbttn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                // Determine which admin page to return to based on user role
                if (loggedInUser.getRole().equals("HR")) {
                    new AdminHR(loggedInUser).setVisible(true);
                } else if (loggedInUser.getRole().equals("IMMEDIATE SUPERVISOR")) {
                    new AdminSupervisor(loggedInUser).setVisible(true);
                }
            }
        });
    }
    
    /**
     * Configure the table selection to update text fields and track selected record
     */
    private void configureTableSelection() {
        AttendanceTrckrHRTbl.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && AttendanceTrckrHRTbl.getSelectedRow() != -1) {
                int row = AttendanceTrckrHRTbl.getSelectedRow();

                // Verify there are rows in the model
                if (AttendanceTrckrHRTbl.getModel().getRowCount() == 0) {
                    return;
                }

                try {
                    // Find the corresponding record in our attendanceRecords list
                    // We need to get the employee ID from column 0 now
                    Object employeeIdObj = AttendanceTrckrHRTbl.getValueAt(row, 0);
                    Object employeeNameObj = AttendanceTrckrHRTbl.getValueAt(row, 1);
                    Object dateObj = AttendanceTrckrHRTbl.getValueAt(row, 3); // Date is now in column 3
                    
                    // Find the record ID internally based on the employee ID and date
                    if (employeeIdObj != null && dateObj != null) {
                        String empId = employeeIdObj.toString();
                        String date = dateObj.toString();
                        
                        // Find the matching record
                        for (AttendanceRecord record : attendanceRecords) {
                            if (String.valueOf(record.getEmployeeID()).equals(empId) && 
                                record.getDate().equals(date)) {
                                selectedAttendanceID = record.getId();
                                break;
                            }
                        }

                        // Set the text fields with the selected employee's info
                        if (employeeIdObj != null && employeeNameObj != null) {
                            InputIDNo.setText(employeeIdObj.toString());
                            inputNameHR.setText(employeeNameObj.toString());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error in table selection: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Load attendance data from CSV file
     * Implements polymorphic behavior by filtering data based on user role:
     * - HR can view all attendance records
     * - Immediate Supervisor can only view records of their subordinates
     */
    private void loadAttendanceData() {
        attendanceRecords.clear();
        String line;
        String csvSplitBy = ",";

        try {
            // Try all possible file paths to locate the CSV file
            String[] possiblePaths = {
                "Attendance Record 2024.csv",
                "src/CSV/Attendance Record 2024.csv",
                "./Attendance Record 2024.csv",
                "../Attendance Record 2024.csv"
            };

            File file = null;
            for (String path : possiblePaths) {
                file = new File(path);
                if (file.exists()) {
                    System.out.println("Found attendance file at: " + path);
                    break;
                }
            }

            if (file == null || !file.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "Attendance file not found. Tried multiple paths.", 
                    "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                // Skip header line
                br.readLine();

                // Create a new table model with proper columns
                DefaultTableModel model = new DefaultTableModel();
                // Add columns EXCEPT Record ID
                model.addColumn("Employee ID"); 
                model.addColumn("Employee Name");
                model.addColumn("Department");
                model.addColumn("Date");
                model.addColumn("Log In");
                model.addColumn("Log Out");
                model.addColumn("Status");

                int id = 1; // We'll still use this internally even though we don't display it

                // Read each line from the CSV
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(csvSplitBy);

                    if (data.length >= 6) {
                        String employeeID = data[0].trim();
                        String lastName = data[1].trim();
                        String firstName = data[2].trim();
                        String date = data[3].trim();
                        String logIn = data[4].trim();
                        String logOut = data[5].trim();
                        String status = "Pending"; // Default status for new records

                        // Look up employee details using CSVDatabaseProcessor
                        Map<String, String> employeeDetails = getEmployeeDetails(employeeID);
                        String department = "Unknown";
                        String supervisorName = "Unknown";

                        if (employeeDetails != null) {
                            department = getDepartmentForEmployee(employeeID);
                            supervisorName = employeeDetails.get("Immediate Supervisor");
                        }

                        // Apply polymorphic filtering based on user role
                        boolean shouldInclude = false;

                        if (loggedInUser instanceof HR) {
                            // HR can see all attendance records
                            shouldInclude = true;
                            System.out.println("HR user, including all records");
                        } else if (loggedInUser instanceof ImmediateSupervisor) {
                            // Get supervisor's full name (for comparison with employee's supervisor field)
                            String supervisorFullName = loggedInUser.getLastName() + ", " + loggedInUser.getFirstName();

                            // Get supervisor's department
                            String supervisorDept = ((ImmediateSupervisor) loggedInUser).getDepartment();

                            // Debug logging
                            System.out.println("Checking employee: " + firstName + " " + lastName);
                            System.out.println("  Employee supervisor: " + supervisorName);
                            System.out.println("  Logged-in supervisor: " + supervisorFullName);
                            System.out.println("  Employee department: " + department);
                            System.out.println("  Supervisor department: " + supervisorDept);

                            // Include record if either:
                            // 1. The employee's supervisor name matches the logged-in user's name, OR
                            // 2. They're in the same department (as a fallback)
                            shouldInclude = supervisorName.equalsIgnoreCase(supervisorFullName) ||
                                            department.equals(supervisorDept);
                        }

                        if (shouldInclude) {
                            // Create a new attendance record and add to our collection
                            AttendanceRecord record = new AttendanceRecord(
                                id,
                                Integer.parseInt(employeeID),
                                firstName + " " + lastName,
                                department,
                                date,
                                logIn,
                                logOut,
                                status
                            );
                            attendanceRecords.add(record);

                            // Add to table model WITHOUT the ID column
                            model.addRow(new Object[]{
                                employeeID,
                                firstName + " " + lastName,
                                department,
                                date,
                                logIn,
                                logOut,
                                status
                            });

                            id++; // Increment record ID
                        }
                    }
                }

                // Set the model on the table
                AttendanceTrckrHRTbl.setModel(model);

                // Add a message if no data was loaded
                if (model.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, 
                        "No attendance records found for your role.", 
                        "No Data", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.out.println("Loaded " + model.getRowCount() + " attendance records.");
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error reading attendance data: " + e.getMessage(), 
                    "File Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading attendance data: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Helper method to get complete employee details from the database
     */
    private Map<String, String> getEmployeeDetails(String employeeId) {
        CSVDatabaseProcessor csvProcessor = new CSVDatabaseProcessor();
        return csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
    }


    // Helper method to get department for an employee
    private String getDepartmentForEmployee(String employeeId) {
        // Implement based on your data structure or return a default
        CSVDatabaseProcessor csvProcessor = new CSVDatabaseProcessor();
        Map<String, String> empRecord = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
        if (empRecord != null && empRecord.containsKey("Position")) {
            String position = empRecord.get("Position");
            // Use your existing logic to determine department from position
            return determineDepartmentFromPosition(position);
        }
        return "Other"; // Default department
    }

    // Reuse your existing department determination logic
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

    /**
     * Update the table model with current attendance records
     */
    private void updateTableModel() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };

        // Define columns WITHOUT Record ID
        model.addColumn("Employee ID");
        model.addColumn("Employee Name");
        model.addColumn("Department");
        model.addColumn("Date");
        model.addColumn("Log In");
        model.addColumn("Log Out");
        model.addColumn("Status");

        // Add rows from attendance records
        for (AttendanceRecord record : attendanceRecords) {
            model.addRow(new Object[]{
                record.getEmployeeID(),
                record.getEmployeeName(),
                record.getDepartment(),
                record.getDate(),
                record.getLogIn(),
                record.getLogOut(),
                record.getStatus()
            });
        }

        AttendanceTrckrHRTbl.setModel(model);
    }

    /**
     * Approve the selected attendance record using polymorphism
     * Uses the attendanceTracker reference to call the appropriate implementation
     */
    private void approveSelectedAttendance() {
        if (selectedAttendanceID == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select an attendance record to approve", 
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Polymorphic call to approval method
        boolean success = attendanceTracker.approveAttendance(selectedAttendanceID);

        if (success) {
            // Update status in our records
            for (AttendanceRecord record : attendanceRecords) {
                if (record.getId() == selectedAttendanceID) {
                    record.setStatus("Approved");
                    break;
                }
            }

            JOptionPane.showMessageDialog(this, 
                "Attendance record approved successfully", 
                "Success", JOptionPane.INFORMATION_MESSAGE);

            // Refresh table
            updateTableModel();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to approve attendance record", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deny the selected attendance record using polymorphism
     * Uses the attendanceTracker reference to call the appropriate implementation
     */
    private void denySelectedAttendance() {
        if (selectedAttendanceID == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select an attendance record to deny", 
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this, 
            "Please provide a reason for denial:", 
            "Deny Attendance", JOptionPane.QUESTION_MESSAGE);

        if (reason == null) {
            // User canceled the dialog
            return;
        }

        if (reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "A reason is required to deny attendance", 
                "Reason Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Polymorphic call to denial method
        boolean success = attendanceTracker.denyAttendance(selectedAttendanceID, reason);

        if (success) {
            // Update status in our records
            for (AttendanceRecord record : attendanceRecords) {
                if (record.getId() == selectedAttendanceID) {
                    record.setStatus("Denied");
                    break;
                }
            }

            JOptionPane.showMessageDialog(this, 
                "Attendance record denied successfully", 
                "Success", JOptionPane.INFORMATION_MESSAGE);

            // Refresh table
            updateTableModel();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to deny attendance record", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Filter attendance records by employee ID
     */
    private void filterByEmployeeID(String idText) {
        if (idText.trim().isEmpty()) {
            // Show all records if filter is empty
            updateTableModel();
            return;
        }

        try {
            // Make sure we're searching for employee ID, not record ID
            String searchID = idText.trim();
            List<AttendanceRecord> filteredRecords = new ArrayList<>();

            for (AttendanceRecord record : attendanceRecords) {
                // Compare as strings to handle leading zeros or non-numeric IDs
                if (String.valueOf(record.getEmployeeID()).equals(searchID)) {
                    filteredRecords.add(record);
                }
            }

            // Update the model with filtered records
            updateFilteredTableModel(filteredRecords);
            
            // Provide feedback if no matches found
            if (filteredRecords.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "No attendance records found for Employee ID: " + searchID, 
                    "No Results", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid employee ID number", 
                "Invalid Input", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Filter attendance records by employee name
     */
    private void filterByEmployeeName(String nameText) {
        if (nameText.trim().isEmpty()) {
            // Show all records if filter is empty
            updateTableModel();
            return;
        }

        List<AttendanceRecord> filteredRecords = new ArrayList<>();
        String searchName = nameText.toLowerCase();

        for (AttendanceRecord record : attendanceRecords) {
            if (record.getEmployeeName().toLowerCase().contains(searchName)) {
                filteredRecords.add(record);
            }
        }

        // Update the model with filtered records
        updateFilteredTableModel(filteredRecords);
        
        // Provide feedback if no matches found
        if (filteredRecords.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No attendance records found for name: " + nameText, 
                "No Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Updates the table model with filtered records
     */
    private void updateFilteredTableModel(List<AttendanceRecord> filteredRecords) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        
        // Define columns WITHOUT Record ID
        model.addColumn("Employee ID");
        model.addColumn("Employee Name");
        model.addColumn("Department");
        model.addColumn("Date");
        model.addColumn("Log In");
        model.addColumn("Log Out");
        model.addColumn("Status");

        // Add filtered rows
        for (AttendanceRecord record : filteredRecords) {
            model.addRow(new Object[]{
                record.getEmployeeID(),
                record.getEmployeeName(),
                record.getDepartment(),
                record.getDate(),
                record.getLogIn(),
                record.getLogOut(),
                record.getStatus()
            });
        }
        
        AttendanceTrckrHRTbl.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        AttendanceTrckrHRTbl = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        IDNoTrckrHR = new javax.swing.JLabel();
        InputIDNo = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        InputEmpNameTrckrHR = new javax.swing.JLabel();
        inputName = new javax.swing.JPanel();
        EmpNameTrckrHR = new javax.swing.JLabel();
        InputEmpNameTrckrHR1 = new javax.swing.JLabel();
        inputNameHR = new javax.swing.JTextField();
        denyattndncbtnHR = new javax.swing.JButton();
        findEmployeeBtn = new javax.swing.JButton();
        approveattndncbtnHR1 = new javax.swing.JButton();
        approveAllBttn = new javax.swing.JButton();
        denyAllBttn = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        backattndncbttn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(868, 442));

        AttendanceTrckrHRTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Employee ID", "Employee Name", "Department", "Date", "Log In", "Log Out", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(AttendanceTrckrHRTbl);

        jPanel3.setBackground(new java.awt.Color(220, 95, 0));

        IDNoTrckrHR.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        IDNoTrckrHR.setForeground(new java.awt.Color(255, 255, 255));
        IDNoTrckrHR.setText("ID #:");

        InputIDNo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InputIDNoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(IDNoTrckrHR)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(InputIDNo, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IDNoTrckrHR)
                    .addComponent(InputIDNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(220, 95, 0));

        InputEmpNameTrckrHR.setForeground(new java.awt.Color(255, 255, 255));

        inputName.setBackground(new java.awt.Color(220, 95, 0));

        EmpNameTrckrHR.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        EmpNameTrckrHR.setForeground(new java.awt.Color(255, 255, 255));
        EmpNameTrckrHR.setText("Name:");

        InputEmpNameTrckrHR1.setForeground(new java.awt.Color(255, 255, 255));

        inputNameHR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputNameHRActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout inputNameLayout = new javax.swing.GroupLayout(inputName);
        inputName.setLayout(inputNameLayout);
        inputNameLayout.setHorizontalGroup(
            inputNameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputNameLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(EmpNameTrckrHR)
                .addGap(12, 12, 12)
                .addComponent(inputNameHR, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(InputEmpNameTrckrHR1, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        inputNameLayout.setVerticalGroup(
            inputNameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputNameLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(inputNameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EmpNameTrckrHR)
                    .addComponent(InputEmpNameTrckrHR1)
                    .addComponent(inputNameHR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(74, 74, 74)
                .addComponent(InputEmpNameTrckrHR, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(1, 1, 1)
                    .addComponent(inputName, javax.swing.GroupLayout.PREFERRED_SIZE, 418, Short.MAX_VALUE)
                    .addGap(1, 1, 1)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(InputEmpNameTrckrHR)
                .addGap(34, 34, 34))
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(inputName, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(13, Short.MAX_VALUE)))
        );

        denyattndncbtnHR.setBackground(new java.awt.Color(207, 10, 10));
        denyattndncbtnHR.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        denyattndncbtnHR.setForeground(new java.awt.Color(255, 255, 255));
        denyattndncbtnHR.setText("Deny");
        denyattndncbtnHR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                denyattndncbtnHRActionPerformed(evt);
            }
        });

        findEmployeeBtn.setBackground(new java.awt.Color(220, 95, 0));
        findEmployeeBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        findEmployeeBtn.setForeground(new java.awt.Color(255, 255, 255));
        findEmployeeBtn.setText("Find employee");
        findEmployeeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findEmployeeBtnActionPerformed(evt);
            }
        });

        approveattndncbtnHR1.setBackground(new java.awt.Color(0, 153, 0));
        approveattndncbtnHR1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        approveattndncbtnHR1.setForeground(new java.awt.Color(255, 255, 255));
        approveattndncbtnHR1.setText("Approve");
        approveattndncbtnHR1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveattndncbtnHR1ActionPerformed(evt);
            }
        });

        approveAllBttn.setBackground(new java.awt.Color(0, 153, 0));
        approveAllBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        approveAllBttn.setForeground(new java.awt.Color(255, 255, 255));
        approveAllBttn.setText("Approve all");
        approveAllBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveAllBttnActionPerformed(evt);
            }
        });

        denyAllBttn.setBackground(new java.awt.Color(207, 10, 10));
        denyAllBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        denyAllBttn.setForeground(new java.awt.Color(255, 255, 255));
        denyAllBttn.setText("Deny all");
        denyAllBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                denyAllBttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 844, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(324, 324, 324)
                        .addComponent(findEmployeeBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(15, 18, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(241, 241, 241)
                .addComponent(approveAllBttn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(approveattndncbtnHR1)
                .addGap(45, 45, 45)
                .addComponent(denyAllBttn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(denyattndncbtnHR)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(findEmployeeBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(denyattndncbtnHR)
                    .addComponent(approveattndncbtnHR1)
                    .addComponent(approveAllBttn)
                    .addComponent(denyAllBttn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("ATTENDANCE TRACKING");

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
                .addContainerGap(531, Short.MAX_VALUE))
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 882, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 882, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 489, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //This handles the "approve" button action.
    private void findEmployeeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findEmployeeBtnActionPerformed
    // Get search criteria from text fields
    String idSearch = InputIDNo.getText().trim();
    String nameSearch = inputNameHR.getText().trim();
    
    // First check if ID search is provided
    if (!idSearch.isEmpty()) {
        filterByEmployeeID(idSearch);
    }
    // Then check if name search is provided
    else if (!nameSearch.isEmpty()) {
        filterByEmployeeName(nameSearch);
    }
    // If no search criteria, show all records
    else {
        JOptionPane.showMessageDialog(this,
            "Please enter an Employee ID or Name to search",
            "No Search Criteria", JOptionPane.INFORMATION_MESSAGE);
        updateTableModel(); // Show all records
    }
    }//GEN-LAST:event_findEmployeeBtnActionPerformed
    //This handles the "deny" button action.
    private void denyattndncbtnHRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyattndncbtnHRActionPerformed
        denySelectedAttendance();
    }//GEN-LAST:event_denyattndncbtnHRActionPerformed
    //This handles the "Name" search field.
    private void inputNameHRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputNameHRActionPerformed
        filterByEmployeeName(inputNameHR.getText());
    }//GEN-LAST:event_inputNameHRActionPerformed
    //This handles the "Employee ID" search field.
    private void InputIDNoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InputIDNoActionPerformed
        filterByEmployeeID(InputIDNo.getText());
    }//GEN-LAST:event_InputIDNoActionPerformed

    private void approveattndncbtnHR1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveattndncbtnHR1ActionPerformed
    approveSelectedAttendance();
    }//GEN-LAST:event_approveattndncbtnHR1ActionPerformed
    //This handles the "Approve all" button action.
    private void approveAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveAllBttnActionPerformed
    DefaultTableModel model = (DefaultTableModel) AttendanceTrckrHRTbl.getModel();
    int rowCount = model.getRowCount();
    
    if (rowCount == 0) {
        JOptionPane.showMessageDialog(this, 
            "No attendance records to approve", 
            "No Records", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    
    // Confirm the bulk action
    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to approve ALL " + rowCount + " attendance records shown in the table?",
        "Confirm Approve All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    
    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }
    
    // Track success and failure
    List<Integer> successIds = new ArrayList<>();
    List<Integer> failedIds = new ArrayList<>();
    
    // Process each row in the table
    for (int row = 0; row < rowCount; row++) {
        try {
            // Get the employee ID and date from the table
            String empId = model.getValueAt(row, 0).toString();
            String date = model.getValueAt(row, 3).toString();
            
            // Find the matching record ID
            int recordId = -1;
            for (AttendanceRecord record : attendanceRecords) {
                if (String.valueOf(record.getEmployeeID()).equals(empId) && 
                    record.getDate().equals(date)) {
                    recordId = record.getId();
                    break;
                }
            }
            
            // Skip records that are already approved
            boolean alreadyApproved = false;
            for (AttendanceRecord record : attendanceRecords) {
                if (record.getId() == recordId && "Approved".equals(record.getStatus())) {
                    alreadyApproved = true;
                    break;
                }
            }
            
            if (alreadyApproved) {
                continue; // Skip already approved records
            }
            
            // If record found, approve it
            if (recordId != -1) {
                // Polymorphic call to approval method
                boolean success = attendanceTracker.approveAttendance(recordId);
                
                if (success) {
                    // Update status in our records
                    for (AttendanceRecord record : attendanceRecords) {
                        if (record.getId() == recordId) {
                            record.setStatus("Approved");
                            break;
                        }
                    }
                    successIds.add(recordId);
                } else {
                    failedIds.add(recordId);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error approving record at row " + row + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // Refresh table to show updated statuses
    updateTableModel();
    
    // Show result message
    if (failedIds.isEmpty()) {
        if (successIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "All records were already approved.", 
                "No Action Needed", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Successfully approved " + successIds.size() + " attendance records", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, 
            "Approved " + successIds.size() + " records\n" +
            "Failed to approve " + failedIds.size() + " records", 
            "Partial Success", JOptionPane.WARNING_MESSAGE);
    }
    }//GEN-LAST:event_approveAllBttnActionPerformed
    //This handles the "Deny all" button action.
    private void denyAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyAllBttnActionPerformed
     DefaultTableModel model = (DefaultTableModel) AttendanceTrckrHRTbl.getModel();
    int rowCount = model.getRowCount();
    
    if (rowCount == 0) {
        JOptionPane.showMessageDialog(this, 
            "No attendance records to deny", 
            "No Records", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    
    // Get common reason for all denials
    String reason = JOptionPane.showInputDialog(this, 
        "Please provide a reason for denying ALL " + rowCount + " displayed attendance records:", 
        "Deny All Attendances", JOptionPane.QUESTION_MESSAGE);
    
    if (reason == null) {
        // User canceled the dialog
        return;
    }
    
    if (reason.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, 
            "A reason is required to deny attendance records", 
            "Reason Required", JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Confirm the bulk action
    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to deny ALL " + rowCount + " attendance records shown in the table?",
        "Confirm Deny All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    
    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }
    
    // Track success and failure
    List<Integer> successIds = new ArrayList<>();
    List<Integer> failedIds = new ArrayList<>();
    
    // Process each row in the table
    for (int row = 0; row < rowCount; row++) {
        try {
            // Get the employee ID and date from the table
            String empId = model.getValueAt(row, 0).toString();
            String date = model.getValueAt(row, 3).toString();
            
            // Find the matching record ID
            int recordId = -1;
            for (AttendanceRecord record : attendanceRecords) {
                if (String.valueOf(record.getEmployeeID()).equals(empId) && 
                    record.getDate().equals(date)) {
                    recordId = record.getId();
                    break;
                }
            }
            
            // Skip records that are already denied
            boolean alreadyDenied = false;
            for (AttendanceRecord record : attendanceRecords) {
                if (record.getId() == recordId && "Denied".equals(record.getStatus())) {
                    alreadyDenied = true;
                    break;
                }
            }
            
            if (alreadyDenied) {
                continue; // Skip already denied records
            }
            
            // If record found, deny it
            if (recordId != -1) {
                // Polymorphic call to denial method
                boolean success = attendanceTracker.denyAttendance(recordId, reason);
                
                if (success) {
                    // Update status in our records
                    for (AttendanceRecord record : attendanceRecords) {
                        if (record.getId() == recordId) {
                            record.setStatus("Denied");
                            break;
                        }
                    }
                    successIds.add(recordId);
                } else {
                    failedIds.add(recordId);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error denying record at row " + row + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // Refresh table
    updateTableModel();
    
    // Show result message
    if (failedIds.isEmpty()) {
        if (successIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "All records were already denied.", 
                "No Action Needed", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Successfully denied " + successIds.size() + " attendance records", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, 
            "Denied " + successIds.size() + " records\n" +
            "Failed to deny " + failedIds.size() + " records", 
            "Partial Success", JOptionPane.WARNING_MESSAGE);
    }
    }//GEN-LAST:event_denyAllBttnActionPerformed

    private void backattndncbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattndncbttnActionPerformed
    // Close this window
    dispose();
    
    // Create new dashboard with the current user object
    // Use getRole() to determine which screen to return to
    if (loggedInUser != null) {
        String role = loggedInUser.getRole().toUpperCase();
        if (role.equals("HR")) {
            new AdminHR(loggedInUser).setVisible(true);
        } else if (role.contains("SUPERVISOR")) {
            new AdminSupervisor(loggedInUser).setVisible(true);
        } else {
            // Fallback for other roles
            JOptionPane.showMessageDialog(null, 
                "Unknown role: " + loggedInUser.getRole(), 
                "Navigation Error", JOptionPane.ERROR_MESSAGE);
        }
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
            java.util.logging.Logger.getLogger(AttendanceTrackingGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AttendanceTrackingGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AttendanceTrackingGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AttendanceTrackingGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AttendanceTrackingGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable AttendanceTrckrHRTbl;
    private javax.swing.JLabel EmpNameTrckrHR;
    private javax.swing.JLabel IDNoTrckrHR;
    private javax.swing.JLabel InputEmpNameTrckrHR;
    private javax.swing.JLabel InputEmpNameTrckrHR1;
    private javax.swing.JTextField InputIDNo;
    private javax.swing.JButton approveAllBttn;
    private javax.swing.JButton approveattndncbtnHR1;
    private javax.swing.JButton backattndncbttn;
    private javax.swing.JButton denyAllBttn;
    private javax.swing.JButton denyattndncbtnHR;
    private javax.swing.JButton findEmployeeBtn;
    private javax.swing.JPanel inputName;
    private javax.swing.JTextField inputNameHR;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
