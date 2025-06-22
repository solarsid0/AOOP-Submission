package DAOs;

// These imports handle database connections and SQL operations
import java.sql.Connection;        // Main interface for database connections
import java.sql.DriverManager;     // Factory class that creates database connections
import java.sql.SQLException;      // Exception thrown when database operations fail
import java.util.Properties;       // Key-value pairs for connection settings

/**
 * Simple database connection for MotorPH payroll system.
 * This class provides both static and instance methods for database connectivity.
 * @author User
 */
public class DatabaseConnection {
    
    
    // The "address" where your database is located (server + port + database name)
    private static final String URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    
    // Username to log into MySQL (change this if your MySQL username is different)
    private static final String USER = "root"; //change depends on your MySQL server's username
    
    // Password to log into MySQL (change this to match your MySQL password)
    private static final String PASSWORD = "Mmdc_2025*"; //change based on your MySQL server's password
    
    
    // Load MySQL driver once when class is loaded
    // This is like "installing the software" that lets Java talk to MySQL
    static {
        try {
            // Tell Java where to find the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            // If the driver isn't found, give a helpful error message
            System.err.println("MySQL driver not found! Add mysql-connector-j-9.3.0.jar to your libraries folder");
            throw new RuntimeException("MySQL driver not found", e);
        }
    }
    
    //STATIC METHODS
    
    /**
     * Get database connection (static method)
     * This is the "simple way" - just call it directly

     * 
     * @return A working database connection you can use for queries
     * @throws java.sql.SQLException if connection fails (server down, wrong password, etc.)
     */
    public static Connection getConnection() throws SQLException {
        // Create a properties object to hold connection settings
        Properties props = new Properties();
        
        // Basic login credentials
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        
        // Security and compatibility settings
        props.setProperty("useSSL", "false");                    // No encryption needed for local development
        props.setProperty("allowPublicKeyRetrieval", "true");    // Needed for newer MySQL versions
        props.setProperty("serverTimezone", "Asia/Manila");      // Philippine timezone for correct date/time handling
        
        // Actually create and return the connection
        return DriverManager.getConnection(URL, props);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    // INSTANCE METHODS (for BaseDAO compatibility)

    // Instance variables - each DatabaseConnection object has its own copy
    private String url;        // Database URL for this specific instance
    private String username;   // Username for this specific instance
    private String password;   // Password for this specific instance
    
    /**
     * Default constructor
     * Uses the same settings as the static methods (URL, USER, PASSWORD)
     * Most of the time, you'll use this one
     */
    public DatabaseConnection() {
        this.url = URL;           // Use the default database URL
        this.username = USER;     // Use the default username
        this.password = PASSWORD; // Use the default password
    }
    
    /**
     * Custom constructor
     * Use this if you need to connect to a different database or use different credentials
     * Useful for testing or if you have multiple database environments
     * 
     * @param url The database URL (like "jdbc:mysql://localhost:3306/payrollsystem_db")
     * @param username The database username
     * @param password The database password
     */
    public DatabaseConnection(String url, String username, String password) {
        this.url = url;           // Store the custom database URL
        this.username = username; // Store the custom username
        this.password = password; // Store the custom password
    }
    
    /**
     * Get database connection (instance method) 
     * This creates a connection using this object's stored settings
     * Called "createConnection" instead of "getConnection" to avoid conflicts with the static method
     * 
     * @return A working database connection you can use for queries
     * @throws java.sql.SQLException if connection fails
     */
    public Connection createConnection() throws SQLException {
        // Create a properties object to hold connection settings
        Properties props = new Properties();
        
        // Use this instance's stored credentials
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        // Same security and compatibility settings as the static method
        props.setProperty("useSSL", "false");                    // No encryption for local development
        props.setProperty("allowPublicKeyRetrieval", "true");    // Compatibility with newer MySQL
        props.setProperty("serverTimezone", "Asia/Manila");      // Philippine timezone
        
        // Create and return the connection using this instance's URL
        return DriverManager.getConnection(url, props);
    }
    
    /**
     * Test connection
     * Quick way to check if the database is reachable and credentials work
     * Like "pinging" the database to see if it responds
     * 
     * @return true if connection works, false if there's a problem
     */
    public boolean testConnection() {
        try (Connection conn = createConnection()) {
            // Try to create a connection and test if it's valid (5 second timeout)
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            // If anything goes wrong, return false
            return false;
        }
        // The "try-with-resources" automatically closes the connection when done
    }
    
    /*
 * WHY WE HAVE BOTH STATIC AND INSTANCE METHODS IN HERE:
 * 
 * STATIC METHODS (DatabaseConnection.getConnection()):
 * • For existing/legacy code that already uses this pattern
 * • Quick and simple - no object creation needed
 * • Backward compatibility with current codebase
 * • One-liner database access: Connection conn = DatabaseConnection.getConnection();
 * 
 * INSTANCE METHODS (dbConn.createConnection()):
 * • Required for BaseDAO pattern - BaseDAO constructor needs a DatabaseConnection OBJECT
 * • Supports dependency injection (passing connection objects to DAOs)
 * • Allows multiple database configurations (test DB, production DB, etc.)
 * • Better for unit testing (can inject mock connections)
 * • Follows OOP principles - each DAO has its own connection object
 * • Future-proof design for scaling the application
 * 
 * USAGE EXAMPLES:
 * • Legacy way: Connection conn = DatabaseConnection.getConnection();
 * • New DAO way: DatabaseConnection dbConn = new DatabaseConnection(); 
 *                EmployeeDAO employeeDAO = new EmployeeDAO(dbConn);
 * 
 * BOTTOM LINE: Static for old code, Instance for new BaseDAO architecture
 */
}