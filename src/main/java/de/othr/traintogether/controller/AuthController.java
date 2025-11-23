package de.othr.traintogether.controller;

import de.othr.traintogether.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAnonymous()")
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PreAuthorize("isAnonymous()")
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/register")
    public String registerSubmit(@RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String username,
                                 @RequestParam String role) {

        userService.registerUser(email, password, role, username);
        return "redirect:/";
    }
}

