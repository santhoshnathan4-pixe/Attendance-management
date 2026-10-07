package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private final AnnouncementRepository announcementRepository;

    public AdminAnnouncementController(
            AnnouncementRepository announcementRepository) {

        this.announcementRepository = announcementRepository;
    }

    public record AnnouncementRequest(
            LocalDate announcementDate,
            String title,
            String message) {
    }

    // =========================
    // LIST (month wise)
    // =========================

    @GetMapping
    public List<Announcement> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {

        YearMonth yearMonth = YearMonth.of(year, month);

        return announcementRepository
                .findByAnnouncementDateBetweenOrderByAnnouncementDateAsc(
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth());
    }

    // =========================
    // ACTIVE ANNOUNCEMENTS
    // =========================
    // Shows announcements from the given date onwards.
    // Example:
    // Announcement date = 20-10-2026
    // Employee dashboard shows it from 06-10-2026
    // until 20-10-2026.
    // It will not be shown from 21-10-2026.

    @GetMapping("/active")
    public List<Announcement> getActiveAnnouncements(
            @RequestParam LocalDate fromDate) {

        return announcementRepository
                .findByAnnouncementDateGreaterThanEqualOrderByAnnouncementDateAsc(
                        fromDate);
    }

    // =========================
    // ADD
    // =========================

    @PostMapping
    public Announcement create(@RequestBody AnnouncementRequest request) {

        Announcement announcement = new Announcement();

        apply(announcement, request);

        LocalDateTime now = LocalDateTime.now(INDIA_ZONE);

        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);

        return announcementRepository.save(announcement);
    }

    // =========================
    // EDIT
    // =========================

    @PutMapping("/{id}")
    public Announcement update(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request) {

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Announcement not found"));

        apply(announcement, request);

        announcement.setUpdatedAt(LocalDateTime.now(INDIA_ZONE));

        return announcementRepository.save(announcement);
    }

    // =========================
    // DELETE
    // =========================

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {

        if (!announcementRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Announcement not found");
        }

        announcementRepository.deleteById(id);

        return "Announcement deleted successfully";
    }

    // =========================
    // VALIDATION + APPLY
    // =========================

    private void apply(
            Announcement announcement,
            AnnouncementRequest request) {

        if (request == null || request.announcementDate() == null) {
            throw bad("Date is required");
        }

        String title = request.title() == null
                ? ""
                : request.title().trim();

        String message = request.message() == null
                ? ""
                : request.message().trim();

        if (title.length() < 2) {
            throw bad("Title is required");
        }

        if (title.length() > 150) {
            throw bad("Title is too long (maximum 150 characters)");
        }

        if (message.length() < 3) {
            throw bad("Message is required");
        }

        if (message.length() > 2000) {
            throw bad("Message is too long (maximum 2000 characters)");
        }

        announcement.setAnnouncementDate(request.announcementDate());
        announcement.setTitle(title);
        announcement.setMessage(message);
    }

    // =========================
    // BAD REQUEST
    // =========================

    private ResponseStatusException bad(String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message);
    }
}