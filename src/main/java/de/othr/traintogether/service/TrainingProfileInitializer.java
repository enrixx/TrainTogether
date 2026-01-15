package de.othr.traintogether.service;

import de.othr.traintogether.model.trainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.*;
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
    private final StandardExerciseRepository standardExerciseRepository;
    private final TrainingDayRepository trainingDayRepo;

    @Transactional
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        User user = event.getUser();
        logger.info("Initializing training profile for user: {}", user.getEmail());

        List<StandardExercise> standardExercises = standardExerciseRepository.findAll();
        List<PersonalExercise> savedExercises = new ArrayList<>();

        for (StandardExercise ex : standardExercises) {
            PersonalExercise pe = new PersonalExercise(ex, user);
            peRepo.save(pe);
            savedExercises.add(pe);
        }

        TrainingProfile profile = new TrainingProfile(user.getId());
        
        BodyMeasurements bm = new BodyMeasurements();
        bmRepo.save(bm);
        profile.addMeasurements(bm);

        TrainingSplit split = new TrainingSplit("Default Split");
        splitRepo.save(split);
        profile.addSplit(split);

        profile = profileRepo.save(profile);

        profile.setActiveTrainingSplitId(split.getId());
        profileRepo.save(profile);

        if (user.getId() == 1L) {
            createDummyData(user, split);
        }
    }

    private void createDummyData(User user, TrainingSplit split) {
        List<String> pushExercises = List.of("Bankdrücken", "Schulterdrücken", "Dips");
        List<String> pullExercises = List.of("Kreuzheben", "Klimmzüge", "Langhantelrudern");
        List<String> legExercises = List.of("Kniebeugen", "Beinpresse", "Wadenheben");

        List<StandardExercise> allStandards = standardExerciseRepository.findAll();

        for (int i = 100; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            int splitDayIndex = i % 3;
            
            List<String> todaysNames;
            if (splitDayIndex == 0) todaysNames = pushExercises;
            else if (splitDayIndex == 1) todaysNames = pullExercises;
            else todaysNames = legExercises;

            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
            TrainingDay trainingDay = split.getDays().stream()
                    .filter(d -> d.getWeekday() == dayOfWeek)
                    .findFirst()
                    .orElse(null);

            if (trainingDay == null) continue;

            for (String name : todaysNames) {
                StandardExercise se = allStandards.stream()
                        .filter(e -> e.getNameDe().equals(name))
                        .findFirst()
                        .orElse(null);

                if (se == null) continue;

                PersonalExercise pe = peRepo.findByStandardExercise_IdAndUser_Id(se.getId(), user.getId())
                        .stream().findFirst().orElse(null);

                if (pe == null) {
                    pe = new PersonalExercise(se, user);
                    pe.setSets(3);
                    pe = peRepo.save(pe);
                }

                double baseWeight = 40.0;
                if (name.equals("Kreuzheben") || name.equals("Kniebeugen")) baseWeight = 80.0;
                if (name.equals("Dips") || name.equals("Klimmzüge")) baseWeight = 0.0;
                if (name.equals("Wadenheben")) baseWeight = 60.0;

                double progress = (100 - i) * 0.5;
                double weight = baseWeight + progress;
                
                if (i % 7 == 0) weight -= 2.5;

                String weightStr = String.format("%.1f,%.1f,%.1f", weight, weight, weight);

                TrainingExercise te = new TrainingExercise(
                        pe,
                        3,
                        "10,10,10",
                        weightStr,
                        trainingDay,
                        date
                );
                teRepo.save(te);

                if (i < 7) {
                    if (!trainingDay.getPersonalExercises().contains(pe)) {
                        trainingDay.addPersonalExercise(pe);
                        trainingDayRepo.save(trainingDay);
                    }
                }
            }
        }
    }
}
