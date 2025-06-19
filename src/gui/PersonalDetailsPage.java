
package gui;

import CSV.CSVDatabaseProcessor;
import java.util.Map;
import javax.swing.JOptionPane;
import oop.classes.actors.User;

public class PersonalDetailsPage extends javax.swing.JFrame {

    private String employeeID;
    private String fullName;
    private String userRole;
    private User loggedInUser;
     
    /**
     * Creates a new PersonalDetailsPage for the specified user
     * This constructor is used from all dashboard screens (AdminIT, AdminHR, etc.)
     * @param user The currently logged in user
     */
    public PersonalDetailsPage(User user) {
        initComponents();
        
        // Store the user object for later use
        this.loggedInUser = user;
        
        // Get user details from the User object
        this.employeeID = String.valueOf(user.getEmployeeID());
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.userRole = user.getRole();
        
        // Set window title
        setTitle("Personal Details - " + fullName);
        
        // Center window on screen
        setLocationRelativeTo(null);
        
        // Debug output to trace execution
        System.out.println("PersonalDetailsPage constructor called for user: " + 
                           fullName + " (ID: " + employeeID + ", Role: " + userRole + ")");
        
        // Load and display the employee details
        loadEmployeeDetails();
    }
    
    /**
     * Default constructor required by NetBeans Form Editor
     * Not used directly in the application
     */
    public PersonalDetailsPage() {
        initComponents();
        System.out.println("PersonalDetailsPage default constructor called (not normally used)");
    }

