package de.othr.traintogether.controller;

import de.othr.traintogether.dto.MatchingCardDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.User;
import de.othr.traintogether.service.CourseService;
import de.othr.traintogether.service.MatchingService;
import de.othr.traintogether.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import de.othr.traintogether.model.Course;
import java.util.Optional;

@Controller
@RequestMapping("/matching")
@PreAuthorize("isAuthenticated()")
public class MatchingController {

    private static final Logger logger = LoggerFactory.getLogger(MatchingController.class);

    private final MatchingService matchingService;
    private final UserService userService;
    private final CourseService courseService;

    public MatchingController(MatchingService matchingService, UserService userService, CourseService courseService) {
        this.matchingService = matchingService;
        this.userService = userService;
        this.courseService = courseService;
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

        List<MatchingCardDto> potentialMatches = matchingService.findPotentialMatches(currentUser, minAge, maxAge, gender, trainingDays, excludedIds, page, 5);
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
    public Map<String, Object> performAction(@RequestParam("targetId") Long targetId,
                                @RequestParam(value = "type", defaultValue = "USER") String type,
                                @RequestParam("action") String action,
                                Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        MatchingAction.ActionType actionType = MatchingAction.ActionType.valueOf(action.toUpperCase());

        logger.debug("Processing action: {} type {} from user {} on target {}", action, type, currentUser.getEmail(), targetId);

        Map<String, Object> response = new HashMap<>();
        try {
            if ("COURSE".equalsIgnoreCase(type)) {
                if (actionType == MatchingAction.ActionType.LIKE) {
                    courseService.joinCourse(targetId, currentUser);
                    // Fetch course to get gym ID redirect
                    Optional<Course> courseOpt = courseService.getCourseById(targetId);
                    if (courseOpt.isPresent() && courseOpt.get().getGym() != null) {
                        response.put("redirectUrl", "/gym/" + courseOpt.get().getGym().getId() + "?highlightCourseId=" + targetId);
                    }
                    response.put("matchName", "Course Joined!");
                } else {
                    courseService.skipCourse(targetId, currentUser);
                    response.put("isMatch", false);
                }
                response.put("status", "success");
            } else {
                boolean isMatch = matchingService.performAction(currentUser, targetId, actionType);
                logger.debug("Action result: isMatch={}", isMatch);

                response.put("status", "success");
                response.put("isMatch", isMatch);
                if (isMatch) {
                    User targetUser = userService.getUserById(targetId);
                    response.put("matchName", targetUser.getFirstName() + " " + targetUser.getLastName());
                }
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}
