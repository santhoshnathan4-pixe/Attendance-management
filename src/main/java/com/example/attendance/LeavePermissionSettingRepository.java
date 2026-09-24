package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeavePermissionSettingRepository
        extends JpaRepository<LeavePermissionSetting, Long> {

    Optional<LeavePermissionSetting>
    findBySettingType(String settingType);

    Optional<LeavePermissionSetting>
    findBySettingTypeAndRole(String settingType, String role);

    Optional<LeavePermissionSetting>
    findBySettingTypeAndEmployeeId(
            String settingType,
            Integer employeeId
    );
}