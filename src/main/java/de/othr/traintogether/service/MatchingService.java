package de.othr.traintogether.service;

import de.othr.traintogether.dto.MatchingCardDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.MatchingAction;
import de.othr.traintogether.model.TrainingModel.ExerciseName;
import de.othr.traintogether.model.TrainingModel.TrainingProfile;
import de.othr.traintogether.model.TrainingModel.TrainingSplit;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.CourseRepository;
import de.othr.traintogether.repository.MatchingActionRepository;
import de.othr.traintogether.repository.TrainingProfileRepository;
import de.othr.traintogether.repository.UserRepository;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    private static final int DISLIKE_COOLDOWN_DAYS = 30;
    private static final int COURSE_SKIP_COOLDOWN_DAYS = 1;

    private final MatchingActionRepository matchingActionRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final TrainingProfileRepository trainingProfileRepository;
    private final CourseRepository courseRepository;
    private final de.othr.traintogether.repository.CourseSkipRepository courseSkipRepository;


    public MatchingService(MatchingActionRepository matchingActionRepository, UserRepository userRepository, FriendshipService friendshipService, TrainingProfileRepository trainingProfileRepository, CourseRepository courseRepository, de.othr.traintogether.repository.CourseSkipRepository courseSkipRepository) {
        this.matchingActionRepository = matchingActionRepository;
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.trainingProfileRepository = trainingProfileRepository;
        this.courseRepository = courseRepository;
        this.courseSkipRepository = courseSkipRepository;
    }

    @Transactional
    public boolean performAction(User actor, Long targetUserId, MatchingAction.ActionType actionType) {
        // ... (existing performAction implementation remains unchanged) ...
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
        // ... (existing checkAndCreateMatch implementation remains unchanged) ...
        Optional<MatchingAction> reverseAction = matchingActionRepository.findByActorAndTarget(user2, user1);
        if (reverseAction.isPresent() && reverseAction.get().getActionType() == MatchingAction.ActionType.LIKE) {
            try {
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
    public List<MatchingCardDto> findPotentialMatches(User currentUser, Integer minAge, Integer maxAge, String gender, List<String> trainingDays, List<Long> currentShownIds, int page, int limit) {
        // ... (existing User filtering logic logic remains unchanged) ...
        // 5. Must have a bio
        // 6. Apply filters

        LocalDateTime dislikeCutoff = LocalDateTime.now().minusDays(DISLIKE_COOLDOWN_DAYS);
        List<Long> excludedUserIds = matchingActionRepository.findExcludedUserIds(currentUser, dislikeCutoff);
        logger.debug("Excluded user IDs: {}", excludedUserIds);
        excludedUserIds.add(currentUser.getId());

        List<User> friends = friendshipService.getFriends(currentUser);
        excludedUserIds.addAll(friends.stream().map(User::getId).toList());

        List<User> blockedUsers = friendshipService.getBlockedUsers(currentUser);
        excludedUserIds.addAll(blockedUsers.stream().map(User::getId).toList());

        List<User> blockers = friendshipService.getBlockers(currentUser);
        excludedUserIds.addAll(blockers.stream().map(User::getId).toList());

        if (currentShownIds != null && !currentShownIds.isEmpty()) {
            excludedUserIds.addAll(currentShownIds);
        }

        String genderFilter = (gender != null && !gender.isBlank() && !gender.equals("all")) ? gender : null;
        LocalDate maxDateForMinAge = (minAge != null) ? LocalDate.now().minusYears(minAge) : null;
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

                Predicate activeSplit = cb.equal(profileRoot.get("activeTraininSplitId"), splitJoin.get("id"));
                Predicate userLink = cb.equal(profileRoot.get("userId"), root.get("id"));

                List<java.time.DayOfWeek> requestedDays = trainingDays.stream()
                        .map(d -> java.time.DayOfWeek.valueOf(d.toUpperCase()))
                        .collect(Collectors.toList());
                Predicate dayMatch = dayJoin.get("weekday").in(requestedDays);

                Join<de.othr.traintogether.model.TrainingModel.TrainingDay, de.othr.traintogether.model.TrainingModel.TrainingExercise> teJoin = dayJoin.join("exercises", JoinType.LEFT);
                Predicate teNotRest = cb.notEqual(teJoin.get("exercise"), ExerciseName.RESTDAY);

                Join<de.othr.traintogether.model.TrainingModel.TrainingDay, de.othr.traintogether.model.TrainingModel.PersonalExercise> peJoin = dayJoin.join("personalExercises", JoinType.LEFT);
                Predicate peNotRest = cb.notEqual(cb.upper(peJoin.get("name")), ExerciseName.RESTDAY.name());

                Predicate teValid = cb.and(cb.isNotNull(teJoin.get("id")), teNotRest);
                Predicate peValid = cb.and(cb.isNotNull(peJoin.get("id")), peNotRest);

                return cb.exists(subquery.select(profileRoot.get("userId"))
                        .where(activeSplit, userLink, dayMatch, cb.or(teValid, peValid)));
            });
        }

        Pageable pageable = PageRequest.of(page, limit);
        List<User> candidates = userRepository.findAll(spec, pageable).getContent();
        logger.debug("Found {} candidates from DB (limit: {}) with db-filters", candidates.size(), limit);

        List<UserDto> result = candidates.stream()
                .map(UserDto::new)
                .collect(Collectors.toList());

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

        // MIX IN COURSES
        List<MatchingCardDto> finalCards = new ArrayList<>();

        for (UserDto u : result) {
            finalCards.add(new MatchingCardDto(u));
        }

        // Only if Users are found also add Courses else it will be a Course matching instead of training partners matching
        if (!finalCards.isEmpty()) {
            // Use Sort here to ensure ordering in the DB query
            Pageable coursePage = PageRequest.of(page, 2, Sort.by("dateTime").ascending());

            LocalDateTime cutoff = LocalDateTime.now().minusDays(COURSE_SKIP_COOLDOWN_DAYS);
            List<Long> skippedCourseIds = courseSkipRepository.findSkippedCourseIdsByUser(currentUser, cutoff);

            Specification<de.othr.traintogether.model.Course> courseSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                // 1. Future courses
                predicates.add(cb.greaterThan(root.get("dateTime"), LocalDateTime.now()));

                // 2. Exclude skipped
                if (!skippedCourseIds.isEmpty()) {
                    predicates.add(cb.not(root.get("id").in(skippedCourseIds)));
                }

                // 3. Not Trainer (Owner)
                predicates.add(cb.notEqual(root.get("trainer").get("id"), currentUser.getId()));

                // 4. Not Joined (User is not a participant)
                predicates.add(cb.isNotMember(currentUser, root.get("participants")));

                // 5. Not Full
                predicates.add(cb.lessThan(cb.size(root.get("participants")), root.get("maxParticipants")));

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // Fetch courses that match ALL criteria directly from DB
            List<de.othr.traintogether.model.Course> courses = courseRepository.findAll(courseSpec, coursePage).getContent();

            for (de.othr.traintogether.model.Course c : courses) {
                finalCards.add(new MatchingCardDto(new de.othr.traintogether.dto.CourseDto(c)));
            }
        }

        Collections.shuffle(finalCards, new Random(187));

        return finalCards;
    }
}
