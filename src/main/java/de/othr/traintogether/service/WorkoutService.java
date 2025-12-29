package de.othr.traintogether.service;

import de.othr.traintogether.dto.*;
import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.TrainingDayRepository;
import de.othr.traintogether.repository.TrainingExerciseRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final UserService userService;
    private final TrainingProfileRepository profileRepo;
    private final TrainingDayRepository dayRepo;
    private final PersonalExerciseRepository exerciseRepo;
    private final TrainingExerciseRepository trainingExerciseRepo;

    @Transactional(readOnly = true)
    public WorkoutPageDto getWorkoutPageData(String email) {
        UserDto user = userService.findUserDTOByEmail(email);
        TrainingProfile profile = profileRepo.findByUserId(user.getId());

        if (profile == null) {
            throw new IllegalStateException("No training profile found. Please create one in your profile.");
        }

        TrainingSplit activeSplit = profile.getActiveTraininSplit();
        if (activeSplit == null) {
            throw new IllegalStateException("No active training split found. Please set one in your profile.");
        }

        // Force initialization of lazy collections
        Hibernate.initialize(profile.getSplits());
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

        List<TrainingExercise> todaysWorkoutEntities = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(LocalDate.now(), user.getId());
        List<WorkoutLogResponseDto> todaysWorkoutDto = new ArrayList<>();
        Long loggedDayId = null;
        DayOfWeek loggedDayName = null;

        if (!todaysWorkoutEntities.isEmpty()) {
            todaysWorkoutDto = todaysWorkoutEntities.stream()
                    .map(ex -> new WorkoutLogResponseDto(
                            ex.getPersonalExercise().getId(),
                            ex.getPersonalExercise().getName(),
                            ex.getSets(),
                            ex.getReps(),
                            ex.getWeight()
                    ))
                    .collect(Collectors.toList());

            if (todaysWorkoutEntities.get(0).getDay() != null) {
                loggedDayId = todaysWorkoutEntities.get(0).getDay().getId();
                loggedDayName = todaysWorkoutEntities.get(0).getDay().getWeekday();
            }
        }

        List<PersonalExercise> allExercisesEntities = exerciseRepo.findAllByUserId(user.getId());
        List<PersonalExerciseDto> allExercisesDto = allExercisesEntities.stream()
                .map(ex -> new PersonalExerciseDto(ex.getId(), ex.getName()))
                .collect(Collectors.toList());

        return WorkoutPageDto.builder()
                .profile(profile)
                .activeSplit(activeSplit)
                .splits(profile.getSplits())
                .todaysWorkout(todaysWorkoutDto)
                .loggedDayId(loggedDayId)
                .loggedDayName(loggedDayName)
                .allExercises(allExercisesDto)
                .activeSplitId(profile.getActiveTraininSplitId())
                .currentDayOfWeek(LocalDate.now().getDayOfWeek().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkoutLogResponseDto> getWorkoutsByDate(String email, String dateStr) {
        UserDto user = userService.findUserDTOByEmail(email);
        LocalDate date = LocalDate.parse(dateStr);

        List<TrainingExercise> exercises = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());

        return exercises.stream()
                .map(ex -> new WorkoutLogResponseDto(
                        ex.getPersonalExercise().getId(),
                        ex.getPersonalExercise().getName(),
                        ex.getSets(),
                        ex.getReps(),
                        ex.getWeight()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgressDataPointDto> getProgressData(String email, Long exerciseId) {
        UserDto user = userService.findUserDTOByEmail(email);

        List<TrainingExercise> exercises = trainingExerciseRepo.findAllByPersonalExercise_IdAndPersonalExercise_User_IdOrderByDateAsc(exerciseId, user.getId());

        List<ProgressDataPointDto> dataPoints = new ArrayList<>();

        for (TrainingExercise ex : exercises) {
            if (ex.getWeight() != null && !ex.getWeight().isEmpty()) {
                try {
                    double maxWeight = Arrays.stream(ex.getWeight().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .mapToDouble(Double::parseDouble)
                            .max()
                            .orElse(0.0);

                    if (maxWeight > 0) {
                        dataPoints.add(new ProgressDataPointDto(ex.getDate().toString(), maxWeight));
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed data
                }
            }
        }
        return dataPoints;
    }

    @Transactional
    public void logWorkout(String email, WorkoutLogRequestDto workoutRequest) {
        UserDto user = userService.findUserDTOByEmail(email);

        TrainingDay trainingDay = dayRepo.findById(workoutRequest.getTrainingDayId()).orElse(null);
        if (trainingDay == null) {
            throw new IllegalArgumentException("Training day not found");
        }

        LocalDate date = LocalDate.now();

        List<TrainingExercise> existingWorkout = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        if (!existingWorkout.isEmpty()) {
            trainingExerciseRepo.deleteAll(existingWorkout);
        }

        if (workoutRequest.getExercises() != null) {
            for (WorkoutLogRequestDto.ExerciseLog exerciseLog : workoutRequest.getExercises()) {
                PersonalExercise personalExercise = exerciseRepo.findById(exerciseLog.getPersonalExerciseId()).orElse(null);
                if (personalExercise != null) {
                    TrainingExercise trainingExercise = new TrainingExercise(
                            personalExercise,
                            exerciseLog.getSets(),
                            exerciseLog.getReps(),
                            exerciseLog.getWeight(),
                            trainingDay,
                            date
                    );
                    trainingExerciseRepo.save(trainingExercise);
                }
            }
        }
    }

    @Transactional
    public void logRestDay(String email, Long trainingDayId) {
        UserDto user = userService.findUserDTOByEmail(email);

        TrainingDay trainingDay = dayRepo.findById(trainingDayId).orElse(null);
        if (trainingDay == null) {
            throw new IllegalArgumentException("Training day not found");
        }

        LocalDate date = LocalDate.now();

        List<TrainingExercise> existingWorkout = trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(date, user.getId());
        if (!existingWorkout.isEmpty()) {
            trainingExerciseRepo.deleteAll(existingWorkout);
        }

        PersonalExercise restDayExercise = exerciseRepo.findByNameAndUser_Id("Restday", user.getId())
                .or(() -> exerciseRepo.findByNameAndUser_Id("RESTDAY", user.getId()))
                .orElse(null);

        if (restDayExercise != null) {
            TrainingExercise trainingExercise = new TrainingExercise(
                    restDayExercise,
                    0,
                    "",
                    "",
                    trainingDay,
                    date
            );
            trainingExerciseRepo.save(trainingExercise);
        } else {
            throw new IllegalStateException("Restday exercise not found");
        }
    }
}
