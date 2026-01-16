package de.othr.traintogether.dto;

import de.othr.traintogether.model.Course;
import java.time.LocalDateTime;

public class CourseDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateTime;
    private int maxParticipants;
    private int currentParticipants;
    private boolean isOutdoors;
    private String gymName;
    private String trainerName;

    public CourseDto(Course course) {
        this.id = course.getId();
        this.name = course.getName();
        this.description = course.getDescription();
        this.dateTime = course.getDateTime();
        this.maxParticipants = course.getMaxParticipants();
        this.currentParticipants = course.getParticipants().size();
        this.isOutdoors = course.isOutdoors();
        if (course.getGym() != null) {
            this.gymName = course.getGym().getName();
        }
        if (course.getTrainer() != null) {
            this.trainerName = course.getTrainer().getFirstName() + " " + course.getTrainer().getLastName();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public boolean isOutdoors() { return isOutdoors; }
    public void setOutdoors(boolean outdoors) { isOutdoors = outdoors; }

    public String getGymName() { return gymName; }
    public void setGymName(String gymName) { this.gymName = gymName; }

    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
}
