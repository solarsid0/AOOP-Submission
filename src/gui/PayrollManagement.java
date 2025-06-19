/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

import oop.classes.actors.User;
import oop.classes.actors.Employee;
import oop.classes.calculations.SalaryCalculation;
import oop.classes.calculations.DeductionCalculation;
import oop.classes.empselfservice.Payslip;
import CSV.CSVDatabaseProcessor;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.time.YearMonth;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
/**
 * This class is the payroll details overview of all employees. Managed by Accounting for approval & disbursement
 * @author USER
 */
public class PayrollManagement extends javax.swing.JFrame {

    private User loggedInUser;
    private SalaryCalculation salaryCalculation;
    private DeductionCalculation deductionCalculation;
    private CSVDatabaseProcessor csvProcessor;
    private YearMonth currentPayrollMonth;
    private boolean payslipsGenerated = false;
    private boolean payrollApproved = false;

    /**
     * Constructor initializes the payroll management form.
     * @param user The logged-in user.
     */
    public PayrollManagement(User user) {
        this.loggedInUser = user;
        this.salaryCalculation = new SalaryCalculation();
        this.deductionCalculation = new DeductionCalculation();
        this.csvProcessor = new CSVDatabaseProcessor();

        // Load attendance data - important for payroll calculations
        this.csvProcessor.loadAttendanceData();

        // Initialize to current payroll month
        this.currentPayrollMonth = YearMonth.now();

        initComponents();
        setupTableColumns();
        setupTableProperties();
        populateEmployeeDropdown();

        // Set default selection to current month and year
        selectMonthJComboBox2.setSelectedItem(currentPayrollMonth.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        selectYearJComboBox3.setSelectedItem(String.valueOf(currentPayrollMonth.getYear()));

        // Initially disable approval/denial buttons until payslips are generated
        updateButtonStates();
    }
    
    /**
     * Sets up table properties including horizontal scrolling
     */
    private void setupTableProperties() {
        // Enable auto resize mode to allow horizontal scrolling
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        
        // Set selection mode to allow multiple row selection
        jTable1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Ensure horizontal scrollbar is always visible
        jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    }
    
    /**
     * Updates the enabled/disabled state of buttons based on the current state
     */
    private void updateButtonStates() {
        // Approval/Denial buttons should only be enabled if payslips have been generated
        approveBttn.setEnabled(payslipsGenerated && !payrollApproved);
        approveAllBttn.setEnabled(payslipsGenerated && !payrollApproved);
        denyBttn.setEnabled(payslipsGenerated && !payrollApproved);
        denyAllBttn.setEnabled(payslipsGenerated && !payrollApproved);
        
        // Download button should only be enabled if payslips have been generated
        downloadPayslip.setEnabled(payslipsGenerated);
    }
    
    /**
     * Populates the employee dropdown with all employee IDs from the CSV database.
     */
    private void populateEmployeeDropdown() {
        try {
            // Clear existing items
            selectEmpJComboBox1.removeAllItems();
            
            // Add "All" option
            selectEmpJComboBox1.addItem("All");
            
            // Get all employees from the database
            List<Map<String, String>> allEmployees = csvProcessor.getAllEmployeeRecords();
            
            // Sort employees by ID
            allEmployees.sort((emp1, emp2) -> {
                String id1 = emp1.get("Employee ID");
                String id2 = emp2.get("Employee ID");
                return id1.compareTo(id2);
            });
            
            // Add each employee ID to the dropdown
            for (Map<String, String> employee : allEmployees) {
                String empId = employee.get("Employee ID");
                if (empId != null && !empId.isEmpty()) {
                    selectEmpJComboBox1.addItem(empId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error populating employee dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets up the table columns to match the required payroll data fields.
     */
    private void setupTableColumns() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setColumnIdentifiers(new Object[]{
            "Employee ID", "Last Name", "First Name", "Position", 
            "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Total Allow.", 
            "Gross Pay", "SSS", "PhilHealth", "Pag-Ibig", "Late Deductions", "With. Tax", 
            "Total Deductions", "Net Pay"
        });

        // Clear any existing data
        model.setRowCount(0);
    }

    /**
     * Loads payroll data for all employees or a selected employee.
     */
    private void loadPayrollData() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // Clear existing data

        try {
            // Get selected employee ID (if any)
            String selectedEmployeeId = (String) selectEmpJComboBox1.getSelectedItem();

            // Get selected month and year
            String selectedMonthStr = (String) selectMonthJComboBox2.getSelectedItem();
            String selectedYearStr = (String) selectYearJComboBox3.getSelectedItem();

            // Set the current payroll month based on selection
            Month selectedMonth = Month.valueOf(selectedMonthStr.toUpperCase());
            int selectedYear = Integer.parseInt(selectedYearStr);
            currentPayrollMonth = YearMonth.of(selectedYear, selectedMonth);

            // Get employee IDs to process
            List<String> employeeIds = new ArrayList<>();

            if ("ALL".equalsIgnoreCase(selectedEmployeeId) || "All".equals(selectedEmployeeId)) {
                // Process all employees by getting IDs from CSVDatabaseProcessor
                List<Map<String, String>> allEmployees = csvProcessor.getAllEmployeeRecords();
                for (Map<String, String> employee : allEmployees) {
                    String empId = employee.get("Employee ID");
                    if (empId != null && !empId.isEmpty()) {
                        employeeIds.add(empId);
                    }
                }
            } else {
                // Process only the selected employee
                employeeIds.add(selectedEmployeeId);
            }

            // Load data for each employee
            for (String employeeId : employeeIds) {
                loadEmployeePayrollData(employeeId, model);
            }

            // Mark payslips as generated
            payslipsGenerated = true;
            updateButtonStates();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading payroll data: " + e.getMessage(), 
                "Payroll Data Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
        /**
         * Loads payroll data for a specific employee.
         * 
         * @param employeeId The employee ID to load data for
         * @param model The table model to add the data to
         */
        private void loadEmployeePayrollData(String employeeId, DefaultTableModel model) {
            try {
                // Get employee details
                Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);

                if (employeeData == null) {
                    System.err.println("No employee record found for ID: " + employeeId);
                    return;
                }

                // Extract employee information
                String lastName = employeeData.get("Last Name");
                String firstName = employeeData.get("First Name");
                String position = employeeData.get("Position");

                // Extract allowances (remove commas and convert to numeric)
                double riceSubsidy = parseAmount(employeeData.get("Rice Subsidy"));
                double phoneAllowance = parseAmount(employeeData.get("Phone Allowance"));
                double clothingAllowance = parseAmount(employeeData.get("Clothing Allowance"));
                double totalAllowances = riceSubsidy + phoneAllowance + clothingAllowance;

                // Calculate gross pay
                double grossPay = salaryCalculation.calculateGrossMonthlySalary(
                    employeeId, currentPayrollMonth, csvProcessor);

                // Debug logging
                System.out.println("Employee ID: " + employeeId + ", Gross Pay: " + grossPay);

                // Get hourly rate for late deduction calculation
                double hourlyRate = parseAmount(employeeData.get("Hourly Rate"));

                // Calculate government deductions
                double sssDeduction = deductionCalculation.calculateSSS(grossPay);
                double philHealthDeduction = deductionCalculation.calculatePhilHealth(grossPay);
                double pagIbigDeduction = deductionCalculation.calculatePagibig(grossPay);

                // Calculate late deductions from attendance records
                double lateHours = csvProcessor.getTotalLateHours(employeeId, currentPayrollMonth);
                double lateDeduction = 0.0;
                if (lateHours > 0) {
                    lateDeduction = deductionCalculation.calculateLateDeductions(lateHours, hourlyRate);
                    System.out.println("Late hours: " + lateHours + ", Late deduction: " + lateDeduction);
                }

                // Calculate total government contributions (without late deductions)
                double totalContributions = sssDeduction + philHealthDeduction + pagIbigDeduction;

                // Calculate taxable income (gross pay minus government contributions AND late deductions)
                double taxableIncome = grossPay - totalContributions - lateDeduction;

                // Calculate withholding tax based on taxable income
                double withholdingTax = deductionCalculation.calculateTax(taxableIncome);

                // Debug logging for deductions
                System.out.println("SSS: " + sssDeduction + ", PhilHealth: " + philHealthDeduction + 
                    ", Pag-Ibig: " + pagIbigDeduction + ", Tax: " + withholdingTax);

                // Calculate total deductions (government + late + tax)
                double totalDeductions = totalContributions + lateDeduction + withholdingTax;

                // Calculate net pay
                double netPay = grossPay - totalDeductions;

                // Add row to the table
                model.addRow(new Object[]{
                    employeeId,
                    lastName,
                    firstName,
                    position,
                    formatCurrency(riceSubsidy),
                    formatCurrency(phoneAllowance),
                    formatCurrency(clothingAllowance),
                    formatCurrency(totalAllowances),
                    formatCurrency(grossPay),
                    formatCurrency(sssDeduction),
                    formatCurrency(philHealthDeduction),
                    formatCurrency(pagIbigDeduction),
                    formatCurrency(lateDeduction),  // Added late deduction column
                    formatCurrency(withholdingTax),
                    formatCurrency(totalDeductions),
                    formatCurrency(netPay)
                });

            } catch (Exception e) {
                System.err.println("Error processing employee ID " + employeeId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

            /**
             * Helper method to parse currency amounts from strings, handling commas.
             * 
             * @param amountStr The amount string to parse
             * @return The parsed numeric value
             */
            private double parseAmount(String amountStr) {
                if (amountStr == null || amountStr.isEmpty()) {
                    return 0.0;
                }
                // Remove commas and other non-numeric characters except decimal point
                String cleanedAmount = amountStr.replaceAll("[^0-9.]", "");
                try {
                    return Double.parseDouble(cleanedAmount);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing amount: " + amountStr);
                    return 0.0;
                }
            }

            /**
             * Formats a numeric value as a currency string.
             * 
             * @param amount The amount to format
             * @return The formatted currency string
             */
            private String formatCurrency(double amount) {
                return String.format("₱%.2f", amount);
            }

            /**
             * Generates and downloads a payslip for a specific employee.
             * 
             * @param employeeId ID of the employee
             * @param firstName First name of the employee
             * @param lastName Last name of the employee
             */
            private void generateAndDownloadPayslip(String employeeId, String firstName, String lastName) {
                try {
                    // Create an Employee object for the Payslip class
                    Employee employee = new Employee(
                        Integer.parseInt(employeeId),
                        firstName,
                        lastName,
                        "", // Email (not needed for payslip)
                        "", // Password (not needed for payslip)
                        ""  // Role (not needed for payslip)
                    );

                    // Find the row in the table that corresponds to this employee
                    int rowCount = jTable1.getRowCount();
                    int rowIndex = -1;

                    for (int i = 0; i < rowCount; i++) {
                        String id = jTable1.getValueAt(i, 0).toString();
                        if (id.equals(employeeId)) {
                            rowIndex = i;
                            break;
                        }
                    }

                    if (rowIndex == -1) {
                        JOptionPane.showMessageDialog(this, 
                            "Employee data not found in the table. Please generate payslip first.", 
                            "Payslip Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Extract values from the table
                    double riceSubsidy = parseAmount(jTable1.getValueAt(rowIndex, 4).toString());
                    double phoneAllowance = parseAmount(jTable1.getValueAt(rowIndex, 5).toString());
                    double clothingAllowance = parseAmount(jTable1.getValueAt(rowIndex, 6).toString());
                    double totalAllowances = parseAmount(jTable1.getValueAt(rowIndex, 7).toString());
                    double grossPay = parseAmount(jTable1.getValueAt(rowIndex, 8).toString());
                    double sssDeduction = parseAmount(jTable1.getValueAt(rowIndex, 9).toString());
                    double philHealthDeduction = parseAmount(jTable1.getValueAt(rowIndex, 10).toString());
                    double pagIbigDeduction = parseAmount(jTable1.getValueAt(rowIndex, 11).toString());
                    double lateDeduction = parseAmount(jTable1.getValueAt(rowIndex, 12).toString());
                    double withholdingTax = parseAmount(jTable1.getValueAt(rowIndex, 13).toString());
                    double totalDeductions = parseAmount(jTable1.getValueAt(rowIndex, 14).toString());
                    double netPay = parseAmount(jTable1.getValueAt(rowIndex, 15).toString());

                    // Calculate taxable income 
                    double taxableIncome = grossPay - sssDeduction - philHealthDeduction - pagIbigDeduction - lateDeduction;

                    // Create Payslip
                    Payslip payslip = new Payslip(employee);

                    // Set the payroll month
                    payslip.setPayrollMonth(currentPayrollMonth);

                    // Set all pre-calculated values
                    payslip.setCalculatedValues(
                        grossPay, // Using gross pay as basic salary too
                        grossPay,
                        sssDeduction,
                        philHealthDeduction,
                        pagIbigDeduction,
                        lateDeduction,
                        withholdingTax,
                        totalDeductions,
                        taxableIncome,
                        netPay
                    );

                    // Generate and download the payslip
                    payslip.printPayslip(
                        currentPayrollMonth.getMonthValue(),
                        currentPayrollMonth.getYear()
                    );

                } catch (IOException | DocumentException e) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error generating payslip: " + e.getMessage(),
                        "Payslip Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    e.printStackTrace();
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
        backpyrllmngmntbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        selectEmpJComboBox1 = new javax.swing.JComboBox<>();
        selectMonthJComboBox2 = new javax.swing.JComboBox<>();
        selectYearJComboBox3 = new javax.swing.JComboBox<>();
        approveBttn = new javax.swing.JButton();
        denyBttn = new javax.swing.JButton();
        generatePayslip = new javax.swing.JButton();
        approveAllBttn = new javax.swing.JButton();
        denyAllBttn = new javax.swing.JButton();
        downloadPayslip = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Payroll Management");

        backpyrllmngmntbttn.setBackground(new java.awt.Color(207, 10, 10));
        backpyrllmngmntbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backpyrllmngmntbttn.setForeground(new java.awt.Color(255, 255, 255));
        backpyrllmngmntbttn.setText("Back");
        backpyrllmngmntbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backpyrllmngmntbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(backpyrllmngmntbttn)
                .addGap(27, 27, 27)
                .addComponent(jLabel1)
                .addContainerGap(686, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(15, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backpyrllmngmntbttn)
                    .addComponent(jLabel1))
                .addGap(15, 15, 15))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Employee ID", "Last Name", "First Name", "Position", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Total Allow.", "Gross Pay", "SSS", "PhilHealth", "PagIbig", "Late Deductions", "With. Tax", "Total Deductions", "Net Pay"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setText("Select Employee");

        jLabel3.setText("Select Month:");

        jLabel4.setText("Select Year:");

        selectEmpJComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "10001", "10002", "10003", "10004", "10005", "10006", "10007", "10008", "10009", "10010", "10011", "10012", "10013", "10014", "10015", "10016", "10017", "10018", "10019", "10020", "10021", "10022", "10023", "10024", "10025" }));
        selectEmpJComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectEmpJComboBox1ActionPerformed(evt);
            }
        });

        selectMonthJComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" }));
        selectMonthJComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMonthJComboBox2ActionPerformed(evt);
            }
        });

        selectYearJComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2024", "2025" }));
        selectYearJComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectYearJComboBox3ActionPerformed(evt);
            }
        });

        approveBttn.setBackground(new java.awt.Color(0, 153, 0));
        approveBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        approveBttn.setForeground(new java.awt.Color(255, 255, 255));
        approveBttn.setText("Approve");
        approveBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveBttnActionPerformed(evt);
            }
        });

        denyBttn.setBackground(new java.awt.Color(207, 10, 10));
        denyBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        denyBttn.setForeground(new java.awt.Color(255, 255, 255));
        denyBttn.setText("Deny");
        denyBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                denyBttnActionPerformed(evt);
            }
        });

        generatePayslip.setBackground(new java.awt.Color(220, 95, 0));
        generatePayslip.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        generatePayslip.setForeground(new java.awt.Color(255, 255, 255));
        generatePayslip.setText("Generate Payslip");
        generatePayslip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generatePayslipActionPerformed(evt);
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

        downloadPayslip.setBackground(new java.awt.Color(220, 95, 0));
        downloadPayslip.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        downloadPayslip.setForeground(new java.awt.Color(255, 255, 255));
        downloadPayslip.setText("Download Payslip");
        downloadPayslip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadPayslipActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(selectEmpJComboBox1, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(49, 49, 49)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(selectMonthJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(43, 43, 43)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(selectYearJComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(generatePayslip, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(approveAllBttn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(denyAllBttn, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(approveBttn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(denyBttn, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGap(552, 552, 552)
                            .addComponent(downloadPayslip, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 939, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                    .addComponent(selectEmpJComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectMonthJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectYearJComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatePayslip)
                    .addComponent(downloadPayslip))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(approveBttn)
                    .addComponent(approveAllBttn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(denyAllBttn)
                    .addComponent(denyBttn))
                .addContainerGap(24, Short.MAX_VALUE))
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
    //Approve button action
    private void approveBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveBttnActionPerformed
       int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select an employee from the table to approve.", 
                "Selection Required", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the selected employee information
        String employeeId = jTable1.getValueAt(selectedRow, 0).toString();
        String lastName = jTable1.getValueAt(selectedRow, 1).toString();
        String firstName = jTable1.getValueAt(selectedRow, 2).toString();
        
        // Display confirmation
        JOptionPane.showMessageDialog(this, 
            "Payroll for " + firstName + " " + lastName + " (ID: " + employeeId + ") has been approved.\n" +
            "Salary has been disbursed for the pay period: " + currentPayrollMonth.getMonth() + " " + currentPayrollMonth.getYear(),
            "Payroll Approved",
            JOptionPane.INFORMATION_MESSAGE);
    // In actual/ real implementation, we would mark all employees as approved
    }//GEN-LAST:event_approveBttnActionPerformed
    // Select month filter
    private void selectMonthJComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectMonthJComboBox2ActionPerformed
        // Reset the payslips generated when user selection changes
        payslipsGenerated = false;
        payrollApproved = false;
        updateButtonStates();
        
        // Clear the table
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
    }//GEN-LAST:event_selectMonthJComboBox2ActionPerformed

    private void backpyrllmngmntbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backpyrllmngmntbttnActionPerformed
        new AdminAccounting(loggedInUser).setVisible(true);
        this.dispose(); //close window
    }//GEN-LAST:event_backpyrllmngmntbttnActionPerformed
    //Select employee ID filter action 
    private void selectEmpJComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectEmpJComboBox1ActionPerformed
        // Reset the payslips generated when user selection changes
        payslipsGenerated = false;
        payrollApproved = false;
        updateButtonStates();
        
        // Clear the table
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
    }//GEN-LAST:event_selectEmpJComboBox1ActionPerformed

    private void selectYearJComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectYearJComboBox3ActionPerformed
        // Reset the payslips generated when user selection changes
        payslipsGenerated = false;
        payrollApproved = false;
        updateButtonStates();
        
        // Clear the table
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

    }//GEN-LAST:event_selectYearJComboBox3ActionPerformed
    //Approve all payslips
    private void approveAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveAllBttnActionPerformed
        if (jTable1.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No payroll data to approve. Please generate payslips first.", 
                "No Data", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Display confirmation
        JOptionPane.showMessageDialog(this, 
            "All payrolls for " + currentPayrollMonth.getMonth() + " " + currentPayrollMonth.getYear() + " have been approved.\n" +
            "Salaries have been disbursed for all employees.",
            "All Payrolls Approved",
            JOptionPane.INFORMATION_MESSAGE);
            
        // Mark all as approved
        payrollApproved = true;
        updateButtonStates();
    

    }//GEN-LAST:event_approveAllBttnActionPerformed

    private void denyAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyAllBttnActionPerformed
        if (jTable1.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No payroll data to deny. Please generate payslips first.", 
                "No Data", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Display confirmation
        JOptionPane.showMessageDialog(this, 
            "All payrolls for " + currentPayrollMonth.getMonth() + " " + currentPayrollMonth.getYear() + " have been denied.\n" +
            "No salaries will be disbursed for this pay period.",
            "All Payrolls Denied",
            JOptionPane.WARNING_MESSAGE);
            
        // Mark as denied (reset the generated state)
        payslipsGenerated = false;
        updateButtonStates();
    // In actual/ real implementation, we would mark all employees as denied
    }//GEN-LAST:event_denyAllBttnActionPerformed
    //Download Payslip Button action
    private void downloadPayslipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadPayslipActionPerformed
        // Get selected rows
        int[] selectedRows = jTable1.getSelectedRows();
        
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one employee from the table to download payslip(s).", 
                "Selection Required", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Download payslips for all selected employees
        for (int row : selectedRows) {
            String employeeId = jTable1.getValueAt(row, 0).toString();
            String lastName = jTable1.getValueAt(row, 1).toString();
            String firstName = jTable1.getValueAt(row, 2).toString();
            
            // Generate and download the payslip
            generateAndDownloadPayslip(employeeId, firstName, lastName);
        }
        
        // Display confirmation
        String message = selectedRows.length == 1 
            ? "Payslip has been downloaded successfully."
            : selectedRows.length + " payslips have been downloaded successfully.";
        
        JOptionPane.showMessageDialog(this, 
            message + "\nFiles saved to your Downloads folder.",
            "Payslips Downloaded",
            JOptionPane.INFORMATION_MESSAGE);

    }//GEN-LAST:event_downloadPayslipActionPerformed
    //Deny payslip button action
    private void denyBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyBttnActionPerformed
    int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select an employee from the table to deny.", 
                "Selection Required", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the selected employee information
        String employeeId = jTable1.getValueAt(selectedRow, 0).toString();
        String lastName = jTable1.getValueAt(selectedRow, 1).toString();
        String firstName = jTable1.getValueAt(selectedRow, 2).toString();
        
        // Display confirmation
        JOptionPane.showMessageDialog(this, 
            "Payroll for " + firstName + " " + lastName + " (ID: " + employeeId + ") has been denied.\n" +
            "Salary will not be disbursed for the pay period: " + currentPayrollMonth.getMonth() + " " + currentPayrollMonth.getYear(),
            "Payroll Denied",
            JOptionPane.WARNING_MESSAGE);
    
    
    // In actual/ real world implementation, we would mark this specific employee as denied
    }//GEN-LAST:event_denyBttnActionPerformed
    //Generate payslip button action; basically generates or calculates all employee's payroll for a pay period
    private void generatePayslipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generatePayslipActionPerformed
    String selectedEmployeeId = (String) selectEmpJComboBox1.getSelectedItem();
    String selectedMonthStr = (String) selectMonthJComboBox2.getSelectedItem();
    String selectedYearStr = (String) selectYearJComboBox3.getSelectedItem();
    
    // Show loading message
    JOptionPane.showMessageDialog(this, 
        "Calculating payroll for " + 
        (selectedEmployeeId.equalsIgnoreCase("ALL") || selectedEmployeeId.equals("All") ? "all employees" : "employee " + selectedEmployeeId) + 
        " for " + selectedMonthStr + " " + selectedYearStr + "...",
        "Generating Payslips",
        JOptionPane.INFORMATION_MESSAGE);
    
    // Load the payroll data
    loadPayrollData();
    
    // Show confirmation message
    int employeeCount = jTable1.getRowCount();
    JOptionPane.showMessageDialog(this, 
        "Successfully generated payslips for " + employeeCount + " employees.\n" +
        "Pay period: " + selectedMonthStr + " " + selectedYearStr + "\n\n" +
        "You can now approve or deny the payroll.",
        "Payslips Generated",
        JOptionPane.INFORMATION_MESSAGE);

    }//GEN-LAST:event_generatePayslipActionPerformed

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
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Create an Employee object (concrete subclass of User)
                Employee user = new Employee(10001, "Test", "User", "test@email.com", "password", "ACCOUNTING");
                new PayrollManagement(user).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton approveAllBttn;
    private javax.swing.JButton approveBttn;
    private javax.swing.JButton backpyrllmngmntbttn;
    private javax.swing.JButton denyAllBttn;
    private javax.swing.JButton denyBttn;
    private javax.swing.JButton downloadPayslip;
    private javax.swing.JButton generatePayslip;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JComboBox<String> selectEmpJComboBox1;
    private javax.swing.JComboBox<String> selectMonthJComboBox2;
    private javax.swing.JComboBox<String> selectYearJComboBox3;
    // End of variables declaration//GEN-END:variables
}
