package de.othr.traintogether.service;

import de.othr.traintogether.model.Course;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import de.othr.traintogether.model.chat.ChatRole;
import de.othr.traintogether.repository.CourseRepository;
import de.othr.traintogether.service.chat.ChatRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChatRoomService chatRoomService;

    public CourseService(CourseRepository courseRepository, ChatRoomService chatRoomService) {
        this.courseRepository = courseRepository;
        this.chatRoomService = chatRoomService;
    }

    @Transactional
    public Course createCourse(Course course, Gym gym) {
        course.setGym(gym);
        
        // Create a read-only group chat for the course
        // Trainer is the owner/admin of the chat
        if (course.getTrainer() != null) {
            String chatName = course.getName();
            if (course.getTrainer().getFirstName() != null) {
                chatName += " (" + course.getTrainer().getFirstName();
                if (course.getTrainer().getLastName() != null) {
                    chatName += " " + course.getTrainer().getLastName();
                }
                chatName += ")";
            }

            Long chatRoomId = chatRoomService.createReadOnlyGroup(
                chatName, 
                null, // No picture initially
                Collections.emptySet(), // No initial members besides owner
                course.getTrainer().getEmail()
            );
            course.setChatRoomId(chatRoomId);
        }
        
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<Course> findByGymId(Long gymId) {
        return courseRepository.findByGymIdOrderByDateTimeAsc(gymId);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isPresent()) {
            Course course = courseOpt.get();
            // Delete the associated chat room
            if (course.getChatRoomId() != null) {
                chatRoomService.deleteChatRoom(course.getChatRoomId());
            }
            courseRepository.delete(course);
        }
    }

    @Transactional
    public void joinCourse(Long courseId, User user) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isPresent()) {
            Course course = courseOpt.get();
            if (course.getParticipants().size() < course.getMaxParticipants()) {
                course.getParticipants().add(user);
                courseRepository.save(course);
                
                // Add user to the course chat room
                if (course.getChatRoomId() != null) {
                    chatRoomService.addUserToGroup(course.getChatRoomId(), user.getEmail(), ChatRole.READ_ONLY);
                }
            } else {
                throw new RuntimeException("Course is full");
            }
        }
    }

    @Transactional
    public void leaveCourse(Long courseId, User user) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isPresent()) {
            Course course = courseOpt.get();
            if (course.getParticipants().remove(user)) {
                courseRepository.save(course);
                
                // Remove user from the course chat room
                if (course.getChatRoomId() != null) {
                    chatRoomService.removeUserFromRoom(course.getChatRoomId(), user.getEmail());
                }
            }
        }
    }
}
