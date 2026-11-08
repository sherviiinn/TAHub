package ir.TAHub.TAHub.repository;

import java.util.List;

import ir.TAHub.TAHub.model.Major;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // How many users have this field of study (a used major cannot be deleted).
    long countByMajor(Major major);
    // Used by login: the student number is the username.
    Optional<User> findByStudentNumber(String studentNumber);

    // Used by registration to reject duplicate student numbers.
    boolean existsByStudentNumber(String studentNumber);

    // Used to create the first admin only when none exists.
    boolean existsByRole(Role role);
    // Used to fill the professor dropdown.
    List<User> findByRoleOrderByFullNameAsc(Role role);

    // Admin accounts panel: search by (part of) the name, the login name or a released number.
    @Query("select u from User u where lower(u.fullName) like lower(concat('%', :q, '%'))"
            + " or u.studentNumber like concat('%', :q, '%')"
            + " or u.releasedNumber like concat('%', :q, '%')"
            + " order by u.fullName")
    List<User> search(@Param("q") String q, Pageable pageable);

    // Admin accounts panel, "invalid numbers" filter: students whose number was not released.
    List<User> findByRoleAndReleasedNumberIsNullOrderByFullNameAsc(Role role);
}
