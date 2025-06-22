/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.util.Objects;

/**
 * EmployeeAddressModel class that maps to the employeeaddress table
 * Fields: employeeAddressId, employeeId, addressId, addressType
 * Handles address associations
 * @author User
 */
public class EmployeeAddressModel {
    
    private Integer employeeId;
    private Integer addressId;
    
    // For convenience, we can include the actual objects (will be loaded by DAO if needed)
    private EmployeeModel employee;
    private AddressModel address;
    
    // Address type enumeration for different types of addresses
    public enum AddressType {
        CURRENT("Current"),
        PERMANENT("Permanent"),
        EMERGENCY("Emergency");
        
        private final String displayName;
        
        AddressType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static AddressType fromString(String text) {
            for (AddressType type : AddressType.values()) {
                if (type.displayName.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    // Constructors
    public EmployeeAddressModel() {}
    
    public EmployeeAddressModel(Integer employeeId, Integer addressId) {
        this.employeeId = employeeId;
        this.addressId = addressId;
    }
    
    public EmployeeAddressModel(EmployeeModel employee, AddressModel address) {
        this.employee = employee;
        this.address = address;
        if (employee != null) this.employeeId = employee.getEmployeeId();
        if (address != null) this.addressId = address.getAddressId();
    }
    
    // Getters and Setters
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    
    public EmployeeModel getEmployee() { return employee; }
    public void setEmployee(EmployeeModel employee) { 
        this.employee = employee;
        if (employee != null) this.employeeId = employee.getEmployeeId();
    }
    
    public AddressModel getAddress() { return address; }
    public void setAddress(AddressModel address) { 
        this.address = address;
        if (address != null) this.addressId = address.getAddressId();
    }
    
    // Business Methods
    
    /**
     * Check if both employee and address IDs are valid
     * @return 
     */
    public boolean isValid() {
        return employeeId != null && employeeId > 0 && 
               addressId != null && addressId > 0;
    }
    
    /**
     * Get employee name if employee object is loaded
     * @return 
     */
    public String getEmployeeName() {
        if (employee != null) {
            return employee.getFirstName() + " " + employee.getLastName();
        }
        return "Employee ID: " + employeeId;
    }
    
    /**
     * Get formatted address if address object is loaded
     * @return 
     */
    public String getFormattedAddress() {
        if (address != null) {
            return address.getFullAddress();
        }
        return "Address ID: " + addressId;
    }
    
    /**
     * Check if this association has complete information loaded
     * @return 
     */
    public boolean isFullyLoaded() {
        return employee != null && address != null;
    }
    
    /**
     * Create a display summary of this employee-address association
     * @return 
     */
    public String getDisplaySummary() {
        StringBuilder summary = new StringBuilder();
        
        if (employee != null) {
            summary.append(employee.getFirstName()).append(" ").append(employee.getLastName());
        } else {
            summary.append("Employee ID: ").append(employeeId);
        }
        
        summary.append(" - ");
        
        if (address != null) {
            summary.append(address.getFullAddress());
        } else {
            summary.append("Address ID: ").append(addressId);
        }
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return String.format("EmployeeAddressModel{employeeId=%d, addressId=%d}", employeeId, addressId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        EmployeeAddressModel that = (EmployeeAddressModel) obj;
        return Objects.equals(employeeId, that.employeeId) && 
               Objects.equals(addressId, that.addressId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(employeeId, addressId);
    }
}

