package oop.test;

import DAOs.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        
        // Test using static method
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null && conn.isValid(5)) {
                System.out.println("Static method connection: SUCCESS");
            } else {
                System.out.println("Static method connection: FAILED");
            }
        } catch (SQLException e) {
            System.out.println("Static method connection: FAILED - " + e.getMessage());
        }
        
        // Test using instance method
        DatabaseConnection dbConn = new DatabaseConnection();
        if (dbConn.testConnection()) {
            System.out.println("Instance method connection: SUCCESS");
        } else {
            System.out.println("Instance method connection: FAILED");
        }
        
        // Test connection details
        try (Connection conn = dbConn.createConnection()) {
            System.out.println("Database URL: " + conn.getMetaData().getURL());
            System.out.println("Database User: " + conn.getMetaData().getUserName());
            System.out.println("Database Product: " + conn.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            System.out.println("Could not get connection details: " + e.getMessage());
        }
    }
}