package com.osunji.melog.inquirySettings.repository;


import com.osunji.melog.inquirySettings.domain.Inquiry;
import com.osunji.melog.inquirySettings.domain.InquiryChildType;
import com.osunji.melog.inquirySettings.domain.InquiryParentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
    List<Inquiry> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    List<Inquiry> findByParentTypeAndChildTypeOrderByCreatedAtDesc(InquiryParentType parent, InquiryChildType child);

    @Query("""
        SELECT i FROM Inquiry i JOIN i.user u
        WHERE (:parentType IS NULL OR i.parentType = :parentType)
          AND (:query IS NULL OR u.email LIKE CONCAT('%', :query, '%'))
          AND (:fromDate IS NULL OR i.createdAt >= :fromDate)
          AND (:toDate IS NULL OR i.createdAt <= :toDate)
        ORDER BY i.createdAt DESC
        """)
    Page<Inquiry> findByAdminFilter(
            @Param("parentType") InquiryParentType parentType,
            @Param("query") String query,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
