package com.osunji.melog.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.osunji.melog.user.domain.Onboarding;

public interface OnboardingRepository extends JpaRepository<Onboarding, UUID> {
    Optional<Onboarding> findByUser_Id(UUID userId);
    boolean existsByUser_Id(UUID userId); // 이미 있는지 빠르게 체크용

}