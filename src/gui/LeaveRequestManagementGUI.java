  
package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import oop.classes.actors.User;
import oop.classes.management.LeaveRequestManagement;
/**
 *This class implements the Leave Request Management interface functionality.
 * Implements polymorphic behavior for HR and Immediate Supervisor roles.
 * @author USER
 */

public class LeaveRequestManagementGUI extends javax.swing.JFrame {
    // Fields for polymorphic implementation
    private User loggedInUser;
    private LeaveRequestManagement leaveRequestManager;
    private List<LeaveRequestRecord> leaveRequestRecords = new ArrayList<>();
    private int selectedLeaveRequestID = -1;
    
    /**
     * Inner class to represent a leave request record
     */
    private static class LeaveRequestRecord {
        private final int id;
        private final int employeeID;
        private final String firstName;
        private final String lastName;
        private final String position;
        private final String status;
        private final String supervisor;
        private final String leaveType;
        private final String note;
        private final String startDate;
        private final String endDate;
        private String leaveStatus;
        private final String vlRemaining;
        private final String slRemaining;
        private final String date;
        
        public LeaveRequestRecord(int id, int employeeID, String firstName, String lastName, 
                                String position, String status, String supervisor, String leaveType,
                                String note, String startDate, String endDate, String leaveStatus,
                                String vlRemaining, String slRemaining, String date) {
            this.id = id;
            this.employeeID = employeeID;
            this.firstName = firstName;
            this.lastName = lastName;
            this.position = position;
            this.status = status;
            this.supervisor = supervisor;
            this.leaveType = leaveType;
            this.note = note;
            this.startDate = startDate;
            this.endDate = endDate;
            this.leaveStatus = leaveStatus;
            this.vlRemaining = vlRemaining;
            this.slRemaining = slRemaining;
            this.date = date;
        }
        
