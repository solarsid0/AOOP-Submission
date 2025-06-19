package oop.classes.actors;

import java.time.LocalDate;
import java.util.Date;
import oop.classes.empselfservice.Payslip;


public class Accounting extends Employee {

    //contructor with basic parameters for login)
    public Accounting (int employeeID, String firstName, String lastName, String email, String password, String role) {
        super(employeeID, firstName, lastName, email, password, role);
    }
    // full constructor with all parameters
    public Accounting(int employeeID, String firstName, String lastName,
                      String address, String phoneNo,
                      String email, String password, String role,
                      LocalDate birthday, String sssNo, String philhealthNo,
                      String pagibigNo, String tinNo, String position, Employee supervisor,
                      String empStatus,
                      double basicSalary, double riceSubsidy, double phoneAllowance,
                      double clothingAllowance, double grossSemiMthlyRate,
                      double hourlyRate, double vacationLeave, double sickLeave, double emergencyLeave,
                      double maternityLeave, double paternityLeave,
                      double specialLeave) {
        super(employeeID, firstName, lastName, email, password, role,
                birthday, address, phoneNo, sssNo, philhealthNo, position,
                pagibigNo, empStatus, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMthlyRate, hourlyRate);
    }


    @Override
   public Payslip viewPayrollDetails(LocalDate payPeriod) {
       // Return a payslip for this accounting employee
       return new Payslip(this);
   }

   // Overloaded generatePayslip method
   public Payslip generatePayslip(Employee employee, Date periodStartDate, Date periodEndDate) {
       // Create a payslip for the specified employee
       return new Payslip(employee);
   }

        public Payslip generatePayslip(int employeeID, Date periodStartDate, Date periodEndDate) {
       // Find the employee with the given ID (placeholder logic)
       Employee employee = findEmployeeById(employeeID);
       if (employee != null) {
           return new Payslip(employee);
       }
       return null; // Or throw an exception if employee not found
   }

   // Helper method to find an employee by ID (needs to be implemented)
   private Employee findEmployeeById(int employeeID) {
       // Implementation to look up an employee by ID
       // This would typically involve a database query or lookup in a collection
       return null; // Placeholder
   }
    // approve payslip
    public boolean approvePayslip(int payslipID) {
        return true;
    }

    // deny payslip
    public boolean denyPayslip(int payslipID, String reason) {
        return true;
    }

    @Override
    public void downloadPayrollDetails(int payslipID) {
        // placeholder logic
    }
}