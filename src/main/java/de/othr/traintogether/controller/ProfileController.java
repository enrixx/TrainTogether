package de.othr.traintogether.controller;

import de.othr.traintogether.dto.UpdateProfileDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.service.GymOwnerRequestService;
import de.othr.traintogether.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserService userService;
    private final GymOwnerRequestService gymOwnerRequestService;

    public ProfileController(UserService userService, GymOwnerRequestService gymOwnerRequestService) {
        this.userService = userService;
        this.gymOwnerRequestService = gymOwnerRequestService;
    }

    @GetMapping
    public String profile(Model model, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Profile");
        model.addAttribute("user", user);
        model.addAttribute("updateProfileDto", new UpdateProfileDto(user.getEmail(), user.getUsername(), user.getFirstName(), user.getLastName()));

        // Check if user has rejected gym owner request (for reapplication button)
        // Only show if user has NO pending request
        boolean hasPendingRequest = gymOwnerRequestService.hasPendingRequest(email);
        boolean hasRejectedRequest = !hasPendingRequest && gymOwnerRequestService.hasRejectedRequest(email);
        model.addAttribute("hasRejectedGymOwnerRequest", hasRejectedRequest);

        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("updateProfileDto") UpdateProfileDto updateDto,
                               BindingResult bindingResult,
                               Authentication authentication,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (bindingResult.hasErrors()) {
            String email = authentication.getName();
            UserDto user = userService.findUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("title", "Profile");
            return "profile";
        }

        try {
            String currentEmail = authentication.getName();
            boolean emailChanged = !currentEmail.equals(updateDto.getEmail());

            userService.updateProfile(currentEmail, updateDto);

            // If email was changed, logout user so they can login with new email
            if (emailChanged) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated! Please login with your new email.");
                return "redirect:/login";
            }

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            String email = authentication.getName();
            UserDto user = userService.findUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("title", "Profile");
            model.addAttribute("errorMessage", e.getMessage());
            return "profile";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(@ModelAttribute("updateProfileDto") UpdateProfileDto updateDto,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        String email = authentication.getName();

        // Validate password fields
        if (updateDto.getCurrentPassword() == null || updateDto.getCurrentPassword().isEmpty()) {
            UserDto user = userService.findUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("title", "Profile");
            model.addAttribute("passwordError", "Current password is required");
            return "profile";
        }

        if (updateDto.getNewPassword() == null || updateDto.getNewPassword().isEmpty()) {
            UserDto user = userService.findUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("title", "Profile");
            model.addAttribute("passwordError", "New password is required");
            return "profile";
        }

        if (!updateDto.getNewPassword().equals(updateDto.getConfirmPassword())) {
            UserDto user = userService.findUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("title", "Profile");
            model.addAttribute("passwordError", "Passwords do not match");
            return "profile";
        }

        boolean success = userService.updatePassword(email, updateDto.getCurrentPassword(), updateDto.getNewPassword());

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
            return "redirect:/profile";
        } else {
            UserDto user = userService.findUserByEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("title", "Profile");
            model.addAttribute("passwordError", "Current password is incorrect");
            return "profile";
        }
    }

    @PostMapping("/upload-picture")
    public String uploadProfilePicture(@RequestParam("profilePicture") MultipartFile file,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            userService.uploadProfilePicture(email, file);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture uploaded successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload profile picture: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/delete-picture")
    public String deleteProfilePicture(Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            userService.deleteProfilePicture(email);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete profile picture: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}

