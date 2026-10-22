package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.RosterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RosterEntryRepository extends JpaRepository<RosterEntry, Long> {

    boolean existsByOfferingAndStudentNumber(CourseOffering offering, String studentNumber);

    // Used at registration: every class list this new student is on.
    List<RosterEntry> findByStudentNumber(String studentNumber);

    List<RosterEntry> findByOfferingOrderByStudentNumberAsc(CourseOffering offering);
}