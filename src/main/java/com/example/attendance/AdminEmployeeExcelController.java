package com.example.attendance;

import org.apache.poi.ss.usermodel.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Excel import.
 *
 * The admin can upload any Excel file. Column headings may be in any order,
 * spelled in different ways (Name / Employee Name / Full Name ...) and extra
 * columns are ignored. Only the data this system needs is read:
 *
 * Name, Email, Contact Number, Role / Department, Designation,
 * Technology, Joining Date, Branch
 *
 * Only a name (or an email to build a name from) is needed to add a row.
 * Anything missing or invalid is left blank and the row is still added, so
 * the admin can complete it later with Edit. Interns, managers and normal
 * employees are recognised from the designation (same rule as Add Employee).
 * Salary is never imported.
 */
@RestController
@RequestMapping("/admin/employees")
public class AdminEmployeeExcelController {

        private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

        private final EmployeeRepository employeeRepository;
        private final AdminRepository adminRepository;
        private final AdminActionHistoryRepository historyRepository;
        private final BranchRepository branchRepository;

        public AdminEmployeeExcelController(
                        EmployeeRepository employeeRepository,
                        AdminRepository adminRepository,
                        AdminActionHistoryRepository historyRepository,
                        BranchRepository branchRepository) {

                this.employeeRepository = employeeRepository;
                this.adminRepository = adminRepository;
                this.historyRepository = historyRepository;
                this.branchRepository = branchRepository;
        }

        // =====================================================
        // COLUMNS WE LOOK FOR
        // =====================================================

        private enum Field {
                NAME, EMAIL, CONTACT, ROLE, DESIGNATION, TECHNOLOGY, JOINING, BRANCH
        }

        /** Exact heading names (letters and digits only, lower case). */
        private static final Map<Field, List<String>> EXACT = new LinkedHashMap<>();

        /** Headings that only need to contain one of these words. */
        private static final Map<Field, List<String>> CONTAINS = new LinkedHashMap<>();

        static {
                EXACT.put(Field.EMAIL, List.of("email", "emailid", "emailaddress", "mail", "mailid",
                                "employeeemail", "officialemail", "workemail", "companyemail", "personalemail"));
                EXACT.put(Field.CONTACT, List.of("contactnumber", "contact", "contactno", "phone", "phonenumber",
                                "phoneno", "mobile", "mobilenumber", "mobileno", "cell", "whatsapp", "whatsappnumber"));
                EXACT.put(Field.JOINING, List.of("joiningdate", "dateofjoining", "doj", "joindate", "dateofjoin",
                                "joined", "joinedon", "startdate", "datejoined"));
                EXACT.put(Field.DESIGNATION, List.of("designation", "position", "jobtitle", "title", "post",
                                "jobrole", "employeedesignation"));
                EXACT.put(Field.TECHNOLOGY, List.of("technology", "technologies", "tech", "techstack", "skills",
                                "skill", "skillset", "primaryskill", "primaryskills"));
                EXACT.put(Field.ROLE, List.of("role", "department", "dept", "team", "function", "employeerole"));
                EXACT.put(Field.BRANCH, List.of("branch", "branchname", "location", "office", "worklocation"));
                EXACT.put(Field.NAME, List.of("name", "employeename", "empname", "fullname", "candidatename",
                                "staffname", "personname", "employee"));

                CONTAINS.put(Field.EMAIL, List.of("mail"));
                CONTAINS.put(Field.CONTACT, List.of("mobile", "phone", "contact"));
                CONTAINS.put(Field.JOINING, List.of("joining", "doj"));
                CONTAINS.put(Field.DESIGNATION, List.of("designation"));
                CONTAINS.put(Field.TECHNOLOGY, List.of("technolog", "skill"));
                CONTAINS.put(Field.ROLE, List.of("department"));
                CONTAINS.put(Field.BRANCH, List.of("branch"));
        }

        private static final Pattern NOT_A_PERSON_NAME = Pattern.compile(
                        "branch|company|father|mother|spouse|bank|nominee|manager|designation|role|department|account|reporting");

        // =====================================================
        // EXCEL IMPORT
        // =====================================================

