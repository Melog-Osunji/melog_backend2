package com.osunji.melog.user.repository;

import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByOidcAndPlatform(String oidc, Platform platform);
}