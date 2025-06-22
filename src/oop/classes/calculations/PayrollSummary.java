package oop.classes.calculations;
import oop.classes.actors.Employee;
import java.time.YearMonth;
import CSV.CSVDatabaseProcessor;
import java.util.Map;

/**
 * Calculates the overall payroll details = net pay
 * Uses SalaryCalculation and DeductionCalculation
 * 
 * @author Admin
 */
public class PayrollSummary {
    // Employee information
    private String employeeId; // Employee's unique ID
    private String employeeName; // Employee's full name
    private String position; // Employee's position
    private String department; // Employee's department
    private YearMonth payrollMonth; // Pay period (month and year)
    
    // Salary components
    private double basicSalary; // Basic monthly salary from attendance records
    private double hourlyRate; // Employee's hourly rate
    private double riceSubsidy; // Rice subsidy allowance
    private double phoneAllowance; // Phone allowance
    private double clothingAllowance; // Clothing allowance
    private double overtimePay; // Overtime pay
    
    // Aggregated salary values
    private double totalAllowances; // Total of all allowances
    private double grossSalary; // Basic salary + overtime
    private double grossMonthlySalary; // Total salary before deductions (includes allowances)
    
    // Deduction components
    private double sssDeduction; // SSS contribution deduction
    private double philHealthDeduction; // PhilHealth contribution deduction
    private double pagIbigDeduction; // Pag-IBIG fund deduction
    private double taxableIncome; // Income subject to tax
    private double withholdingTax; // Withholding tax deduction
    private double lateDeductions; // Deductions for lateness/absences
    private double totalDeductions; // Total of all deductions
    
    // Final pay
    private double netMonthlyPay; // Final salary after deductions
    
    // Helper classes for calculations
    private SalaryCalculation salaryCalculation;
    private DeductionCalculation deductionCalculation;
    private CSVDatabaseProcessor csvProcessor;
    
    /**
     * Constructor initializes payroll details and calculates deductions
     * @param employee
     */
    public PayrollSummary(Employee employee) {
        this.employeeId = String.valueOf(employee.getEmployeeID()); // Map employeeID
        this.employeeName = employee.getFullName(); // Map full name
        this.payrollMonth = YearMonth.now(); // Default to current month
        
        // Initialize the helper classes
        this.salaryCalculation = new SalaryCalculation();
        this.deductionCalculation = new DeductionCalculation();
        this.csvProcessor = new CSVDatabaseProcessor();

        // Load attendance data explicitly 
        this.csvProcessor.loadAttendanceData();
    
        // Load employee data for position, department, and allowances
        loadEmployeeData();
        
        // Calculate the payroll values
        calculatePayroll();
    }
    
    /**
     * Constructor with payroll month specified
     * @param employee
     * @param payrollMonth
     */
    public PayrollSummary(Employee employee, YearMonth payrollMonth) {
        this.employeeId = String.valueOf(employee.getEmployeeID());
        this.employeeName = employee.getFullName();
        this.payrollMonth = payrollMonth;
        
        // Initialize the helper classes
        this.salaryCalculation = new SalaryCalculation();
        this.deductionCalculation = new DeductionCalculation();
        this.csvProcessor = new CSVDatabaseProcessor();

        // Load attendance data 
        this.csvProcessor.loadAttendanceData();
    
        // Load employee data for position, department, and allowances
        loadEmployeeData();
        
        // Calculate the payroll values
        calculatePayroll();
    }
    
    /**
     * Sets pre-calculated payroll values directly
     * This is used when values are already calculated elsewhere
     * 
     * @param basicSalary The basic monthly salary
     * @param grossSalary The gross salary (basic + overtime)
     * @param sssDeduction The SSS contribution
     * @param philHealthDeduction The PhilHealth contribution
     * @param pagIbigDeduction The Pag-IBIG contribution
     * @param lateDeductions The late deductions
     * @param withholdingTax The withholding tax
     * @param totalDeductions The total deductions
     * @param taxableIncome The taxable income
     * @param netMonthlyPay The net monthly pay
     */
    public void setCalculatedValues(double basicSalary, double grossSalary, 
                                  double sssDeduction, double philHealthDeduction, 
                                  double pagIbigDeduction, double lateDeductions,
                                  double withholdingTax, double totalDeductions, 
                                  double taxableIncome, double netMonthlyPay) {
        this.basicSalary = basicSalary;
        this.grossSalary = grossSalary;
        this.sssDeduction = sssDeduction;
        this.philHealthDeduction = philHealthDeduction;
        this.pagIbigDeduction = pagIbigDeduction;
        this.lateDeductions = lateDeductions;
        this.withholdingTax = withholdingTax;
        this.totalDeductions = totalDeductions;
        this.taxableIncome = taxableIncome;
        this.netMonthlyPay = netMonthlyPay;
    }
    
