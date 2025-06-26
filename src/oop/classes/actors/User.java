
package oop.classes.actors;

/**
 *
 * @author Admin
 */



public abstract class User {
    // attributes
    protected int employeeID; // id for each employee
    protected String firstName; // employee's first name
    protected String lastName; // employee's last name
    protected String email; // username for login credentials
    protected String password; // password for login credentials
    protected String role; // user's assigned role in the system

    // default constructor
    public User() {
        // default constructor to initialize an empty user object
    }

    // parameterized constructor to create a user object with specific values
    public User(int employeeID, String firstName, String lastName, String email, String password, String role) {
        this.employeeID = employeeID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
    }
    
    // GETTERS to allow access to private attributes
    public int getEmployeeID() {
        return employeeID;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    // SETTERS to allow modification of protected attributes with validation
    public void setFirstName(String firstName) {
        if (firstName != null && !firstName.isEmpty()) {
            this.firstName = firstName;
        } else {
            throw new IllegalArgumentException("First name cannot be empty");
        }
    }

    public void setLastName(String lastName) {
        if (lastName != null && !lastName.isEmpty()) {
            this.lastName = lastName;
        } else {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
    }

    public void setEmail(String email) {
        if (isValidEmail(email)) {
            this.email = email;
        } else {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    public void setPassword(String password) {
        if (password != null && !password.isEmpty()) {
            this.password = password;
        } else {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    public void setRole(String role) {
        if (role != null) {
            this.role = role;
        } else {
            throw new NullPointerException("Role cannot be null");
        }
    }

    // METHODS, checks if the username and password match the user's credentials
        public String login(String email, String password) {
        if (this.email.equals(email) && this.password.equals(password)) {
            return this.role; // Return the user's role upon successful login
        }
        return null; // Return null if login fails
    }

    // this method logs out the user
    public void logout() {
        System.out.println("You have successfully logged out.");
    }

    
    Employee viewEmployeeDetails() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    //checks if an email is in valid format (ex. juandelacruz@mph.com"
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile(emailRegex, java.util.regex.Pattern.CASE_INSENSITIVE);
        return emailPattern.matcher(email).find();
    }

    public int getEmployeeId() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}

