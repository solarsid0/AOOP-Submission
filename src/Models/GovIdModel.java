/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.util.*;
import java.util.regex.Pattern;

/**
 * GovIdModel class that maps to the govid table
 * Fields: govIdId, employeeId, idType, idNumber
 * Handles multiple government IDs per employee
 * @author User
 */
public class GovIdModel {
    
    private Integer govId;
    private String sss;
    private String philhealth;
    private String tin;
    private String pagibig;
    private Integer employeeId;
    
    // Validation patterns for Philippine government IDs
    private static final Pattern SSS_PATTERN = Pattern.compile("^\\d{2}-\\d{7}-\\d{1}$");
    private static final Pattern PHILHEALTH_PATTERN = Pattern.compile("^\\d{2}-\\d{9}-\\d{1}$");
    private static final Pattern TIN_PATTERN = Pattern.compile("^\\d{3}-\\d{3}-\\d{3}-\\d{3}$");
    private static final Pattern PAGIBIG_PATTERN = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}$");
    
    // Constructors
    public GovIdModel() {}
    
    public GovIdModel(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public GovIdModel(String sss, String philhealth, String tin, String pagibig, Integer employeeId) {
        this.sss = sss;
        this.philhealth = philhealth;
        this.tin = tin;
        this.pagibig = pagibig;
        this.employeeId = employeeId;
    }
    
    public GovIdModel(Integer govId, String sss, String philhealth, String tin, String pagibig, Integer employeeId) {
        this.govId = govId;
        this.sss = sss;
        this.philhealth = philhealth;
        this.tin = tin;
        this.pagibig = pagibig;
        this.employeeId = employeeId;
    }
    
    // Getters and Setters
    public Integer getGovId() { return govId; }
    public void setGovId(Integer govId) { this.govId = govId; }
    
    public String getSss() { return sss; }
    public void setSss(String sss) { this.sss = sss; }
    
    public String getPhilhealth() { return philhealth; }
    public void setPhilhealth(String philhealth) { this.philhealth = philhealth; }
    
    public String getTin() { return tin; }
    public void setTin(String tin) { this.tin = tin; }
    
    public String getPagibig() { return pagibig; }
    public void setPagibig(String pagibig) { this.pagibig = pagibig; }
    
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    // Business Methods - Validation
    
    /**
     * Validate SSS format (XX-XXXXXXX-X)
     * @param sss
     * @return 
     */
    public static boolean isValidSss(String sss) {
        return sss != null && SSS_PATTERN.matcher(sss).matches();
    }
    
    /**
     * Validate PhilHealth format (XX-XXXXXXXXX-X)
     * @param philhealth
     * @return 
     */
    public static boolean isValidPhilhealth(String philhealth) {
        return philhealth != null && PHILHEALTH_PATTERN.matcher(philhealth).matches();
    }
    
    /**
     * Validate TIN format (XXX-XXX-XXX-XXX)
     * @param tin
     * @return 
     */
    public static boolean isValidTin(String tin) {
        return tin != null && TIN_PATTERN.matcher(tin).matches();
    }
    
    /**
     * Validate Pag-IBIG format (XXXX-XXXX-XXXX)
     * @param pagibig
     * @return 
     */
    public static boolean isValidPagibig(String pagibig) {
        return pagibig != null && PAGIBIG_PATTERN.matcher(pagibig).matches();
    }
    
    /**
     * Validate all IDs in this record
     * @return 
     */
    public boolean validateAllIds() {
        boolean valid = true;
        
        if (sss != null && !sss.trim().isEmpty() && !isValidSss(sss)) {
            valid = false;
        }
        
        if (philhealth != null && !philhealth.trim().isEmpty() && !isValidPhilhealth(philhealth)) {
            valid = false;
        }
        
        if (tin != null && !tin.trim().isEmpty() && !isValidTin(tin)) {
            valid = false;
        }
        
        if (pagibig != null && !pagibig.trim().isEmpty() && !isValidPagibig(pagibig)) {
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Get complete ID information as map
     * @return 
     */
    public Map<String, String> getCompleteIdInfo() {
        Map<String, String> idInfo = new HashMap<>();
        
        idInfo.put("sss", this.sss != null ? this.sss : "");
        idInfo.put("philhealth", this.philhealth != null ? this.philhealth : "");
        idInfo.put("tin", this.tin != null ? this.tin : "");
        idInfo.put("pagibig", this.pagibig != null ? this.pagibig : "");
        
        return idInfo;
    }
    
    /**
     * Check if all mandatory IDs are present
     * @return 
     */
    public boolean hasAllMandatoryIds() {
        return (sss != null && !sss.trim().isEmpty()) &&
               (philhealth != null && !philhealth.trim().isEmpty()) &&
               (tin != null && !tin.trim().isEmpty()) &&
               (pagibig != null && !pagibig.trim().isEmpty());
    }
    
    /**
     * Get missing mandatory IDs
     * @return 
     */
    public List<String> getMissingMandatoryIds() {
        List<String> missing = new ArrayList<>();
        
        if (sss == null || sss.trim().isEmpty()) {
            missing.add("SSS");
        }
        if (philhealth == null || philhealth.trim().isEmpty()) {
            missing.add("PhilHealth");
        }
        if (tin == null || tin.trim().isEmpty()) {
            missing.add("TIN");
        }
        if (pagibig == null || pagibig.trim().isEmpty()) {
            missing.add("Pag-IBIG");
        }
        
        return missing;
    }
    
    // Format ID numbers with proper separators
    
    /**
     * Format SSS number
     * @param rawSss
     * @return 
     */
    public static String formatSss(String rawSss) {
        if (rawSss == null) return null;
        
        String clean = rawSss.replaceAll("[^0-9]", "");
        if (clean.length() == 10) {
            return clean.substring(0, 2) + "-" + clean.substring(2, 9) + "-" + clean.substring(9);
        }
        return rawSss; // Return as-is if not valid length
    }
    
    /**
     * Format TIN number
     * @param rawTin
     * @return 
     */
    public static String formatTin(String rawTin) {
        if (rawTin == null) return null;
        
        String clean = rawTin.replaceAll("[^0-9]", "");
        if (clean.length() == 12) {
            return clean.substring(0, 3) + "-" + clean.substring(3, 6) + 
                   "-" + clean.substring(6, 9) + "-" + clean.substring(9);
        }
        return rawTin; // Return as-is if not valid length
    }
    
    /**
     * Format PhilHealth number
     * @param rawPhilhealth
     * @return 
     */
    public static String formatPhilhealth(String rawPhilhealth) {
        if (rawPhilhealth == null) return null;
        
        String clean = rawPhilhealth.replaceAll("[^0-9]", "");
        if (clean.length() == 12) {
            return clean.substring(0, 2) + "-" + clean.substring(2, 11) + "-" + clean.substring(11);
        }
        return rawPhilhealth; // Return as-is if not valid length
    }
    
    /**
     * Format Pag-IBIG number
     * @param rawPagibig
     * @return 
     */
    public static String formatPagibig(String rawPagibig) {
        if (rawPagibig == null) return null;
        
        String clean = rawPagibig.replaceAll("[^0-9]", "");
        if (clean.length() == 12) {
            return clean.substring(0, 4) + "-" + clean.substring(4, 8) + "-" + clean.substring(8);
        }
        return rawPagibig; // Return as-is if not valid length
    }
    
    /**
     * Get formatted display of all IDs
     * @return 
     */
    public String getFormattedIdDisplay() {
        StringBuilder display = new StringBuilder();
        
        if (sss != null && !sss.trim().isEmpty()) {
            display.append("SSS: ").append(sss).append("\n");
        }
        
        if (philhealth != null && !philhealth.trim().isEmpty()) {
            display.append("PhilHealth: ").append(philhealth).append("\n");
        }
        
        if (tin != null && !tin.trim().isEmpty()) {
            display.append("TIN: ").append(tin).append("\n");
        }
        
        if (pagibig != null && !pagibig.trim().isEmpty()) {
            display.append("Pag-IBIG: ").append(pagibig).append("\n");
        }
        
        return display.toString().trim();
    }
    
    /**
     * Check if record has at least one government ID
     * @return 
     */
    public boolean hasAnyId() {
        return (sss != null && !sss.trim().isEmpty()) ||
               (philhealth != null && !philhealth.trim().isEmpty()) ||
               (tin != null && !tin.trim().isEmpty()) ||
               (pagibig != null && !pagibig.trim().isEmpty());
    }
    
    @Override
    public String toString() {
        return String.format("GovIdModel{govId=%d, sss='%s', philhealth='%s', tin='%s', pagibig='%s', employeeId=%d}", 
                           govId, sss, philhealth, tin, pagibig, employeeId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GovIdModel govIdObj = (GovIdModel) obj;
        return Objects.equals(govId, govIdObj.govId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(govId);
    }
}
