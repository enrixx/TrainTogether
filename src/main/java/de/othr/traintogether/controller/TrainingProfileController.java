package de.othr.traintogether.controller;

import de.othr.traintogether.dto.BatchExerciseUpdateRequestDto;
import de.othr.traintogether.model.TrainingModel.PersonalExercise;
import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import de.othr.traintogether.service.TrainingProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TrainingProfileController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingProfileController.class);
    private final TrainingProfileService trainingProfileService;

    @GetMapping("/training/profile/me")
    public String getTrainingProfile(Model model, Authentication authentication) {
        String email = authentication.getName();
        TrainingProfile profile = trainingProfileService.getTrainingProfile(email);
        List<PersonalExercise> allExercises = trainingProfileService.getAllExercises(email);

        if (profile == null) {
            model.addAttribute("error", "Kein Trainingsprofil gefunden.");
            return "TrainingPages/TrainingProfile";
        }

        model.addAttribute("profile", profile);
        model.addAttribute("split", profile.getActiveTraininSplit());
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
        trainingProfileService.updateDescription(authentication.getName(), description);
        return "redirect:/training/profile/me?success=description-updated";
    }

    @PostMapping("/training/profile/me/create-split")
    public String createSplit(@RequestParam("splitName") String splitName,
                              Authentication authentication) {
        trainingProfileService.createSplit(authentication.getName(), splitName);
        return "redirect:/training/profile/me?success=split-created";
    }

    @PostMapping("/training/profile/me/set-active-split")
    public String setActiveSplit(@RequestParam("splitId") Long splitId, Authentication authentication) {
        trainingProfileService.setActiveSplit(authentication.getName(), splitId);
        return "redirect:/training/profile/me?success=active-split-updated";
    }

    @PostMapping("/training/profile/me/delete-split")
    public String deleteSplit(@RequestParam("splitId") Long splitId, Authentication authentication) {
        trainingProfileService.deleteSplit(authentication.getName(), splitId);
        return "redirect:/training/profile/me?success=split-deleted";
    }

    @PostMapping("/training/profile/me/add-exercise-to-day")
    public String addExerciseToDay(
            @RequestParam Long trainingDayId,
            @RequestParam List<Long> personalExerciseIds
    ) {
        // This method was empty in the original controller, keeping it as is or should it be implemented?
        // Assuming it's a placeholder or handled elsewhere for now, but keeping the endpoint.
        return "redirect:/training/profile/me";
    }

    @PostMapping("/training/profile/me/remove-exercise-from-day")
    public String removeExerciseFromDay(
            @RequestParam Long trainingDayId,
            @RequestParam Long exerciseId
    ) {
        trainingProfileService.removeExerciseFromDay(trainingDayId, exerciseId);
        return "redirect:/training/profile/me?success=exercise-removed";
    }

    @PostMapping("/training/profile/me/batch-update-exercises")
    @ResponseBody
    public ResponseEntity<String> batchUpdateExercises(@RequestBody BatchExerciseUpdateRequestDto request, Authentication authentication) {
        try {
            trainingProfileService.batchUpdateExercises(authentication.getName(), request);
            return ResponseEntity.ok("{\"status\":\"success\"}");
        } catch (Exception e) {
            logger.error("Error updating exercises", e);
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
        trainingProfileService.updateMeasurements(authentication.getName(), gewicht, groesse,
                armLinks, armRechts, unterarmLinks, unterarmRechts,
                beinLinks, beinRechts, brust, schulter, taille, huefte);
        return "redirect:/training/profile/me?success=measurements-updated";
    }
}
