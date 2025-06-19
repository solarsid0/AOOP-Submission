package DAOs;

import java.sql.*;
import java.util.List;

/**
 * Abstract base class for all Data Access Objects (DAOs).
 * This class provides common database operations that all DAOs need.
 * Every specific DAO (like EmployeeDAO, AttendanceDAO) should extend this class.
 * 
 * @param <T> The entity type this DAO handles (like Employee, Attendance)
 * @param <ID> The type of the entity's primary key (usually Integer)
 * @author User
 */
public abstract class BaseDAO<T, ID> {
    
    // The database connection instance that this DAO will use
    protected final DatabaseConnection databaseConnection;
    
    /**
     * Constructor that requires a DatabaseConnection
     * Every DAO needs a database connection to work
     * @param databaseConnection The database connection instance to use
     */
    public BaseDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    

    // ABSTRACT METHODS - Subclasses must implement these

    
    /**
     * Converts a database row (ResultSet) into a Java object
     * Each DAO needs to know how to convert database data to their specific object type
     * @param rs The ResultSet containing the database row data
     * @return The Java object created from the database row
     * @throws SQLException if there's an error reading from the database
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    
    /**
     * Returns the name of the database table this DAO works with
     * For example: EmployeeDAO returns "employee", AttendanceDAO returns "attendance"
     * @return The table name as a string
     */
    protected abstract String getTableName();
    
    /**
     * Returns the name of the primary key column for this table
     * For example: "employeeId", "attendanceId", etc.
     * @return The primary key column name
     */
    protected abstract String getPrimaryKeyColumn();
    
    /**
     * Sets the parameters for INSERT SQL statements
     * This method tells the database what values to insert for a new record
     * @param stmt The PreparedStatement to set parameters on
     * @param entity The object containing the values to insert
     * @throws SQLException if there's an error setting the parameters
     */
    protected abstract void setInsertParameters(PreparedStatement stmt, T entity) throws SQLException;
    
    /**
     * Sets the parameters for UPDATE SQL statements  
     * This method tells the database what values to update for an existing record
     * @param stmt The PreparedStatement to set parameters on
     * @param entity The object containing the new values
     * @throws SQLException if there's an error setting the parameters
     */
    protected abstract void setUpdateParameters(PreparedStatement stmt, T entity) throws SQLException;
    
    /**
     * Gets the ID value from an entity object
     * This is used to identify which record to update or delete
     * @param entity The object to get the ID from
     * @return The ID value
     */
    protected abstract ID getEntityId(T entity);
    

