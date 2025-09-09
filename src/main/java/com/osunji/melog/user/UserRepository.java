package com.osunji.melog.user;

import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByOidc(String oidc);
    Optional<User> findByOidcAndPlatform(String oidc, Platform platform);
}
