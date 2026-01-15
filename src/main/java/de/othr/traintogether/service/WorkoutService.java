package de.othr.traintogether.service;

import de.othr.traintogether.dto.*;
import de.othr.traintogether.model.trainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final UserService userService;
    private final TrainingProfileRepository profileRepo;
    private final TrainingDayRepository dayRepo;
    private final PersonalExerciseRepository exerciseRepo;
    private final TrainingExerciseRepository trainingExerciseRepo;
    private final TrainingProfileService trainingProfileService;
    private final StandardExerciseRepository standardExerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public WorkoutPageDto getWorkoutPageData(String email, Locale locale) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        if (profile == null) {
            throw new IllegalStateException("No training profile found.");
        }

        TrainingSplit activeSplit = profile.getActiveTrainingSplit();
        initializeProfileData(profile);

        List<WorkoutLogResponseDto> todaysWorkoutDto = getTodaysWorkoutDto(user.getId());
        
        Long loggedDayId = null;
        DayOfWeek loggedDayName = null;
        if (!todaysWorkoutDto.isEmpty()) {
            List<TrainingExercise> entities = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(LocalDate.now(), user.getId());
            if (!entities.isEmpty() && entities.get(0).getDay() != null) {
                loggedDayId = entities.get(0).getDay().getId();
                loggedDayName = entities.get(0).getDay().getWeekday();
            }
        }

        List<ExerciseOptionDto> allExercisesDto = trainingProfileService.getAvailableExercises(email, locale);

        return WorkoutPageDto.builder()
                .profile(profile)
                .activeSplit(activeSplit)
                .splits(profile.getSplits())
                .todaysWorkout(todaysWorkoutDto)
                .loggedDayId(loggedDayId)
                .loggedDayName(loggedDayName)
                .allExercises(allExercisesDto)
                .activeSplitId(profile.getActiveTrainingSplitId())
                .currentDayOfWeek(LocalDate.now().getDayOfWeek().name())
                .build();
    }

    private void initializeProfileData(TrainingProfile profile) {
        Hibernate.initialize(profile.getSplits());
        if (profile.getSplits() != null) {
            profile.getSplits().forEach(split -> {
                Hibernate.initialize(split.getDays());
                if (split.getDays() != null) {
                    split.getDays().forEach(day -> Hibernate.initialize(day.getPersonalExercises()));
                }
            });
        }
    }

    private List<WorkoutLogResponseDto> getTodaysWorkoutDto(Long userId) {
        List<TrainingExercise> entities = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(LocalDate.now(), userId);
        return entities.stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkoutLogResponseDto> getWorkoutsByDate(String email, String dateStr) {
        UserDto user = userService.findUserDTOByEmail(email);
        LocalDate date = LocalDate.parse(dateStr);
        List<TrainingExercise> exercises = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        return exercises.stream().map(this::mapToLogResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkoutDaySummaryDto> getWorkoutHistory(String email, int page, int size) {
        UserDto user = userService.findUserDTOByEmail(email);
        List<WorkoutDaySummaryDto> history = new ArrayList<>();

        LocalDate endDate = LocalDate.now().minusDays((long) page * size);
        
        for (int i = 0; i < size; i++) {
            LocalDate currentDate = endDate.minusDays(i);
            List<TrainingExercise> exercises = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(currentDate, user.getId());
            
            List<WorkoutLogResponseDto> exerciseDtos = exercises.stream()
                    .map(this::mapToLogResponse)
                    .collect(Collectors.toList());

            boolean isRestDay = exerciseDtos.isEmpty();

            history.add(new WorkoutDaySummaryDto(
                    currentDate,
                    currentDate.getDayOfWeek().name(),
                    exerciseDtos,
                    isRestDay
            ));
        }
        return history;
    }

    private WorkoutLogResponseDto mapToLogResponse(TrainingExercise ex) {
        return new WorkoutLogResponseDto(
                ex.getPersonalExercise().getId(),
                ex.getPersonalExercise().getName(),
                ex.getSets(),
                ex.getReps(),
                ex.getWeight()
        );
    }

    @Transactional(readOnly = true)
    public List<ProgressDataPointDto> getProgressData(String email, String exerciseValue) {
        UserDto user = userService.findUserDTOByEmail(email);
        List<PersonalExercise> personalExercises = findPersonalExercisesByValue(exerciseValue, user.getId());
        
        if (personalExercises.isEmpty()) return new ArrayList<>();

        List<TrainingExercise> allExercises = new ArrayList<>();
        for (PersonalExercise pe : personalExercises) {
            allExercises.addAll(trainingExerciseRepo.findAllByPersonalExercise_IdAndPersonalExercise_User_IdOrderByDateAsc(pe.getId(), user.getId()));
        }
        allExercises.sort((e1, e2) -> e1.getDate().compareTo(e2.getDate()));

        return allExercises.stream()
                .map(this::mapToDataPoint)
                .filter(dp -> dp != null)
                .collect(Collectors.toList());
    }

    private ProgressDataPointDto mapToDataPoint(TrainingExercise ex) {
        if (ex.getWeight() == null || ex.getWeight().isEmpty()) return null;
        try {
            double maxWeight = Arrays.stream(ex.getWeight().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToDouble(Double::parseDouble)
                    .max().orElse(0.0);
            return maxWeight > 0 ? new ProgressDataPointDto(ex.getDate().toString(), maxWeight) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public void logWorkout(String email, WorkoutLogRequestDto workoutRequest) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        TrainingDay trainingDay = dayRepo.findById(workoutRequest.getTrainingDayId())
                .orElseThrow(() -> new IllegalArgumentException("Training day not found"));

        LocalDate date = LocalDate.now();
        
        List<TrainingExercise> existing = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        if (!existing.isEmpty()) trainingExerciseRepo.deleteAll(existing);

        if (workoutRequest.getExercises() != null) {
            for (WorkoutLogRequestDto.ExerciseLog log : workoutRequest.getExercises()) {
                PersonalExercise pe = resolvePersonalExercise(log, user);
                if (pe != null) {
                    TrainingExercise te = new TrainingExercise(pe, log.getSets(), log.getReps(), log.getWeight(), trainingDay, date);
                    trainingExerciseRepo.save(te);
                }
            }
        }
    }

    private PersonalExercise resolvePersonalExercise(WorkoutLogRequestDto.ExerciseLog log, User user) {
        if (log.getPersonalExerciseId() != null) {
            return exerciseRepo.findById(log.getPersonalExerciseId()).orElse(null);
        }
        if (log.getExerciseValue() != null) {
            return findOrCreatePersonalExercise(log.getExerciseValue(), log.getSets(), user);
        }
        return null;
    }

    private PersonalExercise findOrCreatePersonalExercise(String exerciseValue, int sets, User user) {
        List<PersonalExercise> existing = findPersonalExercisesByValue(exerciseValue, user.getId());
        
        for (PersonalExercise pe : existing) {
            if (pe.getSets() == sets) return pe;
        }

        if (exerciseValue.startsWith("S-")) {
            Long id = Long.parseLong(exerciseValue.substring(2));
            StandardExercise se = standardExerciseRepository.findById(id).orElse(null);
            if (se != null) {
                PersonalExercise pe = new PersonalExercise(se, user);
                pe.setSets(sets);
                return exerciseRepo.save(pe);
            }
        } else if (exerciseValue.startsWith("C-")) {
            Long id = Long.parseLong(exerciseValue.substring(2));
            CustomExercise ce = customExerciseRepository.findById(id).orElse(null);
            if (ce != null) {
                PersonalExercise pe = new PersonalExercise(ce, user);
                pe.setSets(sets);
                return exerciseRepo.save(pe);
            }
        }
        return null;
    }

    private List<PersonalExercise> findPersonalExercisesByValue(String exerciseValue, Long userId) {
        if (exerciseValue == null) return new ArrayList<>();
        if (exerciseValue.startsWith("S-")) {
            Long id = Long.parseLong(exerciseValue.substring(2));
            return exerciseRepo.findByStandardExercise_IdAndUser_Id(id, userId);
        } else if (exerciseValue.startsWith("C-")) {
            Long id = Long.parseLong(exerciseValue.substring(2));
            return exerciseRepo.findByCustomExercise_IdAndUser_Id(id, userId);
        }
        return new ArrayList<>();
    }

    @Transactional
    public void logRestDay(String email, Long trainingDayId) {
        UserDto user = userService.findUserDTOByEmail(email);
        if (!dayRepo.existsById(trainingDayId)) {
            throw new IllegalArgumentException("Training day not found");
        }
        List<TrainingExercise> existing = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(LocalDate.now(), user.getId());
        if (!existing.isEmpty()) trainingExerciseRepo.deleteAll(existing);
    }
}
