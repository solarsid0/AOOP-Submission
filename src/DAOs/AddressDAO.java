/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAOs;

import Models.AddressModel;
import java.sql.*;
import java.util.*;

/**
 * AddressDAO - Data Access Object for AddressModel
 * Handles all database operations for addresses
 * @author User
 */
    
    public class AddressDAO {
    // Remove this unused field
    // private final DatabaseConnection dbConnection;
    
    public AddressDAO() {
    }
    
    /**
     * Create - Insert new address into database
     * @param address
     * @return 
     */
    public boolean save(AddressModel address) {
        String sql = "INSERT INTO address (street, barangay, city, province, zipCode) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, address.getStreet());
            pstmt.setString(2, address.getBarangay());
            pstmt.setString(3, address.getCity());
            pstmt.setString(4, address.getProvince());
            pstmt.setString(5, address.getZipCode());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Get the generated address ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        address.setAddressId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving address: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Read - Find address by ID
     * @param addressId
     * @return 
     */
    public AddressModel findById(int addressId) {
        String sql = "SELECT * FROM address WHERE addressId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, addressId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToAddress(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding address: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Get all addresses
     * @return 
     */
    public List<AddressModel> findAll() {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address ORDER BY city, province";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Read - Find addresses by city
     * @param city
     * @return 
     */
    public List<AddressModel> findByCity(String city) {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address WHERE city = ? ORDER BY street";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, city);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding addresses by city: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Read - Find addresses by province
     * @param province
     * @return 
     */
    public List<AddressModel> findByProvince(String province) {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address WHERE province = ? ORDER BY city, street";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, province);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding addresses by province: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Update - Update existing address
     * @param address
     * @return 
     */
    public boolean update(AddressModel address) {
        if (address.getAddressId() == null || address.getAddressId() <= 0) {
            System.err.println("Cannot update address: Invalid address ID");
            return false;
        }
        
        String sql = "UPDATE address SET street = ?, barangay = ?, city = ?, province = ?, zipCode = ? WHERE addressId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, address.getStreet());
            pstmt.setString(2, address.getBarangay());
            pstmt.setString(3, address.getCity());
            pstmt.setString(4, address.getProvince());
            pstmt.setString(5, address.getZipCode());
            pstmt.setInt(6, address.getAddressId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating address: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Delete - Remove address from database
     * @param address
     * @return 
     */
    public boolean delete(AddressModel address) {
        return deleteById(address.getAddressId());
    }
    
    /**
     * Delete - Remove address by ID
     * @param addressId
     * @return 
     */
    public boolean deleteById(int addressId) {
        String sql = "DELETE FROM address WHERE addressId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, addressId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting address: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Search addresses by partial text in any field
     * @param searchText
     * @return 
     */
    public List<AddressModel> searchAddresses(String searchText) {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address WHERE " +
                    "street LIKE ? OR barangay LIKE ? OR city LIKE ? OR province LIKE ? OR zipCode LIKE ? " +
                    "ORDER BY city, street";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchText + "%";
            for (int i = 1; i <= 5; i++) {
                pstmt.setString(i, searchPattern);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Check if address exists
     * @param addressId
     * @return 
     */
    public boolean exists(int addressId) {
        String sql = "SELECT COUNT(*) FROM address WHERE addressId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, addressId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking address existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get count of all addresses
     * @return 
     */
    public int getAddressCount() {
        String sql = "SELECT COUNT(*) FROM address";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting address count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get unique cities
     * @return 
     */
    public List<String> getUniqueCities() {
        List<String> cities = new ArrayList<>();
        String sql = "SELECT DISTINCT city FROM address WHERE city IS NOT NULL ORDER BY city";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting unique cities: " + e.getMessage());
        }
        return cities;
    }
    
    /**
     * Get unique provinces
     * @return 
     */
    public List<String> getUniqueProvinces() {
        List<String> provinces = new ArrayList<>();
        String sql = "SELECT DISTINCT province FROM address WHERE province IS NOT NULL ORDER BY province";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                provinces.add(rs.getString("province"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting unique provinces: " + e.getMessage());
        }
        return provinces;
    }
    
    /**
     * Helper method to map ResultSet to AddressModel
     */
    private AddressModel mapResultSetToAddress(ResultSet rs) throws SQLException {
        return new AddressModel(
            rs.getInt("addressId"),
            rs.getString("street"),
            rs.getString("barangay"),
            rs.getString("city"),
            rs.getString("province"),
            rs.getString("zipCode")
        );
    }
}
