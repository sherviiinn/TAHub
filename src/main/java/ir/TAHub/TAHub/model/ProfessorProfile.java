package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Office information of one professor, shown on their page in the professors section.
 * Every field is optional: the professor (or an admin) fills in only what they want to share.
 */
@Entity
@Table(name = "professor_profiles")
public class ProfessorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false, unique = true)
    private User professor;

    @Column(length = 100)
    private String building;

    @Column(length = 50)
    private String room;

    /** When students can usually find them, in the professor's own words (it may change). */
    @Column(length = 300)
    private String availability;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    /** Telegram username, kept without the "@". */
    @Column(length = 40)
    private String telegram;

    /** A personal or university page (https only). */
    @Column(length = 300)
    private String website;

    @Column(length = 300)
    private String note;

    private Instant updatedAt;

    /** The semester that was active at the last save, so an old text can be marked "may be out of date". */
    @ManyToOne
    @JoinColumn(name = "updated_semester_id")
    private Semester updatedSemester;

    public ProfessorProfile() {
    }

    /** True if nothing at all is filled in. */
    public boolean isEmpty() {
        return blank(building) && blank(room) && blank(availability) && blank(email) && blank(phone)
                && blank(telegram) && blank(website) && blank(note);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public Long getId() { return id; }

    public User getProfessor() { return professor; }
    public void setProfessor(User professor) { this.professor = professor; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTelegram() { return telegram; }
    public void setTelegram(String telegram) { this.telegram = telegram; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Semester getUpdatedSemester() { return updatedSemester; }
    public void setUpdatedSemester(Semester updatedSemester) { this.updatedSemester = updatedSemester; }
}
