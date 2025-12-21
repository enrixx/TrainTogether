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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/gyms")
@Controller
@RequestMapping("/gym")
public class GymController {

    private final GymService gymService;
    private final UserRepository userRepository;

    public GymController(GymService gymService, UserRepository userRepository) {
    public GymController(GymService gymService) {
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
    public String showGymPage(@PathVariable Long id, Model model) {
        Optional<Gym> gym = gymService.getGymById(id);
        if (gym.isPresent()) {
            model.addAttribute("gym", gym.get());
            return "gym";
        } else {
            return "redirect:/map";
        }
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
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('GYMOWNER')")
    public String showEditGymPage(@PathVariable Long id, Model model) {
        Optional<Gym> gym = gymService.getGymById(id);
        if (gym.isPresent()) {
            model.addAttribute("gym", gym.get());
            return "edit-gym";
        } else {
            return "redirect:/map";
        }
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
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('GYMOWNER')")
    public String editGym(@PathVariable Long id, @ModelAttribute Gym gym) {
        gym.setId(id);
        gymService.saveGym(gym);
        return "redirect:/gym/" + id;
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

