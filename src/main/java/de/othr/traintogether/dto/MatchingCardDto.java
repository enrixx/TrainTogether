package de.othr.traintogether.dto;

public class MatchingCardDto {
    private String type; // "USER" or "COURSE"
    private Long id; // ID of the entity (user or course)

    // Exactly one of these will be populated
    private UserDto user;
    private CourseDto course;

    public MatchingCardDto() {}

    public MatchingCardDto(UserDto user) {
        this.type = "USER";
        this.id = user.getId();
        this.user = user;
    }

    public MatchingCardDto(CourseDto course) {
        this.type = "COURSE";
        this.id = course.getId();
        this.course = course;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public CourseDto getCourse() { return course; }
    public void setCourse(CourseDto course) { this.course = course; }
}
