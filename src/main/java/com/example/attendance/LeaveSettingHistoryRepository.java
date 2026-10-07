package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveSettingHistoryRepository
        extends JpaRepository<LeaveSettingHistory, Long> {

    List<LeaveSettingHistory> findAllByOrderByCreatedAtDesc();
}