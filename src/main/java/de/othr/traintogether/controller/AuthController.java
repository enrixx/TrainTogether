package de.othr.traintogether.controller;

import de.othr.traintogether.dto.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.security.JwtUtil;
import de.othr.traintogether.service.EmailService;
import de.othr.traintogether.service.GymOwnerRequestService;
import de.othr.traintogether.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final GymOwnerRequestService gymOwnerRequestService;
    private final EmailService emailService;
    private final MessageSource messageSource;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public AuthController(UserService userService, GymOwnerRequestService gymOwnerRequestService, EmailService emailService, MessageSource messageSource, JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.gymOwnerRequestService = gymOwnerRequestService;
        this.emailService = emailService;
        this.messageSource = messageSource;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PreAuthorize("isAnonymous()")
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/token")
    @ResponseBody
    public AuthResponse tokenForAuthenticatedUser(Authentication authentication) {
        // This endpoint is protected by the MVC security chain (session-based).
        String email = authentication.getName();
        UserDetails ud = userDetailsService.loadUserByUsername(email);
        String token = jwtUtil.generateToken(ud);
        return new AuthResponse(token, jwtUtil.getJwtExpirationMs());
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
            userService.registerGymOwner(registerDto);
            return "redirect:/login?registered=gym-owner";
        } catch (Exception e) {
            model.addAttribute("gymOwnerRegisterDto", registerDto);
            model.addAttribute("errorMessage", "An error occurred during registration. Please try again.");
            return "register-gym-owner";
        }
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('PENDING_GYM_OWNER')")
    @GetMapping("/gym-owner-request")
    public String gymOwnerRequestForm(Model model, Authentication authentication) {
        String email = authentication.getName();

        // Check if user already has pending request
        if (gymOwnerRequestService.hasPendingRequest(email)) {
            return "redirect:/home?error=pending-request";
        }

        // Check if user is already a gym owner
        if (gymOwnerRequestService.isGymOwner(email)) {
            return "redirect:/home?error=already-owner";
        }

        // Check if user has rejected request (allow resubmission)
        boolean hasRejectedRequest = gymOwnerRequestService.hasRejectedRequest(email);
        model.addAttribute("hasRejectedRequest", hasRejectedRequest);
        model.addAttribute("gymOwnerRequestDto", new GymOwnerRequestDto());
        return "gym-owner-request-form";
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('PENDING_GYM_OWNER')")
    @PostMapping("/gym-owner-request")
    public String submitGymOwnerRequest(@Valid @ModelAttribute("gymOwnerRequestDto") GymOwnerRequestDto requestDto,
                                        BindingResult bindingResult,
                                        Authentication authentication,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("gymOwnerRequestDto", requestDto);
            return "gym-owner-request-form";
        }

        try {
            String email = authentication.getName();
            gymOwnerRequestService.submitGymOwnerRequest(
                    email,
                    requestDto.getGymName(),
                    requestDto.getGymAddress(),
                    requestDto.getCity(),
                    requestDto.getPostalCode(),
                    requestDto.getPhoneNumber(),
                    requestDto.getGymDescription()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("gym.owner.request.success", null, LocaleContextHolder.getLocale()));
            return "redirect:/home";
        } catch (RuntimeException e) {
            model.addAttribute("gymOwnerRequestDto", requestDto);
            String errorKey = null;
            if (e.getMessage().contains("already have a pending")) {
                errorKey = "gym.owner.request.error.pending";
            } else if (e.getMessage().contains("already a gym owner")) {
                errorKey = "gym.owner.request.error.already_owner";
            }

            String errorMessage = errorKey != null
                    ? messageSource.getMessage(errorKey, null, LocaleContextHolder.getLocale())
                    : e.getMessage();
            model.addAttribute("errorMessage", errorMessage);
            return "gym-owner-request-form";
        }
    }

    @PreAuthorize("isAnonymous()")
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordDto", new ForgotPasswordDto());
        return "forgot-password";
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("forgotPasswordDto") ForgotPasswordDto forgotPasswordDto,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("forgotPasswordDto", forgotPasswordDto);
            return "forgot-password";
        }

        String email = forgotPasswordDto.getEmail();
        String token = userService.createPasswordResetToken(email);

        // Always show success message (don't reveal if email exists)
        if (token != null) {
            var userDto = userService.findUserDTOByEmail(email);
            if (userDto != null) {
                Locale currentLocale = LocaleContextHolder.getLocale();
                String language = currentLocale.getLanguage();
                String firstName = userDto.getFirstName() != null ? userDto.getFirstName() : "User";

                emailService.sendPasswordResetEmail(email, firstName, token, language);
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "If an account with that email exists, a password reset link has been sent.");
        return "redirect:/forgot-password";
    }

    @PreAuthorize("isAnonymous()")
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.validatePasswordResetToken(token);

            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired password reset token.");
                return "redirect:/login";
            }

            model.addAttribute("resetPasswordDto", new ResetPasswordDto());
            model.addAttribute("token", token);
            return "reset-password";
        } catch (Exception e) {
            logger.error("Error validating password reset token: {}", token, e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while validating the reset token. Please try again.");
            return "redirect:/login";
        }
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @Valid @ModelAttribute("resetPasswordDto") ResetPasswordDto resetPasswordDto,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resetPasswordDto", resetPasswordDto);
            model.addAttribute("token", token);
            return "reset-password";
        }

        if (!resetPasswordDto.getPassword().equals(resetPasswordDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch");
            model.addAttribute("resetPasswordDto", resetPasswordDto);
            model.addAttribute("token", token);
            return "reset-password";
        }

        boolean success = userService.resetPassword(token, resetPasswordDto.getPassword());

        if (!success) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired password reset token.");
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Your password has been reset successfully. You can now log in.");
        return "redirect:/login";
    }
}
