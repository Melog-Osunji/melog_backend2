package com.osunji.melog.user;

import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {  // UUID → String으로 되돌리기

    Optional<User> findByEmail(String email);
    Optional<User> findByOidc(String oidc);
    // ✅ UUID 변환 문제 해결을 위한 명시적 쿼리
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByUUID(@Param("userId") UUID userId);

    // ✅ 백업용 - String으로 직접 조회
    @Query(value = "SELECT * FROM users WHERE id::text = :userId", nativeQuery = true)
    Optional<User> findByIdString(@Param("userId") String userId);

    // ✅ 백업용2 - UUID 캐스팅
    @Query(value = "SELECT * FROM users WHERE id = CAST(:userId AS uuid)", nativeQuery = true)
    Optional<User> findByIdWithCast(@Param("userId") String userId);
    // ID 리스트로 여러 유저 조회 (검색 기능용)
    List<User> findAllByIdIn(List<UUID> ids);
}
