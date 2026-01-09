package de.othr.traintogether.service;

import de.othr.traintogether.dto.BatchExerciseUpdateRequestDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingDayRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrainingProfileService {

    private final UserService userService;
    private final TrainingProfileRepository profileRepo;
    private final PersonalExerciseRepository exerciseRepo;
    private final TrainingDayRepository trainingDayRepo;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TrainingProfile getTrainingProfile(String email) {
        UserDto user = userService.findUserDTOByEmail(email);
        Optional<TrainingProfile> profileOpt = profileRepo.findFirstByUserId(user.getId());

        if (profileOpt.isPresent()) {
            TrainingProfile profile = profileOpt.get();
            Hibernate.initialize(profile.getSplits());
            Hibernate.initialize(profile.getMeasurements());
            if (profile.getSplits() != null) {
                profile.getSplits().forEach(split -> {
                    Hibernate.initialize(split.getDays());
                    if (split.getDays() != null) {
                        split.getDays().forEach(day -> {
                            Hibernate.initialize(day.getPersonalExercises());
                        });
                    }
                });
            }
            return profile;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<PersonalExercise> getAllExercises(String email) {
        UserDto user = userService.findUserDTOByEmail(email);
        return exerciseRepo.findAllByUserId(user.getId());
    }

    @Transactional
    public void updateDescription(String email, String description) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findFirstByUserId(user.getId()).orElse(null);
        if (profile != null) {
            profile.setDescription(description);
            profileRepo.save(profile);
        }
    }

    @Transactional
    public void createSplit(String email, String splitName) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findFirstByUserId(user.getId()).orElse(null);
        if (profile != null) {
            profile.addSplit(new TrainingSplit(splitName));
            profileRepo.save(profile);
        }
    }

    @Transactional
    public void setActiveSplit(String email, Long splitId) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findFirstByUserId(user.getId()).orElse(null);
        if (profile != null) {
            profile.setActiveTraininSplitId(splitId);
            profileRepo.save(profile);
        }
    }

    @Transactional
    public void deleteSplit(String email, Long splitId) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findFirstByUserId(user.getId()).orElse(null);

        if (profile != null) {
            profile.getSplits().removeIf(s -> s.getId().equals(splitId));

            // If active split was deleted, reset active split
            if (profile.getActiveTraininSplitId() != null && profile.getActiveTraininSplitId().equals(splitId)) {
                if (!profile.getSplits().isEmpty()) {
                    profile.setActiveTraininSplitId(profile.getSplits().get(0).getId());
                } else {
                    profile.setActiveTraininSplitId(null);
                }
            }

            profileRepo.save(profile);
        }
    }

    @Transactional
    public void removeExerciseFromDay(Long trainingDayId, Long exerciseId) {
        TrainingDay trainingDay = trainingDayRepo.findById(trainingDayId).orElse(null);
        if (trainingDay != null) {
            trainingDay.getPersonalExercises().removeIf(ex -> ex.getId().equals(exerciseId));
            trainingDayRepo.save(trainingDay);
        }
    }

    @Transactional
    public void batchUpdateExercises(String email, BatchExerciseUpdateRequestDto request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUpdates() != null) {
            for (BatchExerciseUpdateRequestDto.DayUpdate update : request.getUpdates()) {
                TrainingDay trainingDay = trainingDayRepo.findById(update.getDayId()).orElse(null);
                if (trainingDay != null) {
                    // Handle deletions
                    if (update.getExerciseIdsToDelete() != null) {
                        trainingDay.getPersonalExercises().removeIf(ex -> update.getExerciseIdsToDelete().contains(ex.getId()));
                    }

                    if (update.getExercises() != null) {
                        for (BatchExerciseUpdateRequestDto.ExerciseUpdate exerciseUpdate : update.getExercises()) {
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
    }

    @Transactional
    public void updateMeasurements(String email, Double gewicht, Double groesse,
                                   Double armLinks, Double armRechts,
                                   Double unterarmLinks, Double unterarmRechts,
                                   Double beinLinks, Double beinRechts,
                                   Double brust, Double schulter,
                                   Double taille, Double huefte) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findFirstByUserId(user.getId()).orElse(null);

        if (profile != null) {
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
        }
    }
}
