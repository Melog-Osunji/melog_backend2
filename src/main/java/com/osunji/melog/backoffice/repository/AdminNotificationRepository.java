package com.osunji.melog.backoffice.repository;

import com.osunji.melog.backoffice.entity.AdminNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, UUID> {
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
