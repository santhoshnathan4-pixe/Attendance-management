package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/audit")
public class AdminActionHistoryController {

    private final AdminActionHistoryRepository historyRepository;

    public AdminActionHistoryController(
            AdminActionHistoryRepository historyRepository) {

        this.historyRepository = historyRepository;
    }

    @GetMapping
    public List<AdminActionHistory> getHistory() {

        return historyRepository
                .findAllByOrderByActionDateDescActionTimeDesc();
    }
}