    /**
     * Loads employee details from the database and displays them in the UI
     */
    private void loadEmployeeDetails() {
        try {
            // Debug output
            System.out.println("Loading employee details for ID: " + employeeID);
            
            // Create a new CSV processor to access employee data
            CSVDatabaseProcessor csvProcessor = new CSVDatabaseProcessor();
            
            // Get employee record by ID
            Map<String, String> employeeRecord = csvProcessor.getEmployeeRecordsByEmployeeId(employeeID);
            
            if (employeeRecord != null) {
                // Successfully found employee record - display the data
                System.out.println("Found employee record for ID: " + employeeID);
                
                // Update UI labels with employee data
                inputempidLBL.setText(employeeRecord.get("Employee ID"));
                inputfirstnameLBL.setText(employeeRecord.get("First Name"));
                inputlastnameLBL.setText(employeeRecord.get("Last Name"));
                inputbdayLBL.setText(employeeRecord.get("Birthday"));
                inputaddressLBL.setText(employeeRecord.get("Address"));
                inputphonenumLBL.setText(employeeRecord.get("Phone Number"));
                inputstatusLBL.setText(employeeRecord.get("Status"));
                inputpositionLBL.setText(employeeRecord.get("Position"));
                
                // Handle supervisor name carefully (it might be null or empty)
                String supervisorName = employeeRecord.get("Immediate Supervisor");
                if (supervisorName != null && !supervisorName.isEmpty() && !supervisorName.equals("N/A")) {
                    inputsupervisorLBL.setText(supervisorName);
                } else {
                    inputsupervisorLBL.setText("None");
                }
            } else {
                // No employee record found - use fallback values
                System.err.println("No employee record found for ID: " + employeeID);
                
                // Show error message to user
                JOptionPane.showMessageDialog(this, 
                    "Could not find detailed employee record for ID: " + employeeID + "\nShowing basic information only.", 
                    "Employee Data Not Found", JOptionPane.WARNING_MESSAGE);
                
                // Use basic info from User object as fallback
                inputempidLBL.setText(employeeID);
                inputfirstnameLBL.setText(loggedInUser.getFirstName());
                inputlastnameLBL.setText(loggedInUser.getLastName());
                inputbdayLBL.setText("Not available");
                inputaddressLBL.setText("Not available");
                inputphonenumLBL.setText("Not available");
                inputstatusLBL.setText("Not available");
                inputpositionLBL.setText(userRole); // Use role as position
                inputsupervisorLBL.setText("Not available");
            }
        } catch (Exception e) {
            // Handle any exceptions that occur during data loading
            System.err.println("Error loading employee details: " + e.getMessage());
            e.printStackTrace();
            
            // Show error message to user
            JOptionPane.showMessageDialog(this, 
                "Error loading employee details: " + e.getMessage(), 
                "Data Error", JOptionPane.ERROR_MESSAGE);
            
            // Use basic info as fallback
            inputempidLBL.setText(employeeID);
            inputfirstnameLBL.setText(loggedInUser.getFirstName());
            inputlastnameLBL.setText(loggedInUser.getLastName());
            inputbdayLBL.setText("Error loading data");
            inputaddressLBL.setText("Error loading data");
            inputphonenumLBL.setText("Error loading data");
            inputstatusLBL.setText("Error loading data");
            inputpositionLBL.setText(userRole); // Use role as position
            inputsupervisorLBL.setText("Error loading data");
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

        detailsmainPNL = new javax.swing.JPanel();
        detailsheaderPNL = new javax.swing.JPanel();
        detailsheaderLBL = new javax.swing.JLabel();
        backtoemppagePB = new javax.swing.JButton();
        infoboxPNL = new javax.swing.JPanel();
        dtfirstnameLBL = new javax.swing.JLabel();
        dtlastnameLBL = new javax.swing.JLabel();
        dtbdayLBL = new javax.swing.JLabel();
        dtphonenumLBL = new javax.swing.JLabel();
        dtstatusLBL = new javax.swing.JLabel();
        dtpositionLBL = new javax.swing.JLabel();
        dtsupervisorLBL = new javax.swing.JLabel();
        inputfirstnameLBL = new javax.swing.JLabel();
        inputlastnameLBL = new javax.swing.JLabel();
        inputbdayLBL = new javax.swing.JLabel();
        inputphonenumLBL = new javax.swing.JLabel();
        inputstatusLBL = new javax.swing.JLabel();
        inputpositionLBL = new javax.swing.JLabel();
        inputsupervisorLBL = new javax.swing.JLabel();
        dtemployeeidLBL = new javax.swing.JLabel();
        inputempidLBL = new javax.swing.JLabel();
        dtaddressLBL = new javax.swing.JLabel();
        inputaddressLBL = new javax.swing.JLabel();
        detailsiconPNL = new javax.swing.JPanel();
        icondetailsLBL = new javax.swing.JLabel();
        notedetailsLBL = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        detailsmainPNL.setBackground(new java.awt.Color(255, 255, 255));

        detailsheaderPNL.setBackground(new java.awt.Color(220, 95, 0));

        detailsheaderLBL.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        detailsheaderLBL.setForeground(new java.awt.Color(255, 255, 255));
        detailsheaderLBL.setText("EMPLOYEE'S PERSONAL DETAILS");

        backtoemppagePB.setBackground(new java.awt.Color(204, 0, 0));
        backtoemppagePB.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backtoemppagePB.setForeground(new java.awt.Color(255, 255, 255));
        backtoemppagePB.setText("Back");
        backtoemppagePB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backtoemppagePBActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout detailsheaderPNLLayout = new javax.swing.GroupLayout(detailsheaderPNL);
        detailsheaderPNL.setLayout(detailsheaderPNLLayout);
        detailsheaderPNLLayout.setHorizontalGroup(
            detailsheaderPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsheaderPNLLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(backtoemppagePB)
                .addGap(249, 249, 249)
                .addComponent(detailsheaderLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        detailsheaderPNLLayout.setVerticalGroup(
            detailsheaderPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsheaderPNLLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(detailsheaderPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(detailsheaderLBL)
                    .addComponent(backtoemppagePB))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        infoboxPNL.setBackground(new java.awt.Color(255, 255, 255));
        infoboxPNL.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        dtfirstnameLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtfirstnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtfirstnameLBL.setText("First Name:");

        dtlastnameLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtlastnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtlastnameLBL.setText("Last Name:");

        dtbdayLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtbdayLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtbdayLBL.setText("Birthday");

        dtphonenumLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtphonenumLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtphonenumLBL.setText("Phone Number:");

        dtstatusLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtstatusLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtstatusLBL.setText("Status:");

        dtpositionLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtpositionLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtpositionLBL.setText("Position:");

        dtsupervisorLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtsupervisorLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtsupervisorLBL.setText("Immediate Supervisor:");

        inputfirstnameLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputfirstnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputfirstnameLBL.setText(". . .");

        inputlastnameLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputlastnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputlastnameLBL.setText(". . .");

        inputbdayLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputbdayLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputbdayLBL.setText(". . .");

        inputphonenumLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputphonenumLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputphonenumLBL.setText(". . .");

        inputstatusLBL.setFont(new java.awt.Font("Helvetica", 1, 14)); // NOI18N
        inputstatusLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputstatusLBL.setText(". . .");

        inputpositionLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputpositionLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputpositionLBL.setText(". . .");

        inputsupervisorLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputsupervisorLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputsupervisorLBL.setText(". . .");

        dtemployeeidLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtemployeeidLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtemployeeidLBL.setText("Employee ID:");

        inputempidLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputempidLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputempidLBL.setText(". . .");

        dtaddressLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtaddressLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtaddressLBL.setText("Address");

        inputaddressLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputaddressLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputaddressLBL.setText(". . .");

        javax.swing.GroupLayout infoboxPNLLayout = new javax.swing.GroupLayout(infoboxPNL);
        infoboxPNL.setLayout(infoboxPNLLayout);
        infoboxPNLLayout.setHorizontalGroup(
            infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoboxPNLLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(infoboxPNLLayout.createSequentialGroup()
                        .addComponent(dtsupervisorLBL)
                        .addGap(18, 18, 18)
                        .addComponent(inputsupervisorLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(infoboxPNLLayout.createSequentialGroup()
                        .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dtfirstnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtlastnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtbdayLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtphonenumLBL)
                            .addComponent(dtstatusLBL)
                            .addComponent(dtpositionLBL)
                            .addComponent(dtemployeeidLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtaddressLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(66, 66, 66)
                        .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(inputempidLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputpositionLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputstatusLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputphonenumLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputbdayLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputlastnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputfirstnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputaddressLBL, javax.swing.GroupLayout.DEFAULT_SIZE, 523, Short.MAX_VALUE))))
                .addContainerGap())
        );
        infoboxPNLLayout.setVerticalGroup(
            infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoboxPNLLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtemployeeidLBL)
                    .addComponent(inputempidLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtfirstnameLBL)
                    .addComponent(inputfirstnameLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtlastnameLBL)
                    .addComponent(inputlastnameLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtbdayLBL)
                    .addComponent(inputbdayLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtaddressLBL)
                    .addComponent(inputaddressLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtphonenumLBL)
                    .addComponent(inputphonenumLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtstatusLBL)
                    .addComponent(inputstatusLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtpositionLBL)
                    .addComponent(inputpositionLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtsupervisorLBL)
                    .addComponent(inputsupervisorLBL))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        detailsiconPNL.setBackground(new java.awt.Color(220, 95, 0));

        icondetailsLBL.setIcon(new javax.swing.ImageIcon(getClass().getResource("/media/USER 128 X 128.png"))); // NOI18N

        javax.swing.GroupLayout detailsiconPNLLayout = new javax.swing.GroupLayout(detailsiconPNL);
        detailsiconPNL.setLayout(detailsiconPNLLayout);
        detailsiconPNLLayout.setHorizontalGroup(
            detailsiconPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsiconPNLLayout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(icondetailsLBL)
                .addContainerGap(38, Short.MAX_VALUE))
        );
        detailsiconPNLLayout.setVerticalGroup(
            detailsiconPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsiconPNLLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(icondetailsLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        notedetailsLBL.setFont(new java.awt.Font("Helvetica", 3, 12)); // NOI18N
        notedetailsLBL.setForeground(new java.awt.Color(51, 51, 51));
        notedetailsLBL.setText("Please contact HR for any revisions.");

        javax.swing.GroupLayout detailsmainPNLLayout = new javax.swing.GroupLayout(detailsmainPNL);
        detailsmainPNL.setLayout(detailsmainPNLLayout);
        detailsmainPNLLayout.setHorizontalGroup(
            detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(detailsheaderPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, detailsmainPNLLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(detailsiconPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(notedetailsLBL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                .addComponent(infoboxPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );
        detailsmainPNLLayout.setVerticalGroup(
            detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsmainPNLLayout.createSequentialGroup()
                .addComponent(detailsheaderPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailsmainPNLLayout.createSequentialGroup()
                        .addComponent(infoboxPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 25, Short.MAX_VALUE))
                    .addGroup(detailsmainPNLLayout.createSequentialGroup()
                        .addComponent(detailsiconPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(31, 31, 31)
                        .addComponent(notedetailsLBL)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(detailsmainPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(detailsmainPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //back button
    private void backtoemppagePBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backtoemppagePBActionPerformed
        if (loggedInUser == null) {
            // No user data available - return to login screen
            System.err.println("No user data available, returning to login");
            JOptionPane.showMessageDialog(this, 
                "Session error. Please log in again.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            new Login().setVisible(true);
            this.dispose();
            return;
        }
        
        // Get user role to determine which dashboard to return to
        String role = loggedInUser.getRole();
        System.out.println("Navigating back to dashboard for role: " + role);
        
        try {
            // Navigate to the appropriate dashboard based on role
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
                    // Unknown role - show warning and go to login screen
                    System.err.println("Unknown role: " + role);
                    JOptionPane.showMessageDialog(this, 
                        "Unknown user role: " + role + "\nReturning to login screen.", 
                        "Navigation Error", JOptionPane.WARNING_MESSAGE);
                    new Login().setVisible(true);
                    break;
            }
            
            // Close this window after opening the destination
            this.dispose();
            
        } catch (Exception e) {
            // Handle any errors during navigation
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            
            JOptionPane.showMessageDialog(this, 
                "Error returning to dashboard: " + e.getMessage() + "\nPlease restart the application.", 
                "Navigation Error", JOptionPane.ERROR_MESSAGE);
        }
    
    }//GEN-LAST:event_backtoemppagePBActionPerformed
    
    
    

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
            java.util.logging.Logger.getLogger(PersonalDetailsPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new PersonalDetailsPage().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backtoemppagePB;
    private javax.swing.JLabel detailsheaderLBL;
    private javax.swing.JPanel detailsheaderPNL;
    private javax.swing.JPanel detailsiconPNL;
    private javax.swing.JPanel detailsmainPNL;
    private javax.swing.JLabel dtaddressLBL;
    private javax.swing.JLabel dtbdayLBL;
    private javax.swing.JLabel dtemployeeidLBL;
    private javax.swing.JLabel dtfirstnameLBL;
    private javax.swing.JLabel dtlastnameLBL;
    private javax.swing.JLabel dtphonenumLBL;
    private javax.swing.JLabel dtpositionLBL;
    private javax.swing.JLabel dtstatusLBL;
    private javax.swing.JLabel dtsupervisorLBL;
    private javax.swing.JLabel icondetailsLBL;
    private javax.swing.JPanel infoboxPNL;
    private javax.swing.JLabel inputaddressLBL;
    private javax.swing.JLabel inputbdayLBL;
    private javax.swing.JLabel inputempidLBL;
    private javax.swing.JLabel inputfirstnameLBL;
    private javax.swing.JLabel inputlastnameLBL;
    private javax.swing.JLabel inputphonenumLBL;
    private javax.swing.JLabel inputpositionLBL;
    private javax.swing.JLabel inputstatusLBL;
    private javax.swing.JLabel inputsupervisorLBL;
    private javax.swing.JLabel notedetailsLBL;
    // End of variables declaration//GEN-END:variables
}
