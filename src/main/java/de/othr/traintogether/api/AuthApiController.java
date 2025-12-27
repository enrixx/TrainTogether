package de.othr.traintogether.api;

import de.othr.traintogether.dto.AuthRequest;
import de.othr.traintogether.dto.AuthResponse;
import de.othr.traintogether.security.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger logger = LoggerFactory.getLogger(AuthApiController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public AuthApiController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@Valid @RequestBody AuthRequest authRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );

            UserDetails ud = userDetailsService.loadUserByUsername(authRequest.getEmail());
            String token = jwtUtil.generateToken(ud);
            return ResponseEntity.ok(new AuthResponse(token, jwtUtil.getJwtExpirationMs()));
        } catch (BadCredentialsException ex) {
            logger.debug("Invalid credentials for {}", authRequest.getEmail());
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}

