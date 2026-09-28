package com.example.attendance.growbytee.academy.course;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_batches")
public class StudentBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "batch_name",
            nullable = false,
            unique = true
    )
    private String batchName;

    @Column(
            name = "course_id",
            nullable = false
    )
    private Long courseId;

    @Column(
            name = "start_date",
            nullable = false
    )
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "trainer_name")
    private String trainerName;

    @Column(name = "class_days")
    private String classDays;

    @Column(name = "class_time")
    private String classTime;

    @Column(name = "mode")
    private String mode;

    @Column(
            name = "active",
            nullable = false
    )
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public Long getId() {
        return id;
    }

    public String getBatchName() {
        return batchName;
    }

    public Long getCourseId() {
        return courseId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public String getClassDays() {
        return classDays;
    }

    public String getClassTime() {
        return classTime;
    }

    public String getMode() {
        return mode;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setTrainerName(String trainerName) {
        this.trainerName = trainerName;
    }

    public void setClassDays(String classDays) {
        this.classDays = classDays;
    }

    public void setClassTime(String classTime) {
        this.classTime = classTime;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}