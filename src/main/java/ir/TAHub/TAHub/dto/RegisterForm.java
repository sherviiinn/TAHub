package ir.TAHub.TAHub.dto;

/**
 * Data submitted by the registration form.
 * It deliberately has no "role" field: new accounts are always students.
 * Validation is done step by step in RegistrationController.
 */
public class RegisterForm {

    private String fullName;
    private String studentNumber;
    private String password;
    private String confirmPassword;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}