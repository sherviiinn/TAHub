package ir.TAHub.TAHub.repository;


import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
