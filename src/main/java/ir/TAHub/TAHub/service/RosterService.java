package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentSource;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.RosterEntry;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.RosterEntryRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Class lists provided by professors, and the automatic enrollments that come from them. */
@Service
public class RosterService {

    public static final int MAX_ENTRIES = 500;

    /** The valid student numbers found in the pasted text, and how many tokens were not valid. */
    public record ParsedList(Set<String> numbers, int invalid) {
    }

    /** What happened to the numbers of one import. */
    public record ImportResult(int enrolled, int confirmed, int waiting, int skipped, int invalid) {
    }

    private final RosterEntryRepository rosterRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public RosterService(RosterEntryRepository rosterRepository,
                         UserRepository userRepository,
                         EnrollmentRepository enrollmentRepository) {
        this.rosterRepository = rosterRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /** Splits the text on spaces, new lines, commas and semicolons. Duplicates are removed. */
    public ParsedList parse(String text) {
        Set<String> numbers = new LinkedHashSet<>();
        int invalid = 0;
        for (String token : Digits.toAscii(text == null ? "" : text).split("[\\s,;]+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.matches("\\d{4,20}")) {
                numbers.add(token);
            } else {
                invalid++;
            }
        }
        return new ParsedList(numbers, invalid);
    }

    @Transactional
    public ImportResult importNumbers(CourseOffering offering, Collection<String> numbers, int invalid) {
        int enrolled = 0;
        int confirmed = 0;
        int waiting = 0;
        int skipped = 0;

        for (String number : numbers) {
            if (!rosterRepository.existsByOfferingAndStudentNumber(offering, number)) {
                RosterEntry entry = new RosterEntry();
                entry.setOffering(offering);
                entry.setStudentNumber(number);
                entry.setAddedAt(Instant.now());
                rosterRepository.save(entry);
            }

            User user = userRepository.findByStudentNumber(number).orElse(null);
            if (user == null) {
                waiting++; // enrolled automatically when they register
                continue;
            }
            if (user.getRole() != Role.STUDENT) {
                skipped++;
                continue;
            }

            Enrollment enrollment = enrollmentRepository.findByStudentAndOffering(user, offering).orElse(null);
            if (enrollment == null) {
                enrollmentRepository.save(newRosterEnrollment(user, offering));
                enrolled++;
            } else if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                // Already in the course (maybe with the join code): now confirmed by the official list.
                if (enrollment.getSource() != EnrollmentSource.ROSTER) {
                    enrollment.setSource(EnrollmentSource.ROSTER);
                    enrollmentRepository.save(enrollment);
                }
                confirmed++;
            } else {
                // DROPPED by the student or REMOVED by the professor: we respect that decision.
                skipped++;
            }
        }
        return new ImportResult(enrolled, confirmed, waiting, skipped, invalid);
    }

    /** Called right after a student registers: enrolls them in every open offering whose list has their number. */
    @Transactional
    public void enrollFromRosters(User student) {
        for (RosterEntry entry : rosterRepository.findByStudentNumber(student.getStudentNumber())) {
            CourseOffering offering = entry.getOffering();
            if (!offering.getSemester().isActive() || offering.isCancelled()) {
                continue;
            }
            if (enrollmentRepository.findByStudentAndOffering(student, offering).isEmpty()) {
                enrollmentRepository.save(newRosterEnrollment(student, offering));
            }
        }
    }

    private Enrollment newRosterEnrollment(User student, CourseOffering offering) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setOffering(offering);
        enrollment.setEnrolledAt(Instant.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setSource(EnrollmentSource.ROSTER);
        return enrollment;
    }
}