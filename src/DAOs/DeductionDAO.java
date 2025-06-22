package DAOs;

import Models.DeductionModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeductionDAO {
    
    public boolean addDeduction(DeductionModel deduction) {
        String sql = "INSERT INTO deduction (typeName, deductionAmount, lowerLimit, upperLimit, baseTax, deductionRate, payrollId) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, deduction.getDeductionType().getDisplayName());
            pstmt.setBigDecimal(2, deduction.getAmount());
            pstmt.setBigDecimal(3, deduction.getLowerLimit());
            pstmt.setBigDecimal(4, deduction.getUpperLimit());
            pstmt.setBigDecimal(5, deduction.getBaseTax());
            pstmt.setBigDecimal(6, deduction.getDeductionRate());
            pstmt.setInt(7, deduction.getPayPeriodId());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        deduction.setDeductionId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error adding deduction: " + e.getMessage());
        }
        return false;
    }
    
    public DeductionModel getDeductionById(int deductionId) {
        String sql = "SELECT * FROM deduction WHERE deductionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, deductionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractDeductionFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting deduction by ID: " + e.getMessage());
        }
        return null;
    }
    
    public List<DeductionModel> getDeductionsByPayPeriodId(int payPeriodId) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT d.* FROM deduction d " +
                    "JOIN payroll p ON d.payrollId = p.payrollId " +
                    "WHERE p.payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(extractDeductionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting deductions by pay period ID: " + e.getMessage());
        }
        return deductions;
    }
    
    public List<DeductionModel> getDeductionsByEmployeeId(int employeeId) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT d.* FROM deduction d " +
                    "JOIN payroll p ON d.payrollId = p.payrollId " +
                    "WHERE p.employeeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(extractDeductionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting deductions by employee ID: " + e.getMessage());
        }
        return deductions;
    }
    
    public List<DeductionModel> getDeductionsByType(DeductionModel.DeductionType deductionType) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT * FROM deduction WHERE typeName = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deductionType.getDisplayName());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(extractDeductionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting deductions by type: " + e.getMessage());
        }
        return deductions;
    }
    
    public List<DeductionModel> getDeductionsByPayrollId(int payrollId) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT * FROM deduction WHERE payrollId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(extractDeductionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting deductions by payroll ID: " + e.getMessage());
        }
        return deductions;
    }
    
    public boolean updateDeduction(DeductionModel deduction) {
        String sql = "UPDATE deduction SET typeName = ?, deductionAmount = ?, lowerLimit = ?, upperLimit = ?, baseTax = ?, deductionRate = ?, payrollId = ? WHERE deductionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deduction.getDeductionType().getDisplayName());
            pstmt.setBigDecimal(2, deduction.getAmount());
            pstmt.setBigDecimal(3, deduction.getLowerLimit());
            pstmt.setBigDecimal(4, deduction.getUpperLimit());
            pstmt.setBigDecimal(5, deduction.getBaseTax());
            pstmt.setBigDecimal(6, deduction.getDeductionRate());
            pstmt.setInt(7, deduction.getPayPeriodId());
            pstmt.setInt(8, deduction.getDeductionId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating deduction: " + e.getMessage());
        }
        return false;
    }
    
    public boolean deleteDeduction(int deductionId) {
        String sql = "DELETE FROM deduction WHERE deductionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, deductionId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting deduction: " + e.getMessage());
        }
        return false;
    }
    
    public List<DeductionModel> getAllDeductions() {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT * FROM deduction ORDER BY deductionId";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                deductions.add(extractDeductionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all deductions: " + e.getMessage());
        }
        return deductions;
    }
    
    /**
     * Gets deductions for an employee within a specific pay period
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return List of deductions for the employee in the pay period
     */
    public List<DeductionModel> getDeductionsByEmployeeAndPeriod(int employeeId, int payPeriodId) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT d.* FROM deduction d " +
                    "JOIN payroll p ON d.payrollId = p.payrollId " +
                    "WHERE p.employeeId = ? AND p.payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(extractDeductionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting deductions by employee and period: " + e.getMessage());
        }
        return deductions;
    }
    
    /**
     * Creates standard deductions for an employee's payroll
     * @param employeeId Employee ID
     * @param payrollId Payroll ID
     * @param monthlySalary Employee's monthly salary for calculations
     * @return true if all deductions were created successfully
     */
    public boolean createStandardDeductions(int employeeId, int payrollId, java.math.BigDecimal monthlySalary) {
        try {
            // Create SSS deduction
            DeductionModel sssDeduction = new DeductionModel();
            sssDeduction.setEmployeeId(employeeId);
            sssDeduction.setDeductionType(DeductionModel.DeductionType.SSS);
            sssDeduction.setAmount(DeductionModel.calculateSSSDeduction(monthlySalary));
            sssDeduction.setPayPeriodId(payrollId); // Note: using payrollId in place of payPeriodId as per your model
            
            // Create PhilHealth deduction
            DeductionModel philhealthDeduction = new DeductionModel();
            philhealthDeduction.setEmployeeId(employeeId);
            philhealthDeduction.setDeductionType(DeductionModel.DeductionType.PHILHEALTH);
            philhealthDeduction.setAmount(DeductionModel.calculatePhilHealthDeduction(monthlySalary));
            philhealthDeduction.setPayPeriodId(payrollId);
            
            // Create Pag-IBIG deduction
            DeductionModel pagibigDeduction = new DeductionModel();
            pagibigDeduction.setEmployeeId(employeeId);
            pagibigDeduction.setDeductionType(DeductionModel.DeductionType.PAG_IBIG);
            pagibigDeduction.setAmount(DeductionModel.calculatePagIbigDeduction(monthlySalary));
            pagibigDeduction.setPayPeriodId(payrollId);
            
            // Create Withholding Tax deduction
            DeductionModel withholdingTaxDeduction = new DeductionModel();
            withholdingTaxDeduction.setEmployeeId(employeeId);
            withholdingTaxDeduction.setDeductionType(DeductionModel.DeductionType.WITHHOLDING_TAX);
            withholdingTaxDeduction.setAmount(DeductionModel.calculateWithholdingTax(monthlySalary));
            withholdingTaxDeduction.setPayPeriodId(payrollId);
            
            // Add all deductions
            boolean success = true;
            success &= addDeduction(sssDeduction);
            success &= addDeduction(philhealthDeduction);
            success &= addDeduction(pagibigDeduction);
            success &= addDeduction(withholdingTaxDeduction);
            
            return success;
            
        } catch (Exception e) {
            System.err.println("Error creating standard deductions: " + e.getMessage());
            return false;
        }
    }
    
    private DeductionModel extractDeductionFromResultSet(ResultSet rs) throws SQLException {
        DeductionModel deduction = new DeductionModel();
        deduction.setDeductionId(rs.getInt("deductionId"));
        
        // Convert database typeName to enum
        String typeName = rs.getString("typeName");
        try {
            deduction.setDeductionType(DeductionModel.DeductionType.fromString(typeName));
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown deduction type in database: " + typeName);
            // Set a default or handle as needed
            deduction.setDeductionType(DeductionModel.DeductionType.SSS);
        }
        
        deduction.setAmount(rs.getBigDecimal("deductionAmount"));
        deduction.setLowerLimit(rs.getBigDecimal("lowerLimit"));
        deduction.setUpperLimit(rs.getBigDecimal("upperLimit"));
        deduction.setBaseTax(rs.getBigDecimal("baseTax"));
        deduction.setDeductionRate(rs.getBigDecimal("deductionRate"));
        
        // Note: The database has payrollId, but this model uses payPeriodId
        // Mapping payrollId to payPeriodId field in this model
        int payrollId = rs.getInt("payrollId");
        deduction.setPayPeriodId(payrollId);
        
        // To get the actual employeeId, we'd need to join with payroll table
        // For now, setting to null as it's not directly in deduction table
        // You might want to modify the queries to include employee info
        deduction.setEmployeeId(null);
        
        return deduction;
    }
}