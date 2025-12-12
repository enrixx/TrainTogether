package de.othr.traintogether.controller;

import de.othr.traintogether.model.GymOwnerRequest;
import de.othr.traintogether.service.GymOwnerRequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final GymOwnerRequestService gymOwnerRequestService;

    public AdminController(GymOwnerRequestService gymOwnerRequestService) {
        this.gymOwnerRequestService = gymOwnerRequestService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<GymOwnerRequest> allRequests = gymOwnerRequestService.getAllRequests();
        List<GymOwnerRequest> pendingRequests = gymOwnerRequestService.getPendingRequests();

        model.addAttribute("title", "Admin Dashboard");
        model.addAttribute("allRequests", allRequests);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("pendingCount", pendingRequests.size());

        return "admin/dashboard";
    }

    @GetMapping("/gym-owner-requests")
    public String viewGymOwnerRequests(@RequestParam(required = false) String filter, Model model) {
        List<GymOwnerRequest> requests;

        if ("pending".equals(filter)) {
            requests = gymOwnerRequestService.getPendingRequests();
            model.addAttribute("filterTitle", "Pending Requests");
        } else {
            requests = gymOwnerRequestService.getAllRequests();
            model.addAttribute("filterTitle", "All Requests");
        }

        model.addAttribute("title", "Gym Owner Requests");
        model.addAttribute("requests", requests);
        model.addAttribute("currentFilter", filter);

        return "admin/gym-owner-requests";
    }

    @PostMapping("/gym-owner-requests/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = authentication.getName();
            boolean emailSent = gymOwnerRequestService.approveRequest(id, adminEmail);

            if (emailSent) {
                redirectAttributes.addFlashAttribute("successMessage", "Request approved successfully! Notification email sent.");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "Request approved successfully, but notification email could not be sent. Please check your email configuration.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve request: " + e.getMessage());
        }

        return "redirect:/admin/gym-owner-requests?filter=pending";
    }

    @PostMapping("/gym-owner-requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = authentication.getName();
            boolean emailSent = gymOwnerRequestService.rejectRequest(id, adminEmail);

            if (emailSent) {
                redirectAttributes.addFlashAttribute("successMessage", "Request rejected successfully! Notification email sent.");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "Request rejected successfully, but notification email could not be sent. Please check your email configuration.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject request: " + e.getMessage());
        }

        return "redirect:/admin/gym-owner-requests?filter=pending";
    }
}

