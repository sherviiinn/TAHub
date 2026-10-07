package ir.TAHub.TAHub.repository;

import java.util.List;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used by login: the student number is the username.
    Optional<User> findByStudentNumber(String studentNumber);

    // Used by registration to reject duplicate student numbers.
    boolean existsByStudentNumber(String studentNumber);

    // Used to create the first admin only when none exists.
    boolean existsByRole(Role role);
    // Used to fill the professor dropdown.
    List<User> findByRoleOrderByFullNameAsc(Role role);
}