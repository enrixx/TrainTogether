package de.othr.traintogether.controller;

import de.othr.traintogether.dto.CreateGymWorkerDto;
import de.othr.traintogether.dto.GymMapDto;
import de.othr.traintogether.dto.GymPageDto;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.GymWorker;
import de.othr.traintogether.service.GymPageService;
import de.othr.traintogether.service.GymService;
import de.othr.traintogether.service.GymWorkerService;
import de.othr.traintogether.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gym")
public class GymController {

    private final GymService gymService;
    private final GymPageService gymPageService;
    private final UserService userService;
    private final GymWorkerService gymWorkerService;

    public GymController(GymService gymService, GymPageService gymPageService, UserService userService, GymWorkerService gymWorkerService) {
        this.gymService = gymService;
        this.gymPageService = gymPageService;
        this.userService = userService;
        this.gymWorkerService = gymWorkerService;
    }

    // --- API for internal gym data ---
    @GetMapping("/api/internal-gyms")
    @ResponseBody
    public List<GymMapDto> getInternalGyms() {
        return gymService.findAll().stream()
                .map(GymMapDto::new)
                .collect(Collectors.toList());
    }

    // --- Views ---

    @GetMapping("/{id}")
    public String showGymPage(@PathVariable Long id, @RequestParam(required = false) Long highlightCourseId, Model model, Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;
            GymPageDto pageData = gymPageService.getGymPageData(id, email);

            model.addAttribute("highlightCourseId", highlightCourseId);
            model.addAttribute("gym", pageData.getGym());
            model.addAttribute("canEdit", pageData.isCanEdit());
            model.addAttribute("googleReviews", pageData.getGoogleReviews());
            model.addAttribute("courseWeather", pageData.getCourseWeather());
            model.addAttribute("currentUser", pageData.getCurrentUser());

            return "gym";
        } catch (RuntimeException e) {
            return "redirect:/map";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyAuthority('GYM_OWNER', 'GYM_WORKER')")
    public String showEditGymPage(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            String email = authentication.getName();
            GymPageDto pageData = gymPageService.getGymPageData(id, email);
            
            if (!pageData.isCanEdit()) {
                return "redirect:/error?message=AccessDenied";
            }

            model.addAttribute("gym", pageData.getGym());
            
            // Load workers if user is owner
            if (gymService.isOwner(id, pageData.getCurrentUser().getId())) {
                List<GymWorker> workers = gymWorkerService.findByGymId(id);
                model.addAttribute("workers", workers);
            }
            
            return "edit-gym";
        } catch (RuntimeException e) {
            return "redirect:/map";
        }
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyAuthority('GYM_OWNER', 'GYM_WORKER')")
    public String editGym(@PathVariable Long id,
                          @ModelAttribute Gym gym,
                          @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
                          Authentication authentication) {
        try {
            gymPageService.updateGym(id, gym, bannerImage, authentication.getName());
            return "redirect:/gym/" + id;
        } catch (SecurityException e) {
            return "redirect:/error?message=AccessDenied";
        } catch (Exception e) {
            return "redirect:/error?message=GymNotFound";
        }
    }

    // --- Course Management ---

    @PostMapping("/{id}/course/create")
    @PreAuthorize("hasAnyAuthority('GYM_OWNER', 'GYM_WORKER')")
    public String createCourse(@PathVariable Long id,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("dateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
                               @RequestParam("maxParticipants") int maxParticipants,
                               @RequestParam("outdoors") boolean isOutdoors,
                               Authentication authentication) {
        try {
            gymPageService.createCourse(id, name, description, dateTime, maxParticipants, isOutdoors, authentication.getName());
            return "redirect:/gym/edit/" + id + "?success=courseCreated";
        } catch (SecurityException e) {
            return "redirect:/error?message=AccessDenied";
        } catch (Exception e) {
            return "redirect:/gym/edit/" + id + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/{id}/course/{courseId}/join")
    @PreAuthorize("isAuthenticated()")
    public String joinCourse(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        try {
            gymPageService.joinCourse(courseId, authentication.getName());
            return "redirect:/gym/" + id + "?success=joined";
        } catch (Exception e) {
            return "redirect:/gym/" + id + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/{id}/course/{courseId}/leave")
    @PreAuthorize("isAuthenticated()")
    public String leaveCourse(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        try {
            gymPageService.leaveCourse(courseId, authentication.getName());
            return "redirect:/gym/" + id + "?success=left";
        } catch (Exception e) {
            return "redirect:/gym/" + id + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/{id}/course/{courseId}/chat")
    @PreAuthorize("isAuthenticated()")
    public String openCourseChat(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        Long chatId = gymPageService.openCourseChat(courseId, authentication.getName());
        if (chatId != null) {
            return "redirect:/chat/" + chatId;
        }
        return "redirect:/gym/" + id;
    }

    @PostMapping("/{id}/course/{courseId}/delete")
    @PreAuthorize("hasAnyAuthority('GYM_OWNER', 'GYM_WORKER')")
    public String deleteCourse(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        try {
            gymPageService.deleteCourse(id, courseId, authentication.getName());
            return "redirect:/gym/edit/" + id + "?success=courseDeleted";
        } catch (SecurityException e) {
            return "redirect:/error?message=AccessDenied";
        } catch (Exception e) {
            return "redirect:/gym/edit/" + id + "?error=" + e.getMessage();
        }
    }

    // --- Worker Management (Server-Side) ---

    @PostMapping("/{id}/worker/create")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public String createWorker(@PathVariable Long id,
                               @ModelAttribute CreateGymWorkerDto workerDto,
                               Authentication authentication) {
        try {
            gymPageService.createWorker(id, workerDto, authentication.getName());
            return "redirect:/gym/edit/" + id + "?success=workerCreated";
        } catch (Exception e) {
            return "redirect:/gym/edit/" + id + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/{id}/worker/{workerId}/delete")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public String deleteWorker(@PathVariable Long id, @PathVariable Long workerId, Authentication authentication) {
        try {
            gymPageService.deleteWorker(id, workerId, authentication.getName());
            return "redirect:/gym/edit/" + id + "?success=workerDeleted";
        } catch (SecurityException e) {
            return "redirect:/error?message=AccessDenied";
        }
    }
}
