package de.othr.traintogether.controller;

import de.othr.traintogether.model.Gym;
import de.othr.traintogether.service.GymService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/gym")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) {
        this.gymService = gymService;
    }

    @GetMapping("/{id}")
    public String showGymPage(@PathVariable Long id, Model model) {
        Optional<Gym> gym = gymService.getGymById(id);
        if (gym.isPresent()) {
            model.addAttribute("gym", gym.get());
            return "gym";
        } else {
            return "redirect:/map";
        }
    }

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

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('GYMOWNER')")
    public String editGym(@PathVariable Long id, @ModelAttribute Gym gym) {
        gym.setId(id);
        gymService.saveGym(gym);
        return "redirect:/gym/" + id;
    }
}