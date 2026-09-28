package com.example.attendance.growbytee.academy.course;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_courses")
public class StudentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "course_name",
            nullable = false,
            unique = true
    )
    private String courseName;

    @Column(name = "description")
    private String description;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "duration_type")
    private String durationType;

    @Column(name = "training_duration")
    private Integer trainingDuration;

    @Column(name = "internship_duration")
    private Integer internshipDuration;

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

    public String getCourseName() {
        return courseName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getDurationType() {
        return durationType;
    }

    public Integer getTrainingDuration() {
        return trainingDuration;
    }

    public Integer getInternshipDuration() {
        return internshipDuration;
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

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    public void setTrainingDuration(Integer trainingDuration) {
        this.trainingDuration = trainingDuration;
    }

    public void setInternshipDuration(Integer internshipDuration) {
        this.internshipDuration = internshipDuration;
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