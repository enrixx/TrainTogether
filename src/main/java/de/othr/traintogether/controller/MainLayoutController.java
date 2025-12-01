package de.othr.traintogether.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class MainLayoutController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("title", "Home");
        return "home";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("title", "Profile");
        return "profile";
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

