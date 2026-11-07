package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.ProfessorInvite;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.ProfessorInviteRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.util.JoinCodes;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin page for one-time professor invite codes (/admin/** is admin-only). */
@Controller
public class AdminProfessorInviteController {

    public static final int CODE_LENGTH = 10;
    private static final Duration VALID_FOR = Duration.ofDays(14);
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Tehran"));

    private final ProfessorInviteRepository inviteRepository;
    private final UserRepository userRepository;

    public AdminProfessorInviteController(ProfessorInviteRepository inviteRepository, UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/professor-invites")
    public String list(Model model) {
        List<ProfessorInvite> invites = inviteRepository.findTop100ByOrderByCreatedAtDesc();
        Map<Long, String> created = new HashMap<>();
        Map<Long, String> expires = new HashMap<>();
        Map<Long, String> used = new HashMap<>();
        for (ProfessorInvite invite : invites) {
            created.put(invite.getId(), WHEN.format(invite.getCreatedAt()));
            expires.put(invite.getId(), WHEN.format(invite.getExpiresAt()));
            if (invite.getUsedAt() != null) {
                used.put(invite.getId(), WHEN.format(invite.getUsedAt()));
            }
        }
        model.addAttribute("invites", invites);
        model.addAttribute("created", created);
        model.addAttribute("expires", expires);
        model.addAttribute("used", used);
        return "professor-invites";
    }

    @PostMapping("/admin/professor-invites")
    public String create(@RequestParam(defaultValue = "") String forName,
                         Authentication authentication,
                         RedirectAttributes flash) {
        String name = forName.trim();
        if (name.length() > 100) {
            flash.addFlashAttribute("inviteErrorKey", "profInvite.error.nameTooLong");
            return "redirect:/admin/professor-invites";
        }
        User admin = userRepository.findByStudentNumber(authentication.getName()).orElseThrow();

        String code;
        do {
            code = JoinCodes.generate(CODE_LENGTH);
        } while (inviteRepository.existsByCode(code));

        ProfessorInvite invite = new ProfessorInvite();
        invite.setCode(code);
        invite.setForName(name.isEmpty() ? null : name);
        invite.setCreatedBy(admin);
        invite.setCreatedAt(Instant.now());
        invite.setExpiresAt(invite.getCreatedAt().plus(VALID_FOR));
        inviteRepository.save(invite);

        flash.addFlashAttribute("newCode", code);
        return "redirect:/admin/professor-invites";
    }

    @PostMapping("/admin/professor-invites/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        inviteRepository.findById(id).ifPresent(invite -> {
            if (invite.getUsedAt() == null && !invite.isCancelled()) {
                invite.setCancelled(true);
                inviteRepository.save(invite);
            }
        });
        return "redirect:/admin/professor-invites";
    }
}
