package ir.TAHub.TAHub.model;

import jakarta.persistence.*;

/**
 * One course taught in one semester by one professor.
 * Enrollments, TAs, announcements and assignments will belong to this class.
 * Old offerings are never deleted: grades and history depend on them.
 */
@Entity
@Table(name = "course_offerings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "semester_id", "professor_id"}))
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;

    // Students need this code to enroll. null means enrollment is closed.
    @Column(length = 20)
    private String joinCode;

    public CourseOffering() {
    }

    public Long getId() { return id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Semester getSemester() { return semester; }
    public void setSemester(Semester semester) { this.semester = semester; }

    public User getProfessor() { return professor; }
    public void setProfessor(User professor) { this.professor = professor; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    /** True if this user may manage the offering: an admin, or the professor who teaches it. */
    public boolean isManagedBy(User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getRole() == Role.PROFESSOR && professor.getId().equals(user.getId());
    }
}