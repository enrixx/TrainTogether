package de.othr.traintogether.api;

import de.othr.traintogether.dto.CreateGymWorkerDto;
import de.othr.traintogether.model.GymWorker;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.service.GymService;
import de.othr.traintogether.service.GymWorkerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gym-workers")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GYM_OWNER')")
public class GymWorkerController {

    private final GymWorkerService gymWorkerService;
    private final UserRepository userRepository;
    private final GymService gymService;

    public GymWorkerController(GymWorkerService gymWorkerService,
                           UserRepository userRepository,
                           GymService gymService) {
        this.gymWorkerService = gymWorkerService;
        this.userRepository = userRepository;
        this.gymService = gymService;
    }

    @GetMapping("/gym/{gymId}")
    public ResponseEntity<List<GymWorker>> getGymWorkersByGym(@PathVariable Long gymId, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!gymService.isOwner(gymId, owner.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(gymWorkerService.findByGymId(gymId));
    }

    @GetMapping("/gym/{gymId}/active")
    public ResponseEntity<List<GymWorker>> getActiveGymWorkersByGym(@PathVariable Long gymId, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!gymService.isOwner(gymId, owner.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(gymWorkerService.findActiveByGymId(gymId));
    }

    @PostMapping
    public ResponseEntity<?> createGymWorker(@Valid @RequestBody CreateGymWorkerDto gymWorkerDto,
                                          Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            GymWorker gymWorker = gymWorkerService.createGymWorker(
                gymWorkerDto.getEmail(),
                gymWorkerDto.getPassword(),
                gymWorkerDto.getFirstName(),
                gymWorkerDto.getLastName(),
                gymWorkerDto.getGymId(),
                owner
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(gymWorker);
        } catch (RuntimeException e) {
            // Check if it's a permission error (trying to create worker for gym not owned)
            if (e.getMessage().contains("Only gym owner can create gym workers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            // Check if it's a duplicate email error
            if (e.getMessage().contains("User with this email already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            // Other errors
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateGymWorker(@PathVariable Long id, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            gymWorkerService.deactivateGymWorker(id, owner);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateGymWorker(@PathVariable Long id, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            gymWorkerService.activateGymWorker(id, owner);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGymWorker(@PathVariable Long id, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        GymWorker gymWorker = gymWorkerService.getById(id);

        if (!gymService.isOwner(gymWorker.getGym().getId(), owner.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        gymWorkerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

