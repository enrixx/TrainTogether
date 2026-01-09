package de.othr.traintogether.service;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import de.othr.traintogether.model.TrainingModel.TrainingSplit;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.MatchingActionRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.othr.traintogether.model.TrainingModel.ExerciseName;

@Service
public class MatchingService {

    private final MatchingActionRepository matchingActionRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final TrainingProfileRepository trainingProfileRepository;

    public MatchingService(MatchingActionRepository matchingActionRepository, UserRepository userRepository, FriendshipService friendshipService, TrainingProfileRepository trainingProfileRepository) {
        this.matchingActionRepository = matchingActionRepository;
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.trainingProfileRepository = trainingProfileRepository;
    }

    @Transactional
    public void performAction(User actor, Long targetUserId, MatchingAction.ActionType actionType) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (actor.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot perform action on yourself");
        }

        Optional<MatchingAction> existingAction = matchingActionRepository.findByActorAndTarget(actor, target);
        if (existingAction.isPresent()) {
            MatchingAction action = existingAction.get();
            action.setActionType(actionType);
            action.setActionDate(LocalDateTime.now());
            matchingActionRepository.save(action);
        } else {
            MatchingAction action = new MatchingAction(actor, target, actionType);
            matchingActionRepository.save(action);
        }

        if (actionType == MatchingAction.ActionType.LIKE) {
            checkAndCreateMatch(actor, target);
        }
    }

    private void checkAndCreateMatch(User user1, User user2) {
        Optional<MatchingAction> reverseAction = matchingActionRepository.findByActorAndTarget(user2, user1);
        if (reverseAction.isPresent() && reverseAction.get().getActionType() == MatchingAction.ActionType.LIKE) {
            try {
                // Check if they are already friends
                if (!friendshipService.areFriends(user1, user2)) {
                    friendshipService.createFriendship(user1, user2);
                }
            } catch (Exception e) {
                // Handle potential errors (e.g. already friends, blocked)
            }
        }
    }

    @Transactional(readOnly = true)
    public List<UserDto> findPotentialMatches(User currentUser, Integer minAge, Integer maxAge, String gender, List<String> trainingDays) {
        // Logic to find users:
        // 1. Not the current user
        // 2. Not already friends
        // 3. Not liked by current user
        // 4. Not disliked by current user recently (e.g. last 30 days)
        // 5. Must have a bio
        // 6. Apply filters

        LocalDateTime dislikeCutoff = LocalDateTime.now().minusDays(30);
        List<Long> excludedUserIds = matchingActionRepository.findExcludedUserIds(currentUser, dislikeCutoff);
        excludedUserIds.add(currentUser.getId());
        
        // Also exclude existing friends
        List<User> friends = friendshipService.getFriends(currentUser);
        excludedUserIds.addAll(friends.stream().map(User::getId).toList());

        // Prepare filters
        String genderFilter = (gender != null && !gender.isBlank() && !gender.equals("all")) ? gender : null;

        // minAge -> user must be at least minAge -> birthday <= now - minAge
        LocalDate maxDateForMinAge = (minAge != null) ? LocalDate.now().minusYears(minAge) : null;

        // maxAge -> user must be at most maxAge -> birthday > now - (maxAge + 1) -> birthday >= now - (maxAge + 1) + 1 day
        LocalDate minDateForMaxAge = (maxAge != null) ? LocalDate.now().minusYears(maxAge + 1).plusDays(1) : null;

        Specification<User> spec = (root, query, cb) ->
                cb.and(cb.isNotNull(root.get("bio")), cb.notEqual(root.get("bio"), ""));

        if (genderFilter != null) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("gender")), genderFilter.toLowerCase()));
        }

        if (maxDateForMinAge != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("birthday"), maxDateForMinAge));
        }

        if (minDateForMaxAge != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("birthday"), minDateForMaxAge));
        }

        if (!excludedUserIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.not(root.get("id").in(excludedUserIds)));
        }

        List<User> candidates = userRepository.findAll(spec);

        List<UserDto> result = candidates.stream()
                .map(UserDto::new)
                .collect(Collectors.toList());

        // Populate training days for each user
        for (UserDto dto : result) {
            Optional<TrainingProfile> profileOpt = trainingProfileRepository.findFirstByUserId(dto.getId());
            if (profileOpt.isPresent()) {
                TrainingProfile profile = profileOpt.get();
                TrainingSplit activeSplit = profile.getActiveTraininSplit();
                if (activeSplit != null && activeSplit.getDays() != null) {
                    List<String> days = activeSplit.getDays().stream()
                            .filter(day -> day != null && day.getWeekday() != null)
                            .filter(day -> {
                                boolean hasTrainingExercise = day.getExercises() != null && day.getExercises().stream()
                                        .anyMatch(e -> e != null && e.getExercise() != ExerciseName.RESTDAY);
                                boolean hasPersonalExercise = day.getPersonalExercises() != null && day.getPersonalExercises().stream()
                                        .anyMatch(e -> e != null && e.getName() != null && !e.getName().isBlank() && !ExerciseName.RESTDAY.name().equalsIgnoreCase(e.getName()));
                                return hasTrainingExercise || hasPersonalExercise;
                            })
                            .map(day -> day.getWeekday().name())
                            .collect(Collectors.toList());
                    dto.setTrainingDays(days);
                }
            }
        }

        if (trainingDays != null && !trainingDays.isEmpty()) {
            Set<String> requiredDays = new HashSet<>(trainingDays);

            result = result.stream()
                    .filter(dto -> {
                        if (dto.getTrainingDays() != null && !dto.getTrainingDays().isEmpty()) {
                            // Check if user trains on AT LEAST ONE of the required days
                            for (String day : dto.getTrainingDays()) {
                                if (requiredDays.contains(day)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        return result;
    }
}
