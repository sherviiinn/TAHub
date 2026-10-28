package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.CafeteriaMeal;
import ir.TAHub.TAHub.model.CafeteriaSettings;
import ir.TAHub.TAHub.model.Food;
import ir.TAHub.TAHub.model.MealType;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CafeteriaMealRepository;
import ir.TAHub.TAHub.repository.CafeteriaSettingsRepository;
import ir.TAHub.TAHub.repository.FoodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
 * It also knows when reservation for each day opens and closes (CafeteriaSettings).
 */
@Service
public class CafeteriaService {

    public static final int DAYS_IN_WEEK = 7;
    public static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    private static final Logger log = LoggerFactory.getLogger(CafeteriaService.class);

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(TEHRAN);

    /** Reservation state of one day: "before" (not open yet), "open" or "closed". */
    public enum ReservationState { BEFORE, OPEN, CLOSED }

    /**
     * Reservation of one day as the page shows it.
     * percentLeft: how much of the open time is left (100 when it just opened, 0 when closed).
     * color: "bar-green" (more than 2/3 left), "bar-yellow" (1/3 to 2/3), "bar-red" (less than 1/3), "bar-grey".
     * remaining: time until it opens (BEFORE) or closes (OPEN), like "1d 4h 3m".
     */
    public record ReservationView(ReservationState state, long opensMillis, long closesMillis, int percentLeft,
                                  String color, String remaining, String opensText, String closesText) {
    }

    /** What the admin chose for one meal: up to two food ids (null = "-"). */
    public record MealChoice(Long option1Id, Long option2Id) {
    }

    /** Thrown when a chosen food does not exist or is inactive (for example deactivated in another tab). */
    public static class FoodNotAvailableException extends RuntimeException {
    }

    private final CafeteriaMealRepository mealRepository;
    private final FoodRepository foodRepository;
    private final CafeteriaSettingsRepository settingsRepository;
    private final NotificationService notificationService;

    public CafeteriaService(CafeteriaMealRepository mealRepository,
                            FoodRepository foodRepository,
                            CafeteriaSettingsRepository settingsRepository,
                            NotificationService notificationService) {
        this.mealRepository = mealRepository;
        this.foodRepository = foodRepository;
        this.settingsRepository = settingsRepository;
        this.notificationService = notificationService;
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

    // ---------- reservation times ----------

    /** The saved settings, or the defaults if an admin never saved them. */
    public CafeteriaSettings settings() {
        return settingsRepository.findById(CafeteriaSettings.SINGLE_ID).orElseGet(CafeteriaSettings::new);
    }

    @Transactional
    public void saveSettings(int openDaysBefore, LocalTime openTime,
                             int closeDaysBefore, LocalTime closeTime) {
        CafeteriaSettings settings = settings();
        settings.setOpenDaysBefore(openDaysBefore);
        settings.setOpenTime(openTime);
        settings.setCloseDaysBefore(closeDaysBefore);
        settings.setCloseTime(closeTime);
        settings.setUpdatedAt(Instant.now());
        settingsRepository.save(settings);
    }

    public Instant opensAt(LocalDate day, CafeteriaSettings settings) {
        return day.minusDays(settings.getOpenDaysBefore()).atTime(settings.getOpenTime()).atZone(TEHRAN).toInstant();
    }

    public Instant closesAt(LocalDate day, CafeteriaSettings settings) {
        return day.minusDays(settings.getCloseDaysBefore()).atTime(settings.getCloseTime()).atZone(TEHRAN).toInstant();
    }

    public ReservationView reservation(LocalDate day, CafeteriaSettings settings) {
        Instant now = Instant.now();
        Instant opens = opensAt(day, settings);
        Instant closes = closesAt(day, settings);

        ReservationState state;
        int percentLeft;
        String color;
        String remaining;
        if (now.isBefore(opens)) {
            state = ReservationState.BEFORE;
            percentLeft = 100;
            color = "bar-grey";
            remaining = shortDuration(Duration.between(now, opens));
        } else if (now.isBefore(closes)) {
            state = ReservationState.OPEN;
            double left = (double) Duration.between(now, closes).toMillis() / Duration.between(opens, closes).toMillis();
            percentLeft = (int) Math.round(left * 100);
            color = left > 2.0 / 3 ? "bar-green" : left > 1.0 / 3 ? "bar-yellow" : "bar-red";
            remaining = shortDuration(Duration.between(now, closes));
        } else {
            state = ReservationState.CLOSED;
            percentLeft = 0;
            color = "bar-grey";
            remaining = "";
        }
        return new ReservationView(state, opens.toEpochMilli(), closes.toEpochMilli(), percentLeft, color,
                remaining, WHEN.format(opens), WHEN.format(closes));
    }

    /** True if "opens N days before at T1" really comes before "closes M days before at T2". */
    public static boolean opensBeforeCloses(int openDaysBefore, LocalTime openTime,
                                            int closeDaysBefore, LocalTime closeTime) {
        LocalDate sample = LocalDate.of(2026, 1, 1);
        return sample.minusDays(openDaysBefore).atTime(openTime)
                .isBefore(sample.minusDays(closeDaysBefore).atTime(closeTime));
    }

    /** Like "1d 4h 3m" (days are left out when zero). */
    static String shortDuration(Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        long days = minutes / (24 * 60);
        long hours = (minutes / 60) % 24;
        long mins = minutes % 60;
        return (days > 0 ? days + "d " : "") + hours + "h " + mins + "m";
    }

    // ---------- notifications ----------

    /**
     * After the admin changed the menu: students hear about the changed days whose reservation has already
     * opened (they may have reserved already) and that are not over. Days not open yet need no message.
     */
    public void tellAboutChangedDays(List<LocalDate> changedDays) {
        CafeteriaSettings settings = settings();
        Instant now = Instant.now();
        LocalDate today = today();
        List<LocalDate> worthTelling = changedDays.stream()
                .filter(day -> !day.isBefore(today))
                .filter(day -> !now.isBefore(opensAt(day, settings)))
                .toList();
        notificationService.notifyCafeteriaMenuChanged(worthTelling);
    }

    /** Every Wednesday at 08:00 Tehran time: "do not forget to reserve" for every student. */
    @Scheduled(cron = "0 0 8 * * WED", zone = "Asia/Tehran")
    public void weeklyReminder() {
        int count = notificationService.notifyCafeteriaReminder();
        log.info("Sent the weekly cafeteria reminder to {} students", count);
    }

    // ---------- cleanup ----------

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
