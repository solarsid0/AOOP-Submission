
package gui;

import oop.classes.actors.User;
import oop.classes.actors.Employee;
import oop.classes.calculations.PayrollSummary;
import oop.classes.empselfservice.Payslip;
import CSV.CSVDatabaseProcessor;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.time.YearMonth;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * This class represents the payslip view for employees
 */
public class ViewPayslip extends javax.swing.JFrame {
    
    private User loggedInUser;
    private PayrollSummary payrollSummary;
    private CSVDatabaseProcessor csvProcessor;

    /**
     * Creates new form ViewPayslip without user data
     */
    public ViewPayslip() {
        initComponents();
        setupComponents();
        System.out.println("ViewPayslip initialized without user data");
    }
    
    /**
     * Creates new form ViewPayslip with logged in user data
     * @param user The currently logged in user
     */
    public ViewPayslip(User user) {
        this.loggedInUser = user;
        initComponents();
        setupComponents();
        
        // If user is provided, load their data
        if (loggedInUser != null) {
            System.out.println("ViewPayslip initialized for user: " + 
                user.getFirstName() + " " + user.getLastName() + 
                " (ID: " + user.getEmployeeID() + ")");
            loadUserData();
        } else {
            System.err.println("ViewPayslip initialized with null user");
        }
    }
    
