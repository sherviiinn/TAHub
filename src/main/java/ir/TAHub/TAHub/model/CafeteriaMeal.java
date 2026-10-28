package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One meal on one day. Students choose one of (at most) two foods, so a meal has two options.
 * Option 1 is always filled; option 2 is empty when there is only one food.
 * Only the current and the next week are kept; older rows are removed every night.
 */
@Entity
@Table(name = "cafeteria_meals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meal_date", "meal_type"}))
public class CafeteriaMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "option1_id", nullable = false)
    private Food option1;

    @ManyToOne
    @JoinColumn(name = "option2_id")
    private Food option2;

    @Column(nullable = false)
    private Instant updatedAt;

    /** The admin who saved it last. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "updated_by_id", nullable = false)
    private User updatedBy;

    public CafeteriaMeal() {
    }

    /** Option 1 or 2 (pages use this to draw both boxes with one loop). */
    public Food getOption(int slot) {
        return slot == 1 ? option1 : option2;
    }

    public Long getId() { return id; }

    public LocalDate getMealDate() { return mealDate; }
    public void setMealDate(LocalDate mealDate) { this.mealDate = mealDate; }

    public MealType getMealType() { return mealType; }
    public void setMealType(MealType mealType) { this.mealType = mealType; }

    public Food getOption1() { return option1; }
    public void setOption1(Food option1) { this.option1 = option1; }

    public Food getOption2() { return option2; }
    public void setOption2(Food option2) { this.option2 = option2; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
}
