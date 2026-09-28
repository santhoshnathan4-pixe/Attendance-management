package com.example.attendance.growbytee.academy.course;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/student-batches")
@CrossOrigin
public class AdminStudentBatchController {

    private final StudentBatchRepository batchRepository;

    private final StudentCourseRepository courseRepository;


    public AdminStudentBatchController(
            StudentBatchRepository batchRepository,
            StudentCourseRepository courseRepository) {

        this.batchRepository =
                batchRepository;

        this.courseRepository =
                courseRepository;
    }


    @GetMapping
    public List<StudentBatch> getAllBatches() {

        return batchRepository
                .findAllByOrderByBatchNameAsc();
    }


    @GetMapping("/active")
    public List<StudentBatch> getActiveBatches() {

        return batchRepository
                .findByActiveTrueOrderByBatchNameAsc();
    }


    @GetMapping("/course/{courseId}")
    public List<StudentBatch> getBatchesByCourse(
            @PathVariable Long courseId) {

        validateCourseExists(courseId);

        return batchRepository
                .findByCourseIdOrderByBatchNameAsc(
                        courseId
                );
    }


    @PostMapping
    public StudentBatch addBatch(
            @RequestBody StudentBatch request) {

        validateBatch(request);

        validateCourseExists(
                request.getCourseId()
        );

        String batchName =
                request.getBatchName().trim();


        if (batchRepository
                .existsByBatchNameIgnoreCase(
                        batchName
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Batch with this name already exists"
            );
        }


        StudentBatch batch =
                new StudentBatch();


        batch.setBatchName(
                batchName
        );

        batch.setCourseId(
                request.getCourseId()
        );

        batch.setStartDate(
                request.getStartDate()
        );

        batch.setEndDate(
                request.getEndDate()
        );

        batch.setTrainerName(
                cleanValue(
                        request.getTrainerName()
                )
        );

        batch.setClassDays(
                cleanValue(
                        request.getClassDays()
                )
        );

        batch.setClassTime(
                cleanValue(
                        request.getClassTime()
                )
        );

        batch.setMode(
                cleanValue(
                        request.getMode()
                )
        );

        batch.setActive(true);


        LocalDateTime now =
                LocalDateTime.now();


        batch.setCreatedAt(now);

        batch.setUpdatedAt(now);


        try {

            return batchRepository.save(
                    batch
            );

        } catch (
                DataIntegrityViolationException exception
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Batch with this name already exists"
            );
        }
    }


    @PutMapping("/{id}")
    public StudentBatch updateBatch(
            @PathVariable Long id,
            @RequestBody StudentBatch request) {

        validateBatch(request);

        validateCourseExists(
                request.getCourseId()
        );


        StudentBatch batch =
                batchRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Batch not found"
                                )
                        );


        String batchName =
                request.getBatchName().trim();


        boolean duplicate =
                batchRepository
                        .findByBatchNameIgnoreCase(
                                batchName
                        )
                        .filter(existing ->
                                !existing.getId()
                                        .equals(id)
                        )
                        .isPresent();


        if (duplicate) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Batch with this name already exists"
            );
        }


        batch.setBatchName(
                batchName
        );

        batch.setCourseId(
                request.getCourseId()
        );

        batch.setStartDate(
                request.getStartDate()
        );

        batch.setEndDate(
                request.getEndDate()
        );

        batch.setTrainerName(
                cleanValue(
                        request.getTrainerName()
                )
        );

        batch.setClassDays(
                cleanValue(
                        request.getClassDays()
                )
        );

        batch.setClassTime(
                cleanValue(
                        request.getClassTime()
                )
        );

        batch.setMode(
                cleanValue(
                        request.getMode()
                )
        );


        if (request.getActive() != null) {

            batch.setActive(
                    request.getActive()
            );

        } else {

            batch.setActive(true);
        }


        batch.setUpdatedAt(
                LocalDateTime.now()
        );


        try {

            return batchRepository.save(
                    batch
            );

        } catch (
                DataIntegrityViolationException exception
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Batch with this name already exists"
            );
        }
    }


    @DeleteMapping("/{id}")
    public String deleteBatch(
            @PathVariable Long id) {

        StudentBatch batch =
                batchRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Batch not found"
                                )
                        );


        batch.setActive(false);

        batch.setUpdatedAt(
                LocalDateTime.now()
        );


        batchRepository.save(batch);


        return "Batch deleted successfully";
    }


    private void validateBatch(
            StudentBatch request) {

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Batch data is required"
            );
        }


        if (request.getBatchName() == null
                || request.getBatchName()
                        .trim()
                        .isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Batch name is required"
            );
        }


        if (request.getCourseId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Course is required"
            );
        }


        if (request.getStartDate() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date is required"
            );
        }


        LocalDate startDate =
                request.getStartDate();

        LocalDate endDate =
                request.getEndDate();


        if (endDate != null
                && endDate.isBefore(startDate)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date"
            );
        }
    }


    private void validateCourseExists(
            Long courseId) {

        if (!courseRepository
                .existsById(courseId)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected course does not exist"
            );
        }
    }


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