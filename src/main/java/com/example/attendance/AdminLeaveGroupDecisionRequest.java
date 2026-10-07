package com.example.attendance;

import java.time.LocalDate;
import java.util.List;

public record AdminLeaveGroupDecisionRequest(
        String adminEmail,
        String adminPassword,
        List<LocalDate> approvedDates,
        List<LocalDate> cancelledDates,
        String reason) {
}