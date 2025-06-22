/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

import CSV.CSVDatabaseProcessor;
import java.awt.GridLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;
import oop.classes.actors.User;

/**
 * This class is where IT admins manages user credentials of all employees in the payroll system.
 * 
 * @author USER
 */
public class UserManagement extends javax.swing.JFrame {
    private User loggedInUser;
    private CSVDatabaseProcessor csvProcessor; // Handles CSV data
    private DefaultTableModel tableModel; // Manages table data

    public UserManagement(User user) {
        this.loggedInUser = user; //passed user details
        initComponents(); // Initialize GUI components
        csvProcessor = new CSVDatabaseProcessor(); // Create CSV processor
        csvProcessor.loadUserCredentialData(); // Load user credentials
        initializeTable(); // Populate table with user data
    }

    // Populate the table with user data from CSV
    private void initializeTable() {
        // Create the custom table model with column names
        String[] columnNames = {"Employee ID", "Last Name", "First Name", "Email", "Password"};
        tableModel = new PasswordProtectedTableModel(new Object[0][0], columnNames);
        UserMgmtTbl.setModel(tableModel);

        // Fetch all user credentials from CSV
        List<Map<String, String>> userCredentials = csvProcessor.getAllUserCredentialRecords();
        for (Map<String, String> user : userCredentials) {
            tableModel.addRow(new Object[]{
                user.get("Employee ID"),
                user.get("Last Name"),
                user.get("First Name"),
                user.get("Email"),
                user.get("Password")
            });
        }
    }


    // Filter table by employee ID
    private void filterByEmployeeID(String id) {
    tableModel.setRowCount(0); // Clear the table
    List<Map<String, String>> userCredentials = csvProcessor.getAllUserCredentialRecords();
    boolean foundMatch = false; // Track if any match is found

    for (Map<String, String> user : userCredentials) {
        if (user.get("Employee ID").equals(id)) { // Match exact ID
            tableModel.addRow(new Object[]{
                user.get("Employee ID"),
                user.get("Last Name"),
                user.get("First Name"),
                user.get("Email"),
                user.get("Password")
            });
            foundMatch = true; // At least one match found
            break; // Stop after finding the matching ID
        }
    }

    // If no match is found, show a message
    if (!foundMatch) {
        JOptionPane.showMessageDialog(this,
            "No employee found with ID: " + id,
            "No Match", JOptionPane.INFORMATION_MESSAGE);
    }
}

    // Filter table by employee name
    private void filterByEmployeeName(String name) {
    tableModel.setRowCount(0); // Clear the table
    List<Map<String, String>> userCredentials = csvProcessor.getAllUserCredentialRecords();
    boolean foundMatch = false; // Track if any match is found

    for (Map<String, String> user : userCredentials) {
        // Check if the name matches either first or last name (case-insensitive)
        if (user.get("First Name").toLowerCase().contains(name.toLowerCase()) ||
            user.get("Last Name").toLowerCase().contains(name.toLowerCase())) {
            tableModel.addRow(new Object[]{
                user.get("Employee ID"),
                user.get("Last Name"),
                user.get("First Name"),
                user.get("Email"),
                user.get("Password")
            });
            foundMatch = true; // At least one match found
        }
    }

    // If no match is found, show a message
    if (!foundMatch) {
        JOptionPane.showMessageDialog(this,
            "No employee found with name: " + name,
            "No Match", JOptionPane.INFORMATION_MESSAGE);
    }
}
    
        //Method to reset table to show all records
        private void updateTableModel() {
        tableModel.setRowCount(0); // Clear the table
        List<Map<String, String>> userCredentials = csvProcessor.getAllUserCredentialRecords();

        for (Map<String, String> user : userCredentials) {
            tableModel.addRow(new Object[]{
                user.get("Employee ID"),
                user.get("Last Name"),
                user.get("First Name"),
                user.get("Email"),
                user.get("Password")
            });
        }
    }
        
        // Get the next available employee ID
        private String getNextEmployeeID(String currentInput) {
            // If the input is a valid number, suggest the next available ID after it
            // Otherwise, find the next available ID after the highest existing one
            int startingID;

            try {
                startingID = Integer.parseInt(currentInput);
            } catch (NumberFormatException e) {
                // If input is not a valid number, find the highest existing ID
                startingID = getHighestEmployeeID();
            }

            // Start checking from the input ID or the highest existing ID
            int suggestedID = startingID + 1;

            // Keep incrementing until we find an unused ID
            while (isEmployeeIDDuplicate(String.valueOf(suggestedID))) {
                suggestedID++;
            }

            return String.valueOf(suggestedID);
        }
        
