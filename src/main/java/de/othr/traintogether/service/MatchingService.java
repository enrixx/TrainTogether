package de.othr.traintogether.service;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.trainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.MatchingActionRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.UserRepository;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);

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
    public boolean performAction(User actor, Long targetUserId, MatchingAction.ActionType actionType) {
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
            return checkAndCreateMatch(actor, target);
        }
        return false;
    }

    private boolean checkAndCreateMatch(User user1, User user2) {
        Optional<MatchingAction> reverseAction = matchingActionRepository.findByActorAndTarget(user2, user1);
        if (reverseAction.isPresent() && reverseAction.get().getActionType() == MatchingAction.ActionType.LIKE) {
            try {
                // Check if they are already friends
                if (!friendshipService.areFriends(user1, user2)) {
                    friendshipService.createFriendship(user1, user2);
                    logger.info("Created friendship match between user {} and user {}", user1.getId(), user2.getId());
                    return true;
                }
            } catch (IllegalArgumentException e) {
                logger.error("Failed to create friendship match between user {} and user {}: {}",
                    user1.getId(), user2.getId(), e.getMessage(), e);
                return false;
            } catch (IllegalStateException e) {
                logger.warn("Cannot create friendship match between user {} and user {} - blocked or invalid state: {}",
                    user1.getId(), user2.getId(), e.getMessage());
                return false;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findPotentialMatches(User currentUser, Integer minAge, Integer maxAge, String gender, List<String> trainingDays, List<Long> currentShownIds, int page, int limit) {
        // Logic to find users:
        // 1. Not the current user
        // 2. Not already friends
        // 3. Not liked by current user
        // 4. Not disliked by current user recently (last 30 days)
        // 5. Must have a bio
        // 6. Apply filters

        LocalDateTime dislikeCutoff = LocalDateTime.now().minusDays(30);
        List<Long> excludedUserIds = matchingActionRepository.findExcludedUserIds(currentUser, dislikeCutoff);
        logger.debug("Excluded user IDs: {}", excludedUserIds);
        excludedUserIds.add(currentUser.getId());

        // Also exclude existing friends
        List<User> friends = friendshipService.getFriends(currentUser);
        excludedUserIds.addAll(friends.stream().map(User::getId).toList());

        // Also exclude blocked users (both ways)
        List<User> blockedUsers = friendshipService.getBlockedUsers(currentUser);
        excludedUserIds.addAll(blockedUsers.stream().map(User::getId).toList());

        List<User> blockers = friendshipService.getBlockers(currentUser);
        excludedUserIds.addAll(blockers.stream().map(User::getId).toList());

        // Exclude users currently shown on the client
        if (currentShownIds != null && !currentShownIds.isEmpty()) {
            excludedUserIds.addAll(currentShownIds);
        }

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

        if (trainingDays != null && !trainingDays.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<TrainingProfile> profileRoot = subquery.from(TrainingProfile.class);
                Join<TrainingProfile, TrainingSplit> splitJoin = profileRoot.join("splits");
                Join<TrainingSplit, TrainingDay> dayJoin = splitJoin.join("days");

                // Check active split
                Predicate activeSplit = cb.equal(profileRoot.get("activeTrainingSplitId"), splitJoin.get("id"));

                // Check user link
                Predicate userLink = cb.equal(profileRoot.get("userId"), root.get("id"));

                // Check weekdays
                List<java.time.DayOfWeek> requestedDays = trainingDays.stream()
                        .map(d -> java.time.DayOfWeek.valueOf(d.toUpperCase()))
                        .collect(Collectors.toList());
                Predicate dayMatch = dayJoin.get("weekday").in(requestedDays);

                // Check Not RESTDAY
                // We want to ensure that the matching day has at least one real exercise (not just "RESTDAY" or empty)

                // 1. TrainingExercise != RESTDAY
                Join<TrainingDay, TrainingExercise> teJoin = dayJoin.join("exercises", JoinType.LEFT);
                // Assuming TrainingExercise has an enum or similar, but here we check if it exists.
                // If TrainingExercise is used, it's usually not a rest day unless explicitly marked.
                // But wait, TrainingExercise is deprecated/old model? No, it's used for logging.
                // The profile uses PersonalExercise.
                
                // Let's check PersonalExercise
                Join<TrainingDay, PersonalExercise> peJoin = dayJoin.join("personalExercises", JoinType.LEFT);
                
                // We just check if there is ANY personal exercise assigned to that day.
                // If the list is empty, it's a rest day.
                Predicate peValid = cb.isNotNull(peJoin.get("id"));

                return cb.exists(subquery.select(profileRoot.get("userId"))
                        .where(activeSplit, userLink, dayMatch, peValid));
            });
        }

        Pageable pageable = PageRequest.of(page, limit);
        List<User> candidates = userRepository.findAll(spec, pageable).getContent(); // JpaSpecificationExecutor has the method with pagination
        logger.debug("Found {} candidates from DB (limit: {}) with db-filters", candidates.size(), limit);

        List<UserDto> result = candidates.stream()
                .map(UserDto::new)
                .collect(Collectors.toList());

        // Populate training days for each user
        for (UserDto dto : result) {
            Optional<TrainingProfile> profileOpt = trainingProfileRepository.findFirstByUserId(dto.getId());
            if (profileOpt.isPresent()) {
                TrainingProfile profile = profileOpt.get();
                TrainingSplit activeSplit = profile.getActiveTrainingSplit();
                if (activeSplit != null && activeSplit.getDays() != null) {
                    List<String> days = activeSplit.getDays().stream()
                            .filter(day -> day != null && day.getWeekday() != null)
                            .filter(day -> day.getPersonalExercises() != null && !day.getPersonalExercises().isEmpty())
                            .map(day -> day.getWeekday().name())
                            .collect(Collectors.toList());
                    dto.setTrainingDays(days);
                }
            }
        }

        return result;
    }
}
