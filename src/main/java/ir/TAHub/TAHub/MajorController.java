package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Major;
import ir.TAHub.TAHub.repository.MajorRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin-only pages for managing fields of study (see SecurityConfig: /admin/** is for admins). */
@Controller
public class MajorController {

    private static final int MAX_LENGTH = 100;

    private final MajorRepository majorRepository;
    private final UserRepository userRepository;

    public MajorController(MajorRepository majorRepository, UserRepository userRepository) {
        this.majorRepository = majorRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/majors")
    public String list(Model model) {
        fillModel(model);
        return "majors";
    }

    @PostMapping("/admin/majors")
    public String add(@RequestParam(defaultValue = "") String name, Model model) {
        String trimmed = name.trim();

        String errorKey = null;
        if (trimmed.isEmpty()) {
            errorKey = "major.error.required";
        } else if (trimmed.length() > MAX_LENGTH) {
            errorKey = "major.error.tooLong";
        } else if (majorRepository.existsByNameIgnoreCase(trimmed)) {
            errorKey = "major.error.exists";
        }

        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            fillModel(model);
            return "majors";
        }

        Major major = new Major();
        major.setName(trimmed);
        majorRepository.save(major);
        return "redirect:/admin/majors";
    }

    @PostMapping("/admin/majors/{id}/delete")
    public String delete(@PathVariable Long id) {
        majorRepository.findById(id).ifPresent(major -> {
            // A major that students use is kept, so no student is left pointing at nothing.
            if (userRepository.countByMajor(major) == 0) {
                majorRepository.delete(major);
            }
        });
        return "redirect:/admin/majors";
    }

    private void fillModel(Model model) {
        List<Major> majors = majorRepository.findAllByOrderByNameAsc();
        Map<Long, Long> usage = new HashMap<>();
        for (Major major : majors) {
            usage.put(major.getId(), userRepository.countByMajor(major));
        }
        model.addAttribute("majors", majors);
        model.addAttribute("usage", usage);
    }
}