package de.othr.traintogether.controller;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainLayoutController {

    private final UserService userService;

    public MainLayoutController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication authentication) {
        model.addAttribute("title", "Home");

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            UserDto user = userService.findUserByEmail(email);
            if (user != null) {
                model.addAttribute("currentUser", user);
            }
        }

        return "home";
    }


    //TODO: Move To separate Controller
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GYM_OWNER', 'GYM_WORKER', 'PENDING_GYM_WORKER', 'PENDING_GYM_WORKER')")
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        return "dashboard";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/map")
    public String map(Model model) {
        model.addAttribute("title", "Map");
        return "map";
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    @GetMapping("/workouts")
    public String workouts(Model model) {
        model.addAttribute("title", "Workouts");
        return "workouts";
    }

}