        @PostMapping("/import")
        public ResponseEntity<ExcelImportResponse> importEmployees(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam String adminEmail,
                        @RequestParam(required = false) String reason) {

                Admin admin = findAdmin(adminEmail);

                if (file == null || file.isEmpty()) {
                        throw bad("Excel file is required");
                }

                String fileName = file.getOriginalFilename();

                if (fileName == null
                                || !(fileName.toLowerCase().endsWith(".xlsx")
                                                || fileName.toLowerCase().endsWith(".xls"))) {
                        throw bad("Only Excel files (.xlsx / .xls) are allowed");
                }

                int imported = 0;
                int failed = 0;
                int incomplete = 0;
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

                        Sheet sheet = firstSheetWithData(workbook);

                        if (sheet == null) {
                                throw bad("The Excel file does not contain any data");
                        }

                        DataFormatter formatter = new DataFormatter();

                        // -------- find the heading row and the columns --------

                        int headerIndex = -1;
                        Map<Field, Integer> columns = null;

                        int lastScan = Math.min(sheet.getLastRowNum(), 19);

                        for (int r = 0; r <= lastScan; r++) {

                                Row row = sheet.getRow(r);

                                if (row == null) {
                                        continue;
                                }

                                Map<Field, Integer> found = mapColumns(row, formatter);

                                if (!found.containsKey(Field.NAME) && !found.containsKey(Field.EMAIL)) {
                                        continue;
                                }

                                if (columns == null || found.size() > columns.size()) {
                                        columns = found;
                                        headerIndex = r;
                                }
                        }

                        if (columns == null) {
                                throw bad("Could not find a Name or Email column heading in the first rows. "
                                                + "Please keep column headings such as Name, Email, Contact Number, "
                                                + "Role, Designation, Technology, Joining Date.");
                        }

                        // -------- existing data used for duplicate checks and codes --------

                        List<Employee> existing = employeeRepository.findAll();

                        Set<String> emails = new HashSet<>();
                        Set<String> people = new HashSet<>();
                        Set<String> codes = new HashSet<>();
                        Map<String, Integer> highest = new HashMap<>();

                        for (Employee e : existing) {
                                if (e.getEmail() != null && !e.getEmail().isBlank()) {
                                        emails.add(e.getEmail().trim().toLowerCase());
                                }
                                if (e.getName() != null && e.getContactNumber() != null
                                                && !e.getContactNumber().isBlank()) {
                                        people.add(personKey(e.getName(), e.getContactNumber()));
                                }
                                if (e.getEmployeeCode() != null) {
                                        String code = e.getEmployeeCode().trim().toUpperCase();
                                        codes.add(code);
                                        if (code.matches("^[A-Z]+\\d+$")) {
                                                String prefix = code.replaceAll("\\d+$", "");
                                                int number = Integer.parseInt(code.substring(prefix.length()));
                                                highest.merge(prefix, number, Math::max);
                                        }
                                }
                        }

                        Map<String, Branch> branches = new HashMap<>();
                        for (Branch b : branchRepository.findByActiveTrueOrderByBranchNameAsc()) {
                                if (b.getBranchName() != null) {
                                        branches.put(b.getBranchName().trim().toLowerCase(), b);
                                }
                        }

                        // -------- rows --------

                        for (int r = headerIndex + 1; r <= sheet.getLastRowNum(); r++) {

                                Row row = sheet.getRow(r);

                                if (row == null || isEmptyRow(row, formatter)) {
                                        continue;
                                }

                                String where = "Row " + (r + 1);

                                try {

                                        String name = cell(row, columns.get(Field.NAME), formatter);
                                        String emailRaw = cell(row, columns.get(Field.EMAIL), formatter);
                                        String contactRaw = cell(row, columns.get(Field.CONTACT), formatter);
                                        String roleRaw = cell(row, columns.get(Field.ROLE), formatter);
                                        String designationRaw = cell(row, columns.get(Field.DESIGNATION), formatter);
                                        String technologyRaw = cell(row, columns.get(Field.TECHNOLOGY), formatter);
                                        String branchRaw = cell(row, columns.get(Field.BRANCH), formatter);

                                        List<String> missing = new ArrayList<>();

                                        // ---- email ----
                                        String email = emailRaw.trim();

                                        if (email.isEmpty()) {
                                                email = null;
                                                missing.add("email");
                                        } else if (!InputValidator.isValidEmail(email)) {
                                                email = null;
                                                missing.add("email (\"" + emailRaw + "\" is not valid)");
                                        }

                                        // ---- name ----
                                        if (name.isBlank() && email != null) {
                                                name = nameFromEmail(email);
                                                missing.add("name (taken from the email)");
                                        }

                                        if (name.isBlank()) {
                                                failed++;
                                                errors.add(where + ": skipped - no name or valid email");
                                                continue;
                                        }

                                        // ---- duplicates ----
                                        if (email != null && emails.contains(email.toLowerCase())) {
                                                failed++;
                                                errors.add(where + ": skipped - " + email + " already exists");
                                                continue;
                                        }

                                        // ---- contact ----
                                        String contact = normalizeMobile(contactRaw);

                                        if (contact == null) {
                                                if (!contactRaw.isBlank()) {
                                                        missing.add("contact number (\"" + contactRaw
                                                                        + "\" is not 10 digits)");
                                                } else {
                                                        missing.add("contact number");
                                                }
                                                contact = "";
                                        }

                                        if (!contact.isEmpty() && people.contains(personKey(name, contact))) {
                                                failed++;
                                                errors.add(where + ": skipped - " + name
                                                                + " with this contact number already exists");
                                                continue;
                                        }

                                        // ---- role / designation ----
                                        String roleText = roleRaw.isBlank() ? designationRaw : roleRaw;
                                        String designation = designationRaw.isBlank() ? roleRaw : designationRaw;

                                        if (roleRaw.isBlank()) {
                                                missing.add("role");
                                        }
                                        if (designationRaw.isBlank()) {
                                                missing.add("designation");
                                        }

                                        String type = personType(designation);

                                        // ---- technology (optional) ----
                                        String technology = cleanList(technologyRaw);

                                        // ---- joining date ----
                                        LocalDate joining = parseDate(row, columns.get(Field.JOINING), formatter);

                                        if (joining == null) {
                                                missing.add("joining date");
                                        }

                                        // ---- branch ----
                                        Branch branch = null;

                                        if (!branchRaw.isBlank()) {
                                                branch = branches.get(branchRaw.trim().toLowerCase());
                                                if (branch == null) {
                                                        missing.add("branch (\"" + branchRaw + "\" not found)");
                                                }
                                        }

                                        // ---- build employee ----
                                        String prefix = "INTERN".equals(type) ? "INT"
                                                        : "MANAGER".equals(type) ? "MGR" : "EMP";
                                        int number = highest.getOrDefault(prefix, 0) + 1;
                                        String code = String.format("%s%03d", prefix, number);

                                        while (codes.contains(code)) {
                                                number++;
                                                code = String.format("%s%03d", prefix, number);
                                        }

                                        Employee employee = new Employee();

                                        employee.setEmployeeCode(code);
                                        employee.setName(name.trim());
                                        employee.setEmail(email);
                                        employee.setContactNumber(contact.isEmpty() ? null : contact);
                                        employee.setRole(roleText.isBlank() ? null : normalizeKey(roleText));
                                        employee.setTechnology(technology);
                                        employee.setDesignation(designation.isBlank() ? null : designation.trim());
                                        employee.setEmployeeType(type);
                                        employee.setBranch(branch);

                                        // Salary is set by the admin later (never imported).
                                        employee.setSalary(0.0);

                                        if (joining != null) {
                                                employee.setJoiningDate(joining.toString());

                                                if ("INTERN".equals(type)) {
                                                        employee.setInternshipStartDate(joining);
                                                } else {
                                                        employee.setEmploymentStartDate(joining);
                                                }
                                        }

                                        // First login password setup.
                                        employee.setPassword(null);
                                        employee.setActive(true);

                                        employee.setLastActionType("EXCEL IMPORT");
                                        employee.setLastActionReason(reason == null ? null : reason.trim());
                                        employee.setLastActionBy(admin.getAdminName());
                                        employee.setLastActionByEmail(admin.getEmail());
                                        employee.setLastActionAt(OffsetDateTime.now(INDIA));

                                        Employee saved;

                                        try {
                                                saved = employeeRepository.save(employee);
                                        } catch (DataIntegrityViolationException e) {
                                                failed++;
                                                errors.add(where + ": could not be saved (the database rejected it)");
                                                continue;
                                        }

                                        // remember what we just added so later rows are checked against it
                                        codes.add(code);
                                        highest.put(prefix, number);
                                        if (email != null) {
                                                emails.add(email.toLowerCase());
                                        }
                                        if (!contact.isEmpty()) {
                                                people.add(personKey(name, contact));
                                        }

                                        saveHistory(admin, saved, reason);
                                        imported++;

                                        if (!missing.isEmpty()) {
                                                incomplete++;
                                                warnings.add(saved.getName() + " (" + saved.getEmployeeCode()
                                                                + "): please fill " + String.join(", ", missing));
                                        }

                                } catch (Exception e) {
                                        failed++;
                                        errors.add(where + ": " + e.getMessage());
                                }
                        }

                } catch (ResponseStatusException e) {
                        throw e;
                } catch (Exception e) {
                        throw bad("Unable to read the Excel file: " + e.getMessage());
                }

