package de.othr.traintogether.service;

import de.othr.traintogether.dto.BodyMeasurementsDto;
import de.othr.traintogether.dto.ExerciseOptionDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingProfileServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private TrainingProfileRepository profileRepo;
    @Mock
    private PersonalExerciseRepository exerciseRepo;
    @Mock
    private TrainingDayRepository trainingDayRepo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StandardExerciseRepository standardExerciseRepository;
    @Mock
    private CustomExerciseRepository customExerciseRepository;
    @Mock
    private ExerciseServiceHelper exerciseServiceHelper;

    @InjectMocks
    private TrainingProfileService trainingProfileService;

    @Test
    void getTrainingProfile_shouldReturnProfile_whenExists() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        TrainingProfile profile = new TrainingProfile(1L);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findFirstByUserId(1L)).thenReturn(Optional.of(profile));

        TrainingProfile result = trainingProfileService.getTrainingProfile(email);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
    }

    @Test
    void getTrainingProfile_shouldReturnNull_whenNotExists() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findFirstByUserId(1L)).thenReturn(Optional.empty());

        TrainingProfile result = trainingProfileService.getTrainingProfile(email);

        assertNull(result);
    }

    @Test
    void getAvailableExercises_shouldReturnStandardAndCustomExercises() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);

        StandardExercise stdEx = new StandardExercise("Bankdrücken", "Bench Press");
        stdEx.setId(10L);
        CustomExercise custEx = new CustomExercise("My Exercise", new User());
        custEx.setId(20L);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(standardExerciseRepository.findAll()).thenReturn(List.of(stdEx));
        when(customExerciseRepository.findAllByCreatedBy_Id(1L)).thenReturn(List.of(custEx));

        List<ExerciseOptionDto> result = trainingProfileService.getAvailableExercises(email, java.util.Locale.ENGLISH);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getValue().equals("S-10") && e.getName().equals("Bench Press")));
        assertTrue(result.stream().anyMatch(e -> e.getValue().equals("C-20") && e.getName().equals("My Exercise")));
    }

    @Test
    void createSplit_shouldAddSplitToProfile() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        TrainingProfile profile = new TrainingProfile(1L);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findFirstByUserId(1L)).thenReturn(Optional.of(profile));

        trainingProfileService.createSplit(email, "New Split");

        verify(profileRepo).save(profile);
        assertEquals(1, profile.getSplits().size());
        assertEquals("New Split", profile.getSplits().get(0).getSplitName());
    }

    @Test
    void deleteSplit_shouldRemoveSplitFromProfile() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        TrainingProfile profile = new TrainingProfile(1L);
        TrainingSplit split = new TrainingSplit("Split 1");
        split.setId(100L);
        profile.addSplit(split);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findByUserId(1L)).thenReturn(profile);

        trainingProfileService.deleteSplit(email, 100L);

        verify(profileRepo).save(profile);
        assertTrue(profile.getSplits().isEmpty());
    }

    @Test
    void addExerciseToDayFromOption_shouldAddExercise() {
        Long dayId = 50L;
        String exerciseValue = "S-10";
        int sets = 3;
        User user = new User();
        user.setId(1L);
        TrainingDay day = new TrainingDay();
        day.setId(dayId);
        PersonalExercise pe = new PersonalExercise();

        when(trainingDayRepo.findById(dayId)).thenReturn(Optional.of(day));
        when(exerciseServiceHelper.findOrCreatePersonalExercise(exerciseValue, sets, user)).thenReturn(pe);

        trainingProfileService.addExerciseToDayFromOption(dayId, exerciseValue, sets, user);

        verify(trainingDayRepo).save(day);
        assertEquals(1, day.getPersonalExercises().size());
        assertEquals(pe, day.getPersonalExercises().get(0));
    }

    @Test
    void updateMeasurements_shouldAddMeasurementsToProfile() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        TrainingProfile profile = new TrainingProfile(1L);
        BodyMeasurementsDto dto = new BodyMeasurementsDto();
        dto.setGewicht(80.0);
        dto.setGroesse(180.0);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findByUserId(1L)).thenReturn(profile);

        trainingProfileService.updateMeasurements(email, dto);

        verify(profileRepo).save(profile);
        assertEquals(1, profile.getMeasurements().size());
        assertEquals(80.0, profile.getMeasurements().get(0).getGewicht());
        // BMI calculation check: 80 / (1.8 * 1.8) = 24.69... -> 24.7
        assertEquals(24.7, profile.getMeasurements().get(0).getBmi());
    }

    @Test
    void createCustomExercises_shouldSaveNewExercises() {
        String email = "test@test.com";
        User user = new User();
        user.setId(1L);
        List<String> names = List.of("Ex 1", "Ex 2");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        trainingProfileService.createCustomExercises(email, names);

        verify(customExerciseRepository, times(2)).save(any(CustomExercise.class));
    }

    @Test
    void deleteCustomExercises_shouldRemoveExercisesAndUsages() {
        String email = "test@test.com";
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        User user = new User();
        user.setId(1L);
        
        Long customExId = 100L;
        CustomExercise customEx = new CustomExercise("My Ex", user);
        customEx.setId(customExId);
        
        TrainingProfile profile = new TrainingProfile(1L);
        TrainingSplit split = new TrainingSplit("Split");
        TrainingDay day = new TrainingDay();
        PersonalExercise pe = new PersonalExercise(customEx, user);
        day.addPersonalExercise(pe);
        split.addDay(day);
        profile.addSplit(split);

        when(userService.findUserDTOByEmail(email)).thenReturn(userDto);
        when(profileRepo.findByUserId(1L)).thenReturn(profile);
        when(customExerciseRepository.findById(customExId)).thenReturn(Optional.of(customEx));
        when(exerciseRepo.findByCustomExercise_IdAndUser_Id(customExId, 1L)).thenReturn(List.of(pe));

        trainingProfileService.deleteCustomExercises(email, List.of(customExId));

        // Verify removal from day
        assertTrue(day.getPersonalExercises().isEmpty());
        // Verify profile save
        verify(profileRepo).save(profile);
        // Verify personal exercises deletion
        verify(exerciseRepo).deleteAll(anyList());
        // Verify custom exercise deletion
        verify(customExerciseRepository).delete(customEx);
    }
}