        private int getHighestEmployeeID() {
            List<Map<String, String>> userCredentials = csvProcessor.getAllUserCredentialRecords();
            int highestID = 0;

            for (Map<String, String> user : userCredentials) {
                try {
                    int currentID = Integer.parseInt(user.get("Employee ID"));
                    if (currentID > highestID) {
                        highestID = currentID;
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric IDs
                }
            }

            return highestID;
        }        

        // Check if an employee ID already exists
        private boolean isEmployeeIDDuplicate(String id) {
            // First, check if the ID exists in the current table model
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String existingID = (String) tableModel.getValueAt(i, 0); // Column 0 is Employee ID
                if (existingID.equals(id)) {
                    return true; // ID already exists in the current table
                }
            }

            // Second, check if the ID exists in the CSV data
            // Important in case there are records that haven't been loaded to the table yet
            List<Map<String, String>> userCredentials = csvProcessor.getAllUserCredentialRecords();
            for (Map<String, String> user : userCredentials) {
                if (user.get("Employee ID").equals(id)) {
                    return true; // ID already exists in the CSV data
                }
            }

            return false; // ID is unique
        }
 

        //To hide the passoword data in the jTable
        private class PasswordProtectedTableModel extends DefaultTableModel {
        public PasswordProtectedTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public Object getValueAt(int row, int column) {
            // If this is the password column (index 4), mask it with asterisks
            if (column == 4) {
                String password = (String) super.getValueAt(row, column);
                // Create a string of asterisks the same length as the password
                return "*".repeat(password.length());
            }
            return super.getValueAt(row, column);
        }

        // This important method ensures we can still get the real password when needed
        public String getRealPassword(int row) {
            return (String) super.getValueAt(row, 4);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Make all cells non-editable
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

        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        UserMgmtTbl = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        IDNoTrckrHR = new javax.swing.JLabel();
        InputIDNo = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        InputEmpNameTrckrHR = new javax.swing.JLabel();
        inputName = new javax.swing.JPanel();
        EmpNameTrckrHR = new javax.swing.JLabel();
        InputEmpNameTrckrHR1 = new javax.swing.JLabel();
        inputNameHR = new javax.swing.JTextField();
        editUserBtn = new javax.swing.JButton();
        findEmployeeBtn = new javax.swing.JButton();
        createNewUserBtn = new javax.swing.JButton();
        deleteUserBtn = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        backattndncbttn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(868, 442));

        UserMgmtTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Employee ID", "Last Name", "First Name", "Email", "Password"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(UserMgmtTbl);

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

        editUserBtn.setBackground(new java.awt.Color(220, 95, 0));
        editUserBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        editUserBtn.setForeground(new java.awt.Color(255, 255, 255));
        editUserBtn.setText("Edit user");
        editUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editUserBtnActionPerformed(evt);
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

