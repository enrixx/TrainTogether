package de.othr.traintogether.controller.TrainingController;

import de.othr.traintogether.model.TrainingModel.TraininProfile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/trainingprofile")
public class ProfileController {

    @GetMapping
    public String showProfile(Model model) {
        model.addAttribute("trainingprofile", new TraininProfile());
        return "TrainingPages/TrainingProfile";
    }

    @PostMapping
    public String submitProfile(@ModelAttribute TraininProfile profile, Model model) {
        // später speichern
        model.addAttribute("saved", true);
        model.addAttribute("trainingprofile", profile);
        return "TrainingPages/TrainingProfile";
    }
}
