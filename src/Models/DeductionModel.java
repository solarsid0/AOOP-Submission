/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * DeductionModel class that maps to the deduction table
 * Fields: deductionId, employeeId, deductionType, amount, payPeriodId
 * Includes deduction logic
 * @author User
 */
public class DeductionModel {
    
    // Enum for deduction types to match database constraints
    public enum DeductionType {
        SSS("SSS"),
        PHILHEALTH("PhilHealth"),
        PAG_IBIG("Pag-Ibig"),
        WITHHOLDING_TAX("Withholding Tax"),
        LATE_DEDUCTION("Late Deduction");
        
        private final String displayName;
        
        DeductionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static DeductionType fromString(String text) {
            for (DeductionType d : DeductionType.values()) {
                if (d.displayName.equalsIgnoreCase(text)) {
                    return d;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    private Integer deductionId;
    private Integer employeeId;
    private DeductionType deductionType;
    private BigDecimal amount;
    private Integer payPeriodId;
    
    // Additional fields for tax bracket calculations
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private BigDecimal baseTax;
    private BigDecimal deductionRate;
    
    // Constructors
    public DeductionModel() {}
    
    public DeductionModel(Integer employeeId, DeductionType deductionType, BigDecimal amount, Integer payPeriodId) {
        this.employeeId = employeeId;
        this.deductionType = deductionType;
        this.amount = amount;
        this.payPeriodId = payPeriodId;
    }
    
    public DeductionModel(Integer deductionId, Integer employeeId, DeductionType deductionType, 
                         BigDecimal amount, Integer payPeriodId) {
        this.deductionId = deductionId;
        this.employeeId = employeeId;
        this.deductionType = deductionType;
        this.amount = amount;
        this.payPeriodId = payPeriodId;
    }
    
    // Full constructor with tax bracket fields
    public DeductionModel(Integer deductionId, Integer employeeId, DeductionType deductionType, 
                         BigDecimal amount, Integer payPeriodId, BigDecimal lowerLimit, 
                         BigDecimal upperLimit, BigDecimal baseTax, BigDecimal deductionRate) {
        this.deductionId = deductionId;
        this.employeeId = employeeId;
        this.deductionType = deductionType;
        this.amount = amount;
        this.payPeriodId = payPeriodId;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.baseTax = baseTax;
        this.deductionRate = deductionRate;
    }
    
    // Getters and Setters
    public Integer getDeductionId() { return deductionId; }
    public void setDeductionId(Integer deductionId) { this.deductionId = deductionId; }
    
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    public DeductionType getDeductionType() { return deductionType; }
    public void setDeductionType(DeductionType deductionType) { this.deductionType = deductionType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public Integer getPayPeriodId() { return payPeriodId; }
    public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
    
    public BigDecimal getLowerLimit() { return lowerLimit; }
    public void setLowerLimit(BigDecimal lowerLimit) { this.lowerLimit = lowerLimit; }
    
    public BigDecimal getUpperLimit() { return upperLimit; }
    public void setUpperLimit(BigDecimal upperLimit) { this.upperLimit = upperLimit; }
    
    public BigDecimal getBaseTax() { return baseTax; }
    public void setBaseTax(BigDecimal baseTax) { this.baseTax = baseTax; }
    
    public BigDecimal getDeductionRate() { return deductionRate; }
    public void setDeductionRate(BigDecimal deductionRate) { this.deductionRate = deductionRate; }
    
    // Business Methods - Deduction Logic
    
    /**
     * Calculate SSS deduction based on salary
     * @param monthlySalary
     * @return 
     */
    public static BigDecimal calculateSSSDeduction(BigDecimal monthlySalary) {
        if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // SSS salary credit brackets (simplified)
        BigDecimal salaryCredit;
        if (monthlySalary.compareTo(new BigDecimal("3250")) <= 0) {
            salaryCredit = new BigDecimal("3000");
        } else if (monthlySalary.compareTo(new BigDecimal("29750")) >= 0) {
            salaryCredit = new BigDecimal("29700");
        } else {
            // Round to nearest 500
            salaryCredit = monthlySalary.divide(new BigDecimal("500"), 0, BigDecimal.ROUND_HALF_UP)
                                      .multiply(new BigDecimal("500"));
        }
        
        // Employee contribution: 4.5%
        return salaryCredit.multiply(new BigDecimal("0.045")).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate PhilHealth deduction based on salary
     * @param monthlySalary
     * @return 
     */
    public static BigDecimal calculatePhilHealthDeduction(BigDecimal monthlySalary) {
        if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal minContribution = new BigDecimal("500.00");
        BigDecimal maxContribution = new BigDecimal("5000.00");
        
        BigDecimal premium = monthlySalary.multiply(new BigDecimal("0.05"));
        
        if (premium.compareTo(minContribution) < 0) {
            premium = minContribution;
        } else if (premium.compareTo(maxContribution) > 0) {
            premium = maxContribution;
        }
        
        // Employee share: 50% of total premium
        return premium.multiply(new BigDecimal("0.5")).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate Pag-IBIG deduction based on salary
     * @param monthlySalary
     * @return 
     */
    public static BigDecimal calculatePagIbigDeduction(BigDecimal monthlySalary) {
        if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal threshold = new BigDecimal("1500.00");
        
        if (monthlySalary.compareTo(threshold) <= 0) {
            // 1% for salaries ≤ ₱1,500
            return monthlySalary.multiply(new BigDecimal("0.01")).setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            // 2% for salaries > ₱1,500, max ₱100
            BigDecimal contribution = monthlySalary.multiply(new BigDecimal("0.02"));
            BigDecimal maxContribution = new BigDecimal("100.00");
            
            return contribution.compareTo(maxContribution) > 0 ? maxContribution : 
                   contribution.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Calculate withholding tax based on salary
     * @param monthlySalary
     * @return 
     */
    public static BigDecimal calculateWithholdingTax(BigDecimal monthlySalary) {
        if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal tax = BigDecimal.ZERO;
        
        // Tax brackets (monthly)
        BigDecimal[] brackets = {
            new BigDecimal("20833"),   // ₱250,000 annually / 12
            new BigDecimal("33333"),   // ₱400,000 annually / 12
            new BigDecimal("66667"),   // ₱800,000 annually / 12
            new BigDecimal("166667"),  // ₱2,000,000 annually / 12
            new BigDecimal("666667")   // ₱8,000,000 annually / 12
        };
        
        BigDecimal[] rates = {
            new BigDecimal("0.00"),    // 0%
            new BigDecimal("0.20"),    // 20%
            new BigDecimal("0.25"),    // 25%
            new BigDecimal("0.30"),    // 30%
            new BigDecimal("0.32"),    // 32%
            new BigDecimal("0.35")     // 35%
        };
        
        BigDecimal[] baseTax = {
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("2500.00"),
            new BigDecimal("10833.33"),
            new BigDecimal("40833.33"),
            new BigDecimal("200833.33")
        };
        
        for (int i = 0; i < brackets.length; i++) {
            if (monthlySalary.compareTo(brackets[i]) <= 0) {
                if (i == 0) {
                    tax = BigDecimal.ZERO;
                } else {
                    BigDecimal excess = monthlySalary.subtract(i > 0 ? brackets[i-1] : BigDecimal.ZERO);
                    tax = baseTax[i].add(excess.multiply(rates[i]));
                }
                break;
            }
        }
        
        // If salary exceeds highest bracket
        if (monthlySalary.compareTo(brackets[brackets.length - 1]) > 0) {
            BigDecimal excess = monthlySalary.subtract(brackets[brackets.length - 1]);
            tax = baseTax[baseTax.length - 1].add(excess.multiply(rates[rates.length - 1]));
        }
        
        return tax.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate late deduction based on hours and rate
     * @param hourlyRate
     * @param hoursLate
     * @return 
     */
    public static BigDecimal calculateLateDeduction(BigDecimal hourlyRate, BigDecimal hoursLate) {
        if (hourlyRate == null || hoursLate == null || 
            hourlyRate.compareTo(BigDecimal.ZERO) <= 0 || hoursLate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return hourlyRate.multiply(hoursLate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Check if this deduction is within valid salary range
     * @param salary
     * @return 
     */
    public boolean isWithinRange(BigDecimal salary) {
        if (salary == null) return false;
        
        boolean withinLower = (lowerLimit == null) || (salary.compareTo(lowerLimit) >= 0);
        boolean withinUpper = (upperLimit == null) || (salary.compareTo(upperLimit) <= 0);
        
        return withinLower && withinUpper;
    }
    
    /**
     * Check if this is a government mandated deduction
     * @return 
     */
    public boolean isMandatoryDeduction() {
        return deductionType == DeductionType.SSS || 
               deductionType == DeductionType.PHILHEALTH || 
               deductionType == DeductionType.PAG_IBIG || 
               deductionType == DeductionType.WITHHOLDING_TAX;
    }
    
    /**
     * Get formatted deduction description
     * @return 
     */
    public String getFormattedDescription() {
        return String.format("%s: ₱%s", deductionType.getDisplayName(), amount);
    }
    
    @Override
    public String toString() {
        return String.format("DeductionModel{deductionId=%d, employeeId=%d, deductionType=%s, amount=%s, payPeriodId=%d}", 
                           deductionId, employeeId, deductionType, amount, payPeriodId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DeductionModel that = (DeductionModel) obj;
        return Objects.equals(deductionId, that.deductionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(deductionId);
    }
}
