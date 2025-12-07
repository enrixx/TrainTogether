package de.othr.traintogether.controller;

import de.othr.traintogether.dto.GymOwnerRegisterDto;
import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Locale;

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
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
                                 BindingResult bindingResult,
                                 Model model) {


        if (bindingResult.hasErrors()) {
            model.addAttribute("registerDto", registerDto);
            return "register";
        }
        var email = registerDto.getEmail();
        if (userService.emailExists(email)) {
            bindingResult.rejectValue("email", "error.email.exists");
            model.addAttribute("registerDto", registerDto);
            return "register";
        }

        userService.registerUser(registerDto);
        return "redirect:/";
    }

    @PreAuthorize("isAnonymous()")
    @GetMapping("/register/gym-owner")
    public String registerGymOwnerPage(Model model) {
        model.addAttribute("gymOwnerRegisterDto", new GymOwnerRegisterDto());
        return "register-gym-owner";
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/register/gym-owner")
    public String registerGymOwnerSubmit(@Valid @ModelAttribute("gymOwnerRegisterDto") GymOwnerRegisterDto registerDto,
                                        BindingResult bindingResult,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("gymOwnerRegisterDto", registerDto);
            return "register-gym-owner";
        }

        var email = registerDto.getEmail();
        if (userService.emailExists(email)) {
            bindingResult.rejectValue("email", "error.email.exists");
            model.addAttribute("gymOwnerRegisterDto", registerDto);
            return "register-gym-owner";
        }

        try {
            // Get language from current session locale (set by language selector in UI)
            Locale currentLocale = LocaleContextHolder.getLocale();
            String language = currentLocale.getLanguage();
            userService.registerGymOwner(registerDto, language);
            return "redirect:/login?registered=gym-owner";
        } catch (Exception e) {
            model.addAttribute("gymOwnerRegisterDto", registerDto);
            model.addAttribute("errorMessage", "An error occurred during registration. Please try again.");
            return "register-gym-owner";
        }
    }
}
