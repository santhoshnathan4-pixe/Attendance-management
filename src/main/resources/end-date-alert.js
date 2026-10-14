/* =====================================================
   GROWBYTEE - END DATE ALERT (Dashboard)
   Shows a red alert when an intern's internship end date
   or an employee's 1 year term ends within the next 7 days.
   Click the alert to see who, and how many days are left.

   Add this ONE line before </body> of the dashboard page:
   <script src="end-date-alert.js"></script>
===================================================== */

(function () {

    "use strict";

    var ALERT_DAYS = 7;

    var API_BASE =
        (typeof API_BASE_URL !== "undefined")
            ? API_BASE_URL
            : (
                window.location.hostname === "localhost" ||
                window.location.hostname === "127.0.0.1"
                    ? "http://localhost:8080"
                    : "https://attendance-management-d5h6.onrender.com"
            );


    /* ---------------- STYLES ---------------- */

    function injectStyles() {

        var style = document.createElement("style");

        style.textContent = `
            .end-alert-bar { display: none; align-items: center; gap: 12px; background: #dc2626; color: white; padding: 14px 18px; border-radius: 12px; margin-bottom: 20px; cursor: pointer; box-shadow: 0 6px 18px rgba(220, 38, 38, 0.35); animation: endAlertPulse 2s infinite; }

            .end-alert-bar.show { display: flex; }

            .end-alert-icon { font-size: 20px; }

            .end-alert-text { flex: 1; font-size: 14px; font-weight: 700; }

            .end-alert-btn { border: none; background: white; color: #b91c1c; padding: 8px 14px; border-radius: 8px; font-size: 13px; font-weight: 700; cursor: pointer; }

            @keyframes endAlertPulse { 0% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.5); } 70% { box-shadow: 0 0 0 10px rgba(220, 38, 38, 0); } 100% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0); } }

            .end-alert-modal { display: none; position: fixed; inset: 0; background: rgba(15, 23, 42, 0.65); z-index: 200; align-items: center; justify-content: center; padding: 20px; }

            .end-alert-modal.show { display: flex; }

            .end-alert-box { width: 100%; max-width: 560px; background: white; border-radius: 16px; padding: 24px; max-height: 88vh; overflow-y: auto; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25); }

            .end-alert-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }

            .end-alert-head h2 { font-size: 19px; color: #991b1b; }

            .end-alert-close { border: none; background: #f1f5f9; width: 34px; height: 34px; border-radius: 50%; cursor: pointer; font-size: 18px; }

            .end-alert-item { border: 1px solid #fecaca; background: #fef2f2; border-radius: 12px; padding: 14px; margin-bottom: 12px; }

            .end-alert-name { font-size: 15px; font-weight: 700; color: #111827; }

            .end-alert-tag { display: inline-block; margin-left: 8px; padding: 3px 9px; border-radius: 20px; font-size: 11px; font-weight: 700; background: #dbeafe; color: #1e40af; }

            .end-alert-tag.employee { background: #fde68a; color: #92400e; }

            .end-alert-days { margin-top: 6px; font-size: 14px; font-weight: 700; color: #b91c1c; }

            .end-alert-date { margin-top: 2px; font-size: 12px; color: #64748b; }

            .end-alert-hint { margin-top: 6px; font-size: 12px; color: #475569; }

            .end-alert-foot { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }

            .end-alert-foot button { border: none; border-radius: 8px; padding: 10px 16px; font-size: 14px; font-weight: 600; cursor: pointer; }

            .end-alert-open { background: #2563eb; color: white; }

            .end-alert-dismiss { background: #64748b; color: white; }
        `;

        document.head.appendChild(style);

    }


    /* ---------------- DATE HELPERS ---------------- */

    function isIntern(designation) {

        return String(designation || "").trim().toLowerCase().endsWith("intern");

    }


    function parseLocalDate(value) {

        if (!value) { return null; }

        var parts = String(value).substring(0, 10).split("-");

        if (parts.length !== 3) { return null; }

        var date = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));

        return isNaN(date.getTime()) ? null : date;

    }


    // joining date + months - 1 day (same rule as the backend)
    function addMonthsMinusOneDay(dateValue, months) {

        var start = parseLocalDate(dateValue);

        if (!start || !months) { return null; }

        var day = start.getDate();

        var result = new Date(start.getFullYear(), start.getMonth() + months, 1);

        var lastDay = new Date(result.getFullYear(), result.getMonth() + 1, 0).getDate();

        result.setDate(Math.min(day, lastDay));

        result.setDate(result.getDate() - 1);

        return result;

    }


    function formatDisplayDate(date) {

        return (
            String(date.getDate()).padStart(2, "0") + "-" +
            String(date.getMonth() + 1).padStart(2, "0") + "-" +
            date.getFullYear()
        );

    }


    function escapeHtml(value) {

        return String(value === null || value === undefined ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");

    }


    function getEndDate(employee) {

        var intern = isIntern(employee.designation);

        var saved = parseLocalDate(intern ? employee.internshipEndDate : employee.employmentEndDate);

        if (saved) { return saved; }

        if (!employee.joiningDate) { return null; }

        if (intern) {

            var months = Number(employee.internshipDurationMonths || 0);

            return months ? addMonthsMinusOneDay(employee.joiningDate, months) : null;

        }

        return addMonthsMinusOneDay(employee.joiningDate, 12);

    }


    /* ---------------- UI ---------------- */

    var alertItems = [];


    function buildUi() {

        var bar = document.createElement("div");

        bar.id = "endAlertBar";

        bar.className = "end-alert-bar";

        bar.innerHTML =
            '<span class="end-alert-icon">&#9888;</span>' +
            '<span class="end-alert-text" id="endAlertText"></span>' +
            '<button type="button" class="end-alert-btn">View</button>';

        bar.addEventListener("click", openModal);


        var container =
            document.querySelector(".content") ||
            document.querySelector("main") ||
            document.body;

        container.insertBefore(bar, container.firstChild);


        var modal = document.createElement("div");

        modal.id = "endAlertModal";

        modal.className = "end-alert-modal";

        modal.innerHTML =
            '<div class="end-alert-box">' +
                '<div class="end-alert-head">' +
                    '<h2>End date closing soon</h2>' +
                    '<button type="button" class="end-alert-close" id="endAlertClose">&times;</button>' +
                '</div>' +
                '<div id="endAlertList"></div>' +
                '<div class="end-alert-foot">' +
                    '<button type="button" class="end-alert-open" id="endAlertOpen">Open Employees</button>' +
                    '<button type="button" class="end-alert-dismiss" id="endAlertDismiss">Close</button>' +
                '</div>' +
            '</div>';

        document.body.appendChild(modal);


        document.getElementById("endAlertClose").addEventListener("click", closeModal);

        document.getElementById("endAlertDismiss").addEventListener("click", closeModal);

        document.getElementById("endAlertOpen").addEventListener("click", function () {

            window.location.href = "employees.html";

        });

        modal.addEventListener("click", function (event) {

            if (event.target === modal) { closeModal(); }

        });

    }


    function openModal() {

        var list = document.getElementById("endAlertList");

        list.innerHTML = alertItems.map(function (item) {

            var daysText =
                item.days === 0
                    ? "End date is today"
                    : "Only " + item.days + (item.days === 1 ? " day" : " days") + " left";

            var typeText = item.intern ? "Internship" : "Employee (1 year)";

            var hint = item.intern
                ? "Internship is about to finish. You can convert to employee or end the internship."
                : "1 year term is about to finish. You can give a salary increment or renew."

            return (
                '<div class="end-alert-item">' +
                    '<div class="end-alert-name">' + escapeHtml(item.name) + ' (' + escapeHtml(item.code) + ')' +
                        '<span class="end-alert-tag ' + (item.intern ? "" : "employee") + '">' + typeText + '</span>' +
                    '</div>' +
                    '<div class="end-alert-days">' + daysText + '</div>' +
                    '<div class="end-alert-date">End date: ' + formatDisplayDate(item.end) + '</div>' +
                    '<div class="end-alert-hint">' + hint + '</div>' +
                '</div>'
            );

        }).join("");

        document.getElementById("endAlertModal").classList.add("show");

    }


    function closeModal() {

        document.getElementById("endAlertModal").classList.remove("show");

    }


    function renderAlerts(employees) {

        var today = new Date();

        today.setHours(0, 0, 0, 0);

        alertItems = [];

        employees.forEach(function (employee) {

            var end = getEndDate(employee);

            if (!end) { return; }

            var days = Math.round((end.getTime() - today.getTime()) / 86400000);

            if (days < 0 || days > ALERT_DAYS) { return; }

            alertItems.push({
                name: employee.name,
                code: employee.employeeCode,
                intern: isIntern(employee.designation),
                days: days,
                end: end
            });

        });

        alertItems.sort(function (a, b) { return a.days - b.days; });


        var bar = document.getElementById("endAlertBar");

        if (alertItems.length === 0) {

            bar.classList.remove("show");

            return;

        }

        document.getElementById("endAlertText").textContent =
            alertItems.length + (alertItems.length === 1 ? " employee" : " employees") +
            " - end date closing within " + ALERT_DAYS + " days. Click to view.";

        bar.classList.add("show");

    }


    function loadEndDateAlerts() {

        fetch(API_BASE + "/admin/employees")
            .then(function (response) {

                if (!response.ok) { throw new Error("HTTP " + response.status); }

                return response.json();

            })
            .then(function (employees) { renderAlerts(employees || []); })
            .catch(function (error) { console.warn("End date alert load error:", error); });

    }


    function init() {

        injectStyles();

        buildUi();

        loadEndDateAlerts();

    }


    if (document.readyState === "loading") {

        document.addEventListener("DOMContentLoaded", init);

    }
    else { init(); }

})();