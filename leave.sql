CREATE TABLE leave_requests (
    id INT IDENTITY(1,1) PRIMARY KEY,

    employee_id INT NOT NULL,

    leave_type VARCHAR(20) NOT NULL,
    leave_date DATE NOT NULL,

    permission_start TIME NULL,
    permission_end TIME NULL,

    reason VARCHAR(255) NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

	SELECT * FROM leave_requests;

    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_leave_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
);


