package de.othr.traintogether.api;

import de.othr.traintogether.dto.PersonalExerciseDto;
import de.othr.traintogether.service.PersonalExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @GetMapping
    @Operation(summary = "Get all exercises", description = "Retrieve all personal exercises for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    public ResponseEntity<List<PersonalExerciseDto>> getAllExercises(Authentication authentication) {
        return ResponseEntity.ok(personalExerciseService.getAllExercises(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exercise by ID", description = "Retrieve a specific personal exercise by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the exercise"),
            @ApiResponse(responseCode = "404", description = "Exercise not found or access denied")
    })
    public ResponseEntity<PersonalExerciseDto> getExerciseById(
            @Parameter(description = "ID of the exercise to be retrieved") @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(personalExerciseService.getExerciseById(id, authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Create exercise", description = "Create a new personal exercise. Provide either standardExerciseId OR customExerciseId.")
    @ApiResponse(responseCode = "201", description = "Exercise created successfully")
    public ResponseEntity<PersonalExerciseDto> createExercise(@RequestBody PersonalExerciseDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personalExerciseService.createExercise(dto, authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update exercise", description = "Update an existing personal exercise (e.g. change sets)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exercise updated successfully"),
            @ApiResponse(responseCode = "404", description = "Exercise not found or access denied")
    })
    public ResponseEntity<PersonalExerciseDto> updateExercise(
            @Parameter(description = "ID of the exercise to update") @PathVariable Long id,
            @RequestBody PersonalExerciseDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(personalExerciseService.updateExercise(id, dto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exercise", description = "Delete a personal exercise by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Exercise deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Exercise not found or access denied")
    })
    public ResponseEntity<Void> deleteExercise(
            @Parameter(description = "ID of the exercise to delete") @PathVariable Long id,
            Authentication authentication) {
        personalExerciseService.deleteExercise(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
