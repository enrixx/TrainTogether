package de.othr.traintogether.controller;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;

    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public UserDto populateCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            if (!"anonymousUser".equals(email)) {
                return userService.findUserDTOByEmail(email);
            }
        }
        return null;
    }
}
