package com.osunji.melog.backoffice.repository;

import com.osunji.melog.backoffice.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {
    Optional<AdminAccount> findByLoginId(String loginId);
}
