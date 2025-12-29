package de.othr.traintogether.controller;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.RequestStatus;
import de.othr.traintogether.repository.GymOwnerRequestRepository;
import de.othr.traintogether.service.GymOwnerRequestService;
import de.othr.traintogether.service.GymService;
import de.othr.traintogether.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;


@Controller
public class MainLayoutController {

    private final UserService userService;
    private final GymOwnerRequestRepository gymOwnerRequestRepository;
    private final GymOwnerRequestService gymOwnerRequestService;
    private final GymService gymService;

    public MainLayoutController(UserService userService,
                               GymOwnerRequestRepository gymOwnerRequestRepository,
                               GymOwnerRequestService gymOwnerRequestService,
                               GymService gymService) {
        this.userService = userService;
        this.gymOwnerRequestRepository = gymOwnerRequestRepository;
        this.gymOwnerRequestService = gymOwnerRequestService;
        this.gymService = gymService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication authentication) {
        model.addAttribute("title", "Home");

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            UserDto user = userService.findUserDTOByEmail(email);

            // Check if user has a gym (implicitly checks if they are a gym owner)
            Optional<Gym> myGym = gymService.findByOwnerEmail(email);
            myGym.ifPresent(gym -> model.addAttribute("myGymId", gym.getId()));

            if (user != null) {
                model.addAttribute("currentUser", user);

                boolean hasPendingRequest = gymOwnerRequestRepository.existsByUserIdAndStatus(
                    user.getId(), RequestStatus.PENDING
                );
                model.addAttribute("hasPendingGymOwnerRequest", hasPendingRequest);

                boolean hasRejectedRequest = !hasPendingRequest && gymOwnerRequestService.hasRejectedRequest(email);
                model.addAttribute("hasRejectedGymOwnerRequest", hasRejectedRequest);
            }
        }

        return "home";
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'GYM_OWNER', 'GYM_WORKER', 'PENDING_GYM_WORKER')")
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        // Redirect Gym Owners to their edit page if they have a gym
        if (authentication != null) {
            Optional<Gym> myGym = gymService.findByOwnerEmail(authentication.getName());
            if (myGym.isPresent()) {
                return "redirect:/gym/edit/" + myGym.get().getId();
            }
        }

        model.addAttribute("title", "Dashboard");
        return "dashboard";
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER', 'GYM_WORKER', 'GYM_OWNER')")
    @GetMapping("/map")
    public String map(Model model) {
        model.addAttribute("title", "Map");
        return "map";
    }
}
