package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionHistoryRepository
        extends JpaRepository<AdminActionHistory, Integer> {

    List<AdminActionHistory>
    findAllByOrderByActionDateDescActionTimeDesc();
}