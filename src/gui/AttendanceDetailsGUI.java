
package gui;

import CSV.CSVDatabaseProcessor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import oop.classes.actors.User;

/**
 * This class displays attendance details for the logged-in user
 * It can be accessed from different dashboards (AdminIT, AdminHR, etc.)
 */
public class AttendanceDetailsGUI extends javax.swing.JFrame {

    private CSVDatabaseProcessor csvProcessor;
    private String employeeId;
    private String employeeName;
    // Store the logged-in user for navigation back to the correct dashboard
    private User loggedInUser;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
   
    /**
     * Main constructor that takes a User object
     * This constructor should be used when navigating from dashboards
     * 
     * @param user The currently logged-in user
     */
    public AttendanceDetailsGUI(User user) {
        initComponents();
        
        // Store the user for back navigation
        this.loggedInUser = user;
        
        // Set employee ID from the user object
        this.employeeId = String.valueOf(user.getEmployeeID());
        
        // Set employee name
        this.employeeName = user.getFirstName() + " " + user.getLastName();
        
        // Center the form on screen
        this.setLocationRelativeTo(null);
        
        // Initialize CSV processor
        csvProcessor = new CSVDatabaseProcessor();
        
        // Load employee and attendance data
        csvProcessor.loadEmployeeCSVData();
        csvProcessor.loadAttendanceData();
        
        // Display employee info in the UI
        InputIDNo.setText(employeeId);
        InputEmpName.setText(employeeName);
        
        // Set window title
        setTitle("Attendance Details - " + employeeName);
        
        // Load attendance data
        loadAttendanceData();
        
        System.out.println("AttendanceDetailsGUI created for user: " + employeeName + " (ID: " + employeeId + ")");
    }

    /**
     * Default constructor for direct testing or from NetBeans form editor
     * This should NOT be used in production code
     */
    public AttendanceDetailsGUI() {
        initComponents();

        // Center the form on screen
        this.setLocationRelativeTo(null);

        // Initialize CSV processor
        csvProcessor = new CSVDatabaseProcessor();
        
        // For testing purposes only - use a default employee ID
        this.employeeId = "10001"; // Example ID for testing
        
        // Load CSV data
        csvProcessor.loadEmployeeCSVData();
        csvProcessor.loadAttendanceData();

        // Get employee name for the default ID
        Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);

