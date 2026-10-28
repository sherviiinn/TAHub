package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CafeteriaMeal;
import ir.TAHub.TAHub.model.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CafeteriaMealRepository extends JpaRepository<CafeteriaMeal, Long> {

    // All meals of one week (from Saturday to Friday, both included).
    List<CafeteriaMeal> findByMealDateBetween(LocalDate from, LocalDate to);

    Optional<CafeteriaMeal> findByMealDateAndMealType(LocalDate mealDate, MealType mealType);

    // Removes the meals of weeks that are over. Returns how many rows were removed.
    @Transactional
    long deleteByMealDateBefore(LocalDate day);
}
