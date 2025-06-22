/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * BenefitTypeModel class that maps to the benefittype table
 * Fields: benefitTypeId, benefitName, description, amount
 * Includes benefit calculations
 * @author User
 */
public class BenefitTypeModel {
    
    // Enum for benefit names to match database constraints
    public enum BenefitName {
        RICE_SUBSIDY("Rice Subsidy"),
        PHONE_ALLOWANCE("Phone Allowance"),
        CLOTHING_ALLOWANCE("Clothing Allowance");
        
        private final String displayName;
        
        BenefitName(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static BenefitName fromString(String text) {
            for (BenefitName b : BenefitName.values()) {
                if (b.displayName.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    private Integer benefitTypeId;
    private BenefitName benefitName;
    private String benefitDescription;
    private BigDecimal amount; // Default amount for this benefit type
    
    // Constructors
    public BenefitTypeModel() {}
    
    public BenefitTypeModel(BenefitName benefitName, String benefitDescription) {
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
    }
    
    public BenefitTypeModel(BenefitName benefitName, String benefitDescription, BigDecimal amount) {
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
        this.amount = amount;
    }
    
    public BenefitTypeModel(Integer benefitTypeId, BenefitName benefitName, String benefitDescription, BigDecimal amount) {
        this.benefitTypeId = benefitTypeId;
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
        this.amount = amount;
    }
    
    // Getters and Setters
    public Integer getBenefitTypeId() { return benefitTypeId; }
    public void setBenefitTypeId(Integer benefitTypeId) { this.benefitTypeId = benefitTypeId; }
    
    public BenefitName getBenefitName() { return benefitName; }
    public void setBenefitName(BenefitName benefitName) { this.benefitName = benefitName; }
    
    public String getBenefitDescription() { return benefitDescription; }
    public void setBenefitDescription(String benefitDescription) { this.benefitDescription = benefitDescription; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    // Business Methods - Benefit Calculations
    
    /**
     * Calculate prorated benefit amount based on days worked
     * @param daysWorked
     * @param totalDaysInPeriod
     * @return 
     */
    public BigDecimal calculateProratedBenefit(int daysWorked, int totalDaysInPeriod) {
        if (totalDaysInPeriod <= 0 || amount == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal proration = new BigDecimal(daysWorked).divide(new BigDecimal(totalDaysInPeriod), 4, BigDecimal.ROUND_HALF_UP);
        return amount.multiply(proration).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate benefit based on percentage of basic salary
     * @param basicSalary
     * @param percentage
     * @return 
     */
    public BigDecimal calculatePercentageBenefit(BigDecimal basicSalary, BigDecimal percentage) {
        if (basicSalary == null || percentage == null) {
            return BigDecimal.ZERO;
        }
        
        return basicSalary.multiply(percentage.divide(new BigDecimal(100), 4, BigDecimal.ROUND_HALF_UP))
                          .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Check if this benefit type has a fixed amount
     * @return 
     */
    public boolean hasFixedAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted description with amount info
     * @return 
     */
    public String getFormattedDescription() {
        StringBuilder description = new StringBuilder();
        
        if (benefitDescription != null && !benefitDescription.trim().isEmpty()) {
            description.append(benefitDescription);
        }
        
        if (hasFixedAmount()) {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("₱").append(amount);
        }
        
        return description.toString();
    }
    
    /**
     * Validate benefit type data
     * @return 
     */
    public boolean isValid() {
        return benefitName != null &&
               benefitDescription != null && !benefitDescription.trim().isEmpty() &&
               benefitDescription.length() <= 255;
    }
    
    @Override
    public String toString() {
        return String.format("BenefitTypeModel{benefitTypeId=%d, benefitName=%s, benefitDescription='%s', amount=%s}", 
                           benefitTypeId, benefitName, benefitDescription, amount);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BenefitTypeModel that = (BenefitTypeModel) obj;
        return Objects.equals(benefitTypeId, that.benefitTypeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(benefitTypeId);
    }
}