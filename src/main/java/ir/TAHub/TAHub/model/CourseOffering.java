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

    // A cancelled offering is hidden from students and lists, but kept (it may have history).
    @Column(nullable = false)
    private boolean cancelled;

    // A professor opens this when they want TA requests. It is closed by default.
    @Column(nullable = false)
    private boolean taApplicationsOpen;

    /** Link to the course's Telegram group (https://t.me/...). Empty if the professor has not set one. */
    @Column(length = 200)
    private String telegramLink;

    /**
     * The most TAs the team may have (head TAs included, waiting invitations counted too).
     * When it is reached nobody new can join (not even through the professor) until the professor raises it;
     * head TAs can ask for more. A new offering starts with 1. Empty means no limit
     * (offerings made before this rule existed are empty).
     */
    private Integer taTeamLimit = DEFAULT_TA_TEAM_LIMIT;

    public static final int DEFAULT_TA_TEAM_LIMIT = 1;

    /** True when the professor or a TA chose "Hide suggestions" on the resources page (last semester's links). */
    private Boolean resourceSuggestionsHidden;

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

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public boolean isTaApplicationsOpen() { return taApplicationsOpen; }
    public void setTaApplicationsOpen(boolean taApplicationsOpen) { this.taApplicationsOpen = taApplicationsOpen; }

    public String getTelegramLink() { return telegramLink; }
    public void setTelegramLink(String telegramLink) { this.telegramLink = telegramLink; }

    public Boolean getResourceSuggestionsHidden() { return resourceSuggestionsHidden; }
    public void setResourceSuggestionsHidden(Boolean resourceSuggestionsHidden) { this.resourceSuggestionsHidden = resourceSuggestionsHidden; }

    public Integer getTaTeamLimit() { return taTeamLimit; }
    public void setTaTeamLimit(Integer taTeamLimit) { this.taTeamLimit = taTeamLimit; }

}