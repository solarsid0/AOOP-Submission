package CSV;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import oop.classes.actors.Employee;

/**
 * This class is responsible for processing employee and attendance data from CSV files.
 * Loads and parses CSV data into structured formats for further use.
 */
public class CSVDatabaseProcessor {

    // File paths for the CSV files (adjusted to reference the package directly)
    protected static final String CSV_EMPLOYEE_DETAILS = "Employee Details 2024 (2).csv";
    protected static final String CSV_ATTENDANCE_RECORDS = "Attendance Record 2024.csv";
    protected static final String CSV_LEAVE_REQUESTS = "OOP CSV Database - Leave Requests.csv";
    protected static final String CSV_USER_CREDENTIALS = "OOP CSV Database - User Credentials.csv";
    
    //Getter
   public String getEmployeeDetailsFilePath() {
        return CSV_EMPLOYEE_DETAILS;
    }
   
    // Base directory for CSV files
    private String csvDirectory = "src/CSV/";

    // Formatters for parsing dates and times from CSV
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    // Lists to store processed records
    private final List<Map<String, String>> employeeRecords = new ArrayList<>();
    private final List<Map<String, Object>> attendanceRecords = new ArrayList<>();
    private final List<Map<String, String>> leaveRequestRecords = new ArrayList<>();
    private final List<Map<String, String>> userCredentialRecords = new ArrayList<>();

    // Maps to store column index mappings for dynamic CSV parsing
    private final Map<String, Integer> employeeColumnIndexMap = new HashMap<>();
    private final Map<String, Integer> attendanceColumnIndexMap = new HashMap<>();
    private final Map<String, Integer> leaveRequestColumnIndexMap = new HashMap<>();
    private final Map<String, Integer> userCredentialColumnIndexMap = new HashMap<>();

    /**
     * Default constructor that loads employee records from the default CSV file.
     */
    public CSVDatabaseProcessor() {
        loadEmployeeCSVData();
    }

    /**
     * Constructor that loads employee records from a specified CSV file.
     * @param employeeFilePath
     */
    public CSVDatabaseProcessor(String employeeFilePath) {
        loadEmployeeCSVData(employeeFilePath);
    }

    /**
     * Constructor that loads both employee and attendance records from specified CSV files.
     * @param employeeFilePath
     * @param attendanceFilePath
     */
    public CSVDatabaseProcessor(String employeeFilePath, String attendanceFilePath) {
        loadEmployeeCSVData(employeeFilePath);
        loadAttendanceCSVData(attendanceFilePath);
    }

    /**
     * Constructor with custom CSV directory
     * @param csvDirectory The directory containing the CSV files
     */
    public CSVDatabaseProcessor(String csvDirectory, boolean setDirectory) {
        if (setDirectory) {
            this.csvDirectory = csvDirectory;
            if (!this.csvDirectory.endsWith("/") && !this.csvDirectory.endsWith("\\")) {
                this.csvDirectory += "/";
            }
        }
        loadEmployeeCSVData();
    }

    /**
     * Loads employee data from the default file path.
     */
    public void loadEmployeeCSVData() {
        loadEmployeeCSVData(CSV_EMPLOYEE_DETAILS);
    }

    /**
     * Loads employee data from a specified file path.
     * @param resourcePath
     */
    public void loadEmployeeCSVData(String resourcePath) {
        loadCSVData(resourcePath, this::parseEmployeeRecord, this::defineEmployeeColumnMapping, employeeRecords);
    }

    /**
     * Loads attendance data from the default file path.
     */
    public void loadAttendanceData() {
        loadAttendanceCSVData(CSV_ATTENDANCE_RECORDS);
    }

    /**
     * Loads attendance data from a specified file path.
     * @param resourcePath
     */
    public void loadAttendanceCSVData(String resourcePath) {
        loadCSVData(resourcePath, this::parseAttendanceRecord, this::defineAttendanceColumnMapping, attendanceRecords);
    }

    /**
     * Loads leave request data from the default file path.
     */
    public void loadLeaveRequestData() {
        loadCSVData(CSV_LEAVE_REQUESTS, this::parseLeaveRequestRecord, this::defineLeaveRequestColumnMapping, leaveRequestRecords);
    }