        // Getters
        public int getId() { return id; }
        public int getEmployeeID() { return employeeID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPosition() { return position; }
        public String getStatus() { return status; }
        public String getSupervisor() { return supervisor; }
        public String getLeaveType() { return leaveType; }
        public String getNote() { return note; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getLeaveStatus() { return leaveStatus; }
        public String getVlRemaining() { return vlRemaining; }
        public String getSlRemaining() { return slRemaining; }
        public String getDate() { return date; }
        
        
        // Setter for leave status
        public void setLeaveStatus(String leaveStatus) { this.leaveStatus = leaveStatus; }
    }

    /**
     * Creates new form PayrollManagement
     */
    public LeaveRequestManagementGUI() {
        initComponents();
        
        
    }

    /**
     * Constructor that initializes the GUI with the logged-in user.
     * It implements polymorphism by accepting any type of User that implements LeaveRequestManagement.
     * 
     * @param user The logged-in user (either HR or Immediate Supervisor)
     */
    public LeaveRequestManagementGUI(User user) {
        initComponents();
        
        // Add the action listener for employee ID combo box here
        employeeIDComboBox.addActionListener((java.awt.event.ActionEvent evt) -> {
            employeeIDComboBoxActionPerformed(evt);
        });
        
        this.loggedInUser = user;

        // Polymorphic assignment - cast to LeaveRequestManagement interface
        if (user instanceof LeaveRequestManagement) {
            this.leaveRequestManager = (LeaveRequestManagement) user;
        } else {
            throw new IllegalArgumentException("User must implement LeaveRequestManagement interface");
        }

        // Configure table selection listener
        configureTableSelection();

        // Load leave request data from CSV
        loadLeaveRequestData();
        
        
    }

    /**
     * Configure the table selection to update UI elements and track selected record
     */
    private void configureTableSelection() {
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jTable1.getSelectedRow() != -1) {
                int row = jTable1.getSelectedRow();

                // Verify there are rows in the model
                if (jTable1.getModel().getRowCount() == 0) {
                    return;
                }

                try {
                    // Get the employee ID and date from the selected row
                    Object empIdObj = jTable1.getValueAt(row, 1); // ID column (index 1)
                    Object dateObj = jTable1.getValueAt(row, 0);  // Date column (index 0)

                    // Find the record ID internally based on employee ID and date
                    if (empIdObj != null && dateObj != null) {
                        String empId = empIdObj.toString();
                        String date = dateObj.toString();

                        // Find the matching record
                        for (LeaveRequestRecord record : leaveRequestRecords) {
                            if (String.valueOf(record.getEmployeeID()).equals(empId) && 
                                record.getDate().equals(date)) {
                                selectedLeaveRequestID = record.getId();
                                break;
                            }
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
     * Load leave request data from CSV file
     * Implements polymorphic behavior by filtering data based on user role:
     * - HR can view all leave request records
     * - Immediate Supervisor can only view records of their subordinates
     */
    private void loadLeaveRequestData() {
        leaveRequestRecords.clear();
        String line;
        String csvSplitBy = ",";

        try {
            // Try all possible file paths to locate the CSV file
            String[] possiblePaths = {
                "OOP CSV Database - Leave Requests.csv",
                "src/CSV/OOP CSV Database - Leave Requests.csv",
                "./OOP CSV Database - Leave Requests.csv",
                "../OOP CSV Database - Leave Requests.csv"
            };

            File file = null;
            for (String path : possiblePaths) {
                file = new File(path);
                if (file.exists()) {
                    System.out.println("Found leave requests file at: " + path);
                    break;
                }
            }

            if (file == null || !file.exists()) {
                JOptionPane.showMessageDialog(this,
                    "Leave request file not found. Tried multiple paths.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Add debug output to help identify issues
            System.out.println("User role: " + (loggedInUser != null ? loggedInUser.getRole() : "null"));
            System.out.println("User name: " + 
                              (loggedInUser != null ? loggedInUser.getFirstName() + " " + loggedInUser.getLastName() : "null"));

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                // Skip header line
                br.readLine();

                int id = 1; // Use internal ID for tracking
                int recordsLoaded = 0;
                int recordsFiltered = 0;

                // Read each line from the CSV
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(csvSplitBy);
                    recordsLoaded++;

                    if (data.length >= 14) {
                        String date = data[0].trim();
                        String employeeID = data[1].trim();
                        String firstName = data[2].trim();
                        String lastName = data[3].trim();
                        String position = data[4].trim();
                        String status = data[5].trim();
                        String supervisor = data[6].trim();
                        String leaveType = data[7].trim();
                        String note = data[8].trim();
                        String startDate = data[9].trim();
                        String endDate = data[10].trim();
                        String leaveStatus = data[11].trim();
                        String vlRemaining = data[12].trim();
                        String slRemaining = data[13].trim();

                        // Apply polymorphic filtering based on user role
                        boolean shouldInclude = false;

                        // HR users see all records
                        if ("HR".equals(loggedInUser.getRole())) {
                            shouldInclude = true;
                            System.out.println("Including record for HR user (all records visible)");
                        }
                        
                        // Supervisors see records for their subordinates
                        else if ("IMMEDIATE SUPERVISOR".equals(loggedInUser.getRole())) {
                            // Get the full name of the supervisor in multiple formats for comparison
                            String supervisorFullName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
                            String supervisorLastFirst = loggedInUser.getLastName() + ", " + loggedInUser.getFirstName();
                            String supervisorLastFirstNoComma = loggedInUser.getLastName() + " " + loggedInUser.getFirstName();

                            // Normalize supervisor name from record for better comparison
                            String normalizedSupervisor = supervisor.replace(",", " ").trim();

                            // Debug output to see what we're comparing
                            System.out.println("  Record supervisor: '" + supervisor + "'");
                            System.out.println("  Normalized: '" + normalizedSupervisor + "'");
                            System.out.println("  Logged-in supervisor: '" + supervisorFullName + "' or '" + 
                                              supervisorLastFirst + "' or '" + supervisorLastFirstNoComma + "'");

                            // Comparison with case-insensitive contains checks
                            if (normalizedSupervisor.equalsIgnoreCase(supervisorFullName) ||
                                normalizedSupervisor.equalsIgnoreCase(supervisorLastFirst) ||
                                normalizedSupervisor.equalsIgnoreCase(supervisorLastFirstNoComma) ||
                                normalizedSupervisor.toLowerCase().contains(loggedInUser.getLastName().toLowerCase()) ||
                                supervisor.toLowerCase().contains(loggedInUser.getLastName().toLowerCase())) {

                                shouldInclude = true;
                                System.out.println("  -> Supervisor name matched!");
                            } else {
                                System.out.println("  -> Supervisor name did not match");
                            }
                        }
                        // Regular employees only see their own records
                        else {
                            String recordEmpId = employeeID; // We already parsed this above
                            String userEmpId = String.valueOf(loggedInUser.getEmployeeID());

                            if (recordEmpId.equals(userEmpId)) {
                                shouldInclude = true;
                                System.out.println("Including own record for employee ID: " + userEmpId);
                            }
                        }

                        if (shouldInclude) {
                            // Create a new leave request record and add to our collection
                            LeaveRequestRecord record = new LeaveRequestRecord(
                                id,
                                Integer.parseInt(employeeID),
                                firstName,
                                lastName,
                                position,
                                status,
                                supervisor,
                                leaveType,
                                note,
                                startDate,
                                endDate,
                                leaveStatus,
                                vlRemaining,
                                slRemaining,
                                date
                            );
                            leaveRequestRecords.add(record);
                            id++; // Increment record ID
                            recordsFiltered++;
                        }
                    }
                }

                System.out.println("Total records in CSV: " + recordsLoaded);
                System.out.println("Records after filtering: " + recordsFiltered);
                System.out.println("Records in leaveRequestRecords: " + leaveRequestRecords.size());

                // Initialize combo boxes with valid options
                initializeComboBoxes();

                // Update the table model after loading data
                updateTableModel();

                // Add a message if no data was loaded
                if (leaveRequestRecords.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "No leave request records found for your role.",
                        "No Data", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.out.println("Loaded " + leaveRequestRecords.size() + " leave request records.");
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error reading leave request data: " + e.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading leave request data: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Initialize combo boxes with appropriate values from loaded data
     */
    private void initializeComboBoxes() {
        // Initialize Employee ID combo box
        Set<String> employeeIDs = new HashSet<>();
        employeeIDs.add("All"); // Add "All" option
        
        for (LeaveRequestRecord record : leaveRequestRecords) {
            employeeIDs.add(String.valueOf(record.getEmployeeID()));
        }
        
        String[] employeeIDArray = employeeIDs.toArray(new String[0]);
        Arrays.sort(employeeIDArray); // Sort IDs
        
        // If "All" was added, make sure it's first
        if (employeeIDArray.length > 0 && !employeeIDArray[0].equals("All")) {
            // Remove "All" from wherever it is
            int allIndex = -1;
            for (int i = 0; i < employeeIDArray.length; i++) {
                if (employeeIDArray[i].equals("All")) {
                    allIndex = i;
                    break;
                }
            }
            
            if (allIndex != -1) {
                // Create new array with "All" at the beginning
                String[] newArray = new String[employeeIDArray.length];
                newArray[0] = "All";
                int newIndex = 1;
                for (int i = 0; i < employeeIDArray.length; i++) {
                    if (i != allIndex) {
                        newArray[newIndex++] = employeeIDArray[i];
                    }
                }
                employeeIDArray = newArray;
            }
        }
        
        employeeIDComboBox.setModel(new DefaultComboBoxModel<>(employeeIDArray));
        
        // Initialize Month combo box - keep standard months with "All" at beginning
        String[] months = {"All", "January", "February", "March", "April", "May", "June", 
                     "July", "August", "September", "October", "November", "December"};
        monthComboBox.setModel(new DefaultComboBoxModel<>(months));
        
        // Initialize Year combo box
        Set<String> years = new HashSet<>();
        years.add("All");
        
        for (LeaveRequestRecord record : leaveRequestRecords) {
            try {
                String[] dateParts = record.getDate().split("/");
                if (dateParts.length >= 3) {
                    years.add(dateParts[2]); // Year is the third part
                }
            } catch (Exception e) {
                System.err.println("Error parsing date: " + record.getDate());
            }
        }
        
        String[] yearArray = years.toArray(new String[0]);
        Arrays.sort(yearArray);
        
        // Make sure "All" is at the beginning
        if (yearArray.length > 0 && !yearArray[0].equals("All")) {
            // Find "All" in the array
            int allIndex = -1;
            for (int i = 0; i < yearArray.length; i++) {
                if (yearArray[i].equals("All")) {
                    allIndex = i;
                    break;
                }
            }
            
            if (allIndex != -1) {
                // Move "All" to the beginning
                String temp = yearArray[0];
                yearArray[0] = "All";
                yearArray[allIndex] = temp;
            }
        }
        
        yearComboBox.setModel(new DefaultComboBoxModel<>(yearArray));
    }

    /**
     * Filter leave requests based on combo box selections
     */
        private void filterLeaveRequests() {
            String selectedEmployeeID = employeeIDComboBox.getSelectedItem().toString();
            String selectedMonth = monthComboBox.getSelectedItem().toString();
            String selectedYear = yearComboBox.getSelectedItem().toString();

            // Create a new filtered list
            List<LeaveRequestRecord> filteredRecords = new ArrayList<>();

            for (LeaveRequestRecord record : leaveRequestRecords) {
                boolean matchesEmployeeID = selectedEmployeeID.equals("All") || 
                                          String.valueOf(record.getEmployeeID()).equals(selectedEmployeeID);

                // Extract month and year from the date (assuming format MM/DD/YYYY)
                boolean matchesMonth = true;
                boolean matchesYear = true;

                if (!selectedMonth.equals("All")) {
                    String dateStr = record.getDate();
                    try {
                        String[] dateParts = dateStr.split("/");
                        if (dateParts.length >= 3) {
                            int monthNum = Integer.parseInt(dateParts[0]);
                            String monthName = getMonthName(monthNum);
                            matchesMonth = selectedMonth.equals(monthName);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing date: " + dateStr);
                    }
                }

                if (!selectedYear.equals("All")) {
                    String dateStr = record.getDate();
                    try {
                        String[] dateParts = dateStr.split("/");
                        if (dateParts.length >= 3) {
                            String year = dateParts[2];
                            matchesYear = selectedYear.equals(year);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing date: " + dateStr);
                    }
                }

                if (matchesEmployeeID && matchesMonth && matchesYear) {
                    filteredRecords.add(record);
                }
            }

            // Update the table with filtered records
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("Date");
            model.addColumn("ID #");
            model.addColumn("First Name");
            model.addColumn("Last Name");
            model.addColumn("Position");
            model.addColumn("Status");
            model.addColumn("Supervisor");
            model.addColumn("Type of Leave");
            model.addColumn("Note");
            model.addColumn("Start");
            model.addColumn("End");
            model.addColumn("Leave Status");
            model.addColumn("VL Remaining");
            model.addColumn("SL Remaining");

            for (LeaveRequestRecord record : filteredRecords) {
                model.addRow(new Object[] {
                    record.getDate(),
                    record.getEmployeeID(),
                    record.getFirstName(),
                    record.getLastName(),
                    record.getPosition(),
                    record.getStatus(),
                    record.getSupervisor(),
                    record.getLeaveType(),
                    record.getNote(),
                    record.getStartDate(),
                    record.getEndDate(),
                    record.getLeaveStatus(),
                    record.getVlRemaining(),
                    record.getSlRemaining()
                });
            }

            jTable1.setModel(model);

            // Reapply the table configuration after setting the new model
            jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            // Set column widths for better display
            for (int i = 0; i < jTable1.getColumnCount(); i++) {
                switch (i) {
                    case 0: // Date
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(80);
                        break;
                    case 1: // ID
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(60);
                        break;
                    case 2: // First Name
                    case 3: // Last Name
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(100);
                        break;
                    case 4: // Position
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(150);
                        break;
                    case 5: // Status
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(80);
                        break;
                    case 6: // Supervisor
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(180);
                        break;
                    case 7: // Type of Leave
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(120);
                        break;
                    case 8: // Note
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(300); // Make wider to force scrolling
                        break;
                    case 9: // Start
                    case 10: // End
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(80);
                        break;
                    case 11: // Leave Status
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(100);
                        break;
                    case 12: // VL Remaining
                    case 13: // SL Remaining
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(100);
                        break;
                    default:
                        jTable1.getColumnModel().getColumn(i).setPreferredWidth(100);
                        break;
                }
            }

            // Ensure scrollbars appear when needed
            jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

    /**
     * Helper method to convert month number to month name
     */
    private String getMonthName(int monthNum) {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                         "July", "August", "September", "October", "November", "December"};
        if (monthNum >= 1 && monthNum <= 12) {
            return months[monthNum - 1];
        }
        return "";
    }

    /**
     * Update the table model with current leave request records
     */
    private void updateTableModel() {
        // Just trigger the filter function which will update the table
        filterLeaveRequests();
    }

    /**
     * Approve the selected leave request using polymorphism
     */
    private void approveSelectedLeaveRequest() {
        
        
        if (selectedLeaveRequestID == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a leave request to approve",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Polymorphic call to approval method
        boolean success = leaveRequestManager.approveLeaveRequest(selectedLeaveRequestID);

        if (success) {
            // Update status in our records
            for (LeaveRequestRecord record : leaveRequestRecords) {
                if (record.getId() == selectedLeaveRequestID) {
                    record.setLeaveStatus("Approved");
                    break;
                }
            }

            JOptionPane.showMessageDialog(this,
                "Leave request approved successfully",
                "Success", JOptionPane.INFORMATION_MESSAGE);

            // Refresh table
            updateTableModel();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to approve leave request",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        
    }

    /**
     * Deny the selected leave request using polymorphism
     */
    private void denySelectedLeaveRequest() {
        if (selectedLeaveRequestID == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a leave request to deny",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this,
            "Please provide a reason for denial:",
            "Deny Leave Request", JOptionPane.QUESTION_MESSAGE);

        if (reason == null) {
            // User canceled the dialog
            return;
        }

        if (reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "A reason is required to deny leave request",
                "Reason Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Polymorphic call to rejection method
        boolean success = leaveRequestManager.rejectLeaveRequest(selectedLeaveRequestID);

        if (success) {
            // Update status in our records
            for (LeaveRequestRecord record : leaveRequestRecords) {
                if (record.getId() == selectedLeaveRequestID) {
                    record.setLeaveStatus("Denied");
                    break;
                }
            }

            JOptionPane.showMessageDialog(this,
                "Leave request denied successfully",
                "Success", JOptionPane.INFORMATION_MESSAGE);

            // Refresh table
            updateTableModel();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to deny leave request",
                "Error", JOptionPane.ERROR_MESSAGE);
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
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        employeeIDComboBox = new javax.swing.JComboBox<>();
        monthComboBox = new javax.swing.JComboBox<>();
        yearComboBox = new javax.swing.JComboBox<>();
        Logo = new javax.swing.JLabel();
        approveattndncbtnHR1 = new javax.swing.JButton();
        approveAllBttn = new javax.swing.JButton();
        denyAllBttn = new javax.swing.JButton();
        denyattndncbtnHR = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Leave Request Management");

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
                .addGap(28, 28, 28)
                .addComponent(backattndncbttn)
                .addGap(37, 37, 37)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backattndncbttn))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Date", "ID #", "First Name", "Last Name", "Position", "Status", "Supervisor", "Type of Leave", "Note", "Start", "End", "Leave Status", "VL Remaining", "SL Remaining"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setText("Select Employee");

        jLabel3.setText("Select Month:");

        jLabel4.setText("Select Year:");

        employeeIDComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "10001", "10002", "10003", "10004", "10005", "10006", "10007", "10008", "10009", "10010", "10011", "10012", "10013", "10014", "10015", "10016", "10017", "10018", "10019", "10020", "10021", "10022", "10023", "10024", "10025" }));
        employeeIDComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                employeeIDComboBoxActionPerformed(evt);
            }
        });

        monthComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" }));
        monthComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monthComboBoxActionPerformed(evt);
            }
        });

        yearComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "2020", "2021", "2022", "2023", "2024", "2025" }));
        yearComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yearComboBoxActionPerformed(evt);
            }
        });

        Logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/media/MotorLogo 81x84.png"))); // NOI18N

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

        denyattndncbtnHR.setBackground(new java.awt.Color(207, 10, 10));
        denyattndncbtnHR.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        denyattndncbtnHR.setForeground(new java.awt.Color(255, 255, 255));
        denyattndncbtnHR.setText("Deny");
        denyattndncbtnHR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                denyattndncbtnHRActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1155, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(employeeIDComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING))
                                .addGap(49, 49, 49)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(monthComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(43, 43, 43)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(yearComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(Logo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(approveAllBttn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(approveattndncbtnHR1)
                        .addGap(84, 84, 84)
                        .addComponent(denyAllBttn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(denyattndncbtnHR)
                        .addGap(304, 304, 304))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(employeeIDComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(monthComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(yearComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Logo)
                        .addContainerGap(12, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(denyattndncbtnHR)
                            .addComponent(approveattndncbtnHR1)
                            .addComponent(approveAllBttn)
                            .addComponent(denyAllBttn))
                        .addGap(46, 46, 46))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //Month filter
    private void monthComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthComboBoxActionPerformed
    filterLeaveRequests();
    }//GEN-LAST:event_monthComboBoxActionPerformed
    //Year filter
    private void yearComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yearComboBoxActionPerformed
    filterLeaveRequests();
    }//GEN-LAST:event_yearComboBoxActionPerformed
    //Approve button
    private void approveattndncbtnHR1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveattndncbtnHR1ActionPerformed
         approveSelectedLeaveRequest();
    }//GEN-LAST:event_approveattndncbtnHR1ActionPerformed
   //Approve all button
    private void approveAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveAllBttnActionPerformed
      DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int rowCount = model.getRowCount();

        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this,
                "No leave requests to approve",
                "No Records", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Confirm the bulk action
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to approve ALL " + rowCount + " leave requests shown in the table?",
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
                String empId = model.getValueAt(row, 1).toString(); // ID column index 1
                String date = model.getValueAt(row, 0).toString();  // Date column index 0

                // Find the matching record ID
                int recordId = -1;
                for (LeaveRequestRecord record : leaveRequestRecords) {
                    if (String.valueOf(record.getEmployeeID()).equals(empId) &&
                        record.getDate().equals(date)) {
                        recordId = record.getId();
                        break;
                    }
                }

                // Skip records that are already approved
                boolean alreadyApproved = false;
                for (LeaveRequestRecord record : leaveRequestRecords) {
                    if (record.getId() == recordId && "Approved".equals(record.getLeaveStatus())) {
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
                    boolean success = leaveRequestManager.approveLeaveRequest(recordId);

                    if (success) {
                        // Update status in our records
                        for (LeaveRequestRecord record : leaveRequestRecords) {
                            if (record.getId() == recordId) {
                                record.setLeaveStatus("Approved");
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
                    "Successfully approved " + successIds.size() + " leave requests",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Approved " + successIds.size() + " records\n" +
                "Failed to approve " + failedIds.size() + " records",
                "Partial Success", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_approveAllBttnActionPerformed
   //Deny all button
    private void denyAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyAllBttnActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int rowCount = model.getRowCount();

        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this,
                "No leave requests to deny",
                "No Records", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Get common reason for all denials
        String reason = JOptionPane.showInputDialog(this,
            "Please provide a reason for denying ALL " + rowCount + " displayed leave requests:",
            "Deny All Leave Requests", JOptionPane.QUESTION_MESSAGE);

        if (reason == null) {
            // User canceled the dialog
            return;
        }

        if (reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "A reason is required to deny leave requests",
                "Reason Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm the bulk action
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to deny ALL " + rowCount + " leave requests shown in the table?",
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
                String empId = model.getValueAt(row, 1).toString(); // ID column index 1
                String date = model.getValueAt(row, 0).toString();  // Date column index 0

                // Find the matching record ID
                int recordId = -1;
                for (LeaveRequestRecord record : leaveRequestRecords) {
                    if (String.valueOf(record.getEmployeeID()).equals(empId) &&
                        record.getDate().equals(date)) {
                        recordId = record.getId();
                        break;
                    }
                }

                // Skip records that are already denied
                boolean alreadyDenied = false;
                for (LeaveRequestRecord record : leaveRequestRecords) {
                    if (record.getId() == recordId && "Denied".equals(record.getLeaveStatus())) {
                        alreadyDenied = true;
                        break;
                    }
                }

                if (alreadyDenied) {
                    continue; // Skip already denied records
                }

                // If record found, deny it
                if (recordId != -1) {
                    // Polymorphic call to rejection method
                    boolean success = leaveRequestManager.rejectLeaveRequest(recordId);

                    if (success) {
                        // Update status in our records
                        for (LeaveRequestRecord record : leaveRequestRecords) {
                            if (record.getId() == recordId) {
                                record.setLeaveStatus("Denied");
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
                    "Successfully denied " + successIds.size() + " leave requests",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Denied " + successIds.size() + " records\n" +
                "Failed to deny " + failedIds.size() + " records",
                "Partial Success", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_denyAllBttnActionPerformed
   //Deny Button
    private void denyattndncbtnHRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyattndncbtnHRActionPerformed
            denySelectedLeaveRequest();
    }//GEN-LAST:event_denyattndncbtnHRActionPerformed
   //Back button
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
    //Employee ID Filter
    private void employeeIDComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_employeeIDComboBoxActionPerformed
    filterLeaveRequests();
    }//GEN-LAST:event_employeeIDComboBoxActionPerformed

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
            java.util.logging.Logger.getLogger(LeaveRequestManagementGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LeaveRequestManagementGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LeaveRequestManagementGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LeaveRequestManagementGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new LeaveRequestManagementGUI().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Logo;
    private javax.swing.JButton approveAllBttn;
    private javax.swing.JButton approveattndncbtnHR1;
    private javax.swing.JButton backattndncbttn;
    private javax.swing.JButton denyAllBttn;
    private javax.swing.JButton denyattndncbtnHR;
    private javax.swing.JComboBox<String> employeeIDComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JComboBox<String> monthComboBox;
    private javax.swing.JComboBox<String> yearComboBox;
    // End of variables declaration//GEN-END:variables
}
