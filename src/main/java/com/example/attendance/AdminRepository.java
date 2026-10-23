package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {

    Optional<Admin> findByEmail(String email);

    /**
     * Same method name as before, so every existing caller keeps working.
     * It now compares against the stored hash (or an old plain-text value).
     */
    default Optional<Admin> findByEmailAndPassword(
            String email,
            String password) {

        if (email == null || password == null) {
            return Optional.empty();
        }

        return findByEmail(email.trim())
                .filter(admin -> PasswordUtil.matches(password, admin.getPassword()));
    }
}
