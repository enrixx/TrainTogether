package de.othr.traintogether.controller.TrainingController;

import de.othr.traintogether.model.TrainingModel.Exercise;
import de.othr.traintogether.model.TrainingModel.TrainingDay;
import de.othr.traintogether.model.TrainingModel.WorkoutPlan;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/workout")
public class WorkoutController {

    @GetMapping("/days")
    public String selectTrainingDay(Model model) {
        model.addAttribute("day", new TrainingDay());
        return "TrainingPages/workout/workout-days";
    }

    @PostMapping("/days")
    public String trainingDaySelected(@ModelAttribute TrainingDay day, Model model) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setDay(day.getWeekday());

        model.addAttribute("day", day);
        model.addAttribute("exercise", new Exercise());
        model.addAttribute("plan", plan);

        return "TrainingPages/workout/workout-exercises";
    }

    @PostMapping("/exercises")
    public String addExercise(@ModelAttribute Exercise exercise,
                              @RequestParam String day,
                              Model model) {

        model.addAttribute("added", true);
        model.addAttribute("exercise", new Exercise());
        model.addAttribute("day", day);

        return "TrainingPages/workout/workout-exercises";
    }
}