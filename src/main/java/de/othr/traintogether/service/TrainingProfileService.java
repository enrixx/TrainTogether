package de.othr.traintogether.service;

import de.othr.traintogether.dto.BatchExerciseUpdateRequestDto;
import de.othr.traintogether.dto.BodyMeasurementsDto;
import de.othr.traintogether.dto.ExerciseOptionDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.trainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrainingProfileService {

    private final UserService userService;
    private final TrainingProfileRepository profileRepo;
    private final PersonalExerciseRepository exerciseRepo;
    private final TrainingDayRepository trainingDayRepo;
    private final UserRepository userRepository;
    private final StandardExerciseRepository standardExerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final ExerciseServiceHelper exerciseServiceHelper;

    @Transactional(readOnly = true)
    public TrainingProfile getTrainingProfile(String email) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        if (profile == null) {
            return null;
        }

        initializeProfileDeeply(profile);
        return profile;
    }

    private void initializeProfileDeeply(TrainingProfile profile) {
        Hibernate.initialize(profile.getSplits());
        Hibernate.initialize(profile.getMeasurements());

        if (profile.getSplits() == null) return;

        for (TrainingSplit split : profile.getSplits()) {
            Hibernate.initialize(split.getDays());
            if (split.getDays() != null) {
                split.getDays().forEach(day -> Hibernate.initialize(day.getPersonalExercises()));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ExerciseOptionDto> getAvailableExercises(String email, Locale locale) {
        UserDto user = userService.findUserDTOByEmail(email);
        List<ExerciseOptionDto> options = new ArrayList<>();

        // Add Standard Exercises
        List<StandardExercise> standardExercises = standardExerciseRepository.findAll();
        for (StandardExercise ex : standardExercises) {
            String name = (locale != null && locale.getLanguage().equals("de")) ? ex.getNameDe() : ex.getNameEn();
            options.add(new ExerciseOptionDto("S-" + ex.getId(), name, "STANDARD"));
        }

        // Add Custom Exercises
        List<CustomExercise> customExercises = customExerciseRepository.findAllByCreatedBy_Id(user.getId());
        for (CustomExercise ex : customExercises) {
            options.add(new ExerciseOptionDto("C-" + ex.getId(), ex.getName(), "CUSTOM"));
        }

        return options;
    }

    @Transactional(readOnly = true)
    public List<PersonalExercise> getAllExercises(String email) {
        UserDto user = userService.findUserDTOByEmail(email);
        return exerciseRepo.findAllByUserId(user.getId());
    }

    @Transactional
    public void updateDescription(String email, String description) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());
        profile.setDescription(description);
        profileRepo.save(profile);
    }

    @Transactional
    public void createSplit(String email, String splitName) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());
        profile.addSplit(new TrainingSplit(splitName));
        profileRepo.save(profile);
    }

    @Transactional
    public void setActiveSplit(String email, Long splitId) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());
        profile.setActiveTrainingSplitId(splitId);
        profileRepo.save(profile);
    }

    @Transactional
    public void deleteSplit(String email, Long splitId) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        profile.deleteSplit(splitId);

        profileRepo.save(profile);
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

        if (request.getUpdates() == null) return;

        for (BatchExerciseUpdateRequestDto.DayUpdate update : request.getUpdates()) {
            processDayUpdate(update, user);
        }
    }

    private void processDayUpdate(BatchExerciseUpdateRequestDto.DayUpdate update, User user) {
        TrainingDay trainingDay = trainingDayRepo.findById(update.getDayId()).orElse(null);
        if (trainingDay == null) return;

        if (update.getExerciseIdsToDelete() != null && !update.getExerciseIdsToDelete().isEmpty()) {
            trainingDay.getPersonalExercises().removeIf(ex -> update.getExerciseIdsToDelete().contains(ex.getId()));
        }

        if (update.getExercises() != null) {
            for (BatchExerciseUpdateRequestDto.ExerciseUpdate exerciseUpdate : update.getExercises()) {
                addExerciseToDayFromOption(trainingDay.getId(), exerciseUpdate.getExerciseValue(), exerciseUpdate.getSets(), user);
            }
        }
        trainingDayRepo.save(trainingDay);
    }
    
    public void addExerciseToDayFromOption(Long trainingDayId, String exerciseValue, int sets, User user) {
        TrainingDay trainingDay = trainingDayRepo.findById(trainingDayId).orElse(null);
        if (trainingDay == null || exerciseValue == null) return;

        PersonalExercise personalExercise = exerciseServiceHelper.findOrCreatePersonalExercise(exerciseValue, sets, user);

        if (personalExercise != null) {
            trainingDay.addPersonalExercise(personalExercise);
            trainingDayRepo.save(trainingDay);
        }
    }

    @Transactional
    public void updateMeasurements(String email, BodyMeasurementsDto dto) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        BodyMeasurements measurements = new BodyMeasurements();

        if (dto.getGewicht() != null) measurements.setGewicht(dto.getGewicht());
        if (dto.getGroesse() != null) measurements.setGroesse(dto.getGroesse());

        if (dto.getArmLinks() != null) measurements.setArmLinks(new Measurement(dto.getArmLinks()));
        if (dto.getArmRechts() != null) measurements.setArmRechts(new Measurement(dto.getArmRechts()));
        if (dto.getUnterarmLinks() != null) measurements.setUnterarmLinks(new Measurement(dto.getUnterarmLinks()));
        if (dto.getUnterarmRechts() != null) measurements.setUnterarmRechts(new Measurement(dto.getUnterarmRechts()));
        if (dto.getBeinLinks() != null) measurements.setBeinLinks(new Measurement(dto.getBeinLinks()));
        if (dto.getBeinRechts() != null) measurements.setBeinRechts(new Measurement(dto.getBeinRechts()));
        if (dto.getBrust() != null) measurements.setBrust(new Measurement(dto.getBrust()));
        if (dto.getSchulter() != null) measurements.setSchulter(new Measurement(dto.getSchulter()));
        if (dto.getTaille() != null) measurements.setTaille(new Measurement(dto.getTaille()));
        if (dto.getHuefte() != null) measurements.setHuefte(new Measurement(dto.getHuefte()));

        if (measurements.getGroesse() > 0) {
            double heightInMeters = measurements.getGroesse() / 100.0;
            double bmi = measurements.getGewicht() / (heightInMeters * heightInMeters);
            measurements.setBmi(Math.round(bmi * 10.0) / 10.0);
        }

        profile.addMeasurements(measurements);
        profileRepo.save(profile);
    }
}
