package de.othr.traintogether.controller;

import de.othr.traintogether.dto.GymMapDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.Course;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import de.othr.traintogether.service.CourseService;
import de.othr.traintogether.service.GymService;
import de.othr.traintogether.service.MinioService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gym")
public class GymController {

    private final GymService gymService;
    private final UserService userService;
    private final MinioService minioService;
    private final CourseService courseService;

    public GymController(GymService gymService, UserService userService, MinioService minioService, CourseService courseService) {
        this.gymService = gymService;
        this.userService = userService;
        this.minioService = minioService;
        this.courseService = courseService;
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
    public String showGymPage(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Gym> gymOpt = gymService.findById(id);
        if (gymOpt.isPresent()) {
            Gym gym = gymOpt.get();

            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("GYM_OWNER") || a.getAuthority().equals("ROLE_GYM_OWNER"))) {

                UserDto currentUser = userService.findUserDTOByEmail(authentication.getName());

                if (gym.getOwner() == null || !gym.getOwner().getId().equals(currentUser.getId())) {
                    // Redirect to error page to avoid infinite loop with MainLayoutController
                    return "redirect:/error?message=AccessDenied";
                }
            }

            model.addAttribute("gym", gym);
            if (authentication != null) {
                User user = userService.getUserByEmail(authentication.getName());
                model.addAttribute("currentUser", user);
            }
            return "gym";
        } else {
            return "redirect:/map";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public String showEditGymPage(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Gym> gymOpt = gymService.findById(id);
        if (gymOpt.isPresent()) {
            Gym gym = gymOpt.get();

            UserDto currentUser = userService.findUserDTOByEmail(authentication.getName());

            if (gym.getOwner() == null || !gym.getOwner().getId().equals(currentUser.getId())) {
                return "redirect:/error?message=AccessDenied";
            }

            model.addAttribute("gym", gym);
            return "edit-gym";
        } else {
            return "redirect:/map"; 
        }
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public String editGym(@PathVariable Long id,
                          @ModelAttribute Gym gym,
                          @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
                          Authentication authentication) {
        Optional<Gym> existingGymOpt = gymService.findById(id);
        if (existingGymOpt.isPresent()) {
            Gym existingGym = existingGymOpt.get();

            UserDto currentUser = userService.findUserDTOByEmail(authentication.getName());

            if (existingGym.getOwner() == null || !existingGym.getOwner().getId().equals(currentUser.getId())) {
                return "redirect:/error?message=AccessDenied";
            }

            // Handle banner image upload
            if (bannerImage != null && !bannerImage.isEmpty()) {
                // Delete old banner if it exists
                if (existingGym.getBannerImageUrl() != null && !existingGym.getBannerImageUrl().isEmpty()) {
                    minioService.deleteGymBanner(existingGym.getBannerImageUrl());
                }
                // Upload new banner
                String newBannerUrl = minioService.uploadGymBanner(bannerImage, existingGym.getId());
                existingGym.setBannerImageUrl(newBannerUrl);
            }

            // Update other gym details
            existingGym.setName(gym.getName());
            existingGym.setAddress(gym.getAddress());
            existingGym.setCity(gym.getCity());
            existingGym.setPostalCode(gym.getPostalCode());
            existingGym.setPhoneNumber(gym.getPhoneNumber());
            existingGym.setDescription(gym.getDescription());
            existingGym.setBannerText(gym.getBannerText());
            existingGym.setBannerTextColor(gym.getBannerTextColor());

            gymService.update(id, existingGym);

            return "redirect:/gym/" + id;
        }
        return "redirect:/error?message=GymNotFound";
    }

    // --- Course Management ---

    @PostMapping("/{id}/course/create")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public String createCourse(@PathVariable Long id,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("dateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
                               @RequestParam("maxParticipants") int maxParticipants,
                               Authentication authentication) {
        Optional<Gym> gymOpt = gymService.findById(id);
        if (gymOpt.isPresent()) {
            Gym gym = gymOpt.get();
            UserDto currentUser = userService.findUserDTOByEmail(authentication.getName());

            if (gym.getOwner() == null || !gym.getOwner().getId().equals(currentUser.getId())) {
                return "redirect:/error?message=AccessDenied";
            }

            Course course = new Course();
            course.setName(name);
            course.setDescription(description);
            course.setDateTime(dateTime);
            course.setMaxParticipants(maxParticipants);
            // For now, assign the owner as the trainer. Later we can add a dropdown to select a GymWorker.
            course.setTrainer(userService.getUserByEmail(authentication.getName()));

            courseService.createCourse(course, gym);
            return "redirect:/gym/" + id;
        }
        return "redirect:/map";
    }

    @PostMapping("/{id}/course/{courseId}/join")
    @PreAuthorize("isAuthenticated()")
    public String joinCourse(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        try {
            courseService.joinCourse(courseId, user);
        } catch (Exception e) {
            // Handle full course or other errors
        }
        return "redirect:/gym/" + id;
    }

    @PostMapping("/{id}/course/{courseId}/leave")
    @PreAuthorize("isAuthenticated()")
    public String leaveCourse(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        courseService.leaveCourse(courseId, user);
        return "redirect:/gym/" + id;
    }

    @PostMapping("/{id}/course/{courseId}/delete")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public String deleteCourse(@PathVariable Long id, @PathVariable Long courseId, Authentication authentication) {
        Optional<Gym> gymOpt = gymService.findById(id);
        if (gymOpt.isPresent()) {
            Gym gym = gymOpt.get();
            UserDto currentUser = userService.findUserDTOByEmail(authentication.getName());

            if (gym.getOwner() == null || !gym.getOwner().getId().equals(currentUser.getId())) {
                return "redirect:/error?message=AccessDenied";
            }

            courseService.deleteCourse(courseId);
            return "redirect:/gym/" + id;
        }
        return "redirect:/map";
    }
}
