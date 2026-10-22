package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaApplication;
import ir.TAHub.TAHub.model.TaApplicationStatus;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaApplicationRepository extends JpaRepository<TaApplication, Long> {

    Optional<TaApplication> findByStudentAndOffering(User student, CourseOffering offering);

    long countByOfferingAndStatus(CourseOffering offering, TaApplicationStatus status);

    // For a student: all of their requests, newest first.
    List<TaApplication> findByStudentOrderByIdDesc(User student);

    // For a professor: requests for the offerings they teach (offering.professor).
    List<TaApplication> findByOfferingProfessorAndStatusOrderByIdAsc(User professor, TaApplicationStatus status);

    // For an admin: requests for all offerings.
    List<TaApplication> findByStatusOrderByIdAsc(TaApplicationStatus status);
}