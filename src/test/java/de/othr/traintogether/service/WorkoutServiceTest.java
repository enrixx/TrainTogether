package de.othr.traintogether.service;

import de.othr.traintogether.dto.*;
import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private TrainingProfileRepository profileRepo;
    @Mock
    private TrainingDayRepository dayRepo;
    @Mock
    private PersonalExerciseRepository exerciseRepo;
    @Mock
    private TrainingExerciseRepository trainingExerciseRepo;
    @Mock
    private TrainingProfileService trainingProfileService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExerciseServiceHelper exerciseServiceHelper;

    @InjectMocks
    private WorkoutService workoutService;

    @Test
    void getWorkoutPageData_shouldReturnDto_whenProfileExists() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        TrainingProfile profile = new TrainingProfile(1L);
        TrainingSplit split = new TrainingSplit("Split");
        profile.addSplit(split);
        profile.setActiveTrainingSplitId(split.getId());

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findByUserId(1L)).thenReturn(profile);
        when(trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(any(LocalDate.class), eq(1L)))
                .thenReturn(Collections.emptyList());
        when(trainingProfileService.getAvailableExercises(eq(email), any())).thenReturn(new ArrayList<>());

        WorkoutPageDto result = workoutService.getWorkoutPageData(email, java.util.Locale.ENGLISH);

        assertNotNull(result);
        assertEquals(profile, result.getProfile());
        assertEquals(split, result.getActiveSplit());
    }

    @Test
    void getWorkoutPageData_shouldThrowException_whenProfileMissing() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findByUserId(1L)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> workoutService.getWorkoutPageData(email, java.util.Locale.ENGLISH));
    }

    @Test
    void logWorkout_shouldSaveExercises() {
        String email = "test@test.com";
        User user = new User();
        user.setId(1L);
        Long dayId = 10L;
        TrainingDay day = new TrainingDay();
        day.setId(dayId);

        WorkoutLogRequestDto request = new WorkoutLogRequestDto();
        request.setTrainingDayId(dayId);
        WorkoutLogRequestDto.ExerciseLog log = new WorkoutLogRequestDto.ExerciseLog();
        log.setPersonalExerciseId(100L);
        log.setSets(3);
        log.setReps("10,10,10");
        log.setWeight("50,50,50");
        request.setExercises(List.of(log));

        PersonalExercise pe = new PersonalExercise();
        pe.setId(100L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(dayRepo.findById(dayId)).thenReturn(Optional.of(day));
        when(trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(any(LocalDate.class), eq(1L)))
                .thenReturn(Collections.emptyList());
        when(exerciseRepo.findById(100L)).thenReturn(Optional.of(pe));

        workoutService.logWorkout(email, request);

        verify(trainingExerciseRepo).save(any(TrainingExercise.class));
    }

    @Test
    void logWorkout_shouldDeleteExistingExercisesForToday() {
        String email = "test@test.com";
        User user = new User();
        user.setId(1L);
        Long dayId = 10L;
        TrainingDay day = new TrainingDay();
        day.setId(dayId);

        WorkoutLogRequestDto request = new WorkoutLogRequestDto();
        request.setTrainingDayId(dayId);
        request.setExercises(new ArrayList<>()); // No new exercises

        TrainingExercise existingEx = new TrainingExercise();
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(dayRepo.findById(dayId)).thenReturn(Optional.of(day));
        when(trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(any(LocalDate.class), eq(1L)))
                .thenReturn(List.of(existingEx));

        workoutService.logWorkout(email, request);

        verify(trainingExerciseRepo).deleteAll(anyList());
        verify(trainingExerciseRepo, never()).save(any(TrainingExercise.class));
    }

    @Test
    void logRestDay_shouldDeleteAllExercises() {
        String email = "test@test.com";
        User user = new User();
        user.setId(1L);
        Long dayId = 10L;

        TrainingExercise existingEx = new TrainingExercise();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(dayRepo.existsById(dayId)).thenReturn(true);
        when(dayRepo.findById(dayId)).thenReturn(Optional.of(new TrainingDay())); // Mock findById as it is called
        when(trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(any(LocalDate.class), eq(1L)))
                .thenReturn(List.of(existingEx));

        workoutService.logRestDay(email, dayId);

        verify(trainingExerciseRepo).deleteAll(anyList());
    }

    @Test
    void getWorkoutHistory_shouldIdentifyRestDays() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        // Mock empty list for today (Rest Day)
        when(trainingExerciseRepo.findByDateAndPersonalExercise_User_Id(any(LocalDate.class), eq(1L)))
                .thenReturn(Collections.emptyList());

        List<WorkoutDaySummaryDto> history = workoutService.getWorkoutHistory(email, 0, 1);

        assertEquals(1, history.size());
        assertTrue(history.get(0).isRestDay());
    }

    @Test
    void getProgressData_shouldReturnDataPoints() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        String exerciseValue = "S-10";
        
        PersonalExercise pe = new PersonalExercise();
        pe.setId(100L);
        
        TrainingExercise te1 = new TrainingExercise();
        te1.setDate(LocalDate.now().minusDays(1));
        te1.setWeight("50,50,50"); // Max 50
        
        TrainingExercise te2 = new TrainingExercise();
        te2.setDate(LocalDate.now());
        te2.setWeight("60,55,55"); // Max 60

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(exerciseServiceHelper.findPersonalExercisesByValue(exerciseValue, 1L)).thenReturn(List.of(pe));
        when(trainingExerciseRepo.findAllByPersonalExercise_IdAndPersonalExercise_User_IdOrderByDateAsc(100L, 1L))
                .thenReturn(List.of(te1, te2));

        List<ProgressDataPointDto> result = workoutService.getProgressData(email, exerciseValue);

        assertEquals(2, result.size());
        assertEquals(50.0, result.get(0).getWeight());
        assertEquals(60.0, result.get(1).getWeight());
    }
}