    /**
     * Load employee data from CSV database
     * Loads position, department, hourly rate, and allowances
     */
    private void loadEmployeeData() {
        try {
            Map<String, String> employeeData = csvProcessor.getEmployeeRecordsByEmployeeId(employeeId);
            
            // Load position and department
            this.position = employeeData.getOrDefault("Position", "");
            this.department = employeeData.getOrDefault("Department", "");
            
            // Load hourly rate
            this.hourlyRate = parseAmount(employeeData.get("Hourly Rate"));
            
            // Load allowances
            this.riceSubsidy = parseAmount(employeeData.get("Rice Subsidy"));
            this.phoneAllowance = parseAmount(employeeData.get("Phone Allowance"));
            this.clothingAllowance = parseAmount(employeeData.get("Clothing Allowance"));
            this.totalAllowances = riceSubsidy + phoneAllowance + clothingAllowance;
        } catch (Exception e) {
            System.err.println("Error loading employee data: " + e.getMessage());
            e.printStackTrace();
            
            // Set default values in case of error
            this.position = "";
            this.department = "";
            this.hourlyRate = 0.0;
            this.riceSubsidy = 0.0;
            this.phoneAllowance = 0.0;
            this.clothingAllowance = 0.0;
            this.totalAllowances = 0.0;
        }
    }
    
    /**
     * Calculate all payroll values including gross pay, deductions, and net pay
     * Uses actual attendance records to calculate basic salary
     * Modified to match PayrollManagement calculations and sample calculator
     */
    private void calculatePayroll() {
        try {
            // Calculate gross salary using SalaryCalculation based on attendance records
            // This calculates hours worked * hourly rate + overtime
            this.grossSalary = salaryCalculation.calculateGrossMonthlySalary(employeeId, payrollMonth, csvProcessor);
            
            // Check if grossSalary is zero (no attendance records)
            if (this.grossSalary <= 0) {
                // If no attendance records are found, leave the values as zero
                System.out.println("No valid attendance records found for employee ID: " + 
                    employeeId + " in " + payrollMonth.getMonth() + " " + payrollMonth.getYear());

                // Reset all values to zero to ensure consistency with PayrollManagement
                this.grossSalary = 0.0;
                this.basicSalary = 0.0;
                this.sssDeduction = 0.0;
                this.philHealthDeduction = 0.0;
                this.pagIbigDeduction = 0.0;
                this.withholdingTax = 0.0;
                this.lateDeductions = 0.0;
                this.totalDeductions = 0.0;
                this.taxableIncome = 0.0;
                this.netMonthlyPay = 0.0;
            }
            
            // Store the calculated value as basic salary
            this.basicSalary = grossSalary;
            
            // Calculate government deductions based on gross salary BEFORE adding allowances
            this.sssDeduction = deductionCalculation.calculateSSS(grossSalary);
            this.philHealthDeduction = deductionCalculation.calculatePhilHealth(grossSalary);
            this.pagIbigDeduction = deductionCalculation.calculatePagibig(grossSalary);
            
            // Calculate late deductions from attendance records
            double lateHours = csvProcessor.getTotalLateHours(employeeId, payrollMonth);
            this.lateDeductions = deductionCalculation.calculateLateDeductions(lateHours, hourlyRate);
            
            // Calculate taxable income (gross salary minus government contributions AND late deductions)
            // UPDATED: Now subtracting late deductions before calculating taxable income
            double totalContributions = sssDeduction + philHealthDeduction + pagIbigDeduction;
            this.taxableIncome = grossSalary - totalContributions - lateDeductions;
            
            // Calculate withholding tax based on taxable income
            this.withholdingTax = deductionCalculation.calculateTax(taxableIncome);
            
            // Debug output
            System.out.println("Employee ID: " + employeeId + ", Gross Pay: " + grossSalary);
            System.out.println("SSS: " + sssDeduction + ", PhilHealth: " + philHealthDeduction + 
                ", Pag-Ibig: " + pagIbigDeduction + ", Tax: " + withholdingTax);
            if (lateHours > 0) {
                System.out.println("Late hours: " + lateHours + ", Late deduction: " + lateDeductions);
            }
            
            // Calculate total deductions (government contributions + late deductions + tax)
            this.totalDeductions = totalContributions + lateDeductions + withholdingTax;
            
            // Set gross monthly salary (including allowances) AFTER calculating deductions
            this.grossMonthlySalary = grossSalary + totalAllowances;
            
            // Calculate net pay: gross salary + allowances - deductions
            this.netMonthlyPay = grossSalary - totalDeductions;
            
        } catch (Exception e) {
            System.err.println("Error calculating payroll: " + e.getMessage());
            e.printStackTrace();
            
            // Set default values in case of error
            this.grossSalary = 0.0;
            this.grossMonthlySalary = 0.0;
            this.sssDeduction = 0.0;
            this.philHealthDeduction = 0.0;
            this.pagIbigDeduction = 0.0;
            this.taxableIncome = 0.0;
            this.withholdingTax = 0.0;
            this.lateDeductions = 0.0;
            this.totalDeductions = 0.0;
            this.netMonthlyPay = 0.0;
        }
    }
    
