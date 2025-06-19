package Models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model class representing a payslip record in the database.
 * This class contains detailed breakdown of an employee's pay for a specific period.
 * @author User
 */
public class PayslipModel {
    
    // Primary key
    private Integer payslipId;
    
    // Employee information
    private String employeeName;
    
    // Pay period information
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    // Salary information
    private BigDecimal monthlyRate;
    private BigDecimal dailyRate;
    private Integer daysWorked;
    
    // Earnings
    private BigDecimal overtime;
    
    // Benefits/Allowances
    private BigDecimal riceSubsidy;
    private BigDecimal phoneAllowance;
    private BigDecimal clothingAllowance;
    
    // Deductions
    private BigDecimal sss;
    private BigDecimal philhealth;
    private BigDecimal pagibig;
    private BigDecimal withholdingTax;
    
    // Totals
    private BigDecimal grossIncome;
    private BigDecimal takeHomePay;
    
    // Foreign keys
    private Integer payPeriodId;
    private Integer payrollId;
    private Integer employeeId;
    private Integer positionId;
    
    // Default constructor
    public PayslipModel() {
        // Initialize BigDecimal fields to ZERO to prevent null pointer exceptions
        this.monthlyRate = BigDecimal.ZERO;
        this.dailyRate = BigDecimal.ZERO;
        this.overtime = BigDecimal.ZERO;
        this.riceSubsidy = BigDecimal.ZERO;
        this.phoneAllowance = BigDecimal.ZERO;
        this.clothingAllowance = BigDecimal.ZERO;
        this.sss = BigDecimal.ZERO;
        this.philhealth = BigDecimal.ZERO;
        this.pagibig = BigDecimal.ZERO;
        this.withholdingTax = BigDecimal.ZERO;
        this.grossIncome = BigDecimal.ZERO;
        this.takeHomePay = BigDecimal.ZERO;
        this.daysWorked = 0;
    }
    
    // Constructor with basic information
    public PayslipModel(Integer employeeId, String employeeName, Integer payPeriodId) {
        this();
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.payPeriodId = payPeriodId;
    }
    
    // Getters and Setters
    
    public Integer getPayslipId() {
        return payslipId;
    }
    
    public void setPayslipId(Integer payslipId) {
        this.payslipId = payslipId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public LocalDate getPeriodStart() {
        return periodStart;
    }
    
    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }
    
