package ir.TAHub.TAHub;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Small pieces of pages that static/js/live.js reloads without reloading the whole page.
 * The data (navBannerNotice and the rest) comes from GlobalModelAttributes, like on every page.
 */
@Controller
public class LiveController {

    /** Only the yellow top bar. "back" is the page the user is on, so closing the bar returns there. */
    @GetMapping("/live/banner")
    public String banner(@RequestParam(defaultValue = "/") String back, Model model) {
        boolean inside = back.startsWith("/") && !back.startsWith("//") && !back.contains("\\");
        model.addAttribute("navCurrentPath", inside ? back : "/");
        return "fragments/layout :: banner";
    }
}
