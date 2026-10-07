package com.example.attendance;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Employee side. Works like the holiday alert:
 * visible from the day admin adds it until the announcement
 * date is over (the next day it is not returned any more).
 */
@RestController
@CrossOrigin(origins = {
        "https://attendance-management-73dqjuakh-3d-webinar.vercel.app",
        "https://attendance-management-3d-webinar.vercel.app"
})
@RequestMapping("/employee/announcements")
public class EmployeeAnnouncementController {

    private final AnnouncementRepository announcementRepository;

    public EmployeeAnnouncementController(
            AnnouncementRepository announcementRepository) {

        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/upcoming")
    public List<Announcement> getUpcoming(
            @RequestParam(required = false) String date) {

        LocalDate fromDate = (date == null || date.trim().isEmpty())
                ? LocalDate.now(ZoneId.of("Asia/Kolkata"))
                : LocalDate.parse(date);

        return announcementRepository
                .findByAnnouncementDateGreaterThanEqualOrderByAnnouncementDateAsc(
                        fromDate);
    }
}