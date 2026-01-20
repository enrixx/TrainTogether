package de.othr.traintogether.service;

import de.othr.traintogether.dto.*;
import de.othr.traintogether.model.Course;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GymPageService {

    private final GymService gymService;
    private final UserService userService;
    private final MinioService minioService;
    private final CourseService courseService;
    private final OpenWeatherMapService openWeatherMapService;
    private final GooglePlacesService googlePlacesService;
    private final GymWorkerService gymWorkerService;
    private final de.othr.traintogether.service.chat.ChatRoomService chatRoomService;

    @Transactional
    public GymPageDto getGymPageData(Long gymId, String userEmail) {
        Gym gym = gymService.getById(gymId); // Throws if not found

        boolean canEdit = false;
        User currentUser = null;
        if (userEmail != null) {
            UserDto userDto = userService.findUserDTOByEmail(userEmail);
            canEdit = hasAccessToGym(gym, userDto);
            currentUser = userService.getUserByEmail(userEmail);
        }

        // Fetch Google Reviews & Place ID
        if (gym.getGooglePlaceId() == null) {
            updateGooglePlaceId(gym);
        }

        List<ReviewDto> googleReviews = null;
        if ("NOT_FOUND".equals(gym.getGooglePlaceId())) {
            gym.setGooglePlaceId(null); // Transiently set to null for view
        } else if (gym.getGooglePlaceId() != null) {
            googleReviews = googlePlacesService.fetchReviews(gym.getGooglePlaceId());
        }

        // Fetch Weather
        Map<Long, WeatherDto> courseWeather = new HashMap<>();
        double lat = gym.getLat();
        double lon = gym.getLon();
        if (lat != 0 || lon != 0) {
            for (Course course : gym.getCourses()) {
                try {
                    if (course.isOutdoors()) {
                        long epochSeconds = course.getDateTime().atZone(ZoneId.systemDefault()).toEpochSecond();
                        openWeatherMapService.fetchWeather(lat, lon, epochSeconds).ifPresent(w -> courseWeather.put(course.getId(), w));
                    }
                } catch (Exception e) {
                    // Log error and continue
                }
            }
        }

        return GymPageDto.builder()
                .gym(gym)
                .canEdit(canEdit)
                .googleReviews(googleReviews)
                .courseWeather(courseWeather)
                .currentUser(currentUser)
                .build();
    }

    private void updateGooglePlaceId(Gym gym) {
        Runnable handleNotFound = () -> {
            gym.setGooglePlaceId("NOT_FOUND");
            gymService.update(gym.getId(), gym);
        };

        if (gym.getLat() != 0 && gym.getLon() != 0) {
            googlePlacesService.findPlaceId(gym.getName(), gym.getLat(), gym.getLon())
                    .ifPresentOrElse(placeId -> {
                        gym.setGooglePlaceId(placeId);
                        gymService.update(gym.getId(), gym);
                    }, handleNotFound);
        } else {
            googlePlacesService.findPlaceId(gym.getName(), gym.getCity())
                    .ifPresentOrElse(placeId -> {
                        gym.setGooglePlaceId(placeId);
                        gymService.update(gym.getId(), gym);
                    }, handleNotFound);
        }
    }

    @Transactional
    public void updateGym(Long gymId, Gym updatedGym, MultipartFile bannerImage, String userEmail) {
        Gym existingGym = gymService.getById(gymId);
        UserDto currentUser = userService.findUserDTOByEmail(userEmail);

        if (!hasAccessToGym(existingGym, currentUser)) {
            throw new SecurityException("Access Denied");
        }

        if (bannerImage != null && !bannerImage.isEmpty()) {
            if (existingGym.getBannerImageUrl() != null && !existingGym.getBannerImageUrl().isEmpty()) {
                minioService.deleteGymBanner(existingGym.getBannerImageUrl());
            }
            String newBannerUrl = minioService.uploadGymBanner(bannerImage, existingGym.getId());
            existingGym.setBannerImageUrl(newBannerUrl);
        }

        boolean locationChanged = !existingGym.getName().equals(updatedGym.getName())
                || !existingGym.getAddress().equals(updatedGym.getAddress())
                || !existingGym.getCity().equals(updatedGym.getCity())
                || !existingGym.getPostalCode().equals(updatedGym.getPostalCode());

        existingGym.setName(updatedGym.getName());
        existingGym.setAddress(updatedGym.getAddress());
        existingGym.setCity(updatedGym.getCity());
        existingGym.setPostalCode(updatedGym.getPostalCode());
        existingGym.setPhoneNumber(updatedGym.getPhoneNumber());
        existingGym.setDescription(updatedGym.getDescription());
        existingGym.setBannerText(updatedGym.getBannerText());
        existingGym.setBannerTextColor(updatedGym.getBannerTextColor());

        if (locationChanged) {
            existingGym.setGooglePlaceId(null);
        }

        gymService.update(gymId, existingGym);
    }

    @Transactional
    public void createCourse(Long gymId, String name, String description, LocalDateTime dateTime, int maxParticipants, boolean isOutdoors, String userEmail) {
        Gym gym = gymService.getById(gymId);
        UserDto currentUser = userService.findUserDTOByEmail(userEmail);

        if (!hasAccessToGym(gym, currentUser)) {
            throw new SecurityException("Access Denied");
        }

        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        course.setDateTime(dateTime);
        course.setMaxParticipants(maxParticipants);
        course.setOutdoors(isOutdoors);
        course.setTrainer(userService.getUserByEmail(userEmail));

        courseService.createCourse(course, gym);
    }

    @Transactional
    public void joinCourse(Long courseId, String userEmail) {
        User user = userService.getUserByEmail(userEmail);
        courseService.joinCourse(courseId, user);
    }

    @Transactional
    public void leaveCourse(Long courseId, String userEmail) {
        User user = userService.getUserByEmail(userEmail);
        courseService.leaveCourse(courseId, user);
    }

    @Transactional
    public Long openCourseChat(Long courseId, String userEmail) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        if (courseOpt.isPresent()) {
            Course course = courseOpt.get();
            User user = userService.getUserByEmail(userEmail);
            
            // Check if user is participant or trainer/owner/worker
            boolean isParticipant = course.getParticipants().contains(user);
            boolean isTrainer = course.getTrainer().equals(user);
            // We could also check gym owner/worker rights here if needed
            
            if (isParticipant || isTrainer) {
                if (course.getChatRoomId() != null) {
                    ChatRole role = isTrainer ? ChatRole.ADMIN : ChatRole.READ_ONLY;
                    chatRoomService.addUserToGroup(course.getChatRoomId(), userEmail, role);
                    return course.getChatRoomId();
                }
            }
        }
        return null;
    }

    @Transactional
    public void deleteCourse(Long gymId, Long courseId, String userEmail) {
        Gym gym = gymService.getById(gymId);
        UserDto currentUser = userService.findUserDTOByEmail(userEmail);

        if (!hasAccessToGym(gym, currentUser)) {
            throw new SecurityException("Access Denied");
        }

        // Explicitly remove from gym's list to ensure consistency in current transaction/session
        gym.getCourses().removeIf(c -> c.getId().equals(courseId));

        courseService.deleteCourse(courseId);
    }

    @Transactional
    public void createWorker(Long gymId, CreateGymWorkerDto workerDto, String userEmail) {
        User owner = userService.getUserByEmail(userEmail);
        // gymWorkerService checks ownership internally
        gymWorkerService.createGymWorker(
                workerDto.getEmail(),
                workerDto.getPassword(),
                workerDto.getFirstName(),
                workerDto.getLastName(),
                gymId,
                owner
        );
    }

    @Transactional
    public void deleteWorker(Long gymId, Long workerId, String userEmail) {
        User owner = userService.getUserByEmail(userEmail);
        if (!gymService.isOwner(gymId, owner.getId())) {
            throw new SecurityException("Access Denied");
        }
        gymWorkerService.deleteById(workerId);
    }

    public boolean hasAccessToGym(Gym gym, UserDto currentUser) {
        if (gym.getOwner() != null && gym.getOwner().getId().equals(currentUser.getId())) {
            return true;
        }
        // Check if user is a worker for this gym
        return gymWorkerService.findByUserId(currentUser.getId())
                .map(worker -> worker.getGym().getId().equals(gym.getId()))
                .orElse(false);
    }
}
