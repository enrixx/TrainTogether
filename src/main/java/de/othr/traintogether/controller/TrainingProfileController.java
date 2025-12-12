package de.othr.traintogether.controller;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingDayRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.service.UserService;
import lombok.RequiredArgsConstructor;
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
        model.addAttribute("split", profile.getSplits().getFirst());
        model.addAttribute("splits", profile.getSplits());
        model.addAttribute("measurements", profile.getMeasurements().getFirst());
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
    public String updateSplit(@RequestParam(value = "splitId", required = false) Long splitId,
                              @RequestParam("splitName") String splitName,
                              Authentication authentication) {

        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        profile.addSplit(new TrainingSplit(splitName));

        profileRepo.save(profile);

        return "redirect:/training/profile/me?success=split-updated";
    }

    @PostMapping("/training/profile/me/add-exercise-to-day")
    public String addExerciseToDay(
            @RequestParam Long trainingDayId,
            @RequestParam Long personalExerciseId
    ) {
        var trainingDay = trainingDayRepo.getById(trainingDayId);
        PersonalExercise exercise = exerciseRepo.getById(personalExerciseId);
        trainingDay.addPersonalExercise(exercise);

        trainingDayRepo.save(trainingDay);

        return "redirect:/training/profile/me";
    }


}
