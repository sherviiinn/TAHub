package ir.TAHub.TAHub;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        model.addAttribute("appName", "TA Hub");
        model.addAttribute("username", principal.getName());
        return "home";
    }
}