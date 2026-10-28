package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CafeteriaMeal;
import ir.TAHub.TAHub.model.MealType;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.FoodRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.CafeteriaService;
import ir.TAHub.TAHub.service.CafeteriaService.MealChoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The cafeteria menu of the current week and the next week (older weeks are not kept).
 * Everyone who is logged in reads it at /cafeteria. Admins enter it at /admin/cafeteria.
 * ?next=true chooses the next week; without it, the current week is shown.
 */
@Controller
public class CafeteriaController {

    /** One day of the week as the pages show it. meals: meal -> row (missing if nothing was entered). */
    public record DayView(int index, LocalDate date, String dayKey, boolean today, Map<MealType, CafeteriaMeal> meals) {
    }

    private final CafeteriaService cafeteriaService;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final String reservationUrl;

    public CafeteriaController(CafeteriaService cafeteriaService,
                               FoodRepository foodRepository,
                               UserRepository userRepository,
                               @Value("${tahub.cafeteria.reservation-url:https://food.nit.ac.ir}") String reservationUrl) {
        this.cafeteriaService = cafeteriaService;
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.reservationUrl = reservationUrl;
    }

    @GetMapping("/cafeteria")
    public String menu(@RequestParam(defaultValue = "false") boolean next, Model model) {
        addWeek(model, next);
        model.addAttribute("reservationUrl", reservationUrl);
        return "cafeteria";
    }

    @GetMapping("/admin/cafeteria")
    public String editForm(@RequestParam(defaultValue = "false") boolean next, Model model) {
        addWeek(model, next);
        model.addAttribute("activeFoods", foodRepository.findByActiveTrueOrderByNameAsc());
        return "cafeteria-edit";
    }

    @PostMapping("/admin/cafeteria")
    public String save(@RequestParam(defaultValue = "false") boolean next,
                       @RequestParam Map<String, String> params,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        String back = "redirect:/admin/cafeteria" + (next ? "?next=true" : "");

        // The boxes are named meal_<dayIndex>_<MEAL>_<1 or 2>, like meal_0_LUNCH_2 for Saturday lunch, option 2.
        List<Map<MealType, MealChoice>> choices = new ArrayList<>();
        try {
            for (int dayIndex = 0; dayIndex < CafeteriaService.DAYS_IN_WEEK; dayIndex++) {
                Map<MealType, MealChoice> day = new EnumMap<>(MealType.class);
                for (MealType mealType : MealType.values()) {
                    String prefix = "meal_" + dayIndex + "_" + mealType.name() + "_";
                    day.put(mealType, new MealChoice(parseId(params.get(prefix + "1")), parseId(params.get(prefix + "2"))));
                }
                choices.add(day);
            }
            cafeteriaService.saveWeek(cafeteriaService.weekStart(next), choices, currentUser(authentication));
        } catch (NumberFormatException | CafeteriaService.FoodNotAvailableException e) {
            redirect.addFlashAttribute("cafeteriaErrorKey", "cafeteria.error.foodNotAvailable");
            return back;
        }

        redirect.addFlashAttribute("cafeteriaSaved", true);
        return back;
    }

    // ---------- helpers ----------

    /** "" (the "-" choice) means no food. Anything that is not a number is rejected. */
    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private void addWeek(Model model, boolean next) {
        LocalDate weekStart = cafeteriaService.weekStart(next);
        LocalDate today = cafeteriaService.today();
        Map<LocalDate, Map<MealType, CafeteriaMeal>> meals = cafeteriaService.week(weekStart);

        List<DayView> days = new ArrayList<>();
        for (int i = 0; i < CafeteriaService.DAYS_IN_WEEK; i++) {
            LocalDate date = weekStart.plusDays(i);
            days.add(new DayView(i, date, "cafeteria.day." + date.getDayOfWeek().name(), date.equals(today),
                    meals.getOrDefault(date, Map.of())));
        }

        model.addAttribute("days", days);
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("slots", List.of(1, 2));
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekStart.plusDays(CafeteriaService.DAYS_IN_WEEK - 1));
        model.addAttribute("isNextWeek", next);
        model.addAttribute("hasAnyMeal", !meals.isEmpty());
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
