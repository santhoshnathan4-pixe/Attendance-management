package com.example.attendance.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/employee")
@CrossOrigin
public class EmployeeAiController {

    private final EmployeeAiService employeeAiService;

    public EmployeeAiController(
            EmployeeAiService employeeAiService) {

        this.employeeAiService = employeeAiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> askEmployeeAI(
            @RequestBody Map<String, String> request) {

        String employeeEmail = request.get("employeeEmail");

        String question = request.get("question");

        if (employeeEmail == null ||
                employeeEmail.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Employee email is required"));
        }

        if (question == null ||
                question.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Question is required"));
        }

        String response = employeeAiService
                .analyzeEmployeeQuestion(
                        employeeEmail,
                        question);

        return ResponseEntity.ok(
                Map.of(
                        "response",
                        response));
    }
}