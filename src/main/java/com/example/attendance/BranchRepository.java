
package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchName(String branchName);

    List<Branch> findByActiveTrueOrderByBranchNameAsc();

}

