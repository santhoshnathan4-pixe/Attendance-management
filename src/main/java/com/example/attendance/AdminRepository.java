package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {

    Optional<Admin> findByEmailAndPassword(
            String email,
            String password
    );

    Optional<Admin> findByEmail(String email);
}