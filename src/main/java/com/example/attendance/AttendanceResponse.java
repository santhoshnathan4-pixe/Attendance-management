
package com.example.attendance;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceResponse {

    private Integer id;
    private String employeeName;
    private LocalDate attendanceDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String status;

    public AttendanceResponse(
            String employeeName,
            LocalDate attendanceDate,
            LocalTime checkIn,
            LocalTime checkOut,
            String status) {

        this.employeeName = employeeName;
        this.attendanceDate = attendanceDate;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = status;
    }

    // Additional constructor with attendance record ID
    public AttendanceResponse(
            Integer id,
            String employeeName,
            LocalDate attendanceDate,
            LocalTime checkIn,
            LocalTime checkOut,
            String status) {

        this.id = id;
        this.employeeName = employeeName;
        this.attendanceDate = attendanceDate;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public String getStatus() {
        return status;
    }
}