    // CONCRETE CRUD METHODS - These work for all DAOs

    
    /**
     * Saves a new record to the database (CREATE operation)
     * This method handles the SQL INSERT statement
     * @param entity The object to save to the database
     * @return true if the save was successful, false if it failed
     */
    public boolean save(T entity) {
        // Build the INSERT SQL statement
        String sql = buildInsertSQL();
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Set the values for the INSERT statement
            setInsertParameters(stmt, entity);
            
            // Execute the INSERT and see how many rows were affected
            int rowsAffected = stmt.executeUpdate();
            
            // If at least one row was inserted, the operation was successful
            if (rowsAffected > 0) {
                // Get any auto-generated keys (like auto-increment IDs)
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(entity, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return false
            System.err.println("Error saving entity: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Updates an existing record in the database (UPDATE operation)
     * This method handles the SQL UPDATE statement
     * @param entity The object with updated values
     * @return true if the update was successful, false if it failed
     */
    public boolean update(T entity) {
        // Build the UPDATE SQL statement
        String sql = buildUpdateSQL();
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the values for the UPDATE statement
            setUpdateParameters(stmt, entity);
            
            // Execute the UPDATE and see how many rows were affected
            int rowsAffected = stmt.executeUpdate();
            
            // If at least one row was updated, the operation was successful
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return false
            System.err.println("Error updating entity: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Deletes a record from the database by its ID (DELETE operation)
     * This method handles the SQL DELETE statement
     * @param id The ID of the record to delete
     * @return true if the delete was successful, false if it failed
     */
    public boolean delete(ID id) {
        // Build a simple DELETE SQL statement
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getPrimaryKeyColumn() + " = ?";
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the ID parameter for the WHERE clause
            stmt.setObject(1, id);
            
            // Execute the DELETE and see how many rows were affected
            int rowsAffected = stmt.executeUpdate();
            
            // If at least one row was deleted, the operation was successful
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return false
            System.err.println("Error deleting entity with ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Finds a single record by its ID (READ operation)
     * This method handles SELECT statements with WHERE clauses
     * @param id The ID to search for
     * @return The object if found, null if not found
     */
    public T findById(ID id) {
        // Build a SELECT statement to find one record by ID
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getPrimaryKeyColumn() + " = ?";
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the ID parameter for the WHERE clause
            stmt.setObject(1, id);
            
            // Execute the query and get the results
            try (ResultSet rs = stmt.executeQuery()) {
                // If we found a record, convert it to an object and return it
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
            
        } catch (SQLException e) {
            // If something goes wrong, print the error
            System.err.println("Error finding entity by ID " + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Return null if no record was found or if there was an error
        return null;
    }
    
    /**
     * Gets all records from the table (READ operation)
     * This method handles SELECT * statements
     * @return List of all objects in the table
     */
    public List<T> findAll() {
        // Build a simple SELECT * statement
        String sql = "SELECT * FROM " + getTableName();
        return executeQuery(sql);
    }
    

    // UTILITY METHODS - Helper methods for database operations

    
    /**
     * Executes a SELECT query and returns a list of objects
     * This is a helper method that other methods can use to run custom queries
     * @param sql The SQL query to execute
     * @param params Parameters for the query (for WHERE clauses)
     * @return List of objects from the query results
     */
    protected List<T> executeQuery(String sql, Object... params) {
        // Create an empty list to store the results
        List<T> results = new java.util.ArrayList<>();
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set any parameters that were passed in
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            // Execute the query and process each row
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Convert each database row to an object and add it to the list
                    results.add(mapResultSetToEntity(rs));
                }
            }
            
        } catch (SQLException e) {
            // If something goes wrong, print the error
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Return the list of results (empty list if no results or error occurred)
        return results;
    }
    
    /**
     * Executes a SELECT query that should return only one result
     * This is useful for finding a specific record by email, name, etc.
     * @param sql The SQL query to execute
     * @param params Parameters for the query
     * @return The object if found, null if not found
     */
    protected T executeSingleQuery(String sql, Object... params) {
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set any parameters that were passed in
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            // Execute the query and check if we got a result
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Convert the database row to an object and return it
                    return mapResultSetToEntity(rs);
                }
            }
            
        } catch (SQLException e) {
            // If something goes wrong, print the error
            System.err.println("Error executing single query: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Return null if no result was found or if there was an error
        return null;
    }
    
    /**
     * Executes an UPDATE, INSERT, or DELETE statement
     * This is a helper method for operations that modify the database
     * @param sql The SQL statement to execute
     * @param params Parameters for the statement
     * @return Number of rows that were affected by the operation
     */
    protected int executeUpdate(String sql, Object... params) {
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set any parameters that were passed in
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            // Execute the update and return how many rows were affected
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return 0
            System.err.println("Error executing update: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Safely closes database resources
     * This method ensures that ResultSets and Statements are properly closed
     * @param rs ResultSet to close
     * @param stmt Statement to close
     */
    protected void closeResources(ResultSet rs, Statement stmt) {
        try {
            // Close ResultSet if it exists
            if (rs != null) rs.close();
            // Close Statement if it exists
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            // If there's an error closing resources, print it
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
    

    // PRIVATE HELPER METHODS - Internal methods

    
    /**
     * Builds a basic INSERT SQL statement
     * Subclasses can override this if they need custom INSERT logic
     * @return The INSERT SQL string
     */
    private String buildInsertSQL() {
        // This is a very basic implementation
        // Most subclasses will override this with their specific column names
        return "INSERT INTO " + getTableName() + " VALUES (?)";
    }
    
    /**
     * Builds a basic UPDATE SQL statement
     * Subclasses can override this if they need custom UPDATE logic
     * @return The UPDATE SQL string
     */
    private String buildUpdateSQL() {
        // This is a very basic implementation
        // Most subclasses will override this with their specific column names
        return "UPDATE " + getTableName() + " SET ? WHERE " + getPrimaryKeyColumn() + " = ?";
    }
    
    /**
     * Handles auto-generated keys after INSERT operations
     * This method sets the generated ID back on the entity object
     * @param entity The entity that was just inserted
     * @param generatedKeys The ResultSet containing the generated key
     * @throws SQLException if there's an error reading the generated key
     */
    protected void handleGeneratedKey(T entity, ResultSet generatedKeys) throws SQLException {
        // Default implementation does nothing
        // Subclasses can override this to set the generated ID on their entity
    }
    

    // CONNECTION TESTING

    
    /**
     * Tests if the database connection is working
     * This is useful for debugging connection problems
     * @return true if connection works, false if there's a problem
     */
    public boolean testConnection() {
        return databaseConnection.testConnection();
    }
}