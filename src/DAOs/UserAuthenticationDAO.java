
package DAOs;

/**
 *
 * @author USER
 */
import Models.UserAuthenticationModel;
import Utility.PasswordHasher;
import java.sql.*;
import java.util.UUID;

/**
 * Data Access Object for user authentication operations
 */
public class UserAuthenticationDAO {
    
    /**
     * Authenticates a user with email and password
     * @param email User's email
     * @param password Plain text password
     * @return UserAuthenticationModel if successful, null if failed
     */
    public UserAuthenticationModel authenticateUser(String email, String password) {
        String sql = "SELECT employeeId, email, passwordHash, userRole, status, lastLogin FROM employee WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("passwordHash");
                
                // Verify password
                if (PasswordHasher.verifyPassword(password, storedHash)) {
                    UserAuthenticationModel user = extractUserFromResultSet(rs);
                    
                    // Check if account is active
                    if (user.isAccountActive()) {
                        // Generate session token and start session
                        String sessionToken = generateSessionToken();
                        user.startSession(sessionToken, 480); // 8 hours session
                        
                        // Update last login in database
                        updateLastLogin(user.getEmployeeId());
                        
                        return user;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets user information by employee ID
     * @param employeeId The employee ID
     * @return UserAuthenticationModel or null if not found
     */
    public UserAuthenticationModel getUserById(int employeeId) {
        String sql = "SELECT employeeId, email, passwordHash, userRole, status, lastLogin FROM employee WHERE employeeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets user information by email
     * @param email The user's email
     * @return UserAuthenticationModel or null if not found
     */
    public UserAuthenticationModel getUserByEmail(String email) {
        String sql = "SELECT employeeId, email, passwordHash, userRole, status, lastLogin FROM employee WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Updates user's password
     * @param employeeId The employee ID
     * @param newPassword The new plain text password
     * @return true if successful, false otherwise
     */
    public boolean updatePassword(int employeeId, String newPassword) {
        if (!PasswordHasher.isPasswordValid(newPassword)) {
            throw new IllegalArgumentException(PasswordHasher.getPasswordRequirements());
        }
        
        String hashedPassword = PasswordHasher.hashPassword(newPassword);
        String sql = "UPDATE employee SET passwordHash = ?, updatedAt = CURRENT_TIMESTAMP WHERE employeeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, employeeId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Updates the last login timestamp for a user
     * @param employeeId The employee ID
     * @return true if successful, false otherwise
     */
    public boolean updateLastLogin(int employeeId) {
        String sql = "UPDATE employee SET lastLogin = CURRENT_TIMESTAMP WHERE employeeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Creates a new user account
     * @param email User's email
     * @param password Plain text password
     * @param userRole User's role
     * @param firstName First name
     * @param lastName Last name
     * @param positionId Position ID
     * @return true if successful, false otherwise
     */
    public boolean createUser(String email, String password, String userRole, String firstName, String lastName, int positionId) {
        if (!PasswordHasher.isPasswordValid(password)) {
            throw new IllegalArgumentException(PasswordHasher.getPasswordRequirements());
        }
        
        String hashedPassword = PasswordHasher.hashPassword(password);
        String sql = "INSERT INTO employee (firstName, lastName, email, passwordHash, userRole, positionId, birthDate, basicSalary) VALUES (?, ?, ?, ?, ?, ?, '1990-01-01', 0.00)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, userRole);
            pstmt.setInt(6, positionId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Deactivates a user account
     * @param employeeId The employee ID
     * @return true if successful, false otherwise
     */
    public boolean deactivateUser(int employeeId) {
        String sql = "UPDATE employee SET status = 'Terminated', updatedAt = CURRENT_TIMESTAMP WHERE employeeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deactivating user: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Checks if an email already exists in the database
     * @param email The email to check
     * @return true if email exists, false otherwise
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM employee WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Validates user session
     * @param employeeId Employee ID
     * @param sessionToken Session token
     * @return true if session is valid, false otherwise
     */
    public boolean validateSession(int employeeId, String sessionToken) {
        // In a real implementation, you might want to store session tokens in a separate table
        // For now, we'll just validate that the user exists and is active
        UserAuthenticationModel user = getUserById(employeeId);
        return user != null && user.isAccountActive();
    }
    
    /**
     * Generates a unique session token
     * @return Session token string
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Extracts UserAuthenticationModel from ResultSet
     * @param rs ResultSet from database query
     * @return UserAuthenticationModel object
     * @throws SQLException if database error occurs
     */
    private UserAuthenticationModel extractUserFromResultSet(ResultSet rs) throws SQLException {
        UserAuthenticationModel user = new UserAuthenticationModel();
        user.setEmployeeId(rs.getInt("employeeId"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("passwordHash"));
        user.setUserRole(rs.getString("userRole"));
        user.setStatus(rs.getString("status"));
        
        Timestamp lastLogin = rs.getTimestamp("lastLogin");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        return user;
    }
}