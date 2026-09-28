package com.example.attendance.growbytee.academy.course;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/student-courses")
@CrossOrigin
public class AdminStudentCourseController {

    private final StudentCourseRepository courseRepository;

    public AdminStudentCourseController(
            StudentCourseRepository courseRepository) {

        this.courseRepository = courseRepository;
    }

    // =====================================================
    // GET ALL COURSES
    // =====================================================

    @GetMapping
    public List<StudentCourse> getAllCourses() {

        return courseRepository
                .findAll()
                .stream()
                .sorted(
                        (first, second) ->
                                first.getCourseName()
                                        .compareToIgnoreCase(
                                                second.getCourseName()
                                        )
                )
                .toList();
    }

    // =====================================================
    // GET ACTIVE COURSES
    // =====================================================

    @GetMapping("/active")
    public List<StudentCourse> getActiveCourses() {

        return courseRepository
                .findByActiveTrueOrderByCourseNameAsc();
    }

    // =====================================================
    // ADD COURSE
    // =====================================================

    @PostMapping
    public StudentCourse addCourse(
            @RequestBody StudentCourse request) {

        validateCourse(request);

        String courseName =
                request.getCourseName().trim();

        if (courseRepository
                .existsByCourseNameIgnoreCase(courseName)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Course with this name already exists"
            );
        }

        StudentCourse course =
                new StudentCourse();

        course.setCourseName(
                courseName
        );

        course.setDescription(
                cleanValue(
                        request.getDescription()
                )
        );

        course.setDuration(
                request.getDuration()
        );

        course.setDurationType(
                cleanValue(
                        request.getDurationType()
                )
        );

        course.setTrainingDuration(
                request.getTrainingDuration()
        );

        course.setInternshipDuration(
                request.getInternshipDuration()
        );

        course.setMode(
                cleanValue(
                        request.getMode()
                )
        );

        course.setActive(true);

        LocalDateTime now =
                LocalDateTime.now();

        course.setCreatedAt(now);
        course.setUpdatedAt(now);

        try {

            return courseRepository.save(course);

        } catch (DataIntegrityViolationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Course with this name already exists"
            );
        }
    }

    // =====================================================
    // UPDATE COURSE
    // =====================================================

    @PutMapping("/{id}")
    public StudentCourse updateCourse(
            @PathVariable Long id,
            @RequestBody StudentCourse request) {

        validateCourse(request);

        StudentCourse course =
                courseRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Course not found"
                                )
                        );

        String courseName =
                request.getCourseName().trim();

        boolean duplicate =
                courseRepository
                        .findByCourseNameIgnoreCase(
                                courseName
                        )
                        .filter(existing ->
                                !existing.getId()
                                        .equals(id)
                        )
                        .isPresent();

        if (duplicate) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Course with this name already exists"
            );
        }

        course.setCourseName(
                courseName
        );

        course.setDescription(
                cleanValue(
                        request.getDescription()
                )
        );

        course.setDuration(
                request.getDuration()
        );

        course.setDurationType(
                cleanValue(
                        request.getDurationType()
                )
        );

        course.setTrainingDuration(
                request.getTrainingDuration()
        );

        course.setInternshipDuration(
                request.getInternshipDuration()
        );

        course.setMode(
                cleanValue(
                        request.getMode()
                )
        );

        if (request.getActive() != null) {

            course.setActive(
                    request.getActive()
            );

        } else {

            course.setActive(true);
        }

        course.setUpdatedAt(
                LocalDateTime.now()
        );

        try {

            return courseRepository.save(course);

        } catch (DataIntegrityViolationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Course with this name already exists"
            );
        }
    }

    // =====================================================
    // DELETE COURSE
    // =====================================================

    @DeleteMapping("/{id}")
    public String deleteCourse(
            @PathVariable Long id) {

        StudentCourse course =
                courseRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Course not found"
                                )
                        );

        course.setActive(false);

        course.setUpdatedAt(
                LocalDateTime.now()
        );

        courseRepository.save(course);

        return "Course deleted successfully";
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private void validateCourse(
            StudentCourse request) {

        if (request == null
                || request.getCourseName() == null
                || request.getCourseName()
                        .trim()
                        .isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Course name is required"
            );
        }

        if (request.getDuration() != null
                && request.getDuration() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duration must be greater than 0"
            );
        }

        if (request.getTrainingDuration() != null
                && request.getTrainingDuration() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Training duration must be greater than 0"
            );
        }

        if (request.getInternshipDuration() != null
                && request.getInternshipDuration() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Internship duration must be greater than 0"
            );
        }
    }

    // =====================================================
    // CLEAN STRING VALUE
    // =====================================================

    private String cleanValue(
            String value) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }
}