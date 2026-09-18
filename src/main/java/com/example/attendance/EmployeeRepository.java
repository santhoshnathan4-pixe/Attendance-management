package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Integer> {

    Optional<Employee> findByEmailAndPasswordAndActiveTrue(
            String email,
            String password
    );

    Optional<Employee> findByEmailAndActiveTrue(
            String email
    );

    List<Employee> findByActiveTrue();

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmployeeCode(
            String employeeCode
    );

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);
}