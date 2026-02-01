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

public interface InquiryRepository extends JpaRepository<Inquiry, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Inquiry> {
    List<Inquiry> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    List<Inquiry> findByParentTypeAndChildTypeOrderByCreatedAtDesc(InquiryParentType parent, InquiryChildType child);
}
