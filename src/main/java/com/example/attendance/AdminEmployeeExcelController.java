package com.example.attendance;

import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/admin/employees")
public class AdminEmployeeExcelController {

    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final AdminActionHistoryRepository historyRepository;

    public AdminEmployeeExcelController(
            EmployeeRepository employeeRepository,
            AdminRepository adminRepository,
            AdminActionHistoryRepository historyRepository) {

        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
        this.historyRepository = historyRepository;
    }


    // =========================================
    // EXCEL IMPORT
    // =========================================

    @PostMapping("/import")
    public ResponseEntity<ExcelImportResponse> importEmployees(
            @RequestParam("file") MultipartFile file,
            @RequestParam String adminEmail,
            @RequestParam String adminPassword) {

        // -----------------------------------------
        // VERIFY ADMIN
        // -----------------------------------------

        Admin admin = verifyAdmin(
                adminEmail,
                adminPassword
        );


        // -----------------------------------------
        // VALIDATE FILE
        // -----------------------------------------

        if (file == null ||
                file.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Excel file is required"
            );
        }


        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                !(fileName.toLowerCase().endsWith(".xlsx") ||
                  fileName.toLowerCase().endsWith(".xls"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only Excel files are allowed"
            );
        }


        int importedCount = 0;
        int failedCount = 0;

        List<String> errors =
                new ArrayList<>();


        // -----------------------------------------
        // READ EXCEL
        // -----------------------------------------

        try (Workbook workbook =
                     WorkbookFactory.create(
                             file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Excel sheet not found"
                );
            }


            Sheet sheet =
                    workbook.getSheetAt(0);


