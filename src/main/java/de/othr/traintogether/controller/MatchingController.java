package de.othr.traintogether.controller;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.User;
import de.othr.traintogether.service.MatchingService;
import de.othr.traintogether.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/matching")
@PreAuthorize("isAuthenticated()")
public class MatchingController {

    private final MatchingService matchingService;
    private final UserService userService;

    public MatchingController(MatchingService matchingService, UserService userService) {
        this.matchingService = matchingService;
        this.userService = userService;
    }

    @GetMapping
    public String matchingPage(@RequestParam(required = false) Integer minAge,
                               @RequestParam(required = false) Integer maxAge,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) List<String> trainingDays,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(required = false) List<Long> excludedIds,
                               @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                               Model model, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        
        // Check if user has a bio
        if (currentUser.getBio() == null || currentUser.getBio().isBlank()) {
            model.addAttribute("missingBio", true);
            return "matching";
        }

        List<UserDto> potentialMatches = matchingService.findPotentialMatches(currentUser, minAge, maxAge, gender, trainingDays, excludedIds, page, 5);
        model.addAttribute("potentialMatches", potentialMatches);
        
        // Add filter values to model to repopulate form
        model.addAttribute("minAge", minAge);
        model.addAttribute("maxAge", maxAge);
        model.addAttribute("gender", gender);
        model.addAttribute("trainingDays", trainingDays);

        if ("XMLHttpRequest".equals(requestedWith)) {
            return "matching :: cardList";
        }

        return "matching";
    }

    @PostMapping("/action")
    @ResponseBody
    public java.util.Map<String, Object> performAction(@RequestParam("targetUserId") Long targetUserId,
                                @RequestParam("action") String action,
                                Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        MatchingAction.ActionType actionType = MatchingAction.ActionType.valueOf(action.toUpperCase());
        
        System.out.println("Processing action: " + action + " from user " + currentUser.getEmail() + " on target " + targetUserId);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            boolean isMatch = matchingService.performAction(currentUser, targetUserId, actionType);
            System.out.println("Action result: isMatch=" + isMatch);

            response.put("status", "success");
            response.put("isMatch", isMatch);
            if (isMatch) {
                User targetUser = userService.getUserById(targetUserId);
                response.put("matchName", targetUser.getFirstName() + " " + targetUser.getLastName());
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}
