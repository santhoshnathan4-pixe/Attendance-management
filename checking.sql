USE growbytee_attendance;
GO

CREATE TABLE attendance (
    id INT IDENTITY(1,1) PRIMARY KEY,
    employee_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in TIME NULL,
    check_out TIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',

    CONSTRAINT FK_attendance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
);
GO


SELECT *
FROM attendance;


SELECT *
FROM attendance
WHERE employee_id = 1;



