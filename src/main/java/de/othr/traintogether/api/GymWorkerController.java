package de.othr.traintogether.api;

import de.othr.traintogether.dto.CreateGymWorkerDto;
import de.othr.traintogether.dto.UpdateGymWorkerDto;
import de.othr.traintogether.model.GymWorker;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.service.GymService;
import de.othr.traintogether.service.GymWorkerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Gym Workers", description = "APIs to create, manage and list gym workers")
@RestController
@RequestMapping("/api/workers")
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
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = currentUser.getAuthorities().contains("ADMIN");

        if (!gymService.isOwner(gymId, currentUser.getId()) && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(gymWorkerService.findByGymId(gymId));
    }

    @PostMapping
    public ResponseEntity<?> createGymWorker(@Valid @RequestBody CreateGymWorkerDto gymWorkerDto,
                                          Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            GymWorker gymWorker = gymWorkerService.createGymWorker(
                gymWorkerDto.getEmail(),
                gymWorkerDto.getPassword(),
                gymWorkerDto.getFirstName(),
                gymWorkerDto.getLastName(),
                gymWorkerDto.getGymId(),
                currentUser
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(gymWorker);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Only gym owner or admin can create gym workers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            if (e.getMessage().contains("User with this email already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGymWorker(@PathVariable Long id, @Valid @RequestBody UpdateGymWorkerDto gymWorkerDto,
                                             Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            GymWorker gymWorker = gymWorkerService.updateGymWorker(
                    id,
                    gymWorkerDto.getEmail(),
                    gymWorkerDto.getFirstName(),
                    gymWorkerDto.getLastName(),
                    gymWorkerDto.getPassword(),
                    currentUser
            );
            return ResponseEntity.ok(gymWorker);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Only gym owner or admin can update gym workers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            if (e.getMessage().contains("User with this email already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGymWorker(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        GymWorker gymWorker = gymWorkerService.getById(id);

        boolean isAdmin = currentUser.getAuthorities().contains("ADMIN");

        if (!isAdmin && !gymService.isOwner(gymWorker.getGym().getId(), currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        gymWorkerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