        createNewUserBtn.setBackground(new java.awt.Color(0, 153, 0));
        createNewUserBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        createNewUserBtn.setForeground(new java.awt.Color(255, 255, 255));
        createNewUserBtn.setText("Create new user +");
        createNewUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNewUserBtnActionPerformed(evt);
            }
        });

        deleteUserBtn.setBackground(new java.awt.Color(207, 10, 10));
        deleteUserBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        deleteUserBtn.setForeground(new java.awt.Color(255, 255, 255));
        deleteUserBtn.setText("Delete user");
        deleteUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteUserBtnActionPerformed(evt);
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
                .addGap(306, 306, 306)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(createNewUserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(editUserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(deleteUserBtn)))
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
                .addComponent(createNewUserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(editUserBtn)
                    .addComponent(deleteUserBtn))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("USER MANAGEMENT");

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
                .addContainerGap(564, Short.MAX_VALUE))
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
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //This handles the "approve" button action.
    private void findEmployeeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findEmployeeBtnActionPerformed
        // Get search criteria from text fields
        String idSearch = InputIDNo.getText().trim(); // Get Employee ID input
        String nameSearch = inputNameHR.getText().trim(); // Get Name input

        // If both fields are empty, show a warning and reset the table
        if (idSearch.isEmpty() && nameSearch.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter an Employee ID or Name to search",
                "No Search Criteria", JOptionPane.INFORMATION_MESSAGE);
            updateTableModel(); // Reset table to show all records
            return; // Exit the method early
        }

        // If both ID and name are provided, prioritize ID search
        if (!idSearch.isEmpty() && !nameSearch.isEmpty()) {
            filterByEmployeeID(idSearch); // Search by ID first
            return; // Exit after ID search
        }

        // If only ID is provided, search by ID
        if (!idSearch.isEmpty()) {
            filterByEmployeeID(idSearch);
        }
        // If only name is provided, search by name
        else if (!nameSearch.isEmpty()) {
            filterByEmployeeName(nameSearch);
        }
    }//GEN-LAST:event_findEmployeeBtnActionPerformed
    //This handles the "edit user" button action.
    private void editUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editUserBtnActionPerformed
    int selectedRow = UserMgmtTbl.getSelectedRow(); // Get selected row
    if (selectedRow >= 0) {
        // Get current user details
        String id = (String) tableModel.getValueAt(selectedRow, 0);
        String lastName = (String) tableModel.getValueAt(selectedRow, 1);
        String firstName = (String) tableModel.getValueAt(selectedRow, 2);
        String email = (String) tableModel.getValueAt(selectedRow, 3);
        // Get the real password from our custom model
        String password = ((PasswordProtectedTableModel)tableModel).getRealPassword(selectedRow);
        
        // Create a panel for the input form
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2, 10, 10));
        
        // Create labels and text fields
        JLabel idLabel = new JLabel("Employee ID (cannot be changed):");
        JTextField idField = new JTextField(id);
        idField.setEditable(false);  // ID cannot be changed
        
        JLabel lastNameLabel = new JLabel("Last Name:");
        JTextField lastNameField = new JTextField(lastName, 10);
        
        JLabel firstNameLabel = new JLabel("First Name:");
        JTextField firstNameField = new JTextField(firstName, 10);
        
        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(email, 10);
        emailField.setEditable(true);  // Email will be auto-generated
        
        JLabel passwordLabel = new JLabel("Password:");
        // Use JTextField instead of JPasswordField to show the plaintext password
        JTextField passwordField = new JTextField(password, 10);
        
        // Add components to panel
        panel.add(idLabel);
        panel.add(idField);
        panel.add(lastNameLabel);
        panel.add(lastNameField);
        panel.add(firstNameLabel);
        panel.add(firstNameField);
        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        
        // Show the dialog
        int result = JOptionPane.showConfirmDialog(this, panel, "Edit User", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        // Process result if OK was clicked
        if (result == JOptionPane.OK_OPTION) {
            String newLastName = lastNameField.getText().trim();
            String newFirstName = firstNameField.getText().trim();
            String newEmail = emailField.getText().trim();
            String newPassword = passwordField.getText(); // Get text directly
            
            // Validate inputs
            if (newLastName.isEmpty() || newFirstName.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "All fields except email must be filled out.", 
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Update table with new values
            tableModel.setValueAt(newLastName, selectedRow, 1);
            tableModel.setValueAt(newFirstName, selectedRow, 2);
            tableModel.setValueAt(newEmail, selectedRow, 3);
            tableModel.setValueAt(newPassword, selectedRow, 4);
            
            // TODO: Save changes to CSV
            JOptionPane.showMessageDialog(this, 
                "User updated successfully:\nID: " + id + "\nName: " + newFirstName + " " + newLastName +
                "\nEmail: " + newEmail,
                "User Updated", JOptionPane.INFORMATION_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select a user to edit.", 
            "No User Selected", JOptionPane.WARNING_MESSAGE);
    }
    }//GEN-LAST:event_editUserBtnActionPerformed
    //This handles the "Name" search field.
    private void inputNameHRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputNameHRActionPerformed
           filterByEmployeeName(inputNameHR.getText());      
    }//GEN-LAST:event_inputNameHRActionPerformed
    //This handles the "Employee ID" search field.
    private void InputIDNoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InputIDNoActionPerformed
           filterByEmployeeID(InputIDNo.getText());
    }//GEN-LAST:event_InputIDNoActionPerformed
   //This handles the "create new user" button action.
    private void createNewUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNewUserBtnActionPerformed
        // Get the next available ID
    String suggestedID = getNextEmployeeID("");
    
    // Create a panel for the input form
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(5, 2, 10, 10));
    
    // Create labels and text fields
    JLabel idLabel = new JLabel("Employee ID (suggested: " + suggestedID + "):");
    JTextField idField = new JTextField(suggestedID);
    
    JLabel lastNameLabel = new JLabel("Last Name:");
    JTextField lastNameField = new JTextField(10);
    
    JLabel firstNameLabel = new JLabel("First Name:");
    JTextField firstNameField = new JTextField(10);
    
    JLabel emailLabel = new JLabel("Email:");
    JTextField emailField = new JTextField(10);
    emailField.setEditable(true);
    
    JLabel passwordLabel = new JLabel("Password:");
    // Use JTextField instead of JPasswordField to show plaintext
    JTextField passwordField = new JTextField(10);
    
    // Add components to panel
    panel.add(idLabel);
    panel.add(idField);
    panel.add(lastNameLabel);
    panel.add(lastNameField);
    panel.add(firstNameLabel);
    panel.add(firstNameField);
    panel.add(emailLabel);
    panel.add(emailField);
    panel.add(passwordLabel);
    panel.add(passwordField);
    
    // Show the dialog
    int result = JOptionPane.showConfirmDialog(this, panel, "Create New User", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
    // Process result if OK was clicked
    if (result == JOptionPane.OK_OPTION) {
        String id = idField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText(); // Get text directly
        
        // Validate inputs
        if (id.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "All fields except email must be filled out.", 
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check for duplicate ID with better validation and error message
        if (isEmployeeIDDuplicate(id)) {
            // Show error message for duplicate ID
            JOptionPane.showMessageDialog(this,
                "Employee ID " + id + " already exists in the system.\n" +
                "Please choose a different ID or use the suggested ID: " + suggestedID,
                "Duplicate Employee ID Error",
                JOptionPane.ERROR_MESSAGE);

            // Focus back on the ID field
            idField.requestFocus();
            idField.selectAll();

            // Don't proceed with user creation
            return;
        }
        
        // Add new user to table
        tableModel.addRow(new Object[]{id, lastName, firstName, email, password});
        
        // TODO: Save new user to CSV
        JOptionPane.showMessageDialog(this, 
            "New user created successfully:\nID: " + id + "\nName: " + firstName + " " + lastName +
            "\nEmail: " + email,
            "User Created", JOptionPane.INFORMATION_MESSAGE);
    }
    }//GEN-LAST:event_createNewUserBtnActionPerformed

   //This handles the "Delete user" button action.
    private void deleteUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteUserBtnActionPerformed
        int selectedRow = UserMgmtTbl.getSelectedRow(); // Get selected row
        if (selectedRow >= 0) {
            // Get user information for confirmation message
            String id = (String) tableModel.getValueAt(selectedRow, 0);
            String lastName = (String) tableModel.getValueAt(selectedRow, 1);
            String firstName = (String) tableModel.getValueAt(selectedRow, 2);
            
            // Confirm deletion with user
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete user: " + firstName + " " + lastName + " (ID: " + id + ")?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
            if (choice == JOptionPane.YES_OPTION) {
                tableModel.removeRow(selectedRow); // Remove row from table
                // TODO: Remove user from CSV
                JOptionPane.showMessageDialog(this, 
                    "User has been deleted successfully.", 
                    "User Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", 
                "No User Selected", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_deleteUserBtnActionPerformed

    private void backattndncbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattndncbttnActionPerformed
    // Close this window once back button's clicked
    dispose();
    
    // Open AdminIT Page with the logged-in user
    AdminIT adminITPage = new AdminIT(loggedInUser);
    adminITPage.setVisible(true);
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
            java.util.logging.Logger.getLogger(UserManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel EmpNameTrckrHR;
    private javax.swing.JLabel IDNoTrckrHR;
    private javax.swing.JLabel InputEmpNameTrckrHR;
    private javax.swing.JLabel InputEmpNameTrckrHR1;
    private javax.swing.JTextField InputIDNo;
    private javax.swing.JTable UserMgmtTbl;
    private javax.swing.JButton backattndncbttn;
    private javax.swing.JButton createNewUserBtn;
    private javax.swing.JButton deleteUserBtn;
    private javax.swing.JButton editUserBtn;
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
