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
    public List<ExerciseOptionDto> getAvailableExercises(String email) {
        UserDto user = userService.findUserDTOByEmail(email);
        List<ExerciseOptionDto> options = new ArrayList<>();

        // Add Standard Exercises
        List<StandardExercise> standardExercises = standardExerciseRepository.findAll();
        for (StandardExercise ex : standardExercises) {
            options.add(new ExerciseOptionDto("S-" + ex.getId(), ex.getNameDe(), "STANDARD"));
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

        PersonalExercise personalExercise = null;

        if (exerciseValue.startsWith("S-")) {
            personalExercise = handleStandardExercise(exerciseValue, sets, user);
        } else if (exerciseValue.startsWith("C-")) {
            personalExercise = handleCustomExercise(exerciseValue, sets, user);
        }

        if (personalExercise != null) {
            trainingDay.addPersonalExercise(personalExercise);
            trainingDayRepo.save(trainingDay);
        }
    }

    private PersonalExercise handleStandardExercise(String exerciseValue, int sets, User user) {
        Long standardId = Long.parseLong(exerciseValue.substring(2));
        StandardExercise standardExercise = standardExerciseRepository.findById(standardId).orElse(null);
        if (standardExercise == null) return null;

        List<PersonalExercise> existing = exerciseRepo.findByStandardExercise_IdAndUser_Id(standardId, user.getId());
        return findOrCreatePersonalExercise(existing, sets, user, standardExercise, null);
    }

    private PersonalExercise handleCustomExercise(String exerciseValue, int sets, User user) {
        Long customId = Long.parseLong(exerciseValue.substring(2));
        CustomExercise customExercise = customExerciseRepository.findById(customId).orElse(null);
        if (customExercise == null) return null;

        List<PersonalExercise> existing = exerciseRepo.findByCustomExercise_IdAndUser_Id(customId, user.getId());
        return findOrCreatePersonalExercise(existing, sets, user, null, customExercise);
    }

    private PersonalExercise findOrCreatePersonalExercise(List<PersonalExercise> existing, int sets, User user, StandardExercise se, CustomExercise ce) {
        if (!existing.isEmpty()) {
            for (PersonalExercise pe : existing) {
                if (pe.getSets() == sets) {
                    return pe;
                }
            }
        }
        return createPersonalExercise(sets, user, se, ce);
    }

    private PersonalExercise createPersonalExercise(int sets, User user, StandardExercise se, CustomExercise ce) {
        PersonalExercise pe;
        if (se != null) {
            pe = new PersonalExercise(se, user);
        } else {
            pe = new PersonalExercise(ce, user);
        }
        pe.setSets(sets);
        return exerciseRepo.save(pe);
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
