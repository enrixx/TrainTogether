package de.othr.traintogether.repository;

import de.othr.traintogether.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByGymIdOrderByDateTimeAsc(Long gymId);
}
