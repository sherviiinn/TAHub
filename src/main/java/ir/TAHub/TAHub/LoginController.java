package ir.TAHub.TAHub;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // Only shows the page. Spring Security itself handles the form submission.
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}