    /**
     * Loads user credential data from the default file path.
     */
    public void loadUserCredentialData() {
        loadCSVData(CSV_USER_CREDENTIALS, this::parseUserCredentialRecord, this::defineUserCredentialColumnMapping, userCredentialRecords);
    }

     /**
     * Groups employees by their immediate supervisor using supervisor names.
     *
     * @return A map where the key is the supervisor and the value is the list of subordinates.
     */
    public Map<Employee, List<Employee>> groupEmployeesBySupervisor() {
        Map<Employee, List<Employee>> hierarchy = new HashMap<>();
        Map<String, Employee> employeeNameMap = new HashMap<>(); // Map to store employees by their full name

        // First, create a map of Employee full name to Employee object
        for (Map<String, String> record : employeeRecords) {
            Employee employee = Employee.createEmployeeFromCSVRecord(record);
            String fullName = employee.getFullName();
            employeeNameMap.put(fullName, employee);
        }

        // Assign supervisors and build the hierarchy
        for (Employee employee : employeeNameMap.values()) {
            String supervisorName = employee.getSupervisorName();
            if (supervisorName != null && !supervisorName.isEmpty() && !supervisorName.equals("N/A")) {
                Employee supervisor = employeeNameMap.get(supervisorName);
                if (supervisor != null) {
                    employee.setSupervisor(supervisor);
                    hierarchy.computeIfAbsent(supervisor, k -> new ArrayList<>()).add(employee);
                }
            }
        }

        return hierarchy;
    }
        
    /**
     * Helper class to accept pay period dates ; this retrieves attendance records within a date range
     * 
     * @param employeeId
     * @param startDate
     * @param endDate
     * @return 
     */    
    public List<Map<String, Object>> getAttendanceRecordsByEmployeeIdAndDateRange(String employeeId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> filteredRecords = new ArrayList<>();
        for (Map<String, Object> record : attendanceRecords) {
            if (employeeId.equals(record.get("Employee ID"))) {//empl ID used here as it's the common key between Attendance and Employee Details CSV
                LocalDate recordDate = (LocalDate) record.get("Date");
                if (recordDate != null && !recordDate.isBefore(startDate) && !recordDate.isAfter(endDate)) {
                    filteredRecords.add(record);
                }
            }
        }
        return filteredRecords;
    }

