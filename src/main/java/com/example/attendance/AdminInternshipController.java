package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/admin/internship")
@CrossOrigin(origins = {
                "https://attendance-management-d5h6.onrender.com",
                "https://attendance-management.vercel.app"
})
public class AdminInternshipController {

        private final EmployeeRepository employeeRepository;
        private final InternshipStipendRepository internshipStipendRepository;

        public AdminInternshipController(
                        EmployeeRepository employeeRepository,
                        InternshipStipendRepository internshipStipendRepository) {
                this.employeeRepository = employeeRepository;
                this.internshipStipendRepository = internshipStipendRepository;
        }

        @GetMapping("/stipend/{employeeId}")
        public ResponseEntity<?> getStipendHistory(
                        @PathVariable Integer employeeId) {

                if (employeeId == null) {
                        return ResponseEntity.badRequest()
                                        .body("Employee ID is required.");
                }

                if (!employeeRepository.existsById(employeeId)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body("Employee not found.");
                }

                List<InternshipStipend> history = internshipStipendRepository
                                .findByEmployeeIdOrderByStipendYearAscStipendMonthAsc(
                                                employeeId);

                return ResponseEntity.ok(history);
        }

        @PostMapping("/stipend")
        public ResponseEntity<?> createStipend(
                        @RequestBody InternshipStipend request) {

                if (request == null) {
                        return ResponseEntity.badRequest()
                                        .body("Stipend data is required.");
                }

                if (request.getEmployee() == null
                                || request.getEmployee().getId() == null) {

                        return ResponseEntity.badRequest()
                                        .body("Employee ID is required.");
                }

                if (request.getStipendYear() == null
                                || request.getStipendMonth() == null) {

                        return ResponseEntity.badRequest()
                                        .body("Stipend year and month are required.");
                }

                if (request.getStipendMonth() < 1
                                || request.getStipendMonth() > 12) {

                        return ResponseEntity.badRequest()
                                        .body("Stipend month must be between 1 and 12.");
                }

                if (request.getStipendType() == null
                                || request.getStipendType().trim().isEmpty()) {

                        return ResponseEntity.badRequest()
                                        .body("Stipend type is required.");
                }

                String stipendType = request.getStipendType().trim().toUpperCase();

                if (!stipendType.equals("PAID")
                                && !stipendType.equals("UNPAID")) {

                        return ResponseEntity.badRequest()
                                        .body("Stipend type must be PAID or UNPAID.");
                }

                Double amount = request.getStipendAmount();

                if (amount == null) {
                        amount = 0.0;
                }

                if (amount < 0) {
                        return ResponseEntity.badRequest()
                                        .body("Stipend amount cannot be negative.");
                }

                if (stipendType.equals("UNPAID")) {
                        amount = 0.0;
                }

                Integer employeeId = request.getEmployee().getId();

                Employee employee = employeeRepository.findById(employeeId)
                                .orElse(null);

                if (employee == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body("Employee not found.");
                }

                Optional<InternshipStipend> existing = internshipStipendRepository
                                .findByEmployeeIdAndStipendYearAndStipendMonth(
                                                employeeId,
                                                request.getStipendYear(),
                                                request.getStipendMonth());

                if (existing.isPresent()) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body("Stipend record already exists for this month.");
                }

                InternshipStipend stipend = new InternshipStipend();

                stipend.setEmployee(employee);
                stipend.setStipendYear(request.getStipendYear());
                stipend.setStipendMonth(request.getStipendMonth());
                stipend.setStipendType(stipendType);
                stipend.setStipendAmount(amount);

                InternshipStipend saved = internshipStipendRepository.save(stipend);

                return ResponseEntity.ok(saved);
        }

        @PutMapping("/stipend/{id}")
        public ResponseEntity<?> updateStipend(
                        @PathVariable Integer id,
                        @RequestBody InternshipStipend request) {

                InternshipStipend existing = internshipStipendRepository.findById(id)
                                .orElse(null);

                if (existing == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body("Stipend record not found.");
                }

                if (request == null) {
                        return ResponseEntity.badRequest()
                                        .body("Stipend data is required.");
                }

                if (request.getStipendType() == null
                                || request.getStipendType().trim().isEmpty()) {

                        return ResponseEntity.badRequest()
                                        .body("Stipend type is required.");
                }

                String stipendType = request.getStipendType().trim().toUpperCase();

                if (!stipendType.equals("PAID")
                                && !stipendType.equals("UNPAID")) {

                        return ResponseEntity.badRequest()
                                        .body("Stipend type must be PAID or UNPAID.");
                }

                Double amount = request.getStipendAmount();

                if (amount == null) {
                        amount = 0.0;
                }

                if (amount < 0) {
                        return ResponseEntity.badRequest()
                                        .body("Stipend amount cannot be negative.");
                }

                if (stipendType.equals("UNPAID")) {
                        amount = 0.0;
                }

                /*
                 * Only this particular month's record is updated.
                 * Previous months remain unchanged.
                 */
                existing.setStipendType(stipendType);
                existing.setStipendAmount(amount);

                InternshipStipend saved = internshipStipendRepository.save(existing);

                return ResponseEntity.ok(saved);
        }
}