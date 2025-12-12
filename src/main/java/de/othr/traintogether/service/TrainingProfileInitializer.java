package de.othr.traintogether.service;

import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.BodyMeasurementsRepository;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.TrainingSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TrainingProfileInitializer {

    private final TrainingProfileRepository profileRepo;
    private final BodyMeasurementsRepository bmRepo;
    private final TrainingSplitRepository splitRepo;
    private final PersonalExerciseRepository peRepo;

    @Transactional
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        User user = event.getUser();

        BodyMeasurements bm = new BodyMeasurements();
        bmRepo.save(bm);

        TrainingSplit split = new TrainingSplit("Base Split");
        splitRepo.save(split);

        TrainingProfile profile = new TrainingProfile(user.getId());
        profile.getMeasurements().add(bm);
        profile.getSplits().add(split);

        profileRepo.save(profile);

        for (ExerciseName ex : ExerciseName.values()) {
            PersonalExercise pe = new PersonalExercise(ex.name(), user);
            peRepo.save(pe);
        }
    }
}