    /**
     * Generic method for reading and processing CSV files.
     * Now with improved file loading that tries multiple methods.
     */
    private <T> void loadCSVData(String resourcePath, CSVRecordParser<T> recordParser,
                                 ColumnMappingDefinition columnMappingDefinition, List<T> recordList) {
        
        // Clear any existing records to prevent duplication
        recordList.clear();
        
        // First try to load from classpath resources
        try (InputStream inputStream = getInputStreamForResource(resourcePath)) {
            if (inputStream == null) {
                System.err.println("Failed to load resource: " + resourcePath);
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Define the mapping between column names and their indexes
                columnMappingDefinition.defineColumnMapping();

                // Read each line, parse it, and add to the records list
                reader.lines().skip(1)  // Skip header line
                        .map(recordParser::parseRecord)
                        .filter(Objects::nonNull)
                        .forEach(recordList::add);

                System.out.println("Successfully loaded " + recordList.size() + " records from " + resourcePath);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + resourcePath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tries multiple methods to get an input stream for a resource.
     * This helps handle different project structures and deployment scenarios.
     */
    private InputStream getInputStreamForResource(String resourcePath) {
        InputStream inputStream = null;
        
        // Try multiple approaches to locate the file
        try {
            // Method 1: Try direct file access from the csvDirectory
            File file = new File(csvDirectory + resourcePath);
            if (file.exists()) {
                System.out.println("Loading file from direct path: " + file.getAbsolutePath());
                return new FileInputStream(file);
            }
            
            // Method 2: Try as a resource in the same package as this class
            inputStream = getClass().getResourceAsStream(resourcePath);
            if (inputStream != null) {
                System.out.println("Found resource in package: " + resourcePath);
                return inputStream;
            }
            
            // Method 3: Try as a resource with package path
            inputStream = getClass().getResourceAsStream("/CSV/" + resourcePath);
            if (inputStream != null) {
                System.out.println("Found resource with package path: /CSV/" + resourcePath);
                return inputStream;
            }
            
            // Method 4: Try using the class loader (looks in the classpath root)
            inputStream = getClass().getClassLoader().getResourceAsStream("CSV/" + resourcePath);
            if (inputStream != null) {
                System.out.println("Found resource using class loader: CSV/" + resourcePath);
                return inputStream;
            }
            
            // Method 5: Try with just the filename via class loader
            inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream != null) {
                System.out.println("Found resource using class loader with just filename: " + resourcePath);
                return inputStream;
            }
            
            // If we got here, we couldn't find the file
            System.err.println("Could not find resource: " + resourcePath);
            System.err.println("Attempted locations:");
            System.err.println("- " + csvDirectory + resourcePath);
            System.err.println("- as resource: " + resourcePath);
            System.err.println("- as resource: /CSV/" + resourcePath);
            System.err.println("- via class loader: CSV/" + resourcePath);
            System.err.println("- via class loader: " + resourcePath);
            
            // Last resort - try to list available resources to help diagnose
            listAvailableResources();
            
            return null;
            
        } catch (IOException e) {
            System.err.println("Error attempting to access resource: " + resourcePath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Attempts to list resources in the CSV directory to help diagnose file loading issues
     */
    private void listAvailableResources() {
        System.out.println("Attempting to list available files in CSV directory: " + csvDirectory);
        
        // Try direct file system access
        File directory = new File(csvDirectory);
        if (directory.exists() && directory.isDirectory()) {
            System.out.println("Files found in directory:");
            for (File file : directory.listFiles()) {
                System.out.println("- " + file.getName());
            }
        } else {
            System.out.println("CSV directory does not exist or is not a directory: " + csvDirectory);
        }
    }

    /**
     * Defines the columns in the Employee Details CSV
     */
    private void defineEmployeeColumnMapping() {
        String[] columnNames = {
                "Employee ID", "Last Name", "First Name", "Birthday", "Address",
                "Phone Number", "SSS #", "Philhealth #", "TIN #", "Pag-ibig #",
                "Status", "Position", "Immediate Supervisor",
                "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance",
                "Gross Semi-monthly Rate", "Hourly Rate"
        };
        createColumnIndexMap(columnNames, employeeColumnIndexMap);
    }

    /**
     * Defines the column mapping for attendance data.
     */
    private void defineAttendanceColumnMapping() {
        String[] columnNames = {"Employee ID", "Last Name", "First Name", "Date", "Log In", "Log Out"};
        createColumnIndexMap(columnNames, attendanceColumnIndexMap);
    }

    /**
     * Defines the column mapping for leave request data.
     */
    private void defineLeaveRequestColumnMapping() {
        String[] columnNames = {
                "Date of Submission", "Employee ID", "First Name", "Last Name", "Position", "Status", "Immediate Supervisor",
                "Type of Leave", "Note", "Start", "End", "Leave Status", "Remaining Vacation Leave", "Remaining Sick Leave"
        };
        createColumnIndexMap(columnNames, leaveRequestColumnIndexMap);
    }

    /**
     * Defines the column mapping for user credential data.
     */
    private void defineUserCredentialColumnMapping() {
        String[] columnNames = {"Employee ID", "Last Name", "First Name", "Email", "Password"};
        createColumnIndexMap(columnNames, userCredentialColumnIndexMap);
    }

    /**
     * Helper method to create a column index map from an array of column names.
     */
    private void createColumnIndexMap(String[] columnNames, Map<String, Integer> columnIndexMap) {
        columnIndexMap.clear(); // Clear existing mappings
        for (int i = 0; i < columnNames.length; i++) {
            columnIndexMap.put(columnNames[i], i);
        }
    }

    /**
     * Parses a single line of employee data from the CSV file.
     */
    private Map<String, String> parseEmployeeRecord(String line) {
        String[] values = parseCSVLine(line, "employee");
        Map<String, String> employeeData = new HashMap<>();

        // To handle potential parsing issues with column misalignment
        if (values.length < employeeColumnIndexMap.size()) {
            System.err.println("WARNING: CSV line has fewer fields than expected: " + values.length + 
                               " vs " + employeeColumnIndexMap.size() + ": " + line);
        }

        for (Map.Entry<String, Integer> entry : employeeColumnIndexMap.entrySet()) {
            String columnName = entry.getKey();
            int index = entry.getValue();
            
            if (index < values.length) {
                employeeData.put(columnName, values[index].trim());
            } else {
                System.err.println("ERROR: Missing value for column " + columnName);
                employeeData.put(columnName, "");
            }
        }
        
        // Validate position to ensure it's not a numeric value
        String position = employeeData.get("Position");
        if (position != null && isNumeric(position)) {
            System.err.println("Warning: Position appears to be numeric: " + position + " for employee ID: " + employeeData.get("Employee ID"));
            
            // Try to fix by looking up based on known positions by employee ID
            employeeData.put("Position", inferPositionFromEmployeeData(employeeData));
        }
        
        return employeeData;
    }

    /**
     * Infer the correct position based on other data in the employee record.
     * This is a fallback when the Position field contains invalid data.
     */
    private String inferPositionFromEmployeeData(Map<String, String> employeeData) {
        // Use other information to infer the most likely position
        String supervisor = employeeData.get("Immediate Supervisor");
        
        // Common supervisor->position relationships from the CSV
        if ("Lim, Antonio".equals(supervisor)) {
            // Known relationships based on the CSV data
            String id = employeeData.get("Employee ID");
            if ("10006".equals(id)) return "HR Manager";
            if ("10015".equals(id)) return "Account Manager";
            if ("10005".equals(id)) return "IT Operations and Systems";
        }
        else if ("Garcia, Manuel III".equals(supervisor)) {
            return "Chief Operating Officer"; // Default for CEO's direct reports
        }
        else if ("Villanueva, Andrea Mae".equals(supervisor)) {
            return "HR Team Leader";
        }
        else if ("Romualdez, Fredrick".equals(supervisor)) {
            return "Account Team Leader";
        }
        
        // If we can't infer, use a generic fallback based on basic salary
        try {
            String salaryStr = employeeData.get("Basic Salary").replace(",", "");
            double salary = Double.parseDouble(salaryStr);
            
            if (salary >= 50000) return "Manager";
            else if (salary >= 40000) return "Team Leader";
            else return "Rank and File";
            
        } catch (Exception e) {
            // If all else fails, return a default value
            return "Employee";
        }
    }

    /**
     * Parses a single line of attendance data from the CSV file.
     */
    private Map<String, Object> parseAttendanceRecord(String line) {
        String[] values = parseCSVLine(line, "attendance");
        Map<String, Object> attendanceData = new HashMap<>();

        for (Map.Entry<String, Integer> entry : attendanceColumnIndexMap.entrySet()) {
            String columnName = entry.getKey();
            int index = entry.getValue();
            String value = index < values.length ? values[index].trim() : "";

            switch (columnName) {
                case "Date":
                    attendanceData.put(columnName, parseDate(value));
                    break;
                case "Log In":
                case "Log Out":
                    attendanceData.put(columnName, parseTime(value));
                    break;
                default:
                    attendanceData.put(columnName, value);
            }
        }
        return attendanceData;
    }

    /**
     * Method to get total late hours from attendance csv
     * @param employeeId
     * @param payrollMonth
     * @return 
     */
        public double getTotalLateHours(String employeeId, YearMonth payrollMonth) {
        List<Map<String, Object>> records = getAttendanceRecordsByEmployeeId(employeeId);
        double totalLateHours = 0.0;

        LocalTime standardStartTime = LocalTime.of(8, 0); // 8:00 AM
        LocalTime graceEndTime = LocalTime.of(8, 10);     // 8:10 AM (grace period)

        for (Map<String, Object> record : records) {
            LocalDate date = (LocalDate) record.get("Date");

            // Check if this record is for the specified month
            if (date != null && date.getYear() == payrollMonth.getYear() && 
                date.getMonth() == payrollMonth.getMonth()) {

                LocalTime logIn = (LocalTime) record.get("Log In");

                // Calculate late hours if login time is after grace period
                if (logIn != null && logIn.isAfter(graceEndTime)) {
                    // Calculate hours late (difference between actual login and standard start time)
                    double hoursLate = (double) (logIn.toSecondOfDay() - standardStartTime.toSecondOfDay()) / 3600.0;
                    totalLateHours += hoursLate;
                }
            }
        }

        return totalLateHours;
    }
    
    /**
     * Parses a single line of leave request data from the CSV file.
     */
    private Map<String, String> parseLeaveRequestRecord(String line) {
        try {
            String[] values = parseCSVLine(line, "leave");
            Map<String, String> leaveRequestData = new HashMap<>();

            // Make sure we only access valid indexes
            for (Map.Entry<String, Integer> entry : leaveRequestColumnIndexMap.entrySet()) {
                String columnName = entry.getKey();
                int index = entry.getValue();

                // Only access valid array indexes
                if (index < values.length) {
                    leaveRequestData.put(columnName, values[index].trim());
                } else {
                    // Use empty string for missing values
                    leaveRequestData.put(columnName, "");
                    System.out.println("Warning: Missing value for column " + columnName + " in line: " + line);
                }
            }
            return leaveRequestData;
        } catch (Exception e) {
            System.err.println("Error parsing leave request CSV line: " + line);
            e.printStackTrace();
            return new HashMap<>();  // Return empty map instead of null to avoid NPEs
        }
    }

    /**
     * Parses a single line of user credential data from the CSV file.
     */
    private Map<String, String> parseUserCredentialRecord(String line) {
        String[] values = parseCSVLine(line, "credential");
        Map<String, String> userCredentialData = new HashMap<>();

        for (Map.Entry<String, Integer> entry : userCredentialColumnIndexMap.entrySet()) {
            String columnName = entry.getKey();
            int index = entry.getValue();
            userCredentialData.put(columnName, (index < values.length) ? values[index].trim() : "");
        }
        return userCredentialData;
    }

    /**
     * Parses a date string into a LocalDate object.
     */
    private LocalDate parseDate(String value) {
        try {
            return value.isEmpty() ? null : LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date: " + value);
            return null;
        }
    }

    /**
     * Parses a time string into a LocalTime object.
     */
    private LocalTime parseTime(String value) {
        try {
            return value.isEmpty() ? null : LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing time: " + value);
            return null;
        }
    }

    /**
     * Original method for backward compatibility
     * Splits a CSV line into an array of values.
     */
    private String[] parseCSVLine(String line) {
        // Default to a generic context that won't trigger special employee parsing
        return parseCSVLine(line, "generic");
    }

    /**
     * Splits a CSV line into an array of values with context for determining parsing method.
     * This improved version properly handles commas within fields.
     */
    private String[] parseCSVLine(String line, String context) {
        // Only use special parsing for employee records
        if (context.equals("employee") && (line.contains("Regular") || line.contains("Probationary"))) {
            return parseEmployeeCSVLine(line);
        }
        
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue.setLength(0);
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString());
        return values.toArray(new String[0]);
    }

    /**
     * Special parsing method for employee CSV lines which have a fixed format
     * and contain numeric fields with commas as thousands separators.
     * This version preserves commas in monetary values for display purposes.
     */
    private String[] parseEmployeeCSVLine(String line) {
        // We know the exact format of employee data, so we'll parse it according to the expected format
        String[] result = new String[19]; // 19 columns in the employee CSV

        try {
            // First check if the line contains quoted fields
            if (line.contains("\"")) {
                // Handle quoted fields with a more comprehensive parser
                List<String> fields = new ArrayList<>();
                StringBuilder currentField = new StringBuilder();
                boolean inQuotes = false;

                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);

                    if (c == '"') {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        // End of a field
                        fields.add(currentField.toString());
                        currentField = new StringBuilder();
                    } else {
                        currentField.append(c);
                    }
                }

                // Add the last field
                fields.add(currentField.toString());

                // Copy to result array (trimming quotes if needed)
                for (int i = 0; i < Math.min(fields.size(), result.length); i++) {
                    String field = fields.get(i);
                    result[i] = field.replaceAll("^\"|\"$", ""); // Remove surrounding quotes
                }

                return result;
            }

            // If no quotes, handle with the existing approach but preserve commas
            String[] rawSplit = line.split(",");

            // First fields are straightforward
            result[0] = rawSplit[0]; // Employee ID
            result[1] = rawSplit[1]; // Last Name
            result[2] = rawSplit[2]; // First Name
            result[3] = rawSplit[3]; // Birthday

            // Handle address which may contain commas
            StringBuilder address = new StringBuilder(rawSplit[4]);
            int addressEndIndex = 5;
            // Keep appending parts until we find the phone number pattern
            while (addressEndIndex < rawSplit.length && 
                   !rawSplit[addressEndIndex].trim().matches("\\d{3}-\\d{3}-\\d{3}")) {
                address.append(",").append(rawSplit[addressEndIndex]);
                addressEndIndex++;
            }
            result[4] = address.toString(); // Address

            // Continue with remaining fields
            result[5] = rawSplit[addressEndIndex++]; // Phone Number
            result[6] = rawSplit[addressEndIndex++]; // SSS #
            result[7] = rawSplit[addressEndIndex++]; // Philhealth #
            result[8] = rawSplit[addressEndIndex++]; // TIN #
            result[9] = rawSplit[addressEndIndex++]; // Pag-ibig #
            result[10] = rawSplit[addressEndIndex++]; // Status
            result[11] = rawSplit[addressEndIndex++]; // Position

            // Supervisor name may contain a comma
            StringBuilder supervisor = new StringBuilder(rawSplit[addressEndIndex++]);
            if (addressEndIndex < rawSplit.length && 
                !rawSplit[addressEndIndex].trim().matches("\\d+")) {
                supervisor.append(",").append(rawSplit[addressEndIndex++]);
            }
            result[12] = supervisor.toString(); // Immediate Supervisor

            // For monetary fields, we need to reconstruct them to preserve the commas
            // Start with Basic Salary (field 13)
            if (addressEndIndex < rawSplit.length) {
                StringBuilder amount = new StringBuilder(rawSplit[addressEndIndex++]);
                // Look ahead for the next part if it looks like a continuation of a number with commas
                if (addressEndIndex < rawSplit.length && rawSplit[addressEndIndex].trim().matches("\\d{3}")) {
                    amount.append(",").append(rawSplit[addressEndIndex++]);
                }
                result[13] = amount.toString(); // Basic Salary WITH commas
            }

            // Rice Subsidy (field 14)
            if (addressEndIndex < rawSplit.length) {
                StringBuilder amount = new StringBuilder(rawSplit[addressEndIndex++]);
                if (addressEndIndex < rawSplit.length && rawSplit[addressEndIndex].trim().matches("\\d{3}")) {
                    amount.append(",").append(rawSplit[addressEndIndex++]);
                }
                result[14] = amount.toString(); // Rice Subsidy WITH commas
            }

            // Phone Allowance (field 15)
            if (addressEndIndex < rawSplit.length) {
                StringBuilder amount = new StringBuilder(rawSplit[addressEndIndex++]);
                if (addressEndIndex < rawSplit.length && rawSplit[addressEndIndex].trim().matches("\\d{3}")) {
                    amount.append(",").append(rawSplit[addressEndIndex++]);
                }
                result[15] = amount.toString(); // Phone Allowance WITH commas
            }

            // Clothing Allowance (field 16)
            if (addressEndIndex < rawSplit.length) {
                StringBuilder amount = new StringBuilder(rawSplit[addressEndIndex++]);
                if (addressEndIndex < rawSplit.length && rawSplit[addressEndIndex].trim().matches("\\d{3}")) {
                    amount.append(",").append(rawSplit[addressEndIndex++]);
                }
                result[16] = amount.toString(); // Clothing Allowance WITH commas
            }

            // Gross Semi-monthly Rate (field 17)
            if (addressEndIndex < rawSplit.length) {
                StringBuilder amount = new StringBuilder(rawSplit[addressEndIndex++]);
                if (addressEndIndex < rawSplit.length && rawSplit[addressEndIndex].trim().matches("\\d{3}")) {
                    amount.append(",").append(rawSplit[addressEndIndex++]);
                }
                result[17] = amount.toString(); // Gross Semi-monthly Rate WITH commas
            }

            // Hourly Rate (field 18)
            if (addressEndIndex < rawSplit.length) {
                StringBuilder amount = new StringBuilder(rawSplit[addressEndIndex++]);
                if (addressEndIndex < rawSplit.length && rawSplit[addressEndIndex].trim().matches("\\d{3}")) {
                    amount.append(",").append(rawSplit[addressEndIndex++]);
                }
                result[18] = amount.toString(); // Hourly Rate WITH commas
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error parsing employee CSV line: " + line);
            e.printStackTrace();

            // Fallback to basic parsing if special parsing fails
            String[] basicSplit = line.split(",");

            // Just copy what we can
            for (int i = 0; i < Math.min(basicSplit.length, result.length); i++) {
                result[i] = basicSplit[i];
            }

            return result;
        }
    }

    /**
     * Retrieves all employee records from the employee records list.
     *
     * @return A list of maps, where each map represents an employee record.
     */
    public List<Map<String, String>> getAllEmployeeRecords() {
        return new ArrayList<>(this.employeeRecords);
    }

    /**
     * Retrieves all attendance records for a given employee ID.
     * @param employeeId The ID of the employee.
     * @return A list of attendance records for the employee.
     */
    public List<Map<String, Object>> getAttendanceRecordsByEmployeeId(String employeeId) {
        List<Map<String, Object>> filteredRecords = new ArrayList<>();
        for (Map<String, Object> record : attendanceRecords) {
            if (employeeId.equals(record.get("Employee ID"))) {
                filteredRecords.add(record);
            }
        }
        return filteredRecords;
    }

    /**
     * Retrieves an employee's record by their ID.
     * This improved version includes validation to ensure Position field is correct.
     * 
     * @param employeeId The employee's unique identifier.
     * @return The employee record if found; otherwise, null.
     */
    public Map<String, String> getEmployeeRecordsByEmployeeId(String employeeId) {
        for (Map<String, String> record : employeeRecords) {
            String recordId = record.get("Employee ID");
            if (employeeId.equals(recordId)) {
                // Create a safe copy of the record to prevent modification
                Map<String, String> safeCopy = new HashMap<>(record);
                
                // Validate Position field
                String position = safeCopy.get("Position");
                if (position == null || position.isEmpty() || isNumeric(position)) {
                    System.err.println("WARNING: Invalid Position for Employee ID " + employeeId + ": " + position);
                    
                    // Debug info - print all fields
                    System.err.println("Record fields for debugging:");
                    for (Map.Entry<String, String> entry : safeCopy.entrySet()) {
                        System.err.println("  " + entry.getKey() + ": " + entry.getValue());
                    }
                    
                    // Fix position if it's numeric or empty
                    if (isNumeric(position) || position == null || position.isEmpty()) {
                        // Use the inference method to determine position
                        String inferredPosition = inferPositionFromEmployeeData(safeCopy);
                        safeCopy.put("Position", inferredPosition);
                        System.out.println("Fixed position for Employee ID " + employeeId + " to '" + inferredPosition + "'");
                    }
                }
                
                return safeCopy;
            }
        }
        return null; // Employee ID not found
    }

    /**
     * Retrieves all leave request records.
     * @return A list of leave request records.
     */
    public List<Map<String, String>> getAllLeaveRequestRecords() {
        return new ArrayList<>(this.leaveRequestRecords);
    }

    /**
     * Retrieves all user credential records.
     * @return A list of user credential records.
     */
    public List<Map<String, String>> getAllUserCredentialRecords() {
        return new ArrayList<>(this.userCredentialRecords);
    }
    
    /**
     * Sets a custom directory for CSV files
     * @param directory The directory path
     */
    public void setCsvDirectory(String directory) {
        this.csvDirectory = directory;
        if (!this.csvDirectory.endsWith("/") && !this.csvDirectory.endsWith("\\")) {
            this.csvDirectory += "/";
        }
    }

    /**
     * Checks if a string is numeric.
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.chars().allMatch(Character::isDigit);
    }

    @FunctionalInterface
    private interface CSVRecordParser<T> {
        T parseRecord(String line);
    }

    @FunctionalInterface
    private interface ColumnMappingDefinition {
        void defineColumnMapping();
    }
}