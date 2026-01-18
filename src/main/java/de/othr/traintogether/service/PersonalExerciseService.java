package de.othr.traintogether.service;

import de.othr.traintogether.dto.PersonalExerciseDto;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.TrainingModel.CustomExercise;
import de.othr.traintogether.model.TrainingModel.PersonalExercise;
import de.othr.traintogether.model.TrainingModel.StandardExercise;
import de.othr.traintogether.repository.CustomExerciseRepository;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.StandardExerciseRepository;
import de.othr.traintogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalExerciseService {

    private final PersonalExerciseRepository personalExerciseRepository;
    private final UserRepository userRepository;
    private final StandardExerciseRepository standardExerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;

    @Transactional(readOnly = true)
    public List<PersonalExerciseDto> getAllExercises(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return personalExerciseRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PersonalExerciseDto getExerciseById(Long id, String email) {
        PersonalExercise exercise = personalExerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        
        if (!exercise.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied");
        }
        
        return mapToDto(exercise);
    }

    @Transactional
    public PersonalExerciseDto createExercise(PersonalExerciseDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PersonalExercise exercise = new PersonalExercise();
        exercise.setUser(user);
        exercise.setSets(dto.getSets());

        if (dto.getStandardExerciseId() != null) {
            StandardExercise standardExercise = standardExerciseRepository.findById(dto.getStandardExerciseId())
                    .orElseThrow(() -> new RuntimeException("Standard exercise not found"));
            exercise.setStandardExercise(standardExercise);
        } else if (dto.getCustomExerciseId() != null) {
            CustomExercise customExercise = customExerciseRepository.findById(dto.getCustomExerciseId())
                    .orElseThrow(() -> new RuntimeException("Custom exercise not found"));
            exercise.setCustomExercise(customExercise);
        } else {
            throw new IllegalArgumentException("Either standardExerciseId or customExerciseId must be provided");
        }

        PersonalExercise saved = personalExerciseRepository.save(exercise);
        return mapToDto(saved);
    }

    @Transactional
    public PersonalExerciseDto updateExercise(Long id, PersonalExerciseDto dto, String email) {
        PersonalExercise exercise = personalExerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        if (!exercise.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied");
        }

        exercise.setSets(dto.getSets());
        
        PersonalExercise saved = personalExerciseRepository.save(exercise);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteExercise(Long id, String email) {
        PersonalExercise exercise = personalExerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        if (!exercise.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied");
        }

        personalExerciseRepository.delete(exercise);
    }

    private PersonalExerciseDto mapToDto(PersonalExercise entity) {
        PersonalExerciseDto dto = new PersonalExerciseDto();
        dto.setId(entity.getId());
        dto.setSets(entity.getSets());
        if (entity.getStandardExercise() != null) {
            dto.setStandardExerciseId(entity.getStandardExercise().getId());
            dto.setName(entity.getStandardExercise().getNameEn()); // Defaulting to EN for API
        } else if (entity.getCustomExercise() != null) {
            dto.setCustomExerciseId(entity.getCustomExercise().getId());
            dto.setName(entity.getCustomExercise().getName());
        }
        return dto;
    }
}