                return ResponseEntity.ok(
                                new ExcelImportResponse(imported, failed, incomplete, errors, warnings));
        }

        // =====================================================
        // COLUMN MATCHING
        // =====================================================

        private Map<Field, Integer> mapColumns(Row row, DataFormatter formatter) {

                Map<Integer, String> headings = new LinkedHashMap<>();

                for (Cell cell : row) {
                        String key = squash(formatter.formatCellValue(cell));
                        if (!key.isEmpty()) {
                                headings.put(cell.getColumnIndex(), key);
                        }
                }

                Map<Field, Integer> found = new EnumMap<>(Field.class);
                Set<Integer> used = new HashSet<>();

                // 1) exact names, most specific field first
                for (Field field : EXACT.keySet()) {
                        for (Map.Entry<Integer, String> h : headings.entrySet()) {
                                if (!used.contains(h.getKey()) && EXACT.get(field).contains(h.getValue())) {
                                        found.put(field, h.getKey());
                                        used.add(h.getKey());
                                        break;
                                }
                        }
                }

                // 2) headings that merely contain the key word
                for (Field field : CONTAINS.keySet()) {
                        if (found.containsKey(field)) {
                                continue;
                        }
                        for (Map.Entry<Integer, String> h : headings.entrySet()) {
                                if (used.contains(h.getKey())) {
                                        continue;
                                }
                                boolean hit = CONTAINS.get(field).stream().anyMatch(w -> h.getValue().contains(w));
                                if (hit) {
                                        found.put(field, h.getKey());
                                        used.add(h.getKey());
                                        break;
                                }
                        }
                }

                // 3) a heading containing "name" that is not branch / company / father ...
                if (!found.containsKey(Field.NAME)) {
                        for (Map.Entry<Integer, String> h : headings.entrySet()) {
                                if (!used.contains(h.getKey())
                                                && h.getValue().contains("name")
                                                && !NOT_A_PERSON_NAME.matcher(h.getValue()).find()) {
                                        found.put(Field.NAME, h.getKey());
                                        used.add(h.getKey());
                                        break;
                                }
                        }
                }

                return found;
        }

        private String squash(String value) {
                return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        // =====================================================
        // VALUE HELPERS
        // =====================================================

        private Sheet firstSheetWithData(Workbook workbook) {

                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        Sheet sheet = workbook.getSheetAt(i);
                        if (sheet != null && sheet.getPhysicalNumberOfRows() >= 2) {
                                return sheet;
                        }
                }

                return null;
        }

        private String cell(Row row, Integer column, DataFormatter formatter) {

                if (column == null) {
                        return "";
                }

                Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                if (cell == null) {
                        return "";
                }

                return formatter.formatCellValue(cell).trim();
        }

        private boolean isEmptyRow(Row row, DataFormatter formatter) {

                for (Cell cell : row) {
                        if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                                return false;
                        }
                }

                return true;
        }

        /** Returns a 10 digit Indian mobile number, or null when it is not one. */
        private String normalizeMobile(String raw) {

                if (raw == null) {
                        return null;
                }

                String digits = raw.replaceAll("\\D", "");

                if (digits.length() == 12 && digits.startsWith("91")) {
                        digits = digits.substring(2);
                } else if (digits.length() == 11 && digits.startsWith("0")) {
                        digits = digits.substring(1);
                }

                return InputValidator.isValidMobile(digits) ? digits : null;
        }

        private String nameFromEmail(String email) {

                String local = email.substring(0, email.indexOf('@'));
                StringBuilder out = new StringBuilder();

                for (String part : local.split("[._\\-+0-9]+")) {
                        if (part.isBlank()) {
                                continue;
                        }
                        if (out.length() > 0) {
                                out.append(' ');
                        }
                        out.append(Character.toUpperCase(part.charAt(0)))
                                        .append(part.substring(1).toLowerCase());
                }

                return out.toString();
        }

        private String personKey(String name, String contact) {
                return name.trim().toLowerCase() + "|" + contact.replaceAll("\\D", "");
        }

        private String normalizeKey(String value) {
                return value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        }

        /** "Java; Spring / SQL" -> "Java, Spring, SQL". Empty -> null. */
        private String cleanList(String value) {

                if (value == null || value.isBlank()) {
                        return null;
                }

                List<String> parts = new ArrayList<>();

                for (String part : value.split("[,;/|\\n]+")) {
                        if (!part.isBlank()) {
                                parts.add(part.trim());
                        }
                }

                return parts.isEmpty() ? null : String.join(", ", parts);
        }

        /** Same rule as Add Employee: intern / manager (director, head) / employee. */
        private String personType(String designation) {

                String value = designation == null ? "" : designation.trim();
                String key = normalizeKey(value);

                if (key.equals("INTERN") || key.endsWith("_INTERN") || key.startsWith("INTERN_")) {
                        return "INTERN";
                }

                if (Pattern.compile("\\b(manager|director|head)\\b", Pattern.CASE_INSENSITIVE)
                                .matcher(value).find()) {
                        return "MANAGER";
                }

                return "EMPLOYEE";
        }

        // =====================================================
        // DATES
        // =====================================================

        private static final List<DateTimeFormatter> DATE_FORMATS = new ArrayList<>();

        static {
                for (String pattern : new String[] {
                                "d-M-uuuu", "d/M/uuuu", "d.M.uuuu", "uuuu-M-d", "uuuu/M/d",
                                "d-MMM-uuuu", "d MMM uuuu", "d MMMM uuuu", "MMM d, uuuu", "MMMM d, uuuu",
                                "d-M-uu", "d/M/uu" }) {

                        DATE_FORMATS.add(new DateTimeFormatterBuilder()
                                        .parseCaseInsensitive()
                                        .appendPattern(pattern)
                                        .toFormatter(Locale.ENGLISH));
                }
        }

        private LocalDate parseDate(Row row, Integer column, DataFormatter formatter) {

                if (column == null) {
                        return null;
                }

                Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                if (cell == null) {
                        return null;
                }

                try {
                        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                return cell.getLocalDateTimeCellValue().toLocalDate();
                        }
                } catch (Exception ignored) {
                        // fall through to text parsing
                }

                String text = formatter.formatCellValue(cell).trim();

                if (text.isEmpty()) {
                        return null;
                }

                for (DateTimeFormatter format : DATE_FORMATS) {
                        try {
                                return LocalDate.parse(text, format);
                        } catch (Exception ignored) {
                                // try the next format
                        }
                }

                return null;
        }

        // =====================================================
        // ADMIN + HISTORY
        // =====================================================

        private Admin findAdmin(String email) {

                if (email == null || email.trim().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin email required");
                }

                return adminRepository.findByEmail(email.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "Admin not found"));
        }

        private void saveHistory(Admin admin, Employee employee, String reason) {

                AdminActionHistory history = new AdminActionHistory();

                history.setAdminName(admin.getAdminName());
                history.setAction("EXCEL IMPORT");
                history.setEmployeeId(employee.getId());
                history.setEmployeeCode(employee.getEmployeeCode());
                history.setEmployeeName(employee.getName());
                history.setActionDate(LocalDate.now(INDIA));
                history.setActionTime(LocalTime.now(INDIA));
                history.setFieldName("Employee");
                history.setOldValue(null);
                history.setNewValue(employee.getName());
                history.setReason(reason == null ? null : reason.trim());

                historyRepository.save(history);
        }

        private ResponseStatusException bad(String message) {
                return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
}