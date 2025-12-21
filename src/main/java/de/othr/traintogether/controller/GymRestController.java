package de.othr.traintogether.controller;

import de.othr.traintogether.model.Gym;
import de.othr.traintogether.service.GymService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms")
public class GymRestController {

    private final GymService gymService;

    public GymRestController(GymService gymService) {
        this.gymService = gymService;
    }

    @GetMapping
    public List<Gym> getAllGyms() {
        return gymService.getAllGyms();
    }
}