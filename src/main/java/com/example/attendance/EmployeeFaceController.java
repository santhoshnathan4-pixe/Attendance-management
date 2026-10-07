
package com.example.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = {
        "https://attendance-management-3d-webinar.vercel.app",
        "https://attendance-management-git-main-3d-webinar.vercel.app",
        "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
        "https://attendance-management-nine-beige.vercel.app"
})
@RequestMapping("/employee-face")
public class EmployeeFaceController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFaceRepository employeeFaceRepository;

    public EmployeeFaceController(
            EmployeeRepository employeeRepository,
            EmployeeFaceRepository employeeFaceRepository) {

        this.employeeRepository = employeeRepository;
        this.employeeFaceRepository = employeeFaceRepository;
    }


    // =====================================================
    // REGISTER FACE
    // =====================================================

    @PostMapping("/register")
    public String registerFace(
            @RequestBody EmployeeFaceRequest request) {

        if (request == null ||
                request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee email required");
        }

        if (request.getFaceData() == null ||
                request.getFaceData().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face data required");
        }

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(
                        request.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found or inactive"));

        employeeFaceRepository
                .findByEmployeeIdAndActiveTrue(
                        employee.getId().longValue())
                .ifPresent(oldFace -> {

                    oldFace.setActive(false);

                    employeeFaceRepository.save(oldFace);
                });

        EmployeeFace newFace =
                new EmployeeFace();

        newFace.setEmployeeId(
                employee.getId().longValue());

        newFace.setFaceData(
                request.getFaceData().trim());

        newFace.setActive(true);

        employeeFaceRepository.save(
                newFace);

        return "Face registered successfully";
    }


    // =====================================================
    // VERIFY FACE
    // =====================================================

    @PostMapping("/verify")
    public String verifyFace(
            @RequestBody EmployeeFaceRequest request) {

        if (request == null ||
                request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee email required");
        }

        if (request.getFaceData() == null ||
                request.getFaceData().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face data required");
        }

        Employee employee = employeeRepository
                .findByEmailAndActiveTrue(
                        request.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found or inactive"));

        EmployeeFace registeredFace =
                employeeFaceRepository
                        .findByEmployeeIdAndActiveTrue(
                                employee.getId().longValue())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Face is not registered"));


        try {

            List<Double> registeredEmbedding =
                    parseFaceEmbedding(
                            registeredFace.getFaceData());

            List<Double> currentEmbedding =
                    parseFaceEmbedding(
                            request.getFaceData());


            if (registeredEmbedding.isEmpty() ||
                    currentEmbedding.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid face data");
            }


            if (registeredEmbedding.size() !=
                    currentEmbedding.size()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Face data format mismatch");
            }


            double similarity =
                    calculateCosineSimilarity(
                            registeredEmbedding,
                            currentEmbedding);


            double threshold = 0.60;


            if (similarity >= threshold) {

                return "FACE_MATCH";
            }


            return "FACE_MISMATCH";


        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid face data format");
        }
    }


    // =====================================================
    // CHECK FACE REGISTRATION STATUS
    //
    // This checks the employee's assigned branch.
    //
    // FACE_NOT_REQUIRED
    //      Branch not assigned
    //      OR branch inactive
    //      OR Face Verification OFF
    //
    // FACE_REGISTERED
    //      Face Verification ON
    //      and active face exists
    //
    // FACE_NOT_REGISTERED
    //      Face Verification ON
    //      but active face does not exist
    // =====================================================

    @GetMapping("/status")
    public String getFaceStatus(
            @RequestParam String email) {

        if (email == null ||
                email.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Employee email required");
        }


        Employee employee =
                employeeRepository
                        .findByEmailAndActiveTrue(
                                email.trim())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found or inactive"));


        Branch branch =
                employee.getBranch();


        // =================================================
        // FACE VERIFICATION NOT REQUIRED
        // =================================================

        if (branch == null ||
                !Boolean.TRUE.equals(
                        branch.getActive()) ||
                !Boolean.TRUE.equals(
                        branch.getFaceVerificationEnabled())) {

            return "FACE_NOT_REQUIRED";
        }


        // =================================================
        // FACE VERIFICATION REQUIRED
        // =================================================

        boolean faceRegistered =
                employeeFaceRepository
                        .findByEmployeeIdAndActiveTrue(
                                employee.getId().longValue())
                        .isPresent();


        if (faceRegistered) {

            return "FACE_REGISTERED";
        }


        return "FACE_NOT_REGISTERED";
    }


    // =====================================================
    // PARSE FACE EMBEDDING
    // =====================================================

    private List<Double> parseFaceEmbedding(
            String faceData) {

        List<Double> values =
                new ArrayList<>();


        if (faceData == null ||
                faceData.trim().isEmpty()) {

            return values;
        }


        String data =
                faceData.trim();


        if (!data.startsWith("[") ||
                !data.endsWith("]")) {

            return values;
        }


        data = data.substring(
                1,
                data.length() - 1)
                .trim();


        if (data.isEmpty()) {

            return values;
        }


        String[] numbers =
                data.split(",");


        for (String number : numbers) {

            String value =
                    number.trim();


            if (value.isEmpty()) {

                return List.of();
            }


            values.add(
                    Double.parseDouble(value));
        }


        return values;
    }


    // =====================================================
    // CALCULATE COSINE SIMILARITY
    // =====================================================

    private double calculateCosineSimilarity(
            List<Double> first,
            List<Double> second) {

        double dotProduct = 0.0;

        double firstMagnitude = 0.0;

        double secondMagnitude = 0.0;


        for (int i = 0;
             i < first.size();
             i++) {

            double firstValue =
                    first.get(i);

            double secondValue =
                    second.get(i);


            dotProduct +=
                    firstValue *
                            secondValue;


            firstMagnitude +=
                    firstValue *
                            firstValue;


            secondMagnitude +=
                    secondValue *
                            secondValue;
        }


        if (firstMagnitude == 0.0 ||
                secondMagnitude == 0.0) {

            return 0.0;
        }


        return dotProduct /
                (Math.sqrt(firstMagnitude) *
                        Math.sqrt(secondMagnitude));
    }
}

