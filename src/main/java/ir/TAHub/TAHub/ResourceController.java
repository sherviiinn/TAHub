package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.CourseResource;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.CourseResourceRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.OfferingAccess;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Learning resources of an offering (videos, slides, any https link).
 * Who reads: everyone who is logged in (a cancelled offering only its managers).
 * Who adds, edits, deletes: the professor (and admins) and TAs with "Manage resources", only in an
 * uncancelled offering of the active semester. Deleting only hides.
 * Every semester starts empty. Managers see the last semester's resources of the same course as
 * suggestions, copy the ones they want, or hide the suggestions. Students are told about new resources.
 */
@Controller
public class ResourceController {

    private static final int TITLE_MAX = 150;
    private static final int URL_MAX = 500;
    private static final int DESCRIPTION_MAX = 300;
    private static final int CATEGORY_MAX = 100;

    /** One group of resources on the page. category is null for the resources without one. */
    public record ResourceGroup(String category, List<CourseResource> resources) {
    }

    private final CourseOfferingRepository offeringRepository;
    private final CourseResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final OfferingAccess access;
    private final NotificationService notificationService;

    public ResourceController(CourseOfferingRepository offeringRepository,
                              CourseResourceRepository resourceRepository,
                              UserRepository userRepository,
                              OfferingAccess access,
                              NotificationService notificationService) {
        this.offeringRepository = offeringRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.notificationService = notificationService;
    }

