/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Models;

import java.util.Objects;

/**
 * AddressModel class that maps to the address table
 * Fields: addressId, street, barangay, city, province, zipCode
 * @author User
 */
public class AddressModel {
    
    private Integer addressId;
    private String street;
    private String barangay;
    private String city;
    private String province;
    private String zipCode;
    
    // Constructors
    public AddressModel() {}
    
    public AddressModel(String street, String barangay, String city, String province, String zipCode) {
        this.street = street;
        this.barangay = barangay;
        this.city = city;
        this.province = province;
        this.zipCode = zipCode;
    }
    
    public AddressModel(Integer addressId, String street, String barangay, String city, String province, String zipCode) {
        this.addressId = addressId;
        this.street = street;
        this.barangay = barangay;
        this.city = city;
        this.province = province;
        this.zipCode = zipCode;
    }
    
    // Getters and Setters
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    
    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    // Business Methods
    
    /**
     * Get full address as formatted string
     * @return 
     */
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        
        if (street != null && !street.trim().isEmpty()) {
            fullAddress.append(street);
        }
        
        if (barangay != null && !barangay.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(barangay);
        }
        
        if (city != null && !city.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(city);
        }
        
        if (province != null && !province.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(province);
        }
        
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(" ");
            fullAddress.append(zipCode);
        }
        
        return fullAddress.toString();
    }
    
    /**
     * Check if address is valid (has minimum required fields)
     * @return 
     */
    public boolean isValid() {
        return city != null && !city.trim().isEmpty() && 
               province != null && !province.trim().isEmpty();
    }
    
    /**
     * Get formatted address for display
     * @return 
     */
    public String getDisplayAddress() {
        return getFullAddress();
    }
    
    @Override
    public String toString() {
        return String.format("AddressModel{addressId=%d, street='%s', barangay='%s', city='%s', province='%s', zipCode='%s'}", 
                           addressId, street, barangay, city, province, zipCode);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AddressModel address = (AddressModel) obj;
        return Objects.equals(addressId, address.addressId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(addressId);
    }
}