    /**
     * Sets up necessary components
     */
    private void setupComponents() {
        try {
            // Initialize CSV processor
            this.csvProcessor = new CSVDatabaseProcessor();
            
            // Center the form on screen
            this.setLocationRelativeTo(null);
            
            // Set form title
            this.setTitle("MotorPH Payslip Viewer");
            
            
            // Set current year as default in the year dropdown
            int currentYear = java.time.LocalDate.now().getYear();
            for (int i = 0; i < yearcombo.getItemCount(); i++) {
                if (yearcombo.getItemAt(i).equals(String.valueOf(currentYear))) {
                    yearcombo.setSelectedIndex(i);
                    break;
                }
            }
            
            // Set current month as default in the month dropdown
            int currentMonth = java.time.LocalDate.now().getMonthValue() - 1; // 0-based index
            monthcombo.setSelectedIndex(currentMonth);
            
            // Set monospaced font for better text alignment
            jTextArea1.setFont(new java.awt.Font("Monospaced", 0, 12));
            
            System.out.println("Component setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error during component setup: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error initializing application: " + e.getMessage(), 
                "Initialization Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Loads the user data into the form fields
     */
    private void loadUserData() {
        try {
            // Convert int to String if needed
            String empId = String.valueOf(loggedInUser.getEmployeeID());
            System.out.println("Loading employee data for ID: " + empId);
            
            Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(empId);
            
            if (employeeData != null && !employeeData.isEmpty()) {
                // Set employee info fields
                inputpayslipempid.setText(String.valueOf(loggedInUser.getEmployeeID()));
                inputpayslipfirstnm.setText(loggedInUser.getFirstName());
                inputpaysliplastnm.setText(loggedInUser.getLastName());
                
                // Handle potential null value for Position
                String position = employeeData.get("Position");
                if (position == null || position.isEmpty()) {
                    position = loggedInUser.getRole(); // Use role as fallback
                    System.out.println("Position not found for employee ID: " + empId + ", using role instead");
                }
                inputpayslipposition.setText(position);
                
                System.out.println("Employee data loaded successfully");
            } else {
                System.err.println("Employee data not found for ID: " + empId);
                JOptionPane.showMessageDialog(this, 
                    "Employee data not found. Using basic user information.", 
                    "Data Notice", 
                    JOptionPane.WARNING_MESSAGE);
                
                // Set values directly from loggedInUser as fallback
                inputpayslipempid.setText(String.valueOf(loggedInUser.getEmployeeID()));
                inputpayslipfirstnm.setText(loggedInUser.getFirstName());
                inputpaysliplastnm.setText(loggedInUser.getLastName());
                inputpayslipposition.setText(loggedInUser.getRole());
            }
        } catch (Exception e) {
            System.err.println("Error loading employee data: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading employee data: " + e.getMessage(), 
                "Data Error", 
                JOptionPane.ERROR_MESSAGE);
            
            // Set default values from loggedInUser
            if (loggedInUser != null) {
                inputpayslipempid.setText(String.valueOf(loggedInUser.getEmployeeID()));
                inputpayslipfirstnm.setText(loggedInUser.getFirstName());
                inputpaysliplastnm.setText(loggedInUser.getLastName());
                inputpayslipposition.setText(loggedInUser.getRole());
            }
        }
    }
    
    /**
     * Formats a number as Philippine Peso
     */
    private String formatAsPHP(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("tl", "PH"));
        return format.format(amount);
    }
    
    /**
     * Generates and displays payslip information
     */
    private void viewPayslip() {
        try {
            // Get selected month and year
            if (monthcombo.getSelectedItem() == null || yearcombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select month and year.", 
                    "Selection Required", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String selectedMonth = (String) monthcombo.getSelectedItem();
            String selectedYear = (String) yearcombo.getSelectedItem();
            
            // Get the month index (1-based)
            int monthIndex = monthcombo.getSelectedIndex() + 1;
            int year = Integer.parseInt(selectedYear);
            
            System.out.println("Generating payslip for Employee ID: " + loggedInUser.getEmployeeID() + 
                              ", Month: " + selectedMonth + ", Year: " + selectedYear);
            
            // Show status message while calculating
            jTextArea1.setText("Calculating payslip for " + selectedMonth + " " + selectedYear + "...\n" +
                              "Please wait...");
            
            // Create an Employee object from the loggedInUser
            Employee employee = new Employee(
                loggedInUser.getEmployeeID(), // Use directly if Employee expects int
                loggedInUser.getFirstName(),
                loggedInUser.getLastName(),
                loggedInUser.getEmail(),
                "", // password not needed
                loggedInUser.getRole()
            );
            
            // Create a PayrollSummary for calculations
            this.payrollSummary = new PayrollSummary(employee, YearMonth.of(year, monthIndex));
            
            // Format payslip text for display in JTextArea
            StringBuilder payslipText = new StringBuilder();
            
            // Header
            String title = "MOTORPH PAYSLIP";
            int width = 60;
            int padding = (width - title.length()) / 2;
            payslipText.append(" ".repeat(padding)).append(title).append("\n");
            payslipText.append("=".repeat(width)).append("\n\n");
            
            // Employee details section
            payslipText.append("EMPLOYEE DETAILS\n");
            payslipText.append("-".repeat(width)).append("\n");
            payslipText.append(String.format("%-20s %-30s\n", "Employee ID:", loggedInUser.getEmployeeID()));
            payslipText.append(String.format("%-20s %-30s\n", "Name:", 
                loggedInUser.getFirstName() + " " + loggedInUser.getLastName()));
            payslipText.append(String.format("%-20s %-30s\n", "Position:", inputpayslipposition.getText()));
            payslipText.append(String.format("%-20s %-30s\n", "Pay Period:", selectedMonth + " " + selectedYear));
            payslipText.append("-".repeat(width)).append("\n\n");
            
            // Earnings section
            payslipText.append("EARNINGS\n");
            payslipText.append("-".repeat(width)).append("\n");
            payslipText.append(String.format("%-40s %15s\n", "Basic Salary:", formatAsPHP(payrollSummary.getBasicSalary())));
            
            // Include overtime if any
            if (payrollSummary.getOvertimePay() > 0) {
                payslipText.append(String.format("%-40s %15s\n", "Overtime:", formatAsPHP(payrollSummary.getOvertimePay())));
            }
            
            payslipText.append(String.format("%-40s %15s\n", "Gross Pay:", formatAsPHP(payrollSummary.getGrossSalary())));
            payslipText.append("-".repeat(width)).append("\n\n");
            
            // Allowances section
            payslipText.append("ALLOWANCES\n");
            payslipText.append("-".repeat(width)).append("\n");
            payslipText.append(String.format("%-40s %15s\n", "Rice Subsidy:", formatAsPHP(payrollSummary.getRiceSubsidy())));
            payslipText.append(String.format("%-40s %15s\n", "Phone Allowance:", formatAsPHP(payrollSummary.getPhoneAllowance())));
            payslipText.append(String.format("%-40s %15s\n", "Clothing Allowance:", formatAsPHP(payrollSummary.getClothingAllowance())));
            payslipText.append(String.format("%-40s %15s\n", "Total Allowances:", formatAsPHP(payrollSummary.getTotalAllowances())));
            payslipText.append("-".repeat(width)).append("\n\n");
            
            // Deductions section
            payslipText.append("DEDUCTIONS\n");
            payslipText.append("-".repeat(width)).append("\n");
            
            // SSS
            payslipText.append(String.format("%-40s %15s\n", "SSS:", formatAsPHP(payrollSummary.getSssDeduction())));
            payslipText.append("(Based on SSS contribution table)\n");
            payslipText.append("-".repeat(width)).append("\n");
            
            // PhilHealth
            payslipText.append(String.format("%-40s %15s\n", "PhilHealth:", formatAsPHP(payrollSummary.getPhilHealthDeduction())));
            payslipText.append("(Monthly Basic Salary × 3%) ÷ 2\n");
            payslipText.append("-".repeat(width)).append("\n");
            
            // Pag-IBIG
            payslipText.append(String.format("%-40s %15s\n", "Pag-IBIG:", formatAsPHP(payrollSummary.getPagIbigDeduction())));
            payslipText.append("(2% of Basic Salary [Maximum of ₱100])\n");
            payslipText.append("-".repeat(width)).append("\n");
            
            // Taxable Income
            payslipText.append(String.format("%-40s %15s\n", "Taxable Income:", formatAsPHP(payrollSummary.getTaxableIncome())));
            payslipText.append("-".repeat(width)).append("\n");
            
            // Withholding Tax with explanation
            payslipText.append(String.format("%-40s %15s\n", "Withholding Tax:", formatAsPHP(payrollSummary.getWithholdingTax())));
            payslipText.append(payrollSummary.getTaxExplanation() + "\n");
            payslipText.append("-".repeat(width)).append("\n");
            
            // Late/Absence Deductions if applicable
            if (payrollSummary.getLateDeductions() > 0) {
                payslipText.append(String.format("%-40s %15s\n", "Late/Absence Deductions:", formatAsPHP(payrollSummary.getLateDeductions())));
                payslipText.append("Late Hours × Hourly Rate\n");
                payslipText.append("-".repeat(width)).append("\n");
            }
            
            // Total Deductions
            payslipText.append(String.format("%-40s %15s\n", "Total Deductions:", formatAsPHP(payrollSummary.getTotalDeductions())));
            payslipText.append("-".repeat(width)).append("\n\n");
            
            // Net Pay
            payslipText.append("=".repeat(width)).append("\n");
            payslipText.append(String.format("%-40s %15s\n", "NET PAY:", formatAsPHP(payrollSummary.getNetMonthlyPay())));
            payslipText.append("=".repeat(width)).append("\n\n");
            
            payslipText.append("This is a computer-generated payslip. No signature required.");
            
            // Display the payslip in the JTextArea
            jTextArea1.setText(payslipText.toString());
            jTextArea1.setCaretPosition(0); // Scroll to top
            
            System.out.println("Payslip generated successfully");
            
        } catch (Exception e) {
            System.err.println("Error generating payslip: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error generating payslip: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            
            // Display error in text area
            jTextArea1.setText("Error generating payslip:\n\n" + e.getMessage() + 
                              "\n\nPlease try again or contact system administrator.");
        }
    }
    
    /**
     * Downloads the payslip as PDF
     */
    private void downloadPayslip() {
        try {
            // Get selected month and year
            if (monthcombo.getSelectedItem() == null || yearcombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select month and year.", 
                    "Selection Required", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String selectedMonth = (String) monthcombo.getSelectedItem();
            String selectedYear = (String) yearcombo.getSelectedItem();
            int monthIndex = monthcombo.getSelectedIndex() + 1;
            int year = Integer.parseInt(selectedYear);
            
            System.out.println("Downloading payslip PDF for Employee ID: " + loggedInUser.getEmployeeID() + 
                              ", Month: " + selectedMonth + ", Year: " + selectedYear);
            
            // Set status message while generating PDF
            String currentText = jTextArea1.getText();
            jTextArea1.setText(currentText + "\n\nGenerating PDF payslip for " + selectedMonth + " " + selectedYear + "...\n" +
                              "Please wait...");
            
            // Create an Employee object from the loggedInUser
            Employee employee = new Employee(
                loggedInUser.getEmployeeID(), // Use directly if Employee expects int
                loggedInUser.getFirstName(),
                loggedInUser.getLastName(),
                loggedInUser.getEmail(),
                "", // password not needed
                loggedInUser.getRole()
            );
            
            // Create a Payslip object
            Payslip payslip = new Payslip(employee);
            
            // Set payroll month
            payslip.setPayrollMonth(YearMonth.of(year, monthIndex));
            
            // Generate and print the payslip
            payslip.printPayslip(monthIndex, year);
            
            // Update text area with success message
            jTextArea1.setText(currentText + "\n\nPayslip PDF successfully downloaded to your Downloads folder.");
            
            // Show success message
            JOptionPane.showMessageDialog(this, 
                "Payslip successfully downloaded to your Downloads folder.", 
                "Download Complete", 
                JOptionPane.INFORMATION_MESSAGE);
            
            System.out.println("PDF payslip downloaded successfully");
            
        } catch (IOException | DocumentException e) {
            JOptionPane.showMessageDialog(this, 
                "Error downloading payslip: " + e.getMessage(), 
                "Download Error", 
                JOptionPane.ERROR_MESSAGE);
            
            // Update text area with error message
            jTextArea1.setText("Error downloading payslip PDF:\n\n" + e.getMessage() + 
                              "\n\nPlease try again or contact system administrator.");
        }
    }
    
    /**
     * Returns to the appropriate dashboard based on user role
     */
    private void navigateBack() {
        try {
            if (loggedInUser == null) {
                System.out.println("No user logged in, navigating to Login screen");
                new Login().setVisible(true);
                this.dispose();
                return;
            }
            
            String role = loggedInUser.getRole();
            System.out.println("Navigating back to " + role + " dashboard");
            
            switch (role) {
                case "HR":
                    new AdminHR(loggedInUser).setVisible(true);
                    break;
                case "ACCOUNTING":
                    new AdminAccounting(loggedInUser).setVisible(true);
                    break;
                case "IT":
                    new AdminIT(loggedInUser).setVisible(true);
                    break;
                case "IMMEDIATE SUPERVISOR":
                    new AdminSupervisor(loggedInUser).setVisible(true);
                    break;
                case "EMPLOYEE":
                    new EmployeeSelfService(loggedInUser).setVisible(true);
                    break;
                default:
                    // Default to login page if role is unknown
                    System.err.println("Unknown role: " + role + ", defaulting to Login screen");
                    new Login().setVisible(true);
            }
            
            this.dispose(); // Close this window
            
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error navigating back: " + e.getMessage() + "\nReturning to login screen.", 
                "Navigation Error", 
                JOptionPane.ERROR_MESSAGE);
            
            // Fall back to login screen on error
            new Login().setVisible(true);
            this.dispose();
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
        backpayslipbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        payslipempid = new javax.swing.JLabel();
        payslipfirstnm = new javax.swing.JLabel();
        paysliplastnm = new javax.swing.JLabel();
        paysliposition = new javax.swing.JLabel();
        inputpayslipempid = new javax.swing.JLabel();
        inputpayslipfirstnm = new javax.swing.JLabel();
        inputpaysliplastnm = new javax.swing.JLabel();
        inputpayslipposition = new javax.swing.JLabel();
        payslipselectmonth = new javax.swing.JLabel();
        payslipselectyear = new javax.swing.JLabel();
        monthcombo = new javax.swing.JComboBox<>();
        yearcombo = new javax.swing.JComboBox<>();
        viewpayslipbtn = new javax.swing.JButton();
        downloadpayslipbtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("PAYSLIP PAGE");

        backpayslipbttn.setBackground(new java.awt.Color(207, 10, 10));
        backpayslipbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backpayslipbttn.setForeground(new java.awt.Color(255, 255, 255));
        backpayslipbttn.setText("Back");
        backpayslipbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backpayslipbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backpayslipbttn)
                .addGap(39, 39, 39)
                .addComponent(jLabel1)
                .addContainerGap(483, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backpayslipbttn))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        payslipempid.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        payslipempid.setText("Employee ID:");

        payslipfirstnm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        payslipfirstnm.setText("First Name:");

        paysliplastnm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        paysliplastnm.setText("Last Name:");

        paysliposition.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        paysliposition.setText("Position:");

        inputpayslipempid.setText(". . .");

        inputpayslipfirstnm.setText(". . .");

        inputpaysliplastnm.setText(". . .");

        inputpayslipposition.setText(". . .");

        payslipselectmonth.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        payslipselectmonth.setText("Select Month:");

        payslipselectyear.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        payslipselectyear.setText("Select Year:");

        monthcombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" }));
        monthcombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monthcomboActionPerformed(evt);
            }
        });

        yearcombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2020", "2021", "2022", "2023", "2024", "2025" }));
        yearcombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yearcomboActionPerformed(evt);
            }
        });

        viewpayslipbtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        viewpayslipbtn.setText("View Payslip");
        viewpayslipbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewpayslipbtnActionPerformed(evt);
            }
        });

        downloadpayslipbtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        downloadpayslipbtn.setText("Download Payslip");
        downloadpayslipbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadpayslipbtnActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(viewpayslipbtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(payslipempid)
                            .addComponent(payslipfirstnm)
                            .addComponent(paysliplastnm)
                            .addComponent(paysliposition)
                            .addComponent(payslipselectmonth)
                            .addComponent(monthcombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(43, 43, 43)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(yearcombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(payslipselectyear)
                            .addComponent(inputpayslipposition)
                            .addComponent(inputpaysliplastnm)
                            .addComponent(inputpayslipfirstnm)
                            .addComponent(inputpayslipempid)))
                    .addComponent(downloadpayslipbtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 136, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(71, 71, 71))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(payslipempid)
                            .addComponent(inputpayslipempid))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(payslipfirstnm)
                            .addComponent(inputpayslipfirstnm))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(paysliplastnm)
                            .addComponent(inputpaysliplastnm))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(paysliposition)
                            .addComponent(inputpayslipposition))
                        .addGap(89, 89, 89)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(payslipselectmonth)
                            .addComponent(payslipselectyear))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(monthcombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(yearcombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(28, 28, 28)
                        .addComponent(viewpayslipbtn)
                        .addGap(18, 18, 18)
                        .addComponent(downloadpayslipbtn)))
                .addContainerGap(49, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 742, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void viewpayslipbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewpayslipbtnActionPerformed
       // Disable the button temporarily to prevent multiple clicks
        viewpayslipbtn.setEnabled(false);
        
        // Show busy cursor
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        
        // Use SwingUtilities.invokeLater to avoid UI freezing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    viewPayslip();
                } finally {
                    // Re-enable the button and restore cursor
                    viewpayslipbtn.setEnabled(true);
                    setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }//GEN-LAST:event_viewpayslipbtnActionPerformed

    private void downloadpayslipbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadpayslipbtnActionPerformed
       // Disable the button temporarily to prevent multiple clicks
        downloadpayslipbtn.setEnabled(false);
        
        // Show busy cursor
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        
        // Use SwingUtilities.invokeLater to avoid UI freezing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadPayslip();
                } finally {
                    // Re-enable the button and restore cursor
                    downloadpayslipbtn.setEnabled(true);
                    setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }//GEN-LAST:event_downloadpayslipbtnActionPerformed

    private void monthcomboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthcomboActionPerformed
      // Clear any existing payslip data
        if (jTextArea1.getText().contains("NET PAY:")) {
            jTextArea1.setText("Month changed. Click 'View Payslip' to generate payslip for the selected period.");
        }
    }//GEN-LAST:event_monthcomboActionPerformed

    private void yearcomboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yearcomboActionPerformed
        // Clear any existing payslip data
        if (jTextArea1.getText().contains("NET PAY:")) {
            jTextArea1.setText("Year changed. Click 'View Payslip' to generate payslip for the selected period.");
        }
    }//GEN-LAST:event_yearcomboActionPerformed

    private void backpayslipbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backpayslipbttnActionPerformed
        navigateBack();
    }//GEN-LAST:event_backpayslipbttnActionPerformed

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
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ViewPayslip().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backpayslipbttn;
    private javax.swing.JButton downloadpayslipbtn;
    private javax.swing.JLabel inputpayslipempid;
    private javax.swing.JLabel inputpayslipfirstnm;
    private javax.swing.JLabel inputpaysliplastnm;
    private javax.swing.JLabel inputpayslipposition;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JComboBox<String> monthcombo;
    private javax.swing.JLabel payslipempid;
    private javax.swing.JLabel payslipfirstnm;
    private javax.swing.JLabel paysliplastnm;
    private javax.swing.JLabel paysliposition;
    private javax.swing.JLabel payslipselectmonth;
    private javax.swing.JLabel payslipselectyear;
    private javax.swing.JButton viewpayslipbtn;
    private javax.swing.JComboBox<String> yearcombo;
    // End of variables declaration//GEN-END:variables
}
