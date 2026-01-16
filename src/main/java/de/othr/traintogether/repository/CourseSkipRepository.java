package de.othr.traintogether.repository;

import de.othr.traintogether.model.CourseSkip;
import de.othr.traintogether.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourseSkipRepository extends JpaRepository<CourseSkip, Long> {

    @Query("SELECT cs.course.id FROM CourseSkip cs WHERE cs.user = :user AND cs.skippedAt > :cutoff")
    List<Long> findSkippedCourseIdsByUser(@Param("user") User user, @Param("cutoff") LocalDateTime cutoff);
}
