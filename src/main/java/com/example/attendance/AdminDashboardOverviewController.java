package com.example.attendance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * "Company at a glance": one short headline per module (the short news) and
 * the detail rows behind it (the full news), so the MD can see everything
 * on the dashboard without opening each module.
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardOverviewController {

    private static final java.time.ZoneId INDIA = java.time.ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final OvertimeRecordRepository overtimeRepository;
    private final ShiftSettingRepository shiftSettingRepository;
    private final ShiftPolicyService shiftPolicyService;
    private final BranchRepository branchRepository;
    private final EmployeeFaceRepository faceRepository;
    private final AnnouncementRepository announcementRepository;
    private final WorkingDaySettingRepository workingDayRepository;
    private final AdminActionHistoryRepository historyRepository;
    private final PayrollPeriodService payrollPeriodService;

    public AdminDashboardOverviewController(
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository,
            OvertimeRecordRepository overtimeRepository,
            ShiftSettingRepository shiftSettingRepository,
            ShiftPolicyService shiftPolicyService,
            BranchRepository branchRepository,
            EmployeeFaceRepository faceRepository,
            AnnouncementRepository announcementRepository,
            WorkingDaySettingRepository workingDayRepository,
            AdminActionHistoryRepository historyRepository,
            PayrollPeriodService payrollPeriodService) {

        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.overtimeRepository = overtimeRepository;
        this.shiftSettingRepository = shiftSettingRepository;
        this.shiftPolicyService = shiftPolicyService;
        this.branchRepository = branchRepository;
        this.faceRepository = faceRepository;
        this.announcementRepository = announcementRepository;
        this.workingDayRepository = workingDayRepository;
        this.historyRepository = historyRepository;
        this.payrollPeriodService = payrollPeriodService;
    }

    private volatile List<Map<String, Object>> overviewCache = null;
    private volatile long overviewCacheAt = 0;

    @GetMapping("/overview")
    public List<Map<String, Object>> overview() {

        List<Map<String, Object>> cached = overviewCache;
        if (cached != null && System.currentTimeMillis() - overviewCacheAt < 30000) {
            return cached;
        }

        List<Map<String, Object>> fresh = computeOverview();
        overviewCache = fresh;
        overviewCacheAt = System.currentTimeMillis();
        return fresh;
    }

    private List<Map<String, Object>> computeOverview() {

        LocalDate today = LocalDate.now(INDIA);
        List<Employee> staff = employeeRepository.findByActiveTrue();

        Map<Integer, Employee> byId = new HashMap<>();
        for (Employee e : staff) {
            byId.put(e.getId(), e);
        }

        List<Map<String, Object>> modules = new ArrayList<>();

        modules.add(safe("leave", () -> leaveModule(today, byId)));
        modules.add(safe("overtime", () -> overtimeModule(byId)));
        modules.add(safe("payroll", () -> payrollModule(today, staff)));
        modules.add(safe("interns", () -> internModule(today, staff)));
        modules.add(safe("team", () -> teamModule(today, staff)));
        modules.add(safe("shifts", () -> shiftModule(staff)));
        modules.add(safe("verification", () -> verificationModule(staff)));
        modules.add(safe("calendar", () -> calendarModule(today)));
        modules.add(safe("activity", this::activityModule));

        modules.removeIf(Objects::isNull);

        return modules;
    }

    // ---------------------------------------------------------------- modules

    private Map<String, Object> leaveModule(LocalDate today, Map<Integer, Employee> byId) {

        List<LeaveRequest> all = leaveRequestRepository
                .findByLeaveDateBetween(today.minusDays(7), today.plusDays(180));

        Set<String> seen = new HashSet<>();
        List<List<String>> rows = new ArrayList<>();
        int pending = 0;

        List<LeaveRequest> pendingList = all.stream()
                .filter(l -> "PENDING".equalsIgnoreCase(l.getStatus()) && l.getLeaveDate() != null
                        && !l.getLeaveDate().isBefore(today.minusDays(7)))
                .sorted(Comparator.comparing(LeaveRequest::getLeaveDate))
                .toList();

        for (LeaveRequest l : pendingList) {
            String key = l.getRequestGroupId() != null ? l.getRequestGroupId() : "id" + l.getId();
            if (!seen.add(key)) {
                continue;
            }
            pending++;
            rows.add(List.of(nameOf(byId, l.getEmployeeId()), nice(l.getLeaveType()),
                    l.getLeaveDate().format(DAY), "Waiting for approval"));
        }

        long onLeaveToday = all.stream()
                .filter(l -> today.equals(l.getLeaveDate())
                        && "APPROVED".equalsIgnoreCase(l.getStatus())
                        && l.getLeaveType() != null
                        && !"PERMISSION".equalsIgnoreCase(l.getLeaveType()))
                .map(LeaveRequest::getEmployeeId)
                .distinct()
                .count();

        String headline = pending == 0
                ? "No leave requests waiting"
                : pending + (pending == 1 ? " leave request is" : " leave requests are") + " waiting";

        return module("leave", "Leave", pending > 0 ? "warn" : "ok", headline,
                onLeaveToday + (onLeaveToday == 1 ? " person is" : " people are") + " on approved leave today.",
                "/admin/leave.html", "Open Leave",
                List.of("Employee", "Type", "Date", "Status"), rows,
                "Nothing is waiting. All requests are decided.");
    }

    private Map<String, Object> overtimeModule(Map<Integer, Employee> byId) {

        List<OvertimeRecord> pending = overtimeRepository.findByStatusOrderByOtDateAsc("PENDING");

        List<List<String>> rows = new ArrayList<>();
        for (OvertimeRecord o : pending) {
            rows.add(List.of(nameOf(byId, o.getEmployeeId()),
                    o.getOtDate() == null ? "" : o.getOtDate().format(DAY),
                    nice(o.getDayType()), nice(o.getWorkType())));
        }

        return module("overtime", "Overtime", pending.isEmpty() ? "ok" : "warn",
                pending.isEmpty() ? "No overtime waiting" : pending.size() + " overtime entries need a decision",
                "Approve or reject so salary can be finalised.",
                "/admin/leave.html", "Open Leave & Overtime",
                List.of("Employee", "Date", "Day", "Work"), rows, "Nothing is waiting.");
    }

    private Map<String, Object> payrollModule(LocalDate today, List<Employee> staff) {

        PayrollPeriodService.Period period = payrollPeriodService.resolve(today.getYear(), today.getMonthValue());

        Map<String, double[]> byRole = new TreeMap<>();
        double total = 0;
        int paid = 0;

        for (Employee e : staff) {
            if (isIntern(e) || e.getSalary() == null || e.getSalary() <= 0) {
                continue;
            }
            String role = e.getRole() == null || e.getRole().isBlank() ? "Other" : e.getRole();
            double[] cell = byRole.computeIfAbsent(role, k -> new double[2]);
            cell[0] += 1;
            cell[1] += e.getSalary();
            total += e.getSalary();
            paid++;
        }

        List<List<String>> rows = new ArrayList<>();
        byRole.forEach((role, cell) -> rows.add(List.of(role, String.valueOf((int) cell[0]), money(cell[1]))));

        return module("payroll", "Salary", "info",
                "Salary period " + period.start().format(SHORT) + " to " + period.end().format(SHORT),
                paid + " salaried staff, fixed monthly total " + money(total) + ". Interns' stipend is separate.",
                "/admin/salary.html", "Open Salary",
                List.of("Role", "People", "Monthly salary"), rows, "No salary data yet.");
    }

    private Map<String, Object> internModule(LocalDate today, List<Employee> staff) {

        List<Employee> interns = staff.stream().filter(this::isIntern).toList();

        List<List<String>> rows = new ArrayList<>();
        int endingSoon = 0;

        List<Employee> sorted = interns.stream()
                .filter(e -> e.getInternshipEndDate() != null)
                .sorted(Comparator.comparing(Employee::getInternshipEndDate))
                .toList();

        for (Employee e : sorted) {
            long days = ChronoUnit.DAYS.between(today, e.getInternshipEndDate());
            if (days <= 30) {
                endingSoon++;
            }
            if (days <= 60) {
                rows.add(List.of(e.getName(), e.getEmployeeCode() == null ? "" : e.getEmployeeCode(),
                        e.getInternshipEndDate().format(DAY),
                        days < 0 ? "Ended " + (-days) + " days ago" : days + " days left"));
            }
        }

        return module("interns", "Interns", endingSoon > 0 ? "warn" : "ok",
                endingSoon == 0 ? interns.size() + " interns, none ending this month"
                        : endingSoon + (endingSoon == 1 ? " internship ends" : " internships end") + " within 30 days",
                interns.size() + " active interns in total. Decide to extend or convert before the end date.",
                "/admin/employees.html", "Open Employees",
                List.of("Name", "Code", "Ends on", "Time left"), rows, "No internship ends in the next 60 days.");
    }

    private Map<String, Object> teamModule(LocalDate today, List<Employee> staff) {

        long interns = staff.stream().filter(this::isIntern).count();
        long managers = staff.stream().filter(e -> "MANAGER".equalsIgnoreCase(e.getEmployeeType())).count();
        long employees = staff.size() - interns - managers;

        List<List<String>> rows = new ArrayList<>();

        staff.stream()
                .filter(e -> joined(e) != null && !joined(e).isBefore(today.minusDays(30)))
                .sorted(Comparator.comparing(this::joined).reversed())
                .forEach(e -> rows.add(List.of(e.getName(), e.getRole() == null ? "" : e.getRole(),
                        e.getDesignation() == null ? "" : e.getDesignation(), joined(e).format(DAY))));

        return module("team", "People", "info",
                staff.size() + " active people",
                employees + " employees, " + managers + " managers, " + interns + " interns. "
                        + rows.size() + " joined in the last 30 days.",
                "/admin/employees.html", "Open Employees",
                List.of("New joiner", "Role", "Designation", "Joined"), rows, "No new joiners in the last 30 days.");
    }

    private Map<String, Object> shiftModule(List<Employee> staff) {

        Map<String, Integer> count = new TreeMap<>();
        for (Employee e : staff) {
            String type;
            try {
                type = shiftPolicyService.getEffectiveShift(e.getId()).getShiftType();
            } catch (Exception ex) {
                type = "GENERAL";
            }
            count.merge(type == null ? "GENERAL" : type, 1, Integer::sum);
        }

        Map<String, ShiftSetting> settings = new HashMap<>();
        for (ShiftSetting s : shiftSettingRepository.findAll()) {
            settings.put(s.getShiftType(), s);
        }

        List<List<String>> rows = new ArrayList<>();
        count.forEach((type, n) -> {
            ShiftSetting s = settings.get(type);
            String time = s == null || s.getStartTime() == null || s.getEndTime() == null
                    ? "-" : s.getStartTime() + " to " + s.getEndTime();
            rows.add(List.of(nice(type), time, String.valueOf(n)));
        });

        String headline = count.size() <= 1
                ? "Everyone is on " + (count.isEmpty() ? "General" : nice(count.keySet().iterator().next())) + " shift"
                : count.size() + " shifts are in use";

        return module("shifts", "Shifts", "info", headline,
                "Each person follows the shift set for them or for their role.",
                "/admin/shift-settings.html", "Open Shift Settings",
                List.of("Shift", "Timing", "People"), rows, "No shifts set.");
    }

    private Map<String, Object> verificationModule(List<Employee> staff) {

        List<Branch> branches = branchRepository.findByActiveTrueOrderByBranchNameAsc();

        Set<Long> registered = new HashSet<>();
        registered.addAll(faceRepository.findActiveEmployeeIds());

        boolean anyFace = branches.stream().anyMatch(b -> Boolean.TRUE.equals(b.getFaceVerificationEnabled()));

        List<List<String>> rows = new ArrayList<>();
        for (Branch b : branches) {
            rows.add(List.of(b.getBranchName(),
                    Boolean.TRUE.equals(b.getLocationVerificationEnabled())
                            ? "On, within " + b.getAllowedRadiusMeters() + " m" : "Off",
                    Boolean.TRUE.equals(b.getFaceVerificationEnabled()) ? "On" : "Off"));
        }

        long notEnrolled = anyFace
                ? staff.stream().filter(e -> !registered.contains(e.getId().longValue())).count()
                : 0;

        return module("verification", "Location and Face", anyFace && notEnrolled > 0 ? "warn" : "info",
                anyFace
                        ? (notEnrolled == 0 ? "Face is registered for everyone"
                                : notEnrolled + " people have not registered their face")
                        : "Location and face checks",
                branches.size() + " active branches. Face check works only after the person registers once.",
                "/admin/branch-management.html", "Open Branches",
                List.of("Branch", "Location check", "Face check"), rows, "No branch is set up yet.");
    }

    private Map<String, Object> calendarModule(LocalDate today) {

        List<List<String>> rows = new ArrayList<>();
        LocalDate until = today.plusDays(30);

        for (WorkingDaySetting w : workingDayRepository
                .findBySettingDateBetweenAndActiveTrueOrderBySettingDateAsc(today, until)) {
            rows.add(List.of(w.getSettingDate().format(DAY), nice(w.getSettingType()),
                    w.getReason() == null ? "" : w.getReason(), w.getSettingDate().toString()));
        }

        for (Announcement a : announcementRepository
                .findByAnnouncementDateBetweenOrderByAnnouncementDateAsc(today, until)) {
            rows.add(List.of(a.getAnnouncementDate().format(DAY), "Announcement",
                    a.getTitle() == null ? "" : a.getTitle(), a.getAnnouncementDate().toString()));
        }

        rows.sort(Comparator.comparing(r -> r.get(3)));
        List<List<String>> clean = new ArrayList<>();
        for (List<String> r : rows) {
            clean.add(r.subList(0, 3));
        }

        return module("calendar", "Holidays and Announcements", "info",
                clean.isEmpty() ? "Nothing planned in the next 30 days" : clean.size() + " dates coming up",
                "Holidays, special working days and announcements for the next 30 days.",
                "/admin/working-day-settings.html", "Open Working Days",
                List.of("Date", "Type", "Note"), clean, "Nothing is planned.");
    }

    private Map<String, Object> activityModule() {

        List<AdminActionHistory> latest = historyRepository
                .findAllByOrderByActionDateDescActionTimeDesc()
                .stream().limit(10).toList();

        List<List<String>> rows = new ArrayList<>();
        for (AdminActionHistory h : latest) {
            rows.add(List.of(
                    h.getActionDate() == null ? "" : h.getActionDate().format(SHORT)
                            + (h.getActionTime() == null ? "" : " " + h.getActionTime().toString().substring(0, 5)),
                    h.getAdminName() == null ? "" : h.getAdminName(),
                    nice(h.getAction()),
                    h.getEmployeeName() == null ? "" : h.getEmployeeName()));
        }

        return module("activity", "Recent admin actions", "info",
                latest.isEmpty() ? "No admin action yet" : "Last action: " + rows.get(0).get(2) + " by " + rows.get(0).get(1),
                "The latest 10 changes made by admins.",
                "/admin/history.html", "Open History",
                List.of("When", "Admin", "Action", "About"), rows, "No actions yet.");
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> module(String key, String title, String tone, String headline,
            String detail, String link, String linkLabel,
            List<String> columns, List<List<String>> rows, String empty) {

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("title", title);
        m.put("tone", tone);
        m.put("headline", headline);
        m.put("detail", detail);
        m.put("link", link);
        m.put("linkLabel", linkLabel);
        m.put("columns", columns);
        m.put("rows", rows);
        m.put("empty", empty);
        return m;
    }

    /** One broken module must never blank the whole dashboard. */
    private Map<String, Object> safe(String key, java.util.function.Supplier<Map<String, Object>> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return module(key, nice(key), "info", "Could not load this summary", "Open the module to see it.",
                    "", "", List.of(), List.of(), "No data.");
        }
    }

    private boolean isIntern(Employee e) {
        return "INTERN".equalsIgnoreCase(e.getEmployeeType()) || e.getInternshipEndDate() != null;
    }

    private LocalDate joined(Employee e) {
        try {
            return e.getJoiningDate() == null || e.getJoiningDate().isBlank()
                    ? null : LocalDate.parse(e.getJoiningDate().trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String nameOf(Map<Integer, Employee> byId, Integer id) {
        Employee e = byId.get(id);
        return e == null ? "Employee #" + id : e.getName();
    }

    private String nice(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String t = value.trim().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private String money(double amount) {
        return "Rs " + NumberFormat.getIntegerInstance(new Locale("en", "IN")).format(Math.round(amount));
    }
}
