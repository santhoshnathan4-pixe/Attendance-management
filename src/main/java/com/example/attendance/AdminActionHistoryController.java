
package com.example.attendance;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/audit")
public class AdminActionHistoryController {

    private final AdminActionHistoryRepository historyRepository;

    public AdminActionHistoryController(
            AdminActionHistoryRepository historyRepository) {

        this.historyRepository =
                historyRepository;
    }


    @GetMapping
    public List<AdminActionHistory> getHistory(
            @RequestParam(required = false)
            Integer year,

            @RequestParam(required = false)
            Integer month) {

        /*
         * If Year and Month are not selected,
         * return all history.
         */
        if (year == null && month == null) {

            return historyRepository
                    .findAllByOrderByActionDateDescActionTimeDesc();
        }


        /*
         * Year is required when Month is selected.
         */
        if (year == null) {

            throw new IllegalArgumentException(
                    "Year is required"
            );
        }


        /*
         * If only Year is selected,
         * return the complete year's history.
         */
        if (month == null) {

            LocalDate startDate =
                    LocalDate.of(
                            year,
                            1,
                            1
                    );

            LocalDate endDate =
                    LocalDate.of(
                            year,
                            12,
                            31
                    );

            return historyRepository
                    .findByActionDateBetweenOrderByActionDateDescActionTimeDesc(
                            startDate,
                            endDate
                    );
        }


        /*
         * Validate Month.
         */
        if (
                month < 1 ||
                month > 12
        ) {

            throw new IllegalArgumentException(
                    "Invalid month"
            );
        }


        /*
         * Selected Year + Month
         */
        LocalDate startDate =
                LocalDate.of(
                        year,
                        month,
                        1
                );


        LocalDate endDate =
                startDate
                        .withDayOfMonth(
                                startDate.lengthOfMonth()
                        );


        return historyRepository
                .findByActionDateBetweenOrderByActionDateDescActionTimeDesc(
                        startDate,
                        endDate
                );
    }
}