        if (employeeData != null) {
            // Set employee info in the UI
            String firstName = employeeData.get("First Name");
            String lastName = employeeData.get("Last Name");
            employeeName = firstName + " " + lastName;

            InputIDNo.setText(employeeId);
            InputEmpName.setText(employeeName);

            // Load attendance data
            loadAttendanceData();
        } else {
            JOptionPane.showMessageDialog(this, "Employee data not found for ID: " + employeeId,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        System.out.println("AttendanceDetailsGUI created with default constructor (testing only)");
    }

    /**
     * Load attendance data from CSV into the table
     * This method filters data to only show the logged-in user's records
     */
    private void loadAttendanceData() {
        try {
            // Make sure attendance data is loaded
            csvProcessor.loadAttendanceData();

            // Get attendance records for the specific employee only
            List<Map<String, Object>> attendanceRecords = csvProcessor.getAttendanceRecordsByEmployeeId(employeeId);

            if (attendanceRecords == null || attendanceRecords.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No attendance records found for employee ID: " + employeeId,
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE);
                
                // Clear the table if no records
                DefaultTableModel model = (DefaultTableModel) AttendanceDetailsTbl.getModel();
                model.setRowCount(0);
                return;
            }

            // Get the table model and clear it
            DefaultTableModel model = (DefaultTableModel) AttendanceDetailsTbl.getModel();
            model.setRowCount(0); // Clear existing data

            // Add attendance records to the table
            for (Map<String, Object> record : attendanceRecords) {
                LocalDate date = (LocalDate) record.get("Date");
                LocalTime timeIn = (LocalTime) record.get("Log In");
                LocalTime timeOut = (LocalTime) record.get("Log Out");

                model.addRow(new Object[]{
                    employeeId,
                    date != null ? date.format(dateFormatter) : "N/A",
                    timeIn != null ? timeIn.format(timeFormatter) : "N/A",
                    timeOut != null ? timeOut.format(timeFormatter) : "N/A"
                });
            }
            
            System.out.println("Loaded " + attendanceRecords.size() + " attendance records for employee ID: " + employeeId);
        } catch (Exception e) {
            System.err.println("Error loading attendance data: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading attendance data: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Navigate back to the appropriate dashboard based on user role
     */
    private void goBack() {
        if (loggedInUser != null) {
            String role = loggedInUser.getRole();
            System.out.println("Navigating back based on role: " + role);
            
            try {
                switch (role) {
                    case "IT":
                        AdminIT adminIT = new AdminIT(loggedInUser);
                        adminIT.setVisible(true);
                        break;
                        
                    case "HR":
                        AdminHR adminHR = new AdminHR(loggedInUser);
                        adminHR.setVisible(true);
                        break;
                        
                    case "ACCOUNTING":
                        AdminAccounting adminAccounting = new AdminAccounting(loggedInUser);
                        adminAccounting.setVisible(true);
                        break;
                        
                    case "IMMEDIATE SUPERVISOR":
                        AdminSupervisor adminSupervisor = new AdminSupervisor(loggedInUser);
                        adminSupervisor.setVisible(true);
                        break;
                        
                    case "EMPLOYEE":
                        EmployeeSelfService employeeSelfService = new EmployeeSelfService(loggedInUser);
                        employeeSelfService.setVisible(true);
                        break;
                        
                    default:
                        // Unknown role - return to login screen
                        System.err.println("Unknown role: " + role + ", returning to login");
                        new Login().setVisible(true);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error navigating back: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Error navigating back: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                // Return to login as fallback
                new Login().setVisible(true);
            }
        } else {
            // No user data - return to login
            System.err.println("No user data available, returning to login");
            new Login().setVisible(true);
        }
        
        // Close this window regardless of navigation result
        this.dispose();
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
        backattnddtlsbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        AttendanceDetailsTbl = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        IDNo = new javax.swing.JLabel();
        InputIDNo = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        EmpName = new javax.swing.JLabel();
        InputEmpName = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("ATTENDANCE DETAILS");

        backattnddtlsbttn.setBackground(new java.awt.Color(207, 10, 10));
        backattnddtlsbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backattnddtlsbttn.setForeground(new java.awt.Color(255, 255, 255));
        backattnddtlsbttn.setText("Back");
        backattnddtlsbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backattnddtlsbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backattnddtlsbttn)
                .addGap(39, 39, 39)
                .addComponent(jLabel1)
                .addContainerGap(561, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backattnddtlsbttn))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(984, 442));

        AttendanceDetailsTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Employee ID", "Date", "Log In", "Log Out"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(AttendanceDetailsTbl);

        jPanel3.setBackground(new java.awt.Color(220, 95, 0));

        IDNo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        IDNo.setForeground(new java.awt.Color(255, 255, 255));
        IDNo.setText("ID #:");

        InputIDNo.setForeground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(IDNo)
                .addGap(18, 18, 18)
                .addComponent(InputIDNo, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IDNo)
                    .addComponent(InputIDNo))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(220, 95, 0));

        EmpName.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        EmpName.setForeground(new java.awt.Color(255, 255, 255));
        EmpName.setText("Name:");

        InputEmpName.setForeground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(EmpName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(InputEmpName, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EmpName)
                    .addComponent(InputEmpName))
                .addGap(14, 14, 14))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 844, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(20, 20, 20))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backattnddtlsbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattnddtlsbttnActionPerformed
      // Navigate back to appropriate dashboard based on user role
        goBack();
        this.dispose();        
    }//GEN-LAST:event_backattnddtlsbttnActionPerformed

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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AttendanceDetailsGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new AttendanceDetailsGUI().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable AttendanceDetailsTbl;
    private javax.swing.JLabel EmpName;
    private javax.swing.JLabel IDNo;
    private javax.swing.JLabel InputEmpName;
    private javax.swing.JLabel InputIDNo;
    private javax.swing.JButton backattnddtlsbttn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
