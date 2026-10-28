package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.CafeteriaMeal;
import ir.TAHub.TAHub.model.Food;
import ir.TAHub.TAHub.model.MealType;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CafeteriaMealRepository;
import ir.TAHub.TAHub.repository.FoodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The cafeteria menu. In Iran the week starts on Saturday.
 * Only two weeks exist: the current one and the next one.
 */
@Service
public class CafeteriaService {

    public static final int DAYS_IN_WEEK = 7;
    public static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    private static final Logger log = LoggerFactory.getLogger(CafeteriaService.class);

    /** What the admin chose for one meal: up to two food ids (null = "-"). */
    public record MealChoice(Long option1Id, Long option2Id) {
    }

    /** Thrown when a chosen food does not exist or is inactive (for example deactivated in another tab). */
    public static class FoodNotAvailableException extends RuntimeException {
    }

    private final CafeteriaMealRepository mealRepository;
    private final FoodRepository foodRepository;

    public CafeteriaService(CafeteriaMealRepository mealRepository, FoodRepository foodRepository) {
        this.mealRepository = mealRepository;
        this.foodRepository = foodRepository;
    }

    /** Today in Tehran (the server may run in another time zone). */
    public LocalDate today() {
        return LocalDate.now(TEHRAN);
    }

    /** The Saturday of the current week, or of the next week. */
    public LocalDate weekStart(boolean nextWeek) {
        LocalDate thisWeek = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        return nextWeek ? thisWeek.plusWeeks(1) : thisWeek;
    }

    /** The meals of one week: day -> (meal -> row). Days or meals with nothing entered are missing. */
    public Map<LocalDate, Map<MealType, CafeteriaMeal>> week(LocalDate weekStart) {
        Map<LocalDate, Map<MealType, CafeteriaMeal>> result = new HashMap<>();
        for (CafeteriaMeal meal : mealRepository.findByMealDateBetween(weekStart, weekStart.plusDays(DAYS_IN_WEEK - 1))) {
            result.computeIfAbsent(meal.getMealDate(), d -> new EnumMap<>(MealType.class))
                    .put(meal.getMealType(), meal);
        }
        return result;
    }

    /**
     * Saves a whole week at once. choices.get(dayIndex).get(meal) is what the admin chose (dayIndex 0 is Saturday).
     * Everything is checked first, so either the whole week is saved or nothing is.
     * A food can be chosen if it is active, or if it was already in that meal (so a food made inactive
     * later does not disappear from a menu just because the admin saved again).
     * Returns the days whose menu changed (the next step uses it for notifications).
     */
    @Transactional
    public List<LocalDate> saveWeek(LocalDate weekStart, List<Map<MealType, MealChoice>> choices, User admin) {
        Map<LocalDate, Map<MealType, CafeteriaMeal>> existingWeek = week(weekStart);

        // 1. Check everything and find the Food objects.
        List<Map<MealType, Food[]>> resolved = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < DAYS_IN_WEEK; dayIndex++) {
            Map<MealType, CafeteriaMeal> existingDay = existingWeek.getOrDefault(weekStart.plusDays(dayIndex), Map.of());
            Map<MealType, Food[]> day = new EnumMap<>(MealType.class);
            for (MealType mealType : MealType.values()) {
                MealChoice choice = choices.get(dayIndex).get(mealType);
                CafeteriaMeal existing = existingDay.get(mealType);
                Food first = resolve(choice.option1Id(), existing);
                Food second = resolve(choice.option2Id(), existing);

                // "-" in the first box but a food in the second: move it up. The same food twice: keep one.
                if (first == null) {
                    first = second;
                    second = null;
                }
                if (first != null && second != null && first.getId().equals(second.getId())) {
                    second = null;
                }
                day.put(mealType, new Food[]{first, second});
            }
            resolved.add(day);
        }

        // 2. Save what changed.
        List<LocalDate> changedDays = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < DAYS_IN_WEEK; dayIndex++) {
            LocalDate date = weekStart.plusDays(dayIndex);
            Map<MealType, CafeteriaMeal> existingDay = existingWeek.getOrDefault(date, Map.of());
            boolean dayChanged = false;

            for (MealType mealType : MealType.values()) {
                Food first = resolved.get(dayIndex).get(mealType)[0];
                Food second = resolved.get(dayIndex).get(mealType)[1];
                CafeteriaMeal existing = existingDay.get(mealType);

                if (first == null) {
                    if (existing != null) {
                        mealRepository.delete(existing);
                        dayChanged = true;
                    }
                    continue;
                }
                if (existing != null && sameFood(existing.getOption1(), first) && sameFood(existing.getOption2(), second)) {
                    continue;
                }
                CafeteriaMeal meal = existing != null ? existing : new CafeteriaMeal();
                meal.setMealDate(date);
                meal.setMealType(mealType);
                meal.setOption1(first);
                meal.setOption2(second);
                meal.setUpdatedAt(Instant.now());
                meal.setUpdatedBy(admin);
                mealRepository.save(meal);
                dayChanged = true;
            }
            if (dayChanged) {
                changedDays.add(date);
            }
        }
        return changedDays;
    }

    /** Every night (00:10 Tehran time) the menus of finished weeks are removed. */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Tehran")
    @Transactional
    public void removeOldWeeks() {
        long removed = mealRepository.deleteByMealDateBefore(weekStart(false));
        if (removed > 0) {
            log.info("Removed {} cafeteria meals of finished weeks", removed);
        }
    }

    // ---------- helpers ----------

    private Food resolve(Long foodId, CafeteriaMeal existing) {
        if (foodId == null) {
            return null;
        }
        Food food = foodRepository.findById(foodId).orElseThrow(FoodNotAvailableException::new);
        boolean alreadyThere = existing != null
                && (sameFood(existing.getOption1(), food) || sameFood(existing.getOption2(), food));
        if (!food.isActive() && !alreadyThere) {
            throw new FoodNotAvailableException();
        }
        return food;
    }

    private boolean sameFood(Food a, Food b) {
        return Objects.equals(a == null ? null : a.getId(), b == null ? null : b.getId());
    }
}
