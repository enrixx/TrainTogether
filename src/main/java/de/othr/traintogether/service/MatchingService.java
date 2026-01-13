package de.othr.traintogether.service;

import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.TrainingModel.ExerciseName;
import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import de.othr.traintogether.model.TrainingModel.TrainingSplit;
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
                    return true;
                }
            } catch (Exception e) {
                // matching with self or blocked user should not happen
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
        // 4. Not disliked by current user recently (e.g. last 30 days)
        // 5. Must have a bio
        // 6. Apply filters

        LocalDateTime dislikeCutoff = LocalDateTime.now().minusDays(30);
        List<Long> excludedUserIds = matchingActionRepository.findExcludedUserIds(currentUser, dislikeCutoff);
        System.out.println(excludedUserIds.toString());
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
                Join<TrainingSplit, de.othr.traintogether.model.TrainingModel.TrainingDay> dayJoin = splitJoin.join("days");

                // Check active split
                Predicate activeSplit = cb.equal(profileRoot.get("activeTraininSplitId"), splitJoin.get("id"));

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
                Join<de.othr.traintogether.model.TrainingModel.TrainingDay, de.othr.traintogether.model.TrainingModel.TrainingExercise> teJoin = dayJoin.join("exercises", JoinType.LEFT);
                Predicate teNotRest = cb.notEqual(teJoin.get("exercise"), ExerciseName.RESTDAY);

                // 2. PersonalExercise != RESTDAY
                Join<de.othr.traintogether.model.TrainingModel.TrainingDay, de.othr.traintogether.model.TrainingModel.PersonalExercise> peJoin = dayJoin.join("personalExercises", JoinType.LEFT);
                Predicate peNotRest = cb.notEqual(cb.upper(peJoin.get("name")), ExerciseName.RESTDAY.name());

                // Combine: Active Split AND User Link AND Day Match AND (Valid TE OR Valid PE)
                // We use Left Joins to check existence.

                Predicate teValid = cb.and(cb.isNotNull(teJoin.get("id")), teNotRest);
                Predicate peValid = cb.and(cb.isNotNull(peJoin.get("id")), peNotRest);

                return cb.exists(subquery.select(profileRoot.get("userId"))
                        .where(activeSplit, userLink, dayMatch, cb.or(teValid, peValid)));
            });
        }

        Pageable pageable = PageRequest.of(page, limit);
        List<User> candidates = userRepository.findAll(spec, pageable).getContent();
        logger.info("Found {} candidates from DB (limit: {}) with db-filters", candidates.size(), limit);

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

        return result;
    }
}
