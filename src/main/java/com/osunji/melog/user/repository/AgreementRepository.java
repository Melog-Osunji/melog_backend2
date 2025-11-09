package com.osunji.melog.user.repository;

import com.osunji.melog.user.domain.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    boolean existsByUser_Id(UUID userId);
    Optional<Agreement> findByUserId(UUID userId);
}
