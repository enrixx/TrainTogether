package de.othr.traintogether.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class MainLayoutController {

    private void addNavigation(Model model) {
        Map<String, String> nav = new LinkedHashMap<>();
        nav.put("Home", "/");
        nav.put("Profile", "/profile");
        nav.put("Workouts", "/workouts");
        model.addAttribute("navLinks", nav);
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        addNavigation(model);
        model.addAttribute("title", "Home");
        return "home";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        addNavigation(model);
        model.addAttribute("title", "Profile");
        return "profile";
    }

    @GetMapping("/workouts")
    public String workouts(Model model) {
        addNavigation(model);
        model.addAttribute("title", "Workouts");
        return "workouts";
    }
}

