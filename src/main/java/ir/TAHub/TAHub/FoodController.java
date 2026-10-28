package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Food;
import ir.TAHub.TAHub.repository.FoodRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

/**
 * The list of foods the weekly menu can use. Admins only (SecurityConfig allows /admin/** for admins).
 * Foods are never deleted, only made inactive, because old menus may still show them.
 */
@Controller
public class FoodController {

    private static final int NAME_MAX = 100;

    private final FoodRepository foodRepository;

    public FoodController(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    @GetMapping("/admin/foods")
    public String list(Model model) {
        model.addAttribute("foods", foodRepository.findAllByOrderByActiveDescNameAsc());
        return "foods";
    }

    @PostMapping("/admin/foods")
    public String add(@RequestParam(defaultValue = "") String name, RedirectAttributes redirect) {
        String cleanName = cleanName(name);
        String errorKey = findFirstError(cleanName, null);
        if (errorKey != null) {
            redirect.addFlashAttribute("foodErrorKey", errorKey);
            return "redirect:/admin/foods";
        }

        Food food = new Food();
        food.setName(cleanName);
        food.setActive(true);
        food.setCreatedAt(Instant.now());
        foodRepository.save(food);
        return "redirect:/admin/foods";
    }

    @PostMapping("/admin/foods/{id}/rename")
    public String rename(@PathVariable Long id, @RequestParam(defaultValue = "") String name,
                         RedirectAttributes redirect) {
        Food food = findFood(id);
        String cleanName = cleanName(name);
        String errorKey = findFirstError(cleanName, food);
        if (errorKey != null) {
            redirect.addFlashAttribute("foodErrorKey", errorKey);
            return "redirect:/admin/foods";
        }
        // The new name shows everywhere, old menus included (it is meant for fixing typos).
        food.setName(cleanName);
        foodRepository.save(food);
        return "redirect:/admin/foods";
    }

    @PostMapping("/admin/foods/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        Food food = findFood(id);
        food.setActive(false);
        foodRepository.save(food);
        return "redirect:/admin/foods";
    }

    @PostMapping("/admin/foods/{id}/activate")
    public String activate(@PathVariable Long id) {
        Food food = findFood(id);
        food.setActive(true);
        foodRepository.save(food);
        return "redirect:/admin/foods";
    }

    // ---------- helpers ----------

    /**
     * Trims, joins repeated spaces, and turns Arabic letters into Persian ones,
     * so "قورمه  سبزي" and "قورمه سبزی" count as the same food.
     */
    static String cleanName(String name) {
        return name.replace('ي', 'ی')   // Arabic yeh -> Persian yeh
                .replace('ك', 'ک')      // Arabic kaf -> Persian kaf
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** Returns the message key of the first problem. "self" is the food being renamed (null when adding). */
    private String findFirstError(String name, Food self) {
        if (name.isEmpty()) {
            return "foods.error.nameRequired";
        }
        if (name.length() > NAME_MAX) {
            return "foods.error.nameTooLong";
        }
        boolean taken = foodRepository.findByNameIgnoreCase(name)
                .filter(other -> self == null || !other.getId().equals(self.getId()))
                .isPresent();
        if (taken) {
            return "foods.error.duplicate";
        }
        return null;
    }

    private Food findFood(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
