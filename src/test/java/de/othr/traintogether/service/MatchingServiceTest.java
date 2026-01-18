package de.othr.traintogether.service;

import de.othr.traintogether.dto.MatchingCardDto;
import de.othr.traintogether.model.Course;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.CourseRepository;
import de.othr.traintogether.repository.MatchingActionRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.repository.CourseSkipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MatchingActionRepository matchingActionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private TrainingProfileRepository trainingProfileRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSkipRepository courseSkipRepository;

    @InjectMocks
    private MatchingService matchingService;

    // --- performAction Tests ---

    @Test
    void performAction_shouldThrowException_whenTargetUserNotFound() {
        User actor = new User();
        actor.setId(1L);
        Long targetId = 99L;

        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                matchingService.performAction(actor, targetId, MatchingAction.ActionType.LIKE)
        );
    }

    @Test
    void performAction_shouldThrowException_whenActingOnSelf() {
        User actor = new User();
        actor.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        assertThrows(IllegalArgumentException.class, () ->
                matchingService.performAction(actor, 1L, MatchingAction.ActionType.LIKE)
        );
    }

    @Test
    void performAction_shouldCreateNewAction_whenNoneExists() {
        User actor = new User();
        actor.setId(1L);
        User target = new User();
        target.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(matchingActionRepository.findByActorAndTarget(actor, target)).thenReturn(Optional.empty());

        matchingService.performAction(actor, 2L, MatchingAction.ActionType.DISLIKE);

        verify(matchingActionRepository).save(any(MatchingAction.class));
    }

    @Test
    void performAction_shouldUpdateExistingAction_whenItExists() {
        User actor = new User();
        actor.setId(1L);
        User target = new User();
        target.setId(2L);

        MatchingAction existingAction = new MatchingAction(actor, target, MatchingAction.ActionType.LIKE);

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(matchingActionRepository.findByActorAndTarget(actor, target)).thenReturn(Optional.of(existingAction));

        matchingService.performAction(actor, 2L, MatchingAction.ActionType.DISLIKE);

        verify(matchingActionRepository).save(existingAction);
        assertEquals(MatchingAction.ActionType.DISLIKE, existingAction.getActionType());
    }

    @Test
    void performAction_shouldReturnFalse_whenActionIsDislike() {
        User actor = new User();
        actor.setId(1L);
        User target = new User();
        target.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(matchingActionRepository.findByActorAndTarget(actor, target)).thenReturn(Optional.empty());

        boolean result = matchingService.performAction(actor, 2L, MatchingAction.ActionType.DISLIKE);

        assertFalse(result);
        verify(friendshipService, never()).createFriendship(any(), any());
    }

    @Test
    void performAction_shouldCreateMatch_whenReverseLikeExists() {
        User actor = new User();
        actor.setId(1L);
        User target = new User();
        target.setId(2L);

        // Mock actor finding target
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        // Mock no existing action from actor -> target
        when(matchingActionRepository.findByActorAndTarget(actor, target)).thenReturn(Optional.empty());

        // Mock reverse action (Target LIKED Actor)
        MatchingAction reverseAction = new MatchingAction(target, actor, MatchingAction.ActionType.LIKE);
        when(matchingActionRepository.findByActorAndTarget(target, actor)).thenReturn(Optional.of(reverseAction));

        // Mock they are not friends yet
        when(friendshipService.areFriends(actor, target)).thenReturn(false);

        boolean isMatch = matchingService.performAction(actor, 2L, MatchingAction.ActionType.LIKE);

        assertTrue(isMatch);
        verify(friendshipService).createFriendship(actor, target);
    }

    @Test
    void performAction_shouldNotCreateMatch_whenReverseLikeMissing() {
        User actor = new User();
        actor.setId(1L);
        User target = new User();
        target.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(matchingActionRepository.findByActorAndTarget(actor, target)).thenReturn(Optional.empty());
        // Reverse action is empty
        when(matchingActionRepository.findByActorAndTarget(target, actor)).thenReturn(Optional.empty());

        boolean isMatch = matchingService.performAction(actor, 2L, MatchingAction.ActionType.LIKE);

        assertFalse(isMatch);
        verify(friendshipService, never()).createFriendship(any(), any());
    }

    // --- findPotentialMatches Tests ---

    @Test
    void findPotentialMatches_shouldReturnMixedList_whenUsersAndCoursesFound() {
        User currentUser = new User();
        currentUser.setId(1L);

        // 1. Mock Exclusions
        when(matchingActionRepository.findExcludedUserIds(eq(currentUser), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(friendshipService.getFriends(currentUser)).thenReturn(Collections.emptyList());
        when(friendshipService.getBlockedUsers(currentUser)).thenReturn(Collections.emptyList());
        when(friendshipService.getBlockers(currentUser)).thenReturn(Collections.emptyList());

        // 2. Mock User Finding
        User activeUser = new User();
        activeUser.setId(10L);
        // Note: Actual Spec logic requires Integration Test. We create a relaxed mock for Unit Test.
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeUser)));

        // 3. Mock Training Profile (to populate training days)
        when(trainingProfileRepository.findFirstByUserId(10L)).thenReturn(Optional.empty());

        // 4. Mock Course Finding
        when(courseSkipRepository.findSkippedCourseIdsByUser(eq(currentUser), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        Course course = new Course();
        course.setId(20L);
        course.setParticipants(Collections.emptySet());
        // Mock trainer for course
        User trainer = new User();
        trainer.setId(30L);
        course.setTrainer(trainer);

        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(course)));

        // Execution
        List<MatchingCardDto> result = matchingService.findPotentialMatches(
                currentUser, 18, 99, "male", null, null, 0, 5
        );

        // Verification
        assertNotNull(result);
        // Expecting 1 User + 1 Course = 2 cards
        assertEquals(2, result.size());

        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(courseRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findPotentialMatches_shouldNotReturnCourses_IfNoUsersFound() {
        // Logic check: "Only if Users are found also add Courses"
        User currentUser = new User();
        currentUser.setId(1L);

        when(matchingActionRepository.findExcludedUserIds(any(), any())).thenReturn(new ArrayList<>());
        when(friendshipService.getFriends(any())).thenReturn(Collections.emptyList());
        when(friendshipService.getBlockedUsers(any())).thenReturn(Collections.emptyList());
        when(friendshipService.getBlockers(any())).thenReturn(Collections.emptyList());

        // Mock NO users found
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        List<MatchingCardDto> result = matchingService.findPotentialMatches(
                currentUser, null, null, null, null, null, 0, 10
        );

        assertTrue(result.isEmpty());
        verify(courseRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
