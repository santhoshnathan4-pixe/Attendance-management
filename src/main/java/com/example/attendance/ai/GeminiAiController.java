package com.example.attendance.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class GeminiAiController {

    private final GeminiAiService geminiAiService;
    private final AttendanceAiService attendanceAiService;

    public GeminiAiController(
            GeminiAiService geminiAiService,
            AttendanceAiService attendanceAiService) {

        this.geminiAiService = geminiAiService;
        this.attendanceAiService = attendanceAiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> askAI(
            @RequestBody Map<String, String> request) {

        String prompt = request.get("prompt");

        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Prompt is required"));
        }

        String response = geminiAiService.generateResponse(prompt);

        return ResponseEntity.ok(
                Map.of("response", response));
    }

    @PostMapping("/attendance")
    public ResponseEntity<?> askAttendanceAI(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Question is required"));
        }

        String response = attendanceAiService.analyzeCurrentMonth(question);

        return ResponseEntity.ok(
                Map.of("response", response));
    }
}