    /**
     * Get hourly rate from employee records
     */
    private double getHourlyRate() {
        return this.hourlyRate;
    }
    
    /**
     * Helper method to parse amount from string
     */
    private double parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return 0.0;
        }
        
        // Remove any commas, currency symbols, etc.
        String cleanedAmount = amountStr.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(cleanedAmount);
        } catch (NumberFormatException e) {
            System.err.println("Couldn't parse this as a number: " + amountStr);
            return 0.0;
        }
    }
    
    /**
     * Generate tax explanation based on tax bracket
     */
    public String getTaxExplanation() {
        if (taxableIncome <= 20832) {
            return "No withholding tax for income ₱20,832 and below";
        } 
        else if (taxableIncome <= 33332) {
            double excess = taxableIncome - 20833;
            return String.format("20%% of excess over ₱20,833: (%.2f - 20,833) × 20%% = %.2f", taxableIncome, withholdingTax);
        } 
        else if (taxableIncome <= 66666) {
            double excess = taxableIncome - 33333;
            return String.format("₱2,500 + 25%% of excess over ₱33,333: 2,500 + ((%.2f - 33,333) × 25%%) = %.2f", taxableIncome, withholdingTax);
        } 
        else if (taxableIncome <= 166666) {
            double excess = taxableIncome - 66667;
            return String.format("₱10,833 + 30%% of excess over ₱66,667: 10,833 + ((%.2f - 66,667) × 30%%) = %.2f", taxableIncome, withholdingTax);
        } 
        else if (taxableIncome <= 666666) {
            double excess = taxableIncome - 166667;
            return String.format("₱40,833.33 + 32%% of excess over ₱166,667: 40,833.33 + ((%.2f - 166,667) × 32%%) = %.2f", taxableIncome, withholdingTax);
        } 
        else {
            double excess = taxableIncome - 666667;
            return String.format("₱200,833.33 + 35%% of excess over ₱666,667: 200,833.33 + ((%.2f - 666,667) × 35%%) = %.2f", taxableIncome, withholdingTax);
        }
    }

    // Getter for payroll month
    public YearMonth getPayrollMonth() {
        return payrollMonth;
    }
    
    // Setter for payroll month
    public void setPayrollMonth(YearMonth payrollMonth) {
        this.payrollMonth = payrollMonth;
        calculatePayroll(); // Recalculate with new month
    }
    
    // Getter methods for all payroll components
    public String getEmployeeId() {
        return employeeId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public String getPosition() {
        return position;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public double getBasicSalary() {
        return basicSalary;
    }
    
    public double getRiceSubsidy() {
        return riceSubsidy;
    }
    
    public double getPhoneAllowance() {
        return phoneAllowance;
    }
    
    public double getClothingAllowance() {
        return clothingAllowance;
    }
    
    public double getTotalAllowances() {
        return totalAllowances;
    }
    
    public double getOvertimePay() {
        return overtimePay;
    }
    
    public double getGrossSalary() {
        return grossSalary;
    }
    
    public double getGrossMonthlySalary() {
        return grossMonthlySalary;
    }
    
    public double getSssDeduction() {
        return sssDeduction;
    }
    
    public double getPhilHealthDeduction() {
        return philHealthDeduction;
    }
    
    public double getPagIbigDeduction() {
        return pagIbigDeduction;
    }
    
    public double getTaxableIncome() {
        return taxableIncome;
    }
    
    public double getWithholdingTax() {
        return withholdingTax;
    }
    
    public double getLateDeductions() {
        return lateDeductions;
    }
    
    public double getTotalDeductions() {
        return totalDeductions;
    }
    
    public double getNetMonthlyPay() {
        return netMonthlyPay;
    }
    
    // Returns a formatted summary of payroll details
    @Override
    public String toString() {
        return "PayrollSummary{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", payrollMonth=" + payrollMonth +
                ", grossMonthlySalary=" + grossMonthlySalary +
                ", netMonthlyPay=" + netMonthlyPay +
                ", totalDeductions=" + totalDeductions +
                '}';
    }
}