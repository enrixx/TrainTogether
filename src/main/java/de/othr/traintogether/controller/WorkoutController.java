package de.othr.traintogether.controller;

import de.othr.traintogether.dto.PersonalExerciseDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.dto.WorkoutLogRequest;
import de.othr.traintogether.dto.WorkoutLogResponse;
import de.othr.traintogether.model.TrainingModel.PersonalExercise;
import de.othr.traintogether.model.TrainingModel.TrainingDay;
import de.othr.traintogether.model.TrainingModel.TrainingExercise;
import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import de.othr.traintogether.model.TrainingModel.TrainingSplit;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingDayRepository;
import de.othr.traintogether.repository.TrainingExerciseRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final UserService userService;
    private final TrainingProfileRepository profileRepo;
    private final TrainingDayRepository dayRepo;
    private final PersonalExerciseRepository exerciseRepo;
    private final TrainingExerciseRepository trainingExerciseRepo;

    @GetMapping
    public String showWorkoutsPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        if (profile == null) {
            model.addAttribute("error", "No training profile found. Please create one in your profile.");
            return "workouts";
        }
        
        TrainingSplit activeSplit = profile.getActiveTraininSplit();
        if (activeSplit == null) {
            model.addAttribute("error", "No active training split found. Please set one in your profile.");
            return "workouts";
        }

        // Check for today's workout
        List<TrainingExercise> todaysWorkoutEntities = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(LocalDate.now(), user.getId());
        
        if (!todaysWorkoutEntities.isEmpty()) {
            // Map to DTO to avoid serialization issues with Hibernate proxies
            List<WorkoutLogResponse> todaysWorkoutDto = todaysWorkoutEntities.stream()
                .map(ex -> new WorkoutLogResponse(
                    ex.getPersonalExercise().getId(),
                    ex.getPersonalExercise().getName(),
                    ex.getSets(),
                    ex.getReps()
                ))
                .collect(Collectors.toList());

            model.addAttribute("todaysWorkout", todaysWorkoutDto);
            
            // We can also find the day that was logged
            if (todaysWorkoutEntities.get(0).getDay() != null) {
                model.addAttribute("loggedDayId", todaysWorkoutEntities.get(0).getDay().getId());
                model.addAttribute("loggedDayName", todaysWorkoutEntities.get(0).getDay().getWeekday());
            }
        }

        model.addAttribute("profile", profile);
        model.addAttribute("split", activeSplit);
        model.addAttribute("splits", profile.getSplits());
        
        // Map allExercises to DTO to avoid serialization issues
        List<PersonalExercise> allExercisesEntities = exerciseRepo.findAllByUserId(user.getId());
        List<PersonalExerciseDto> allExercisesDto = allExercisesEntities.stream()
                .map(ex -> new PersonalExerciseDto(ex.getId(), ex.getName()))
                .collect(Collectors.toList());
        model.addAttribute("allExercises", allExercisesDto);
        
        model.addAttribute("activeSplit", profile.getActiveTraininSplitId());
        
        // Add current day of week (MONDAY, TUESDAY, etc.)
        model.addAttribute("currentDayOfWeek", LocalDate.now().getDayOfWeek().name());

        return "workouts";
    }

    @GetMapping("/previous")
    public String showPreviousWorkoutsPage() {
        return "previousWorkouts";
    }

    @GetMapping("/api/by-date")
    @ResponseBody
    public ResponseEntity<List<WorkoutLogResponse>> getWorkoutsByDate(@RequestParam("date") String dateStr, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        LocalDate date = LocalDate.parse(dateStr);

        List<TrainingExercise> exercises = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        
        List<WorkoutLogResponse> response = exercises.stream()
                .map(ex -> new WorkoutLogResponse(
                        ex.getPersonalExercise().getId(),
                        ex.getPersonalExercise().getName(),
                        ex.getSets(),
                        ex.getReps()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping("/log")
    public String logWorkout(@ModelAttribute WorkoutLogRequest workoutRequest, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        
        TrainingDay trainingDay = dayRepo.findById(workoutRequest.getTrainingDayId()).orElse(null);
        if (trainingDay == null) {
            return "redirect:/workouts?error=dayNotFound";
        }

        LocalDate date = LocalDate.now();

        // Delete existing workout for today to allow "editing" by overwriting
        List<TrainingExercise> existingWorkout = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        if (!existingWorkout.isEmpty()) {
            trainingExerciseRepo.deleteAll(existingWorkout);
        }

        // Add a null check here to prevent NullPointerException
        if (workoutRequest.getExercises() != null) {
            for (WorkoutLogRequest.ExerciseLog exerciseLog : workoutRequest.getExercises()) {
                PersonalExercise personalExercise = exerciseRepo.findById(exerciseLog.getPersonalExerciseId()).orElse(null);
                if (personalExercise != null) {
                    TrainingExercise trainingExercise = new TrainingExercise(
                        personalExercise, 
                        exerciseLog.getSets(), 
                        exerciseLog.getReps(), 
                        trainingDay,
                        date
                    );
                    trainingExerciseRepo.save(trainingExercise);
                }
            }
        }
        return "redirect:/workouts?success";
    }

    @Transactional
    @PostMapping("/log-restday")
    public String logRestDay(@RequestParam Long trainingDayId, Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.findUserByEmail(email);
        
        TrainingDay trainingDay = dayRepo.findById(trainingDayId).orElse(null);
        if (trainingDay == null) {
            return "redirect:/workouts?error=dayNotFound";
        }

        LocalDate date = LocalDate.now();

        // Delete existing workout for today
        List<TrainingExercise> existingWorkout = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        if (!existingWorkout.isEmpty()) {
            trainingExerciseRepo.deleteAll(existingWorkout);
        }

        // Find RESTDAY exercise - trying "Restday" as requested, falling back to "RESTDAY" if needed
        PersonalExercise restDayExercise = exerciseRepo.findByNameAndUser_Id("Restday", user.getId())
                .or(() -> exerciseRepo.findByNameAndUser_Id("RESTDAY", user.getId()))
                .orElse(null);
                
        if (restDayExercise != null) {
            TrainingExercise trainingExercise = new TrainingExercise(
                restDayExercise, 
                0, 
                "", 
                trainingDay,
                date
            );
            trainingExerciseRepo.save(trainingExercise);
        } else {
             // If Restday exercise doesn't exist, maybe create it? 
             // For now, let's assume it exists or fail silently/redirect with error
             return "redirect:/workouts?error=restdayNotFound";
        }

        return "redirect:/workouts?success=restday";
    }
}
