package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Course;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    // "CourseCode" means: sort by the code of the related course.
    List<CourseOffering> findBySemesterOrderByCourseCodeAsc(Semester semester);

    boolean existsByCourseAndSemesterAndProfessor(Course course, Semester semester, User professor);
}