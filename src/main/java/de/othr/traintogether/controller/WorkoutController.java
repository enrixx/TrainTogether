package de.othr.traintogether.controller;

import de.othr.traintogether.dto.*;
import de.othr.traintogether.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @GetMapping
    public String showWorkoutsPage(Model model, Authentication authentication, Locale locale) {
        try {
            WorkoutPageDto pageData = workoutService.getWorkoutPageData(authentication.getName(), locale);
            
            model.addAttribute("profile", pageData.getProfile());
            model.addAttribute("split", pageData.getActiveSplit());
            model.addAttribute("splits", pageData.getSplits());
            model.addAttribute("todaysWorkout", pageData.getTodaysWorkout());
            if (pageData.getLoggedDayId() != null) {
                model.addAttribute("loggedDayId", pageData.getLoggedDayId());
                model.addAttribute("loggedDayName", pageData.getLoggedDayName());
            }
            model.addAttribute("allExercises", pageData.getAllExercises());
            model.addAttribute("activeSplit", pageData.getActiveSplitId());
            model.addAttribute("currentDayOfWeek", pageData.getCurrentDayOfWeek());
            
            return "workouts";
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "workouts";
        }
    }

    @GetMapping("/previous")
    public String showPreviousWorkoutsPage() {
        return "previousWorkouts";
    }

    @GetMapping("/api/by-date")
    @ResponseBody
    public ResponseEntity<List<WorkoutLogResponseDto>> getWorkoutsByDate(@RequestParam("date") String dateStr, Authentication authentication) {
        return ResponseEntity.ok(workoutService.getWorkoutsByDate(authentication.getName(), dateStr));
    }

    @GetMapping("/api/history")
    @ResponseBody
    public ResponseEntity<List<WorkoutDaySummaryDto>> getWorkoutHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return ResponseEntity.ok(workoutService.getWorkoutHistory(authentication.getName(), page, size));
    }

    @GetMapping("/api/progress")
    @ResponseBody
    public ResponseEntity<List<ProgressDataPointDto>> getProgressData(@RequestParam("exerciseId") String exerciseValue, Authentication authentication) {
        return ResponseEntity.ok(workoutService.getProgressData(authentication.getName(), exerciseValue));
    }

    @GetMapping("/fragments/adhoc-row")
    public String getAdHocRowFragment(Model model, Authentication authentication, Locale locale) {
        WorkoutPageDto pageData = workoutService.getWorkoutPageData(authentication.getName(), locale);
        model.addAttribute("allExercises", pageData.getAllExercises());
        return "fragments/adhoc-row";
    }

    @PostMapping("/log")
    public String logWorkout(@ModelAttribute WorkoutLogRequestDto workoutRequest, Authentication authentication) {
        try {
            workoutService.logWorkout(authentication.getName(), workoutRequest);
            return "redirect:/workouts?success";
        } catch (IllegalArgumentException e) {
            return "redirect:/workouts?error=dayNotFound";
        }
    }

    @PostMapping("/log-restday")
    public String logRestDay(@RequestParam Long trainingDayId, Authentication authentication) {
        try {
            workoutService.logRestDay(authentication.getName(), trainingDayId);
            return "redirect:/workouts?success=restday";
        } catch (IllegalArgumentException e) {
            return "redirect:/workouts?error=dayNotFound";
        } catch (IllegalStateException e) {
            return "redirect:/workouts?error=restdayNotFound";
        }
    }
}
