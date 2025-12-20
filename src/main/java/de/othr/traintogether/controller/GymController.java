package de.othr.traintogether.controller;

import de.othr.traintogether.dto.GymDto;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.service.GymService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gyms")
public class GymController {

    private final GymService gymService;
    private final UserRepository userRepository;

    public GymController(GymService gymService, UserRepository userRepository) {
        this.gymService = gymService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Gym>> getAllGyms() {
        return ResponseEntity.ok(gymService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Gym> getGymById(@PathVariable Long id) {
        return gymService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/city")
    public ResponseEntity<List<Gym>> searchByCity(@RequestParam String city) {
        return ResponseEntity.ok(gymService.searchByCity(city));
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Gym>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(gymService.searchByName(name));
    }

    @GetMapping("/owner/my-gyms")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public ResponseEntity<List<Gym>> getMyGyms(Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(gymService.findByOwnerId(owner.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public ResponseEntity<Gym> createGym(@Valid @RequestBody GymDto gymDto, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Gym gym = new Gym();
        gym.setName(gymDto.getName());
        gym.setAddress(gymDto.getAddress());
        gym.setCity(gymDto.getCity());
        gym.setPostalCode(gymDto.getPostalCode());
        gym.setPhoneNumber(gymDto.getPhoneNumber());
        gym.setDescription(gymDto.getDescription());

        Gym savedGym = gymService.create(gym, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedGym);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public ResponseEntity<Gym> updateGym(@PathVariable Long id,
                                         @Valid @RequestBody GymDto gymDto,
                                         Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!gymService.isOwner(id, owner.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Gym gym = new Gym();
        gym.setName(gymDto.getName());
        gym.setAddress(gymDto.getAddress());
        gym.setCity(gymDto.getCity());
        gym.setPostalCode(gymDto.getPostalCode());
        gym.setPhoneNumber(gymDto.getPhoneNumber());
        gym.setDescription(gymDto.getDescription());

        Gym updatedGym = gymService.update(id, gym);
        return ResponseEntity.ok(updatedGym);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GYM_OWNER')")
    public ResponseEntity<Void> deleteGym(@PathVariable Long id, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!gymService.isOwner(id, owner.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        gymService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

