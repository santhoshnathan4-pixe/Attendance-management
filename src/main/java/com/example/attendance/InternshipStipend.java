package com.example.attendance;

import jakarta.persistence.*;

@Entity
@Table(name = "internship_stipend")
public class InternshipStipend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "stipend_year", nullable = false)
    private Integer stipendYear;

    @Column(name = "stipend_month", nullable = false)
    private Integer stipendMonth;

    @Column(name = "stipend_type", nullable = false)
    private String stipendType;

    @Column(name = "stipend_amount", nullable = false)
    private Double stipendAmount = 0.0;

    public Integer getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Integer getStipendYear() {
        return stipendYear;
    }

    public Integer getStipendMonth() {
        return stipendMonth;
    }

    public String getStipendType() {
        return stipendType;
    }

    public Double getStipendAmount() {
        return stipendAmount;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public void setStipendYear(Integer stipendYear) {
        this.stipendYear = stipendYear;
    }

    public void setStipendMonth(Integer stipendMonth) {
        this.stipendMonth = stipendMonth;
    }

    public void setStipendType(String stipendType) {
        this.stipendType = stipendType;
    }

    public void setStipendAmount(Double stipendAmount) {
        this.stipendAmount = stipendAmount;
    }
}