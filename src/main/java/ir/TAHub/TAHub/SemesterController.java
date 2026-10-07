package ir.TAHub.TAHub;

import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.service.SemesterService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Admin-only pages for managing semesters (see SecurityConfig). */
@Controller
public class SemesterController {

    private final SemesterService semesterService;
    private final SemesterRepository semesterRepository;

    public SemesterController(SemesterService semesterService, SemesterRepository semesterRepository) {
        this.semesterService = semesterService;
        this.semesterRepository = semesterRepository;
    }

    @GetMapping("/admin/semesters")
    public String list(Model model) {
        model.addAttribute("semesters", semesterRepository.findAllByOrderByIdDesc());
        return "semesters";
    }

    @PostMapping("/admin/semesters")
    public String startNew(@RequestParam String code, Model model) {
        String trimmed = code == null ? "" : code.trim();

        String errorKey = null;
        if (trimmed.isEmpty()) {
            errorKey = "semester.error.required";
        } else if (!trimmed.matches("\\d{4,5}")) {
            errorKey = "semester.error.format";
        } else if (semesterRepository.existsByCode(trimmed)) {
            errorKey = "semester.error.exists";
        }

        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            model.addAttribute("semesters", semesterRepository.findAllByOrderByIdDesc());
            return "semesters";
        }

        semesterService.startNewSemester(trimmed);
        return "redirect:/admin/semesters";
    }
}