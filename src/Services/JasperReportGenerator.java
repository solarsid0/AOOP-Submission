
package Services;

import DAOs.DatabaseConnection;
import DAOs.ReferenceDataDAO;
import DAOs.EmployeeDAO;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Comprehensive JasperReports generator for MotorPH Payroll System
 * Handles various report types including payslips, attendance, employee lists, etc.
 * Integrates with your existing DAO structure
 * @author chadley
 */

public class JasperReportGenerator {
  private final DatabaseConnection databaseConnection;
    private final ReferenceDataDAO referenceDataDAO;
    private final EmployeeDAO employeeDAO;
    
    // Report template paths (you'll create these .jrxml files)
    private static final String REPORTS_PATH = "src/reports/";
    private static final String OUTPUT_PATH = "reports/output/";
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public JasperReportGenerator(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        
        // Create output directory if it doesn't exist
        createOutputDirectory();
    }
    
    /**
     * Generates a payslip report for a specific employee and pay period
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @param format Output format ("PDF", "EXCEL", "XLSX")
     * @return Generated file path
     */
    public String generatePayslipReport(Integer employeeId, Integer payPeriodId, String format) {
        try {
            // Prepare report data
            Map<String, Object> parameters = new HashMap<>();
            
            // Get employee details
            var employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Employee not found: " + employeeId);
            }
            
            // Get pay period info
            String payPeriodQuery = "SELECT * FROM payperiod WHERE payPeriodId = ?";
            
            // Add parameters for the report
            parameters.put("EMPLOYEE_ID", employeeId);
            parameters.put("PAY_PERIOD_ID", payPeriodId);
            parameters.put("EMPLOYEE_NAME", employee.getFirstName() + " " + employee.getLastName());
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            
            // Get payroll data (you'll need to adjust this based on your payroll tables)
            List<Map<String, Object>> payrollData = getPayrollData(employeeId, payPeriodId);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(payrollData);
            
            // Generate report
            String templatePath = REPORTS_PATH + "payslip_template.jrxml";
            String outputFileName = String.format("Payslip_%s_%s_%s.%s", 
                employee.getLastName(), employeeId, payPeriodId, format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating payslip report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates attendance report for an employee within date range
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @param format Output format
     * @return Generated file path
     */
    public String generateAttendanceReport(Integer employeeId, LocalDate startDate, LocalDate endDate, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", java.sql.Date.valueOf(startDate));
            parameters.put("END_DATE", java.sql.Date.valueOf(endDate));
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            
            String reportScope = "All_Employees";
            if (employeeId != null) {
                parameters.put("EMPLOYEE_ID", employeeId);
                var employee = employeeDAO.findById(employeeId);
                if (employee != null) {
                    parameters.put("EMPLOYEE_NAME", employee.getFirstName() + " " + employee.getLastName());
                    reportScope = employee.getLastName() + "_" + employeeId;
                }
            }
            
            // Get attendance data
            List<Map<String, Object>> attendanceData = getAttendanceData(employeeId, startDate, endDate);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(attendanceData);
            
            String templatePath = REPORTS_PATH + "attendance_template.jrxml";
            String outputFileName = String.format("Attendance_%s_%s_to_%s.%s", 
                reportScope, startDate.toString(), endDate.toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating attendance report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates employee list report
     * @param department Department filter (null for all departments)
     * @param status Status filter (null for all statuses)
     * @param format Output format
     * @return Generated file path
     */
    public String generateEmployeeListReport(String department, String status, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("DEPARTMENT_FILTER", department != null ? department : "All Departments");
            parameters.put("STATUS_FILTER", status != null ? status : "All Statuses");
            
            // Get employee data
            List<Map<String, Object>> employeeData = getEmployeeListData(department, status);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(employeeData);
            
            String templatePath = REPORTS_PATH + "employee_list_template.jrxml";
            String outputFileName = String.format("Employee_List_%s_%s_%s.%s", 
                department != null ? department : "All", 
                status != null ? status : "All",
                LocalDate.now().toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating employee list report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates leave report for employees
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @param leaveTypeId Leave type filter (null for all types)
     * @param format Output format
     * @return Generated file path
     */
    public String generateLeaveReport(Integer employeeId, LocalDate startDate, LocalDate endDate, 
                                    Integer leaveTypeId, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", java.sql.Date.valueOf(startDate));
            parameters.put("END_DATE", java.sql.Date.valueOf(endDate));
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            
            String reportScope = "All_Employees";
            if (employeeId != null) {
                parameters.put("EMPLOYEE_ID", employeeId);
                var employee = employeeDAO.findById(employeeId);
                if (employee != null) {
                    reportScope = employee.getLastName() + "_" + employeeId;
                }
            }
            
            if (leaveTypeId != null) {
                var leaveType = referenceDataDAO.getLeaveTypeById(leaveTypeId);
                if (leaveType != null) {
                    parameters.put("LEAVE_TYPE", leaveType.get("leaveTypeName"));
                }
            }
            
            // Get leave data
            List<Map<String, Object>> leaveData = getLeaveData(employeeId, startDate, endDate, leaveTypeId);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(leaveData);
            
            String templatePath = REPORTS_PATH + "leave_template.jrxml";
            String outputFileName = String.format("Leave_Report_%s_%s_to_%s.%s", 
                reportScope, startDate.toString(), endDate.toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating leave report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates overtime report
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @param format Output format
     * @return Generated file path
     */
    public String generateOvertimeReport(Integer employeeId, LocalDate startDate, LocalDate endDate, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", java.sql.Date.valueOf(startDate));
            parameters.put("END_DATE", java.sql.Date.valueOf(endDate));
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            
            String reportScope = "All_Employees";
            if (employeeId != null) {
                parameters.put("EMPLOYEE_ID", employeeId);
                var employee = employeeDAO.findById(employeeId);
                if (employee != null) {
                    reportScope = employee.getLastName() + "_" + employeeId;
                }
            }
            
            // Get overtime data
            List<Map<String, Object>> overtimeData = getOvertimeData(employeeId, startDate, endDate);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(overtimeData);
            
            String templatePath = REPORTS_PATH + "overtime_template.jrxml";
            String outputFileName = String.format("Overtime_Report_%s_%s_to_%s.%s", 
                reportScope, startDate.toString(), endDate.toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating overtime report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates payroll summary report for a pay period
     * @param payPeriodId Pay period ID
     * @param department Department filter (null for all)
     * @param format Output format
     * @return Generated file path
     */
    public String generatePayrollSummaryReport(Integer payPeriodId, String department, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PAY_PERIOD_ID", payPeriodId);
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("DEPARTMENT_FILTER", department != null ? department : "All Departments");
            
            // Get payroll summary data
            List<Map<String, Object>> payrollSummaryData = getPayrollSummaryData(payPeriodId, department);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(payrollSummaryData);
            
            String templatePath = REPORTS_PATH + "payroll_summary_template.jrxml";
            String outputFileName = String.format("Payroll_Summary_%s_%s_%s.%s", 
                payPeriodId, department != null ? department : "All", 
                LocalDate.now().toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating payroll summary report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Core method to generate reports
     * @param templatePath Path to .jrxml template
     * @param parameters Report parameters
     * @param dataSource Data source for the report
     * @param outputFileName Output file name
     * @param format Output format
     * @return Generated file path
     */
    private String generateReport(String templatePath, Map<String, Object> parameters, 
                                JRBeanCollectionDataSource dataSource, String outputFileName, String format) {
        try {
            // Compile the report template
            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);
            
            // Fill the report with data
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            
            // Export based on format
            String outputPath = OUTPUT_PATH + outputFileName;
            
            switch (format.toUpperCase()) {
                case "PDF":
                    JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
                    break;
                case "EXCEL":
                case "XLS":
                case "XLSX":
                    // For now, just export as PDF - you can add Excel export later
                    outputPath = outputPath.replace(".xls", ".pdf").replace(".xlsx", ".pdf");
                    JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
                    System.out.println("Note: Excel export not implemented yet, exported as PDF instead");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }
            
            System.out.println("Report generated successfully: " + outputPath);
            return outputPath;
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // DATA RETRIEVAL METHODS
    // These methods retrieve data for reports - adjust SQL based on your actual database schema
    
    private List<Map<String, Object>> getPayrollData(Integer employeeId, Integer payPeriodId) {
        String sql = """
            SELECT e.employeeId, e.firstName, e.lastName, e.basicSalary, e.hourlyRate,
                   p.positionTitle, p.department,
                   pp.startDate, pp.endDate, pp.payDate,
                   pr.grossPay, pr.netPay, pr.totalDeductions
            FROM employee e
            JOIN position p ON e.positionId = p.positionId
            JOIN payperiod pp ON pp.payPeriodId = ?
            LEFT JOIN payroll pr ON e.employeeId = pr.employeeId AND pr.payPeriodId = ?
            WHERE e.employeeId = ?
            """;
        
        return executeQuery(sql, payPeriodId, payPeriodId, employeeId);
    }
    
    private List<Map<String, Object>> getAttendanceData(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName,
                   a.attendanceDate, a.timeIn, a.timeOut, a.hoursWorked
            FROM employee e
            LEFT JOIN attendance a ON e.employeeId = a.employeeId
            WHERE a.attendanceDate BETWEEN ? AND ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(startDate));
        params.add(java.sql.Date.valueOf(endDate));
        
        if (employeeId != null) {
            sql.append(" AND e.employeeId = ?");
            params.add(employeeId);
        }
        
        sql.append(" ORDER BY e.lastName, e.firstName, a.attendanceDate");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getEmployeeListData(String department, String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName, e.email, e.phoneNumber,
                   e.status, e.userRole, p.positionTitle, p.department, e.basicSalary
            FROM employee e
            JOIN position p ON e.positionId = p.positionId
            WHERE 1=1
            """);
        
        List<Object> params = new ArrayList<>();
        
        if (department != null && !department.trim().isEmpty()) {
            sql.append(" AND p.department = ?");
            params.add(department);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND e.status = ?");
            params.add(status);
        }
        
        sql.append(" ORDER BY p.department, e.lastName, e.firstName");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getLeaveData(Integer employeeId, LocalDate startDate, 
                                                  LocalDate endDate, Integer leaveTypeId) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName,
                   lr.leaveRequestId, lr.startDate, lr.endDate, lr.status,
                   lt.leaveTypeName, lr.reason
            FROM employee e
            LEFT JOIN leaverequest lr ON e.employeeId = lr.employeeId
            LEFT JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId
            WHERE lr.startDate <= ? AND lr.endDate >= ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(endDate));
        params.add(java.sql.Date.valueOf(startDate));
        
        if (employeeId != null) {
            sql.append(" AND e.employeeId = ?");
            params.add(employeeId);
        }
        
        if (leaveTypeId != null) {
            sql.append(" AND lr.leaveTypeId = ?");
            params.add(leaveTypeId);
        }
        
        sql.append(" ORDER BY e.lastName, e.firstName, lr.startDate");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getOvertimeData(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName,
                   o.overtimeDate, o.overtimeHours, o.status, o.reason
            FROM employee e
            LEFT JOIN overtimerequest o ON e.employeeId = o.employeeId
            WHERE o.overtimeDate BETWEEN ? AND ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(startDate));
        params.add(java.sql.Date.valueOf(endDate));
        
        if (employeeId != null) {
            sql.append(" AND e.employeeId = ?");
            params.add(employeeId);
        }
        
        sql.append(" ORDER BY e.lastName, e.firstName, o.overtimeDate");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getPayrollSummaryData(Integer payPeriodId, String department) {
        StringBuilder sql = new StringBuilder("""
            SELECT p.department, COUNT(e.employeeId) as employeeCount,
                   SUM(pr.grossPay) as totalGross, SUM(pr.netPay) as totalNet,
                   SUM(pr.totalDeductions) as totalDeductions
            FROM employee e
            JOIN position p ON e.positionId = p.positionId
            LEFT JOIN payroll pr ON e.employeeId = pr.employeeId AND pr.payPeriodId = ?
            WHERE 1=1
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(payPeriodId);
        
        if (department != null && !department.trim().isEmpty()) {
            sql.append(" AND p.department = ?");
            params.add(department);
        }
        
        sql.append(" GROUP BY p.department ORDER BY p.department");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    /**
     * Generic query execution method
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             var stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (var rs = stmt.executeQuery()) {
                var metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * Creates output directory if it doesn't exist
     */
    private void createOutputDirectory() {
        File dir = new File(OUTPUT_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Created output directory: " + OUTPUT_PATH);
        }
    }
    
    /**
     * Gets list of available report templates
     * @return List of available templates
     */
    public List<String> getAvailableTemplates() {
        List<String> templates = new ArrayList<>();
        File reportsDir = new File(REPORTS_PATH);
        
        if (reportsDir.exists() && reportsDir.isDirectory()) {
            File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".jrxml"));
            if (files != null) {
                for (File file : files) {
                    templates.add(file.getName());
                }
            }
        }
        
        return templates;
    }
    
    /**
     * Validates if a template exists
     * @param templateName Template file name
     * @return true if template exists
     */
    public boolean templateExists(String templateName) {
        File templateFile = new File(REPORTS_PATH + templateName);
        return templateFile.exists() && templateFile.isFile();
    }
}