package de.othr.traintogether.api;

import de.othr.traintogether.dto.PersonalExerciseDto;
import de.othr.traintogether.service.PersonalExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Personal Exercises", description = "API for managing user's personal exercises")
public class PersonalExerciseController {

    private final PersonalExerciseService personalExerciseService;

    // Get all exercises for the authenticated user
    @GetMapping
    @Operation(summary = "Get all exercises", description = "Retrieve all personal exercises for the authenticated user")
    public ResponseEntity<List<PersonalExerciseDto>> getAllExercises(Authentication authentication) {
        return ResponseEntity.ok(personalExerciseService.getAllExercises(authentication.getName()));
    }

    // Get a specific exercise by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get exercise by ID", description = "Retrieve a specific personal exercise by its ID")
    public ResponseEntity<PersonalExerciseDto> getExerciseById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(personalExerciseService.getExerciseById(id, authentication.getName()));
    }

    // Create a new personal exercise
    @PostMapping
    @Operation(summary = "Create exercise", description = "Create a new personal exercise")
    public ResponseEntity<PersonalExerciseDto> createExercise(@RequestBody PersonalExerciseDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personalExerciseService.createExercise(dto, authentication.getName()));
    }

    // Update an existing personal exercise
    @PutMapping("/{id}")
    @Operation(summary = "Update exercise", description = "Update an existing personal exercise")
    public ResponseEntity<PersonalExerciseDto> updateExercise(@PathVariable Long id, @RequestBody PersonalExerciseDto dto, Authentication authentication) {
        return ResponseEntity.ok(personalExerciseService.updateExercise(id, dto, authentication.getName()));
    }

    // Delete a personal exercise by its ID
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exercise", description = "Delete a personal exercise by its ID")
    public ResponseEntity<Void> deleteExercise(@PathVariable Long id, Authentication authentication) {
        personalExerciseService.deleteExercise(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
