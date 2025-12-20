package de.othr.traintogether.controller;

import de.othr.traintogether.model.GymOwnerRequest;
import de.othr.traintogether.service.GymOwnerRequestService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messageSource;

    public AdminController(GymOwnerRequestService gymOwnerRequestService, MessageSource messageSource) {
        this.gymOwnerRequestService = gymOwnerRequestService;
        this.messageSource = messageSource;
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
            model.addAttribute("filterTitle", messageSource.getMessage("admin.requests.filter.title.pending", null, LocaleContextHolder.getLocale()));
        } else {
            requests = gymOwnerRequestService.getAllRequests();
            model.addAttribute("filterTitle", messageSource.getMessage("admin.requests.filter.title.all", null, LocaleContextHolder.getLocale()));
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
                redirectAttributes.addFlashAttribute("success",
                    messageSource.getMessage("admin.requests.approve.success", null, LocaleContextHolder.getLocale()));
            } else {
                redirectAttributes.addFlashAttribute("warning",
                    messageSource.getMessage("admin.requests.approve.warning", null, LocaleContextHolder.getLocale()));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                messageSource.getMessage("admin.requests.approve.error", new Object[]{e.getMessage()}, LocaleContextHolder.getLocale()));
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
                redirectAttributes.addFlashAttribute("success",
                    messageSource.getMessage("admin.requests.reject.success", null, LocaleContextHolder.getLocale()));
            } else {
                redirectAttributes.addFlashAttribute("warning",
                    messageSource.getMessage("admin.requests.reject.warning", null, LocaleContextHolder.getLocale()));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                messageSource.getMessage("admin.requests.reject.error", new Object[]{e.getMessage()}, LocaleContextHolder.getLocale()));
        }

        return "redirect:/admin/gym-owner-requests?filter=pending";
    }
}

