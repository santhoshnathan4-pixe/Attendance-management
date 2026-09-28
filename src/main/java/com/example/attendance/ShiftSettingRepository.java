package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftSettingRepository
        extends JpaRepository<ShiftSetting, Long> {

    Optional<ShiftSetting> findByShiftType(String shiftType);
}