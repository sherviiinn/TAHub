package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // For the admin list: active foods first, then inactive ones, each group by name.
    List<Food> findAllByOrderByActiveDescNameAsc();

    // The foods that can be chosen for a menu.
    List<Food> findByActiveTrueOrderByNameAsc();

    Optional<Food> findByNameIgnoreCase(String name);
}