    public LocalDate getPeriodEnd() {
        return periodEnd;
    }
    
    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
    
    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }
    
    public void setMonthlyRate(BigDecimal monthlyRate) {
        this.monthlyRate = monthlyRate != null ? monthlyRate : BigDecimal.ZERO;
    }
    
    public BigDecimal getDailyRate() {
        return dailyRate;
    }
    
    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate != null ? dailyRate : BigDecimal.ZERO;
    }
    
    public Integer getDaysWorked() {
        return daysWorked;
    }
    
    public void setDaysWorked(Integer daysWorked) {
        this.daysWorked = daysWorked != null ? daysWorked : 0;
    }
    
    public BigDecimal getOvertime() {
        return overtime;
    }
    
    public void setOvertime(BigDecimal overtime) {
        this.overtime = overtime != null ? overtime : BigDecimal.ZERO;
    }
    
    public BigDecimal getRiceSubsidy() {
        return riceSubsidy;
    }
    
    public void setRiceSubsidy(BigDecimal riceSubsidy) {
        this.riceSubsidy = riceSubsidy != null ? riceSubsidy : BigDecimal.ZERO;
    }
    
    public BigDecimal getPhoneAllowance() {
        return phoneAllowance;
    }
    
    public void setPhoneAllowance(BigDecimal phoneAllowance) {
        this.phoneAllowance = phoneAllowance != null ? phoneAllowance : BigDecimal.ZERO;
    }
    
    public BigDecimal getClothingAllowance() {
        return clothingAllowance;
    }
    
    public void setClothingAllowance(BigDecimal clothingAllowance) {
        this.clothingAllowance = clothingAllowance != null ? clothingAllowance : BigDecimal.ZERO;
    }
    
    public BigDecimal getSss() {
        return sss;
    }
    
    public void setSss(BigDecimal sss) {
        this.sss = sss != null ? sss : BigDecimal.ZERO;
    }
    
    public BigDecimal getPhilhealth() {
        return philhealth;
    }
    
    public void setPhilhealth(BigDecimal philhealth) {
        this.philhealth = philhealth != null ? philhealth : BigDecimal.ZERO;
    }
    
    public BigDecimal getPagibig() {
        return pagibig;
    }
    
    public void setPagibig(BigDecimal pagibig) {
        this.pagibig = pagibig != null ? pagibig : BigDecimal.ZERO;
    }
    
    public BigDecimal getWithholdingTax() {
        return withholdingTax;
    }
    
    public void setWithholdingTax(BigDecimal withholdingTax) {
        this.withholdingTax = withholdingTax != null ? withholdingTax : BigDecimal.ZERO;
    }
    
    public BigDecimal getGrossIncome() {
        return grossIncome;
    }
    
    public void setGrossIncome(BigDecimal grossIncome) {
        this.grossIncome = grossIncome != null ? grossIncome : BigDecimal.ZERO;
    }
    
    public BigDecimal getTakeHomePay() {
        return takeHomePay;
    }
    
    public void setTakeHomePay(BigDecimal takeHomePay) {
        this.takeHomePay = takeHomePay != null ? takeHomePay : BigDecimal.ZERO;
    }
    
    public Integer getPayPeriodId() {
        return payPeriodId;
    }
    
    public void setPayPeriodId(Integer payPeriodId) {
        this.payPeriodId = payPeriodId;
    }
    
    public Integer getPayrollId() {
        return payrollId;
    }
    
    public void setPayrollId(Integer payrollId) {
        this.payrollId = payrollId;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public Integer getPositionId() {
        return positionId;
    }
    
    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }
    
    // Utility methods
    
    /**
     * Returns formatted pay period string (e.g., "January 1, 2024 - January 31, 2024")
     * @return 
     */
    public String getFormattedPayPeriod() {
        if (periodStart != null && periodEnd != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            return periodStart.format(formatter) + " - " + periodEnd.format(formatter);
        }
        return "N/A";
    }
    
    /**
     * Calculates total deductions
     * @return 
     */
    public BigDecimal getTotalDeductions() {
        return sss.add(philhealth).add(pagibig).add(withholdingTax);
    }
    
    /**
     * Calculates total benefits/allowances
     * @return 
     */
    public BigDecimal getTotalBenefits() {
        return riceSubsidy.add(phoneAllowance).add(clothingAllowance);
    }
    
    /**
     * Calculates basic pay (days worked * daily rate)
     * @return 
     */
    public BigDecimal getBasicPay() {
        return dailyRate.multiply(new BigDecimal(daysWorked));
    }
    
    /**
     * Validates that all required fields are set
     * @return 
     */
    public boolean isValid() {
        return employeeId != null && 
               employeeName != null && !employeeName.trim().isEmpty() &&
               payPeriodId != null &&
               periodStart != null &&
               periodEnd != null &&
               monthlyRate != null &&
               grossIncome != null &&
               takeHomePay != null;
    }
    
    @Override
    public String toString() {
        return "PayslipModel{" +
                "payslipId=" + payslipId +
                ", employeeName='" + employeeName + '\'' +
                ", employeeId=" + employeeId +
                ", payPeriodId=" + payPeriodId +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", grossIncome=" + grossIncome +
                ", takeHomePay=" + takeHomePay +
                ", daysWorked=" + daysWorked +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayslipModel that = (PayslipModel) obj;
        return payslipId != null ? payslipId.equals(that.payslipId) : that.payslipId == null;
    }
    
    @Override
    public int hashCode() {
        return payslipId != null ? payslipId.hashCode() : 0;
    }
}