    @GetMapping("/courses/{id}/resources")
    public String list(@PathVariable Long id, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        if (offering.isCancelled() && !access.isManager(current, offering)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        boolean canManage = canManage(current, offering);

        List<CourseResource> resources = resourceRepository.findByOfferingAndDeletedFalseOrderByCreatedAtAscIdAsc(offering);
        model.addAttribute("offering", offering);
        model.addAttribute("groups", group(resources));
        model.addAttribute("isEmpty", resources.isEmpty());
        model.addAttribute("isCurrentSemester", offering.getSemester().isActive());
        model.addAttribute("canManage", canManage);
        if (canManage) {
            model.addAttribute("suggestions", suggestions(offering, resources));
            // Categories already used, so the form can offer them (a datalist).
            model.addAttribute("categories", resources.stream()
                    .map(CourseResource::getCategory).filter(c -> c != null).distinct().toList());
        }
        return "resources";
    }

    @PostMapping("/courses/{id}/resources")
    public String add(@PathVariable Long id,
                      @RequestParam(defaultValue = "") String title,
                      @RequestParam(defaultValue = "") String url,
                      @RequestParam(defaultValue = "") String description,
                      @RequestParam(defaultValue = "") String category,
                      Authentication authentication,
                      RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        requireManage(current, offering);

        String errorKey = findFirstError(title.trim(), url.trim(), description.trim(), category.trim());
        if (errorKey != null) {
            redirect.addFlashAttribute("resourceErrorKey", errorKey);
            redirect.addFlashAttribute("draftTitle", title);
            redirect.addFlashAttribute("draftUrl", url);
            redirect.addFlashAttribute("draftDescription", description);
            redirect.addFlashAttribute("draftCategory", category);
            return "redirect:/courses/" + id + "/resources";
        }

        CourseResource resource = new CourseResource();
        resource.setOffering(offering);
        fill(resource, title, url, description, category);
        resource.setCreatedAt(Instant.now());
        resourceRepository.save(resource);

        notificationService.notifyNewResources(offering);
        return "redirect:/courses/" + id + "/resources";
    }

    @GetMapping("/resources/{resourceId}/edit")
    public String editForm(@PathVariable Long resourceId, Model model, Authentication authentication) {
        CourseResource resource = findManageable(resourceId, currentUser(authentication));
        // After a validation error the form shows what was typed, not the saved text.
        model.addAttribute("title", draftOr(model, "draftTitle", resource.getTitle()));
        model.addAttribute("url", draftOr(model, "draftUrl", resource.getUrl()));
        model.addAttribute("description", draftOr(model, "draftDescription", resource.getDescription()));
        model.addAttribute("category", draftOr(model, "draftCategory", resource.getCategory()));
        model.addAttribute("resource", resource);
        model.addAttribute("offering", resource.getOffering());
        return "resource-edit";
    }

    @PostMapping("/resources/{resourceId}/edit")
    public String edit(@PathVariable Long resourceId,
                       @RequestParam(defaultValue = "") String title,
                       @RequestParam(defaultValue = "") String url,
                       @RequestParam(defaultValue = "") String description,
                       @RequestParam(defaultValue = "") String category,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        CourseResource resource = findManageable(resourceId, currentUser(authentication));
        String errorKey = findFirstError(title.trim(), url.trim(), description.trim(), category.trim());
        if (errorKey != null) {
            redirect.addFlashAttribute("resourceErrorKey", errorKey);
            redirect.addFlashAttribute("draftTitle", title);
            redirect.addFlashAttribute("draftUrl", url);
            redirect.addFlashAttribute("draftDescription", description);
            redirect.addFlashAttribute("draftCategory", category);
            return "redirect:/resources/" + resourceId + "/edit";
        }
        fill(resource, title, url, description, category);
        resourceRepository.save(resource);
        return "redirect:/courses/" + resource.getOffering().getId() + "/resources";
    }

    @PostMapping("/resources/{resourceId}/delete")
    public String delete(@PathVariable Long resourceId, Authentication authentication) {
        CourseResource resource = findManageable(resourceId, currentUser(authentication));
        resource.setDeleted(true);
        resourceRepository.save(resource);
        return "redirect:/courses/" + resource.getOffering().getId() + "/resources";
    }

    /** Copies the chosen suggestions (resources of the last semester) into this offering. */
    @PostMapping("/courses/{id}/resources/copy")
    public String copy(@PathVariable Long id,
                       @RequestParam(name = "resourceId", required = false) List<Long> chosen,
                       Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        requireManage(current, offering);

        if (chosen != null && !chosen.isEmpty()) {
            // Only real suggestions can be copied (never trust ids from a form).
            List<CourseResource> existing = resourceRepository.findByOfferingAndDeletedFalseOrderByCreatedAtAscIdAsc(offering);
            int copied = 0;
            for (CourseResource old : suggestions(offering, existing)) {
                if (chosen.contains(old.getId())) {
                    CourseResource copy = new CourseResource();
                    copy.setOffering(offering);
                    copy.setTitle(old.getTitle());
                    copy.setUrl(old.getUrl());
                    copy.setDescription(old.getDescription());
                    copy.setCategory(old.getCategory());
                    copy.setCreatedAt(Instant.now());
                    resourceRepository.save(copy);
                    copied++;
                }
            }
            if (copied > 0) {
                notificationService.notifyNewResources(offering);
            }
        }
        return "redirect:/courses/" + id + "/resources";
    }

    /** "Hide suggestions": nothing more from the last semester is offered for this offering. */
    @PostMapping("/courses/{id}/resources/hide-suggestions")
    public String hideSuggestions(@PathVariable Long id, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        requireManage(current, offering);
        offering.setResourceSuggestionsHidden(true);
        offeringRepository.save(offering);
        return "redirect:/courses/" + id + "/resources";
    }

    // ---------- helpers ----------

    /**
     * The resources of the LAST earlier semester in which this course was offered (all its groups),
     * without links this offering already has and without repeats. Empty when hidden.
     * Only the last semester counts, so a resource nobody copied this semester is not offered again later.
     */
    private List<CourseResource> suggestions(CourseOffering offering, List<CourseResource> existing) {
        if (Boolean.TRUE.equals(offering.getResourceSuggestionsHidden())) {
            return List.of();
        }
        Long semesterId = offering.getSemester().getId();
        List<CourseOffering> earlier = offeringRepository.findByCourseOrderBySemesterIdDesc(offering.getCourse()).stream()
                .filter(o -> o.getSemester().getId() < semesterId && !o.isCancelled())
                .toList();
        if (earlier.isEmpty()) {
            return List.of();
        }
        Long lastSemesterId = earlier.get(0).getSemester().getId();
        List<CourseOffering> lastSemester = earlier.stream()
                .filter(o -> o.getSemester().getId().equals(lastSemesterId))
                .toList();

        Set<String> seen = new HashSet<>();
        existing.forEach(r -> seen.add(r.getUrl()));
        List<CourseResource> result = new ArrayList<>();
        for (CourseResource old : resourceRepository.findByOfferingInAndDeletedFalseOrderByCreatedAtAscIdAsc(lastSemester)) {
            if (seen.add(old.getUrl())) {
                result.add(old);
            }
        }
        return result;
    }

    /** Resources without a category first (as one group), then each category in the order it first appears. */
    private List<ResourceGroup> group(List<CourseResource> resources) {
        List<CourseResource> plain = new ArrayList<>();
        Map<String, List<CourseResource>> byCategory = new LinkedHashMap<>();
        for (CourseResource resource : resources) {
            if (resource.getCategory() == null) {
                plain.add(resource);
            } else {
                byCategory.computeIfAbsent(resource.getCategory(), c -> new ArrayList<>()).add(resource);
            }
        }
        List<ResourceGroup> groups = new ArrayList<>();
        if (!plain.isEmpty()) {
            groups.add(new ResourceGroup(null, plain));
        }
        byCategory.forEach((category, list) -> groups.add(new ResourceGroup(category, list)));
        return groups;
    }

    private void fill(CourseResource resource, String title, String url, String description, String category) {
        resource.setTitle(title.trim());
        resource.setUrl(url.trim());
        resource.setDescription(description.trim().isEmpty() ? null : description.trim());
        resource.setCategory(category.trim().isEmpty() ? null : category.trim().replaceAll("\\s+", " "));
    }

    /** Returns the message key of the first problem, in the order of the form fields. */
    private String findFirstError(String title, String url, String description, String category) {
        if (title.isEmpty()) {
            return "resources.error.titleRequired";
        }
        if (title.length() > TITLE_MAX) {
            return "resources.error.titleTooLong";
        }
        if (url.isEmpty()) {
            return "resources.error.urlRequired";
        }
        if (url.length() > URL_MAX) {
            return "resources.error.urlTooLong";
        }
        if (!isHttpsLink(url)) {
            return "resources.error.urlNotHttps";
        }
        if (description.length() > DESCRIPTION_MAX) {
            return "resources.error.descriptionTooLong";
        }
        if (category.length() > CATEGORY_MAX) {
            return "resources.error.categoryTooLong";
        }
        return null;
    }

    /** Only https links with a real host (never "javascript:" or other tricks). */
    private boolean isHttpsLink(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !url.contains(" ");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Object draftOr(Model model, String draftName, String saved) {
        return model.containsAttribute(draftName) ? model.getAttribute(draftName) : saved;
    }

    /** The professor and TAs with "Manage resources", in an uncancelled offering of the active semester. */
    private boolean canManage(User current, CourseOffering offering) {
        return access.isOpen(offering) && access.can(current, offering, TaPermission.MANAGE_RESOURCES);
    }

    private void requireManage(User current, CourseOffering offering) {
        if (!canManage(current, offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private CourseResource findManageable(Long resourceId, User current) {
        CourseResource resource = resourceRepository.findById(resourceId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireManage(current, resource.getOffering());
        return resource;
    }

    private CourseOffering findOffering(Long id) {
        return offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
