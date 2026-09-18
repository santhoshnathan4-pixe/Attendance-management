USE growbytee_attendance;

ALTER TABLE leave_requests
ADD leave_duration DECIMAL(3,1) NOT NULL
    CONSTRAINT DF_leave_duration DEFAULT 1.0;

ALTER TABLE leave_requests
ADD half_day_session VARCHAR(20) NULL;

SELECT
    id,
    employee_code,
    name,
    email,
    role
FROM employees
WHERE role = 'ADMIN';

INSERT INTO employees
(employee_code, name, email, password, role, salary, joining_date)
VALUES
('GBADMIN01', 'Growbytee Admin', 'admin@growbytee.com', 'admin123', 'ADMIN', 0, '2026-09-16');