
package com.example.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationPolicyRepository
        extends JpaRepository<VerificationPolicy, Long> {

    List<VerificationPolicy> findByActiveTrueOrderByIdDesc();

    List<VerificationPolicy> findByBranch_IdAndActiveTrueOrderByIdDesc(
            Long branchId
    );

    List<VerificationPolicy> findByTargetTypeAndActiveTrue(
            String targetType
    );

    List<VerificationPolicy> findByTargetTypeAndTargetValueAndActiveTrue(
            String targetType,
            String targetValue
    );
}
