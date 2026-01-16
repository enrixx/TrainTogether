package de.othr.traintogether.service;

import de.othr.traintogether.model.User;
import de.othr.traintogether.model.trainingModel.CustomExercise;
import de.othr.traintogether.model.trainingModel.PersonalExercise;
import de.othr.traintogether.model.trainingModel.StandardExercise;
import de.othr.traintogether.repository.CustomExerciseRepository;
import de.othr.traintogether.repository.PersonalExerciseRepository;
import de.othr.traintogether.repository.StandardExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExerciseServiceHelper {

    private final PersonalExerciseRepository exerciseRepo;
    private final StandardExerciseRepository standardExerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;

    public PersonalExercise findOrCreatePersonalExercise(String exerciseValue, int sets, User user) {
        if (exerciseValue == null || !exerciseValue.matches("^[SC]-\\d+$")) {
            return null;
        }

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

    public List<PersonalExercise> findPersonalExercisesByValue(String exerciseValue, Long userId) {
        if (exerciseValue == null || !exerciseValue.matches("^[SC]-\\d+$")) {
            return new ArrayList<>();
        }
        if (exerciseValue.startsWith("S-")) {
            Long id = Long.parseLong(exerciseValue.substring(2));
            return exerciseRepo.findByStandardExercise_IdAndUser_Id(id, userId);
        } else if (exerciseValue.startsWith("C-")) {
            Long id = Long.parseLong(exerciseValue.substring(2));
            return exerciseRepo.findByCustomExercise_IdAndUser_Id(id, userId);
        }
        return new ArrayList<>();
    }
}
