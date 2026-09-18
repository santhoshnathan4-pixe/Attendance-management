USE growbytee_attendance;
GO

CREATE TABLE employees (
    id INT IDENTITY(1,1) PRIMARY KEY,
    employee_code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    salary DECIMAL(10,2),
    joining_date DATE
);
GO

SELECT * FROM employees;

USE growbytee_attendance;
GO

INSERT INTO employees
(employee_code, name, email, password, role, salary, joining_date)
VALUES
('GB001', 'Test Employee', 'employee@growbytee.com', '1234', 'EMPLOYEE', 25000, '2026-09-16');
GO