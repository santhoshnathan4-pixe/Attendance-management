package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAnnouncementDateBetweenOrderByAnnouncementDateAsc(
            LocalDate startDate,
            LocalDate endDate);

    List<Announcement> findByAnnouncementDateGreaterThanEqualOrderByAnnouncementDateAsc(
            LocalDate fromDate);
}