package com.osunji.melog.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.osunji.melog.user.domain.Onboarding;

public interface OnboardingRepository extends JpaRepository<Onboarding, String> {
    Optional<Onboarding> findByUser_Id(UUID userId);
}