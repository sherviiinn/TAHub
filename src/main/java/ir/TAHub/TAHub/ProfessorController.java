package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.ProfessorProfile;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.ProfessorProfileRepository;
import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The professors section: a list of professors, and for each professor their office information and
 * the courses they teach this semester with their TAs. Every logged-in user can see it.
 * The office information (all of it optional) is edited by the professor themselves or by an admin.
 */
@Controller
public class ProfessorController {

    /** One course of a professor this semester, with its TAs. */
    public record CourseRow(CourseOffering offering, List<TaAssignment> teachingAssistants) {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Tehran"));
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEGRAM = Pattern.compile("^[A-Za-z0-9_]{5,32}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\-() ]{3,30}$");

    // The fields of the form, in order, with their longest allowed length.
    private static final Map<String, Integer> MAX = new LinkedHashMap<>();
    static {
        MAX.put("building", 100);
        MAX.put("room", 50);
        MAX.put("availability", 300);
        MAX.put("email", 150);
        MAX.put("phone", 30);
        MAX.put("telegram", 40);
        MAX.put("website", 300);
        MAX.put("note", 300);
    }

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final TaAssignmentRepository taRepository;
    private final ProfessorProfileRepository profileRepository;
    private final SemesterRepository semesterRepository;

    public ProfessorController(UserRepository userRepository,
                               CourseOfferingRepository offeringRepository,
                               TaAssignmentRepository taRepository,
                               ProfessorProfileRepository profileRepository,
                               SemesterRepository semesterRepository) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.taRepository = taRepository;
        this.profileRepository = profileRepository;
        this.semesterRepository = semesterRepository;
    }

    @GetMapping("/professors")
    public String list(Model model) {
        model.addAttribute("professors", userRepository.findByRoleOrderByFullNameAsc(Role.PROFESSOR));
        return "professors";
    }

    @GetMapping("/professors/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        User professor = findProfessor(id);

        // Active semester only, and cancelled offerings are hidden, like in the course list.
        List<CourseRow> courses = offeringRepository.findByProfessorOrderBySemesterIdDescCourseCodeAsc(professor)
                .stream()
                .filter(o -> o.getSemester().isActive() && !o.isCancelled())
                .map(o -> new CourseRow(o, taRepository.findByOfferingAndActiveTrueOrderByUserFullNameAsc(o)))
                .toList();

        ProfessorProfile profile = profileRepository.findByProfessor(professor)
                .filter(p -> !p.isEmpty())
                .orElse(null);
        Semester active = semesterRepository.findByActiveTrue().orElse(null);

        model.addAttribute("professor", professor);
        model.addAttribute("courses", courses);
        model.addAttribute("profile", profile);
        if (profile != null && profile.getUpdatedAt() != null) {
            model.addAttribute("profileUpdated", DAY.format(profile.getUpdatedAt()));
            // Saved in an earlier semester: the times may have changed since.
            model.addAttribute("profileOld", active != null && (profile.getUpdatedSemester() == null
                    || !profile.getUpdatedSemester().getId().equals(active.getId())));
        }
        model.addAttribute("canEditProfile", canEdit(currentUser(authentication), professor));
        return "professor-detail";
    }

    @GetMapping("/professors/{id}/office")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        User professor = findProfessor(id);
        requireEdit(currentUser(authentication), professor);
        ProfessorProfile profile = profileRepository.findByProfessor(professor).orElseGet(ProfessorProfile::new);

        // After a validation error the form shows what was typed (flash attributes), otherwise the saved values.
        if (!model.containsAttribute("building")) {
            model.addAttribute("building", profile.getBuilding());
            model.addAttribute("room", profile.getRoom());
            model.addAttribute("availability", profile.getAvailability());
            model.addAttribute("email", profile.getEmail());
            model.addAttribute("phone", profile.getPhone());
            model.addAttribute("telegram", profile.getTelegram());
            model.addAttribute("website", profile.getWebsite());
            model.addAttribute("note", profile.getNote());
        }
        model.addAttribute("professor", professor);
        return "professor-office-edit";
    }

    @PostMapping("/professors/{id}/office")
    public String save(@PathVariable Long id,
                       @RequestParam Map<String, String> params,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        User professor = findProfessor(id);
        requireEdit(currentUser(authentication), professor);

        Map<String, String> values = new LinkedHashMap<>();
        for (String field : MAX.keySet()) {
            values.put(field, params.getOrDefault(field, "").trim());
        }
        // Persian digits in the phone number are fine; "@name" or a t.me link for Telegram too.
        values.put("phone", Digits.toAscii(values.get("phone")));
        String telegram = values.get("telegram");
        telegram = telegram.replaceFirst("^https://(t\\.me|telegram\\.me)/", "").replaceFirst("^@", "");
        values.put("telegram", telegram);

        String errorKey = findFirstError(values);
        if (errorKey != null) {
            redirect.addFlashAttribute("officeErrorKey", errorKey);
            values.forEach(redirect::addFlashAttribute);
            return "redirect:/professors/" + id + "/office";
        }

        ProfessorProfile profile = profileRepository.findByProfessor(professor).orElseGet(() -> {
            ProfessorProfile created = new ProfessorProfile();
            created.setProfessor(professor);
            return created;
        });
        profile.setBuilding(emptyToNull(values.get("building")));
        profile.setRoom(emptyToNull(values.get("room")));
        profile.setAvailability(emptyToNull(values.get("availability")));
        profile.setEmail(emptyToNull(values.get("email")));
        profile.setPhone(emptyToNull(values.get("phone")));
        profile.setTelegram(emptyToNull(values.get("telegram")));
        profile.setWebsite(emptyToNull(values.get("website")));
        profile.setNote(emptyToNull(values.get("note")));
        profile.setUpdatedAt(Instant.now());
        profile.setUpdatedSemester(semesterRepository.findByActiveTrue().orElse(null));
        profileRepository.save(profile);

        redirect.addFlashAttribute("officeSaved", true);
        return "redirect:/professors/" + id;
    }

    // ---------- helpers ----------

    /** Every field may be empty. Filled fields must be short enough and look right. */
    private String findFirstError(Map<String, String> values) {
        for (Map.Entry<String, Integer> field : MAX.entrySet()) {
            if (values.get(field.getKey()).length() > field.getValue()) {
                return "office.error.tooLong." + field.getKey();
            }
            String value = values.get(field.getKey());
            if (value.isEmpty()) {
                continue;
            }
            if (field.getKey().equals("email") && !EMAIL.matcher(value).matches()) {
                return "office.error.email";
            }
            if (field.getKey().equals("phone") && !PHONE.matcher(value).matches()) {
                return "office.error.phone";
            }
            if (field.getKey().equals("telegram") && !TELEGRAM.matcher(value).matches()) {
                return "office.error.telegram";
            }
            if (field.getKey().equals("website") && !isHttpsLink(value)) {
                return "office.error.website";
            }
        }
        return null;
    }

    private boolean isHttpsLink(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !url.contains(" ");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    /** The professor themselves, or an admin. */
    private boolean canEdit(User current, User professor) {
        return current.getRole() == Role.ADMIN || current.getId().equals(professor.getId());
    }

    private void requireEdit(User current, User professor) {
        if (!canEdit(current, professor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /** Only users who really are professors have a page (other ids give "not found"). */
    private User findProfessor(Long id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() == Role.PROFESSOR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
