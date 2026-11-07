package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The format of student numbers (only one row, id 1). Noshirvani numbers have 11 digits and start with the
 * entry year (403... for 1403). The current year comes from the active semester's code (4052 -> 405), so the
 * allowed first three digits are from (current year - yearsBack) to the current year. Admins can change both.
 */
@Entity
@Table(name = "student_number_settings")
public class StudentNumberSettings {

    public static final long SINGLE_ID = 1L;

    @Id
    private Long id = SINGLE_ID;

    @Column(nullable = false)
    private int digits = 11;

    @Column(nullable = false)
    private int yearsBack = 10;

    public StudentNumberSettings() {
    }

    public Long getId() { return id; }

    public int getDigits() { return digits; }
    public void setDigits(int digits) { this.digits = digits; }

    public int getYearsBack() { return yearsBack; }
    public void setYearsBack(int yearsBack) { this.yearsBack = yearsBack; }
}
