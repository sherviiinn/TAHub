package ir.TAHub.TAHub.model;

import jakarta.persistence.*;
import jakarta.persistence.EnumType;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    // The login name: the student number for students, a username for professors and admins.
    @Column(nullable = false, unique = true)
    private String studentNumber;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Field of study. Only students have one; professors and admins leave it empty.
    @ManyToOne
    @JoinColumn(name = "major_id")
    private Major major;

    // When an admin disabled the account (null = active). A disabled account cannot sign in.
    private Instant disabledAt;

    // Set when an admin released the student number: the old number is kept here and
    // studentNumber becomes "released-<id>", so the real owner of the number can register.
    @Column(length = 50)
    private String releasedNumber;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Major getMajor() { return major; }
    public void setMajor(Major major) { this.major = major; }

    public Instant getDisabledAt() { return disabledAt; }
    public void setDisabledAt(Instant disabledAt) { this.disabledAt = disabledAt; }

    public String getReleasedNumber() { return releasedNumber; }
    public void setReleasedNumber(String releasedNumber) { this.releasedNumber = releasedNumber; }

    public boolean isDisabled() { return disabledAt != null; }
}
