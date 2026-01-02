package de.othr.traintogether.service;

import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.BodyMeasurementsRepository;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingExerciseRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.TrainingSplitRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrainingProfileInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TrainingProfileInitializer.class);

    private final TrainingProfileRepository profileRepo;
    private final BodyMeasurementsRepository bmRepo;
    private final TrainingSplitRepository splitRepo;
    private final PersonalExerciseRepository peRepo;
    private final TrainingExerciseRepository teRepo;

    @Transactional
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        User user = event.getUser();
        logger.info("Initializing training profile for user: {}", user.getEmail());

        List<PersonalExercise> savedExercises = new ArrayList<>();
        for (ExerciseName ex : ExerciseName.values()) {
            PersonalExercise pe = new PersonalExercise(ex.name(), user);
            peRepo.save(pe);
            savedExercises.add(pe);
        }

        // Create Training Profile
        TrainingProfile profile = new TrainingProfile(user.getId());
        
        // Create initial Body Measurements
        BodyMeasurements bm = new BodyMeasurements();
        bmRepo.save(bm);
        profile.addMeasurements(bm);

        // Create Default Split
        TrainingSplit split = new TrainingSplit("Default Split");
        splitRepo.save(split);
        profile.addSplit(split);

        // Save profile first to generate ID
        profile = profileRepo.save(profile);

        // Set active split
        profile.setActiveTraininSplitId(split.getId());
        profileRepo.save(profile);

        // Dummy data for User ID 1 (Seed Data)
        if (user.getId() == 1L) {
            createDummyData(user, split, savedExercises);
        }
    }

    private void createDummyData(User user, TrainingSplit split, List<PersonalExercise> exercises) {
        LocalDate today = LocalDate.now();
        
        // Create data for the last 7 days
        for (int i = 1; i <= 7; i++) {
            LocalDate date = today.minusDays(i);
            
            // Find the TrainingDay corresponding to this date's weekday
            String dayName = date.getDayOfWeek().name();
            TrainingDay trainingDay = split.getDays().stream()
                    .filter(d -> d.getWeekday().name().equals(dayName))
                    .findFirst()
                    .orElse(null);

            if (trainingDay != null && !exercises.isEmpty()) {
                // Add a dummy exercise (e.g., the first one, or random)
                PersonalExercise pe = exercises.get(i % exercises.size()); // Rotate through exercises
                
                TrainingExercise te = new TrainingExercise(
                        pe,
                        3, // sets
                        "10,10,10", // reps
                        "50,50,50", // weight
                        trainingDay,
                        date
                );
                teRepo.save(te);
            }
        }
    }
}
