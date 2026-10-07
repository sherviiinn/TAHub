package ir.TAHub.TAHub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data submitted by the registration form.
 * It deliberately has no "role" field: new accounts are always students.
 */
public class RegisterForm {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "\\d{4,20}")
    private String studentNumber;

    // BCrypt only uses the first 72 bytes, so we cap the length.
    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
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