            if (sheet.getPhysicalNumberOfRows() < 2) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Excel file does not contain employee data"
                );
            }


            // -----------------------------------------
            // READ HEADER
            // -----------------------------------------

            Row headerRow =
                    sheet.getRow(0);

            if (headerRow == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Excel header row not found"
                );
            }


            Map<String, Integer> headers =
                    readHeaders(headerRow);


            // -----------------------------------------
            // REQUIRED COLUMNS
            // -----------------------------------------

            Integer employeeCodeColumn =
                    findColumn(
                            headers,
                            "employee code",
                            "employee_code",
                            "emp code",
                            "emp_code",
                            "code"
                    );

            Integer nameColumn =
                    findColumn(
                            headers,
                            "name",
                            "employee name",
                            "employee_name"
                    );

            Integer emailColumn =
                    findColumn(
                            headers,
                            "email",
                            "employee email",
                            "employee_email"
                    );

            Integer contactColumn =
                    findColumn(
                            headers,
                            "contact number",
                            "contact_number",
                            "phone",
                            "phone number",
                            "mobile",
                            "mobile number"
                    );

            Integer roleColumn =
                    findColumn(
                            headers,
                            "role",
                            "designation",
                            "position"
                    );

            Integer joiningDateColumn =
                    findColumn(
                            headers,
                            "joining date",
                            "joining_date",
                            "date of joining",
                            "doj"
                    );


            // -----------------------------------------
            // CHECK REQUIRED COLUMNS
            // -----------------------------------------

            List<String> missingColumns =
                    new ArrayList<>();

            if (employeeCodeColumn == null) {
                missingColumns.add("Employee Code");
            }

            if (nameColumn == null) {
                missingColumns.add("Name");
            }

            if (emailColumn == null) {
                missingColumns.add("Email");
            }

            if (roleColumn == null) {
                missingColumns.add("Role");
            }

            if (joiningDateColumn == null) {
                missingColumns.add("Joining Date");
            }


            if (!missingColumns.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing columns: "
                                + String.join(
                                        ", ",
                                        missingColumns
                                )
                );
            }


            // -----------------------------------------
            // READ EMPLOYEE ROWS
            // -----------------------------------------

            DataFormatter formatter =
                    new DataFormatter();


            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row =
                        sheet.getRow(rowIndex);


                if (row == null ||
                        isEmptyRow(row)) {

                    continue;
                }


                int excelRowNumber =
                        rowIndex + 1;


                try {

                    // -----------------------------------------
                    // FETCH ONLY REQUIRED DATA
                    // -----------------------------------------

                    String employeeCode =
                            getCellValue(
                                    row,
                                    employeeCodeColumn,
                                    formatter
                            );

                    String name =
                            getCellValue(
                                    row,
                                    nameColumn,
                                    formatter
                            );

                    String email =
                            getCellValue(
                                    row,
                                    emailColumn,
                                    formatter
                            );

                    String contactNumber =
                            contactColumn != null
                                    ? getCellValue(
                                            row,
                                            contactColumn,
                                            formatter
                                    )
                                    : "";

                    String role =
                            getCellValue(
                                    row,
                                    roleColumn,
                                    formatter
                            );

                    String joiningDateText =
                            getCellValue(
                                    row,
                                    joiningDateColumn,
                                    formatter
                            );


                    // -----------------------------------------
                    // VALIDATION
                    // -----------------------------------------

                    if (employeeCode.isBlank()) {

                        throw new IllegalArgumentException(
                                "Employee Code is required"
                        );
                    }

                    if (name.isBlank()) {

                        throw new IllegalArgumentException(
                                "Name is required"
                        );
                    }

                    if (email.isBlank()) {

                        throw new IllegalArgumentException(
                                "Email is required"
                        );
                    }

                    if (!email.contains("@")) {

                        throw new IllegalArgumentException(
                                "Invalid email"
                        );
                    }

                    if (role.isBlank()) {

                        throw new IllegalArgumentException(
                                "Role is required"
                        );
                    }

                    if (joiningDateText.isBlank()) {

                        throw new IllegalArgumentException(
                                "Joining Date is required"
                        );
                    }


                    // -----------------------------------------
                    // DUPLICATE CHECK
                    // -----------------------------------------

                    if (employeeRepository
                            .existsByEmployeeCode(
                                    employeeCode)) {

                        throw new IllegalArgumentException(
                                "Employee Code already exists: "
                                        + employeeCode
                        );
                    }


                    if (employeeRepository
                            .existsByEmail(email)) {

                        throw new IllegalArgumentException(
                                "Email already exists: "
                                        + email
                        );
                    }


                    // -----------------------------------------
                    // DATE
                    // -----------------------------------------

                    LocalDate joiningDate =
                            parseExcelDate(
                                    row,
                                    joiningDateColumn,
                                    joiningDateText
                            );


                    // -----------------------------------------
                    // CREATE EMPLOYEE
                    // -----------------------------------------

                    Employee employee =
                            new Employee();

                    employee.setEmployeeCode(
                            employeeCode
                    );

                    employee.setName(
                            name
                    );

                    employee.setEmail(
                            email
                    );

                    employee.setContactNumber(
                            contactNumber
                    );

                    employee.setRole(
                            role
                    );

                    // Salary is separate.
                    employee.setSalary(0.0);

                    employee.setJoiningDate(
                            joiningDateText
                    );

                    // First login password setup.
                    employee.setPassword(null);

                    employee.setActive(true);


                    Employee savedEmployee =
                            employeeRepository.save(
                                    employee
                            );


                    // -----------------------------------------
                    // HISTORY
                    // -----------------------------------------

                    saveHistory(
                            admin,
                            "EXCEL IMPORT",
                            savedEmployee,
                            "Employee",
                            null,
                            savedEmployee.getName()
                    );


                    importedCount++;

                } catch (Exception e) {

                    failedCount++;

                    errors.add(
                            "Row "
                                    + excelRowNumber
                                    + ": "
                                    + e.getMessage()
                    );
                }
            }


        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read Excel file: "
                            + e.getMessage()
            );
        }


        ExcelImportResponse response =
                new ExcelImportResponse(
                        importedCount,
                        failedCount,
                        errors
                );


        return ResponseEntity.ok(response);
    }


    // =========================================
    // READ HEADERS
    // =========================================

    private Map<String, Integer> readHeaders(
            Row headerRow) {

        Map<String, Integer> headers =
                new HashMap<>();

        for (Cell cell : headerRow) {

            String value =
                    cell.getStringCellValue();

            if (value == null) {
                continue;
            }

            String normalized =
                    normalize(value);

            headers.put(
                    normalized,
                    cell.getColumnIndex()
            );
        }

        return headers;
    }


    // =========================================
    // FIND COLUMN
    // =========================================

    private Integer findColumn(
            Map<String, Integer> headers,
            String... possibleNames) {

        for (String name : possibleNames) {

            Integer column =
                    headers.get(
                            normalize(name)
                    );

            if (column != null) {
                return column;
            }
        }

        return null;
    }


    // =========================================
    // NORMALIZE HEADER
    // =========================================

    private String normalize(
            String value) {

        return value
                .trim()
                .toLowerCase()
                .replace("_", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ");
    }


    // =========================================
    // GET CELL VALUE
    // =========================================

    private String getCellValue(
            Row row,
            Integer column,
            DataFormatter formatter) {

        if (column == null) {
            return "";
        }

        Cell cell =
                row.getCell(
                        column,
                        Row.MissingCellPolicy
                                .RETURN_BLANK_AS_NULL
                );

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }


    // =========================================
    // PARSE DATE
    // =========================================

    private LocalDate parseExcelDate(
            Row row,
            Integer column,
            String text) {

        Cell cell =
                row.getCell(
                        column,
                        Row.MissingCellPolicy
                                .RETURN_BLANK_AS_NULL
                );

        if (cell != null &&
                cell.getCellType() == CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(cell)) {

            return cell
                    .getLocalDateTimeCellValue()
                    .toLocalDate();
        }


        String[] formats = {
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "MM-dd-yyyy",
                "MM/dd/yyyy"
        };


        for (String format : formats) {

            try {

                return LocalDate.parse(
                        text,
                        DateTimeFormatter.ofPattern(
                                format
                        )
                );

            } catch (DateTimeParseException ignored) {
            }
        }


        throw new IllegalArgumentException(
                "Invalid Joining Date: " + text
        );
    }


    // =========================================
    // EMPTY ROW CHECK
    // =========================================

    private boolean isEmptyRow(Row row) {

        for (Cell cell : row) {

            if (cell != null &&
                    cell.getCellType() != CellType.BLANK) {

                String value =
                        new DataFormatter()
                                .formatCellValue(cell)
                                .trim();

                if (!value.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }


    // =========================================
    // VERIFY ADMIN
    // =========================================

    private Admin verifyAdmin(
            String email,
            String password) {

        if (email == null ||
                email.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin email required"
            );
        }


        if (password == null ||
                password.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Admin password required"
            );
        }


        return adminRepository
                .findByEmailAndPassword(
                        email,
                        password
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid admin email or password"
                        )
                );
    }


    // =========================================
    // SAVE HISTORY
    // =========================================

    private void saveHistory(
            Admin admin,
            String action,
            Employee employee,
            String fieldName,
            String oldValue,
            String newValue) {

        AdminActionHistory history =
                new AdminActionHistory();

        history.setAdminName(
                admin.getAdminName()
        );

        history.setAction(action);

        history.setEmployeeId(
                employee.getId()
        );

        history.setEmployeeCode(
                employee.getEmployeeCode()
        );

        history.setEmployeeName(
                employee.getName()
        );

        history.setActionDate(
                LocalDate.now()
        );

        history.setActionTime(
                LocalTime.now()
        );

        history.setFieldName(
                fieldName
        );

        history.setOldValue(
                oldValue
        );

        history.setNewValue(
                newValue
        );

        historyRepository.save(history);
    }
}