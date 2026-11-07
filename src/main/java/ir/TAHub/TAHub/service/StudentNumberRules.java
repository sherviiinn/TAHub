package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.StudentNumberSettings;
import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.repository.StudentNumberSettingsRepository;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One place that decides whether a text is a valid STUDENT number. Used by registration, the class list,
 * the admin's "add user" form and the accounts panel. Professors' and admins' usernames do not follow it.
 */
@Service
public class StudentNumberRules {

    /** What the page tells people: "{digits} digits, starting with the entry year ({fromYear} to {toYear})". */
    public record Hint(int digits, Integer fromYear, Integer toYear) {
    }

    private final StudentNumberSettingsRepository settingsRepository;
    private final SemesterRepository semesterRepository;

    public StudentNumberRules(StudentNumberSettingsRepository settingsRepository, SemesterRepository semesterRepository) {
        this.settingsRepository = settingsRepository;
        this.semesterRepository = semesterRepository;
    }

    public StudentNumberSettings settings() {
        return settingsRepository.findById(StudentNumberSettings.SINGLE_ID).orElseGet(StudentNumberSettings::new);
    }

    @Transactional
    public void save(int digits, int yearsBack) {
        StudentNumberSettings settings = settings();
        settings.setDigits(digits);
        settings.setYearsBack(yearsBack);
        settingsRepository.save(settings);
    }

    /** Persian or Arabic digits become English ones, spaces around are removed. */
    public String clean(String number) {
        return number == null ? "" : Digits.toAscii(number).trim();
    }

    /** True if this (cleaned) text has the right number of digits and starts with an allowed entry year. */
    public boolean isValid(String number) {
        StudentNumberSettings settings = settings();
        if (number == null || !number.matches("\\d{" + settings.getDigits() + "}")) {
            return false;
        }
        Hint hint = hint();
        if (hint.fromYear() == null || settings.getDigits() < 3) {
            return true; // no active semester yet: only the length is checked
        }
        int year = Integer.parseInt(number.substring(0, 3));
        return year >= hint.fromYear() && year <= hint.toYear();
    }

    public Hint hint() {
        StudentNumberSettings settings = settings();
        Integer current = currentYear();
        if (current == null) {
            return new Hint(settings.getDigits(), null, null);
        }
        return new Hint(settings.getDigits(), current - settings.getYearsBack(), current);
    }

    /** The first three digits of the active semester's code (4052 -> 405), or null. */
    private Integer currentYear() {
        return semesterRepository.findByActiveTrue()
                .map(semester -> semester.getCode())
                .filter(code -> code.matches("\\d{3,}"))
                .map(code -> Integer.parseInt(code.substring(0, 3)))
                .orElse(null);
    }
}
