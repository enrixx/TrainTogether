package de.othr.traintogether.controller;

import de.othr.traintogether.dto.BatchExerciseUpdateRequest;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingDayRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Controller
@RequiredArgsConstructor
public class TrainingProfileController {

    private final UserService userService;
    private final TrainingProfileRepository profileRepo;
    private final PersonalExerciseRepository exerciseRepo;
    private final TrainingDayRepository trainingDayRepo;
    private final UserRepository userRepository;

    @GetMapping("/training/profile/me")
    public String getTrainingProfile( Model model, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        List<PersonalExercise> allExercises = exerciseRepo.findAllByUserId(user.getId());

        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        if (profile == null) {
            model.addAttribute("error", "Kein Trainingsprofil gefunden.");
            return "TrainingPages/TrainingProfile";
        }

        model.addAttribute("profile", profile);
        model.addAttribute("split", profile.getActiveTraininSplit()); // Use the active split method
        model.addAttribute("splits", profile.getSplits());
        if (!profile.getMeasurements().isEmpty()) {
            model.addAttribute("latestBodyMeasurements", profile.getMeasurements().getLast());
        }
        model.addAttribute("allExercises", allExercises);
        model.addAttribute("activeSplit", profile.getActiveTraininSplitId());

        return "TrainingPages/TrainingProfile";
    }


    @PostMapping("/training/profile/update-description")
    public String updateDescription(@RequestParam("description") String description,
                                    Authentication authentication) {

        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);

        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        profile.setDescription(description);
        profileRepo.save(profile);

        return "redirect:/training/profile/me?success=description-updated";
    }

    @PostMapping("/training/profile/me/create-split")
    public String createSplit(@RequestParam("splitName") String splitName,
                              Authentication authentication) {

        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        profile.addSplit(new TrainingSplit(splitName));

        profileRepo.save(profile);

        return "redirect:/training/profile/me?success=split-created";
    }

    @PostMapping("/training/profile/me/set-active-split")
    public String setActiveSplit(@RequestParam("splitId") Long splitId, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        profile.setActiveTraininSplitId(splitId);
        profileRepo.save(profile);

        return "redirect:/training/profile/me?success=active-split-updated";
    }

    @PostMapping("/training/profile/me/delete-split")
    public String deleteSplit(@RequestParam("splitId") Long splitId, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        profile.getSplits().removeIf(s -> s.getId().equals(splitId));
        
        // If active split was deleted, reset active split
        if (profile.getActiveTraininSplitId().equals(splitId)) {
            if (!profile.getSplits().isEmpty()) {
                profile.setActiveTraininSplitId(profile.getSplits().get(0).getId());
            } else {
                profile.setActiveTraininSplitId(null);
            }
        }

        profileRepo.save(profile);

        return "redirect:/training/profile/me?success=split-deleted";
    }

    @PostMapping("/training/profile/me/add-exercise-to-day")
    public String addExerciseToDay(
            @RequestParam Long trainingDayId,
            @RequestParam List<Long> personalExerciseIds
    ) {
        return "redirect:/training/profile/me";
    }

    @PostMapping("/training/profile/me/remove-exercise-from-day")
    public String removeExerciseFromDay(
            @RequestParam Long trainingDayId,
            @RequestParam Long exerciseId,
            Authentication authentication
    ) {
        TrainingDay trainingDay = trainingDayRepo.findById(trainingDayId).orElse(null);
        if (trainingDay != null) {
            trainingDay.getPersonalExercises().removeIf(ex -> ex.getId().equals(exerciseId));
            trainingDayRepo.save(trainingDay);
        }
        return "redirect:/training/profile/me?success=exercise-removed";
    }

    @PostMapping("/training/profile/me/batch-update-exercises")
    @ResponseBody
    public ResponseEntity<String> batchUpdateExercises(@RequestBody BatchExerciseUpdateRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

            if (request.getUpdates() != null) {
                for (BatchExerciseUpdateRequest.DayUpdate update : request.getUpdates()) {
                    TrainingDay trainingDay = trainingDayRepo.findById(update.getDayId()).orElse(null);
                    if (trainingDay != null) {
                        // Handle deletions
                        if (update.getExerciseIdsToDelete() != null) {
                            trainingDay.getPersonalExercises().removeIf(ex -> update.getExerciseIdsToDelete().contains(ex.getId()));
                        }

                        if (update.getExercises() != null) {
                            for (BatchExerciseUpdateRequest.ExerciseUpdate exerciseUpdate : update.getExercises()) {
                                PersonalExercise templateExercise = exerciseRepo.findById(exerciseUpdate.getExerciseId()).orElse(null);
                                if (templateExercise != null) {
                                    PersonalExercise newExercise = new PersonalExercise();
                                    newExercise.setName(templateExercise.getName());
                                    newExercise.setSets(exerciseUpdate.getSets());
                                    newExercise.setUser(user);
                                    
                                    exerciseRepo.save(newExercise);
                                    trainingDay.addPersonalExercise(newExercise);
                                }
                            }
                        }
                        trainingDayRepo.save(trainingDay);
                    }
                }
            }
            return ResponseEntity.ok("{\"status\":\"success\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/training/profile/me/update-measurements")
    public String updateMeasurements(
            @RequestParam Double gewicht, @RequestParam Double groesse,
            @RequestParam Double armLinks, @RequestParam Double armRechts,
            @RequestParam Double unterarmLinks, @RequestParam Double unterarmRechts,
            @RequestParam Double beinLinks, @RequestParam Double beinRechts,
            @RequestParam Double brust, @RequestParam Double schulter,
            @RequestParam Double taille, @RequestParam Double huefte,
            Authentication authentication
    ) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        BodyMeasurements measurements = new BodyMeasurements();
        measurements.setGewicht(gewicht);
        measurements.setGroesse(groesse);
        measurements.setArmLinks(new Measurement(armLinks));
        measurements.setArmRechts(new Measurement(armRechts));
        measurements.setUnterarmLinks(new Measurement(unterarmLinks));
        measurements.setUnterarmRechts(new Measurement(unterarmRechts));
        measurements.setBeinLinks(new Measurement(beinLinks));
        measurements.setBeinRechts(new Measurement(beinRechts));
        measurements.setBrust(new Measurement(brust));
        measurements.setSchulter(new Measurement(schulter));
        measurements.setTaille(new Measurement(taille));
        measurements.setHuefte(new Measurement(huefte));

        double heightInMeters = measurements.getGroesse() / 100.0;
        double bmi = measurements.getGewicht() / (heightInMeters * heightInMeters);
        measurements.setBmi(Math.round(bmi * 10.0) / 10.0);

        profile.addMeasurements(measurements);
        profileRepo.save(profile);
        return "redirect:/training/profile/me?success=measurements-updated";
    }
}