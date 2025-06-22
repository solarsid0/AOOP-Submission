package gui;

import CSV.CSVDatabaseProcessor;
import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import oop.classes.actors.Employee;
import oop.classes.actors.User;

/**
 * This class handles CRUD (create, read, update, & delete) operations on employee data
 * Integrates with CSVDatabaseProcessor to manage employee records
 */
public class EmployeeManagement extends javax.swing.JFrame {
    
    private String fullName; // Store the full name
    private String employeeID; // Store the employee ID from login page
    private String userRole; // Store the user role
    private CSVDatabaseProcessor csvProcessor;
    private List<Map<String, String>> employeeRecords;
    
    public EmployeeManagement(String fullName, int employeeID, String userRole) {
        try {
            initComponents();

            // Add tooltips to monetary fields
            addMonetaryFieldTooltips();

            // Applies wrapping to address field
            TFAddress.setLineWrap(true);
            TFAddress.setWrapStyleWord(true);

            // Makes horizontal scroll bar appear
            tblERecords.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            this.fullName = fullName; 
            this.employeeID = String.valueOf(employeeID);
            this.userRole = userRole;

            // Initialize the CSV processor and load data
            csvProcessor = new CSVDatabaseProcessor();
            csvProcessor.loadEmployeeCSVData();
            loadEmployeeData();

            // Setup department filter
            setupDepartmentFilter();

            // Add mouse listener to the table
            tblERecords.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    int selectedRow = tblERecords.getSelectedRow();
                    if (selectedRow != -1) {
                        displayEmployeeDetails(selectedRow);
                    }
                }
            });

            // Display instruction to user about not using commas in monetary fields
            JOptionPane.showMessageDialog(this, 
                "Please do not use commas when entering monetary values (salaries, allowances, etc.).\n" +
                "The system will automatically format these values for display.",
                "Important Instructions", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error initializing Employee Management: " + e.getMessage(), 
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Set up the department filter dropdown
     */
    private void setupDepartmentFilter() {
        departmentFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { 
            "All Departments", "Leadership", "HR", "IT", "Accounting", "Accounts", 
            "Sales and Marketing", "Supply Chain and Logistics", "Customer Service", "Other"
        }));
        
        departmentFilter.addActionListener(this::departmentFilterActionPerformed);
    }
    
    /**
     * Default constructor with added user instructions
     */
    public EmployeeManagement() {
        initComponents();

        // Add tooltips to monetary fields
        addMonetaryFieldTooltips();

        // Add text wrapping to address field
        TFAddress.setLineWrap(true);
        TFAddress.setWrapStyleWord(true);    

        // Add horizontal scroll bar
        tblERecords.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);  

        backbuttondetailsPB.addActionListener(this::backbuttondetailsPBActionPerformed);

        // Initialize fullName to an empty string or any default value
        this.fullName = "";
        this.employeeID = null;
        this.userRole = null;

        // Initialize the CSV processor and load data
        csvProcessor = new CSVDatabaseProcessor();
        csvProcessor.loadEmployeeCSVData();
        loadEmployeeData();

        // Setup department filter
        setupDepartmentFilter();

        // Add mouse listener to the table
        tblERecords.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = tblERecords.getSelectedRow();
                if (selectedRow != -1) {
                    displayEmployeeDetails(selectedRow);
                }
            }
        });

        // Display instruction to user about not using commas in monetary fields
        JOptionPane.showMessageDialog(this, 
            "Please do not use commas when entering monetary values (salaries, allowances, etc.).\n" +
            "The system will automatically format these values for display.",
            "Important Instructions", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Load employee data from CSVDatabaseProcessor into the table
     */
    private void loadEmployeeData() {
        // Get employee records from CSV processor
        employeeRecords = csvProcessor.getAllEmployeeRecords();

        // Clear existing table data
        DefaultTableModel model = (DefaultTableModel) tblERecords.getModel();
        model.setRowCount(0);

        // Loop through employee records and add to table model
        for (Map<String, String> record : employeeRecords) {
            Object[] rowData = new Object[18]; // 18 columns as per your table model

            // Basic employee information
            rowData[0] = record.get("Employee ID");
            rowData[1] = record.get("Last Name");
            rowData[2] = record.get("First Name");
            rowData[3] = record.get("Birthday");
            rowData[4] = record.get("Address");
            rowData[5] = record.get("Phone Number");
            rowData[6] = record.get("SSS #");
            rowData[7] = record.get("Philhealth #");
            rowData[8] = record.get("Pag-ibig #");
            rowData[9] = record.get("TIN #");
            rowData[10] = record.get("Status");
            rowData[11] = record.get("Position");
            rowData[12] = record.get("Immediate Supervisor");

            // Monetary values - format with commas for display
            String[] monetaryFields = {
                "Basic Salary", "Rice Subsidy", "Phone Allowance", 
                "Clothing Allowance", "Hourly Rate"
            };

            for (int i = 0; i < monetaryFields.length; i++) {
                String value = record.get(monetaryFields[i]);

                // If the value doesn't have commas but is numeric, format it with commas
                if (value != null && !value.contains(",") && value.matches("\\d+")) {
                    try {
                        // Parse as double and format with commas
                        double numValue = Double.parseDouble(value);
                        // Format with commas for thousands
                        value = String.format("%,.0f", numValue);
                    } catch (NumberFormatException e) {
                        // Just use the original value if parsing fails
                    }
                }

                rowData[13 + i] = value;
            }

            model.addRow(rowData);
        }

        // Adjust row heights and column widths
        adjustRowHeight();
        adjustColumnWidths();
    }
    
    /**
     * Display employee details in the form fields when a row is selected
     * @param selectedRow The selected row index
     */
    private void displayEmployeeDetails(int selectedRow) {
        try {
            TFenum.setText(tblERecords.getValueAt(selectedRow, 0).toString());
            TFlastn.setText(tblERecords.getValueAt(selectedRow, 1).toString());
            TFfirstn.setText(tblERecords.getValueAt(selectedRow, 2).toString());
            
            // Get the date string from the table and convert it to Date
            String dateString = tblERecords.getValueAt(selectedRow, 3).toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            try {
                Date date = dateFormat.parse(dateString);
                jDateChooserBday.setDate(date);
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(null, "Invalid date format: " + dateString);
            }
            
            TFAddress.setText(tblERecords.getValueAt(selectedRow, 4).toString());
            TFphonenum.setText(tblERecords.getValueAt(selectedRow, 5).toString());
            TFsss.setText(tblERecords.getValueAt(selectedRow, 6).toString());
            TFphilh.setText(tblERecords.getValueAt(selectedRow, 7).toString());
            TFpagibig.setText(tblERecords.getValueAt(selectedRow, 8).toString());
            TFtin.setText(tblERecords.getValueAt(selectedRow, 9).toString());
            TFstatus.setSelectedItem(tblERecords.getValueAt(selectedRow, 10).toString());
            TFpos.setText(tblERecords.getValueAt(selectedRow, 11).toString());
            TFsupervisor.setText(tblERecords.getValueAt(selectedRow, 12).toString());
            TFbasicsalary.setText(tblERecords.getValueAt(selectedRow, 13).toString());
            TFricesub.setText(tblERecords.getValueAt(selectedRow, 14).toString());
            TFphoneallow.setText(tblERecords.getValueAt(selectedRow, 15).toString());
            TFclothingallow.setText(tblERecords.getValueAt(selectedRow, 16).toString());
            TFhourlyrate.setText(tblERecords.getValueAt(selectedRow, 17).toString());
        } catch (HeadlessException e) {
            JOptionPane.showMessageDialog(this, "Error displaying employee details: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Adjust row heights to fit content
     */
    private void adjustRowHeight() {
        for (int row = 0; row < tblERecords.getRowCount(); row++) {
            int rowHeight = tblERecords.getRowHeight();
            for (int column = 0; column < tblERecords.getColumnCount(); column++) {
                Component comp = tblERecords.prepareRenderer(tblERecords.getCellRenderer(row, column), row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }
            tblERecords.setRowHeight(row, rowHeight);
        }
    }

    /**
     * Adjust column widths to fit content
     */
    private void adjustColumnWidths() {
        for (int column = 0; column < tblERecords.getColumnCount(); column++) {
            int width = getColumnWidth(column);
            tblERecords.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
    }
    
    /**
     * Get the preferred width of a column based on the content
     */
    private int getColumnWidth(int column) {
        int width = 0;
        TableCellRenderer headerRenderer = tblERecords.getTableHeader().getDefaultRenderer();
        Component headerComp = headerRenderer.getTableCellRendererComponent(tblERecords, tblERecords.getColumnModel().getColumn(column).getHeaderValue(), false, false, 0, column);
        width = Math.max(headerComp.getPreferredSize().width + tblERecords.getIntercellSpacing().width, width);
        for (int row = 0; row < tblERecords.getRowCount(); row++) {
            TableCellRenderer renderer = tblERecords.getCellRenderer(row, column);
            Component comp = tblERecords.prepareRenderer(renderer, row, column);
            width = Math.max(comp.getPreferredSize().width + tblERecords.getIntercellSpacing().width, width);
        }
        return width;
    }
    /**
     * Parse a CSV line, handling quoted fields
     */
    /**
     * Special parsing method for employee CSV lines which have a fixed format
     * and contain numeric fields with commas as thousands separators.
     */    /**
     * Check if employee ID already exists
     */
    private boolean isDuplicateEmployeeID(String empID) {
        DefaultTableModel model = (DefaultTableModel) tblERecords.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            String existingID = model.getValueAt(row, 0).toString();
            if (existingID.equals(empID)) {
                return true; // Found a duplicate Employee ID
            }
        }
        return false; // No duplicate Employee ID found
    }
    
    /**
     * Validate/Check user input
     * Only checks that required fields are filled
     */
    private boolean validateInput() {
        // Check that all required fields are filled
        return !TFenum.getText().isEmpty() &&
               !TFlastn.getText().isEmpty() &&
               !TFfirstn.getText().isEmpty() &&  
               jDateChooserBday.getDate() != null &&
               !TFAddress.getText().isEmpty() &&
               !TFphonenum.getText().isEmpty() &&
               !TFsss.getText().isEmpty() &&
               !TFphilh.getText().isEmpty() &&
               !TFpagibig.getText().isEmpty() &&
               !TFtin.getText().isEmpty() &&
               TFstatus.getSelectedItem() != null &&
               !TFpos.getText().isEmpty() &&
               !TFsupervisor.getText().isEmpty() &&
               !TFbasicsalary.getText().isEmpty() &&
               !TFricesub.getText().isEmpty() &&
               !TFphoneallow.getText().isEmpty() &&
               !TFclothingallow.getText().isEmpty() &&
               !TFhourlyrate.getText().isEmpty();
    }

    
    /**
     * Add a new employee to the table and records
     */
    private void addEmployee() {
        DefaultTableModel model = (DefaultTableModel) tblERecords.getModel();
        
        // Create a new map for the employee record
        Map<String, String> newEmployee = new HashMap<>();
        newEmployee.put("Employee ID", TFenum.getText());
        newEmployee.put("Last Name", TFlastn.getText());
        newEmployee.put("First Name", TFfirstn.getText());
        newEmployee.put("Birthday", jDateChooserBday.getDate() != null ? new SimpleDateFormat("MM/dd/yyyy").format(jDateChooserBday.getDate()) : "");
        newEmployee.put("Address", TFAddress.getText());
        newEmployee.put("Phone Number", TFphonenum.getText());
        newEmployee.put("SSS #", TFsss.getText());
        newEmployee.put("Philhealth #", TFphilh.getText());
        newEmployee.put("Pag-ibig #", TFpagibig.getText());
        newEmployee.put("TIN #", TFtin.getText());
        newEmployee.put("Status", TFstatus.getSelectedItem() != null ? TFstatus.getSelectedItem().toString() : "");
        newEmployee.put("Position", TFpos.getText());
        newEmployee.put("Immediate Supervisor", TFsupervisor.getText());
        newEmployee.put("Basic Salary", TFbasicsalary.getText().replace(",", ""));
        newEmployee.put("Rice Subsidy", TFricesub.getText().replace(",", ""));
        newEmployee.put("Phone Allowance", TFphoneallow.getText().replace(",", ""));
        newEmployee.put("Clothing Allowance", TFclothingallow.getText().replace(",", ""));
        newEmployee.put("Gross Semi-monthly Rate", "0.00"); // Default value
        newEmployee.put("Hourly Rate", TFhourlyrate.getText());
        
        // Add to employeeRecords list
        employeeRecords.add(newEmployee);
        
        // Add to table model
        model.addRow(new Object[]{
            TFenum.getText(),
            TFlastn.getText(),
            TFfirstn.getText(),
            jDateChooserBday.getDate() != null ? new SimpleDateFormat("MM/dd/yyyy").format(jDateChooserBday.getDate()) : "", // Birthday
            TFAddress.getText(), // Address
            TFphonenum.getText(),
            TFsss.getText(),
            TFphilh.getText(),
            TFpagibig.getText(),
            TFtin.getText(),
            TFstatus.getSelectedItem() != null ? TFstatus.getSelectedItem().toString() : "", 
            TFpos.getText(),
            TFsupervisor.getText(),
            TFbasicsalary.getText(),
            TFricesub.getText(),
            TFphoneallow.getText(),
            TFclothingallow.getText(),
            TFhourlyrate.getText()
        });
        
        clearFields();
    }
    
    /**
     * Clear form fields
     */
    private void clearFields() {
        TFenum.setText("");
        TFlastn.setText("");
        TFfirstn.setText("");
        TFsss.setText("");
        TFphilh.setText("");
        TFtin.setText("");
        TFpagibig.setText("");
        TFstatus.setSelectedIndex(0); // Reset to "Select"
        TFpos.setText("");
        TFsupervisor.setText("");
        TFbasicsalary.setText("");
        TFricesub.setText("");
        TFphoneallow.setText("");
        TFclothingallow.setText("");
        TFhourlyrate.setText("");
        jDateChooserBday.setDate(null); // Clear birthday field
        TFAddress.setText(""); // Clear address field
        TFphonenum.setText("");
    }
    
    /**
     * Save employee data to CSV file
     */
    private void saveToCSV() {
        try {
            // Get the file path - using the CSV processor's default path
            String filePath = "src/CSV/" + csvProcessor.getEmployeeDetailsFilePath();
            File file = new File(filePath);

            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.write("""
                             Employee ID,Last Name,First Name,Birthday,Address,Phone Number,SSS #,Philhealth #,TIN #,Pag-ibig #,Status,Position,Immediate Supervisor,Basic Salary,Rice Subsidy,Phone Allowance,Clothing Allowance,Gross Semi-monthly Rate,Hourly Rate
                             """);

                // Inside saveToCSV method, modify the writing loop:
                for (Map<String, String> employee : employeeRecords) {
                    StringBuilder line = new StringBuilder();

                    // Process each field, using quotes where needed
                    String[] fieldNames = {
                        "Employee ID", "Last Name", "First Name", "Birthday", 
                        "Address", "Phone Number", "SSS #", "Philhealth #", 
                        "TIN #", "Pag-ibig #", "Status", "Position", 
                        "Immediate Supervisor", "Basic Salary", "Rice Subsidy", 
                        "Phone Allowance", "Clothing Allowance", 
                        "Gross Semi-monthly Rate", "Hourly Rate"
                    };

                    for (int i = 0; i < fieldNames.length; i++) {
                        String fieldName = fieldNames[i];
                        String value = employee.getOrDefault(fieldName, "");

                        // Special handling for various field types
                        switch (fieldName) {
                            case "Address", "Immediate Supervisor" -> // These fields may contain commas, always quote them
                                line.append("\"").append(value).append("\"");
                            case "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Gross Semi-monthly Rate", "Hourly Rate" -> {
                                // Monetary values - preserve format with commas but ensure they're quoted
                                // Remove existing quotes first
                                value = value.replaceAll("^\"|\"$", "");
                                if (value.contains(",")) {
                                    line.append("\"").append(value).append("\"");
                                } else {
                                    line.append(value);
                                }
                            }
                            default -> {
                                // Handle other fields - quote only if they contain commas
                                if (value.contains(",")) {
                                    line.append("\"").append(value).append("\"");
                                } else {
                                    line.append(value);
                                }
                            }
                        }

                        // Add comma separator unless this is the last field
                        if (i < fieldNames.length - 1) {
                            line.append(",");
                        }
                    }

                    writer.write(line.toString() + "\n");
                }
            }

            // Reload employee data from the CSV
            csvProcessor.loadEmployeeCSVData();
            loadEmployeeData(); // Reload the table data

            JOptionPane.showMessageDialog(this, "Employee records saved successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving to CSV: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Determines the department based on the position
     * @param position The job title
     * @return The department name
     */
    private String getDepartmentFromPosition(String position) {
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
     * Add tooltips to monetary fields as a reminder not to use commas
     */
    private void addMonetaryFieldTooltips() {
        // Add tooltips to all monetary fields
        TFbasicsalary.setToolTipText("Enter amount without commas (e.g., 90000 not 90,000)");
        TFricesub.setToolTipText("Enter amount without commas (e.g., 1500 not 1,500)");
        TFphoneallow.setToolTipText("Enter amount without commas (e.g., 1500 not 1,500)");
        TFclothingallow.setToolTipText("Enter amount without commas (e.g., 2000 not 2,000)");
        TFhourlyrate.setToolTipText("Enter amount without commas (e.g., 1000 not 1,000)");
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tblERecords = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        backbuttondetailsPB = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        btnShowAll = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        TFenum = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        TFphilh = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        TFpos = new javax.swing.JTextField();
        lblphoneallow = new javax.swing.JLabel();
        TFphoneallow = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        TFlastn = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        TFtin = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        TFsupervisor = new javax.swing.JTextField();
        lblclothingallow = new javax.swing.JLabel();
        TFclothingallow = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        TFfirstn = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        TFpagibig = new javax.swing.JTextField();
        lblbasicsalary = new javax.swing.JLabel();
        TFbasicsalary = new javax.swing.JTextField();
        lblricesubsidy = new javax.swing.JLabel();
        TFricesub = new javax.swing.JTextField();
        TFhourlyrate = new javax.swing.JTextField();
        lbhourlyrate = new javax.swing.JLabel();
        TFsss = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        TFstatus = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        jDateChooserBday = new com.toedter.calendar.JDateChooser();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        TFAddress = new javax.swing.JTextArea();
        LBLphonenum = new javax.swing.JLabel();
        TFphonenum = new javax.swing.JTextField();
        btnSave = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnReset1 = new javax.swing.JButton();
        btnSearch1 = new javax.swing.JButton();
        departmentFilter = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        tblERecords.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Employee #", "Last Name", "First Name", "Birthday", "Address", "Phone #", "SSS #", "PhilHealth #", "Pag-Ibig #", "TIN", "Status", "Position", "Immediate Supervisor", "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Hourly Rate"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblERecords.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(tblERecords);
        if (tblERecords.getColumnModel().getColumnCount() > 0) {
            tblERecords.getColumnModel().getColumn(0).setMinWidth(60);
            tblERecords.getColumnModel().getColumn(0).setPreferredWidth(60);
            tblERecords.getColumnModel().getColumn(12).setPreferredWidth(60);
            tblERecords.getColumnModel().getColumn(13).setMinWidth(80);
            tblERecords.getColumnModel().getColumn(13).setPreferredWidth(80);
        }

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));

        backbuttondetailsPB.setBackground(new java.awt.Color(204, 0, 0));
        backbuttondetailsPB.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backbuttondetailsPB.setForeground(new java.awt.Color(255, 255, 255));
        backbuttondetailsPB.setText("Back");
        backbuttondetailsPB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backbuttondetailsPBActionPerformed(evt);
            }
        });

        jLabel14.setBackground(new java.awt.Color(220, 95, 0));
        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel14.setText("Employee Management");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backbuttondetailsPB)
                .addGap(30, 30, 30)
                .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 1049, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(17, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel14)
                    .addComponent(backbuttondetailsPB, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26))
        );

        btnShowAll.setBackground(new java.awt.Color(220, 95, 0));
        btnShowAll.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnShowAll.setForeground(new java.awt.Color(255, 255, 255));
        btnShowAll.setText("Show all");
        btnShowAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowAllActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Employee Number");

        TFenum.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel10.setText("PhilHealth Number");

        TFphilh.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel15.setText("Position");

        TFpos.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lblphoneallow.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblphoneallow.setText("Phone Allowance");

        TFphoneallow.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Last Name");

        TFlastn.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel11.setText("TIN");

        TFtin.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel16.setText("Immediate Supervisor");

        TFsupervisor.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lblclothingallow.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblclothingallow.setText("Clothing Allowance");

        TFclothingallow.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("First Name");

        TFfirstn.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel12.setText("Pag-Ibig Number");

        TFpagibig.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lblbasicsalary.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblbasicsalary.setText("Basic Salary");

        TFbasicsalary.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lblricesubsidy.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblricesubsidy.setText("Rice Subsidy");

        TFricesub.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        TFhourlyrate.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lbhourlyrate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbhourlyrate.setText("Hourly Rate");

        TFsss.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel9.setText("SSS Number");

        TFstatus.setEditable(true);
        TFstatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select", "Regular", "Probationary" }));
        TFstatus.setToolTipText("");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel13.setText("Status");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("Birthday");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setText("Address");

        TFAddress.setColumns(20);
        TFAddress.setRows(5);
        jScrollPane3.setViewportView(TFAddress);

        LBLphonenum.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        LBLphonenum.setText("Phone Number");

        TFphonenum.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        btnSave.setBackground(new java.awt.Color(0, 153, 0));
        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSave.setForeground(new java.awt.Color(255, 255, 255));
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnDelete.setBackground(new java.awt.Color(207, 10, 10));
        btnDelete.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnUpdate.setBackground(new java.awt.Color(220, 95, 0));
        btnUpdate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnUpdate.setForeground(new java.awt.Color(255, 255, 255));
        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnReset1.setBackground(new java.awt.Color(220, 95, 0));
        btnReset1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnReset1.setForeground(new java.awt.Color(255, 255, 255));
        btnReset1.setText("Reset");
        btnReset1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReset1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(161, 161, 161))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(TFtin, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(44, 44, 44)
                                .addComponent(TFphilh))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lblphoneallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(65, 65, 65))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(128, 128, 128))
                                    .addComponent(TFphoneallow)
                                    .addComponent(TFpos))
                                .addGap(47, 47, 47)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(31, 31, 31))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lblclothingallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(52, 52, 52))
                                    .addComponent(TFsupervisor)
                                    .addComponent(TFclothingallow, javax.swing.GroupLayout.Alignment.TRAILING)))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(105, 105, 105))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(TFenum, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(119, 119, 119))
                                    .addComponent(TFlastn)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addGap(53, 53, 53)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(lblricesubsidy, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(98, 98, 98))
                            .addComponent(TFbasicsalary, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TFpagibig, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TFricesub, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(62, 62, 62))
                            .addComponent(TFfirstn, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(46, 46, 46))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lblbasicsalary, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(180, 180, 180))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TFhourlyrate)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lbhourlyrate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(101, 101, 101))
                    .addComponent(TFsss)
                    .addComponent(TFstatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(137, 137, 137))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(121, 121, 121))
                    .addComponent(jDateChooserBday, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(47, 47, 47)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(135, 135, 135))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(LBLphonenum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(87, 87, 87))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TFphonenum)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnReset1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnDelete, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSave, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(25, 25, 25))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(TFenum, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                                    .addComponent(TFlastn)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TFfirstn, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TFtin, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                            .addComponent(TFphilh))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(TFpos, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblphoneallow, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(TFsupervisor, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(TFbasicsalary, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(TFsss))
                                .addGap(12, 12, 12)
                                .addComponent(lblclothingallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(4, 4, 4)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TFclothingallow)
                            .addComponent(TFphoneallow)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(98, 98, 98)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TFpagibig, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(TFstatus, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblbasicsalary, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(58, 58, 58)
                        .addComponent(lblricesubsidy, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TFricesub, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jDateChooserBday, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(37, 37, 37))
                                    .addComponent(jScrollPane3))
                                .addGap(18, 18, 18)
                                .addComponent(LBLphonenum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TFphonenum))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(43, 43, 43)))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbhourlyrate)
                            .addComponent(btnUpdate)
                            .addComponent(btnSave))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TFhourlyrate, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnDelete)
                            .addComponent(btnReset1))))
                .addGap(51, 51, 51))
        );

        btnSearch1.setBackground(new java.awt.Color(220, 95, 0));
        btnSearch1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSearch1.setForeground(new java.awt.Color(255, 255, 255));
        btnSearch1.setText("Search employee");
        btnSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearch1ActionPerformed(evt);
            }
        });

        departmentFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All Departments", "Accounting", "Accounts", "Customer Service", "Leadership", "HR", "IT", "Sales and Marketing", "Supply Chain and Logistics", "Other", " " }));
        departmentFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                departmentFilterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(departmentFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(54, 54, 54)
                                .addComponent(btnShowAll)
                                .addGap(18, 18, 18)
                                .addComponent(btnSearch1)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(23, 23, 23))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnShowAll)
                    .addComponent(btnSearch1)
                    .addComponent(departmentFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    //This button handles "save" button action
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
    if (validateInput()) {
        if (isDuplicateEmployeeID(TFenum.getText())) {
            JOptionPane.showMessageDialog(this, "Employee ID " + TFenum.getText() + " already exists.", 
                    "Duplicate Employee ID", JOptionPane.ERROR_MESSAGE);
        } else {
            addEmployee();
            saveToCSV();
            JOptionPane.showMessageDialog(this, "Employee saved successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please fill in all fields to save the record.", 
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    }//GEN-LAST:event_btnSaveActionPerformed
    //This button handles "delete" button action
    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        DefaultTableModel model = (DefaultTableModel) tblERecords.getModel();
        int selectedRow = tblERecords.getSelectedRow();
        
        if (selectedRow != -1) {
            // Get confirmation
            String empID = model.getValueAt(selectedRow, 0).toString();
            String firstName = model.getValueAt(selectedRow, 2).toString();
            String lastName = model.getValueAt(selectedRow, 1).toString();
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to delete employee " + empID + ": " + firstName + " " + lastName + "?", 
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                // Remove from employeeRecords list
                String employeeIDToRemove = model.getValueAt(selectedRow, 0).toString();
                for (int i = 0; i < employeeRecords.size(); i++) {
                    if (employeeRecords.get(i).get("Employee ID").equals(employeeIDToRemove)) {
                        employeeRecords.remove(i);
                        break;
                    }
                }
                
                // Remove from table
                model.removeRow(selectedRow);
                
                // Save changes to CSV
                saveToCSV();
                
                JOptionPane.showMessageDialog(this, "Employee deleted successfully!", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Clear the form fields
                clearFields();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDeleteActionPerformed
    //This button handles "reset" button action
    private void btnReset1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReset1ActionPerformed
        // clear all the text fields
        clearFields();
    }//GEN-LAST:event_btnReset1ActionPerformed
    //This button handles "update" button action
    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
    int selectedRow = tblERecords.getSelectedRow();
    
    if (selectedRow != -1) {
        // debug to check if validation is failing
        if (!validateInput()) {
            JOptionPane.showMessageDialog(this, "Please check your input fields.", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        DefaultTableModel model = (DefaultTableModel) tblERecords.getModel();
        
        // Get the current employee ID
        String currentID = model.getValueAt(selectedRow, 0).toString();
        String newID = TFenum.getText();
        
        // Check if employee ID is being changed and if the new ID already exists
        if (!currentID.equals(newID) && isDuplicateEmployeeID(newID)) {
            JOptionPane.showMessageDialog(this, "Employee ID " + newID + " already exists.", 
                    "Duplicate Employee ID", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Update the employee record in the employee records list
        Map<String, String> recordToUpdate = null;
        int recordIndex = -1;
        
        for (int i = 0; i < employeeRecords.size(); i++) {
            Map<String, String> record = employeeRecords.get(i);
            if (record.get("Employee ID").equals(currentID)) {
                recordToUpdate = record;
                recordIndex = i;
                break;
            }
        }
        
        if (recordToUpdate != null) {
            // Update the record
            recordToUpdate.put("Employee ID", TFenum.getText());
            recordToUpdate.put("Last Name", TFlastn.getText());
            recordToUpdate.put("First Name", TFfirstn.getText());
            recordToUpdate.put("Birthday", jDateChooserBday.getDate() != null ? 
                    new SimpleDateFormat("MM/dd/yyyy").format(jDateChooserBday.getDate()) : "");
            recordToUpdate.put("Address", TFAddress.getText());
            recordToUpdate.put("Phone Number", TFphonenum.getText());
            recordToUpdate.put("SSS #", TFsss.getText());
            recordToUpdate.put("Philhealth #", TFphilh.getText());
            recordToUpdate.put("Pag-ibig #", TFpagibig.getText());
            recordToUpdate.put("TIN #", TFtin.getText());
            recordToUpdate.put("Status", TFstatus.getSelectedItem() != null ? 
                    TFstatus.getSelectedItem().toString() : "");
            recordToUpdate.put("Position", TFpos.getText());
            recordToUpdate.put("Immediate Supervisor", TFsupervisor.getText());
            recordToUpdate.put("Basic Salary", TFbasicsalary.getText());
            recordToUpdate.put("Rice Subsidy", TFricesub.getText());
            recordToUpdate.put("Phone Allowance", TFphoneallow.getText());
            recordToUpdate.put("Clothing Allowance", TFclothingallow.getText());
            recordToUpdate.put("Hourly Rate", TFhourlyrate.getText());
            
            // Update the employee records list
            employeeRecords.set(recordIndex, recordToUpdate);
            
            // Update the table model
            model.setValueAt(TFenum.getText(), selectedRow, 0); 
            model.setValueAt(TFlastn.getText(), selectedRow, 1);
            model.setValueAt(TFfirstn.getText(), selectedRow, 2);
            model.setValueAt(jDateChooserBday.getDate() != null ? 
                    new SimpleDateFormat("MM/dd/yyyy").format(jDateChooserBday.getDate()) : "", selectedRow, 3);
            model.setValueAt(TFAddress.getText(), selectedRow, 4);
            model.setValueAt(TFphonenum.getText(), selectedRow, 5);
            model.setValueAt(TFsss.getText(), selectedRow, 6);
            model.setValueAt(TFphilh.getText(), selectedRow, 7);
            model.setValueAt(TFpagibig.getText(), selectedRow, 8);
            model.setValueAt(TFtin.getText(), selectedRow, 9);
            model.setValueAt(TFstatus.getSelectedItem() != null ? 
                    TFstatus.getSelectedItem().toString() : "", selectedRow, 10);
            model.setValueAt(TFpos.getText(), selectedRow, 11);
            model.setValueAt(TFsupervisor.getText(), selectedRow, 12);
            model.setValueAt(TFbasicsalary.getText(), selectedRow, 13);
            model.setValueAt(TFricesub.getText(), selectedRow, 14);
            model.setValueAt(TFphoneallow.getText(), selectedRow, 15);
            model.setValueAt(TFclothingallow.getText(), selectedRow, 16);
            model.setValueAt(TFhourlyrate.getText(), selectedRow, 17);
            
            // Save changes to CSV
            saveToCSV();
            
            JOptionPane.showMessageDialog(this, "Employee record updated successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select a row to update.", 
                "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnUpdateActionPerformed
    //This button handles "search" button action
    private void btnSearch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearch1ActionPerformed
        String searchQuery = JOptionPane.showInputDialog(this, "Enter search query:");
        
        if (searchQuery != null && !searchQuery.isEmpty()) {
            // Create a TableRowSorter for filtering
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(tblERecords.getModel());
            tblERecords.setRowSorter(sorter);
            
            // Create filters for multiple columns
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 0)); // Employee ID
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 1)); // Last Name
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 2)); // First Name
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 11)); // Position
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 12)); // Supervisor
            
            // Combine filters with OR condition (match any)
            RowFilter<Object, Object> combinedFilter = RowFilter.orFilter(filters);
            sorter.setRowFilter(combinedFilter);
            
            // Check if any results were found
            if (tblERecords.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No matching records found.", 
                        "Search Results", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // If search query is empty or cancelled, clear the filter
            tblERecords.setRowSorter(null);
        }
    }//GEN-LAST:event_btnSearch1ActionPerformed

    //This button handles "back" button action; also implements polymorphism as employee (child class)is treated as parent class object since user can't be initialized cause abstract 
    private void backbuttondetailsPBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backbuttondetailsPBActionPerformed
    // Check if user information is available
    if (fullName != null && !fullName.isEmpty() && 
        employeeID != null && userRole != null) {
        try {
            // Split the full name 
            String[] nameParts = fullName.split("\\s+");
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "";
            
            // Create an Employee object using the simplified constructor
            User backUser = new Employee(
                Integer.parseInt(employeeID),  // Employee ID
                firstName,                     // First name
                lastName,                      // Last name
                "",                            // Email (optional)
                "",                            // Password (optional)
                userRole                       // Role
            );
            
            // Open AdminHR with the User object
            new AdminHR(backUser).setVisible(true);
        } catch (NumberFormatException e) {
            // Fallback if parsing fails
            new Login().setVisible(true);
            // Log the error for debugging
        }
    } else {
        // Fallback if no user info
        new Login().setVisible(true);
    }
    
    // Close the current window
    this.dispose();
    }//GEN-LAST:event_backbuttondetailsPBActionPerformed
    //This button handles "show all" button action
    private void btnShowAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowAllActionPerformed
     // Clear any existing row sorter
    tblERecords.setRowSorter(null);
    
    // Reload the original data
    loadEmployeeData();       // TODO add your handling code here:
    }//GEN-LAST:event_btnShowAllActionPerformed
    //This button handles "dept. filter" button action
    private void departmentFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_departmentFilterActionPerformed
        String selectedDepartment = departmentFilter.getSelectedItem().toString();

        if ("All Departments".equals(selectedDepartment)) {
            tblERecords.setRowSorter(null);
            loadEmployeeData();
            return;
        }

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tblERecords.getModel());

        // Rename this variable to avoid shadowing the class field
        RowFilter<Object, Object> deptFilter = new RowFilter<Object, Object>() {
            @Override
            public boolean include(Entry<? extends Object, ? extends Object> entry) {
                String position = (String) entry.getValue(11); // Position column (index 11)
                String department = getDepartmentFromPosition(position);
                return selectedDepartment.equals(department);
            }
        };

        // Use the renamed variable here
        sorter.setRowFilter(deptFilter);
        tblERecords.setRowSorter(sorter);

        // Check if any results were found
        if (tblERecords.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No employees found in the " + selectedDepartment + " department.", 
                    "Filter Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_departmentFilterActionPerformed

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
            java.util.logging.Logger.getLogger(EmployeeManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new EmployeeManagement().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LBLphonenum;
    private javax.swing.JTextArea TFAddress;
    private javax.swing.JTextField TFbasicsalary;
    private javax.swing.JTextField TFclothingallow;
    private javax.swing.JTextField TFenum;
    private javax.swing.JTextField TFfirstn;
    private javax.swing.JTextField TFhourlyrate;
    private javax.swing.JTextField TFlastn;
    private javax.swing.JTextField TFpagibig;
    private javax.swing.JTextField TFphilh;
    private javax.swing.JTextField TFphoneallow;
    private javax.swing.JTextField TFphonenum;
    private javax.swing.JTextField TFpos;
    private javax.swing.JTextField TFricesub;
    private javax.swing.JTextField TFsss;
    private javax.swing.JComboBox<String> TFstatus;
    private javax.swing.JTextField TFsupervisor;
    private javax.swing.JTextField TFtin;
    private javax.swing.JButton backbuttondetailsPB;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnReset1;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSearch1;
    private javax.swing.JButton btnShowAll;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> departmentFilter;
    private com.toedter.calendar.JDateChooser jDateChooserBday;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbhourlyrate;
    private javax.swing.JLabel lblbasicsalary;
    private javax.swing.JLabel lblclothingallow;
    private javax.swing.JLabel lblphoneallow;
    private javax.swing.JLabel lblricesubsidy;
    private javax.swing.JTable tblERecords;
    // End of variables declaration//GEN-END:variables

    


}

       
