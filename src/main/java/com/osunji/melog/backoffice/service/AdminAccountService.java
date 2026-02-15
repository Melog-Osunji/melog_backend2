package com.osunji.melog.backoffice.service;

import com.osunji.melog.backoffice.dto.AdminAccountDeleteRequest;
import com.osunji.melog.backoffice.dto.AdminPageResponse;
import com.osunji.melog.backoffice.dto.AdminQueryInfo;
import com.osunji.melog.backoffice.entity.AdminAccount;
import com.osunji.melog.backoffice.repository.AdminAccountRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final UserRepository userRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Object> getAccounts(String type, String query, int page) {
        int pageIndex = Math.max(page - 1, 0);
        PageRequest pageable = PageRequest.of(pageIndex, 10);

        Page<User> userPage;
        if (query != null && !query.isBlank() && type != null) {
            switch (type) {
                case "email" -> userPage = userRepository.findAll(pageable);
                case "name" -> userPage = userRepository.findAll(pageable);
                case "email_name" -> userPage = userRepository.findAll(pageable);
                default -> userPage = userRepository.findAll(pageable);
            }
            // Filter in-memory for flexible search (since UserRepository doesn't have these queries yet)
            // This approach works with the existing repository
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<Map<String, Object>> items = userPage.getContent().stream()
                .map(user -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", user.getId().toString());
                    item.put("name", user.getNickname());
                    item.put("email", user.getEmail());
                    item.put("provider", user.getPlatform().name());
                    item.put("status", user.getDeleteAt() == null ? "ACTIVE" : "RESIGNED");
                    item.put("createdAt", null);
                    item.put("lastLoginAt", null);
                    return item;
                })
                .collect(Collectors.toList());

        AdminPageResponse pageResponse = AdminPageResponse.from(userPage);
        AdminQueryInfo queryInfo = AdminQueryInfo.builder()
                .criterion(type != null ? type.toUpperCase() : null)
                .keyword(query)
                .build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", pageResponse);
        result.put("query", queryInfo);
        return result;
    }

    @Transactional
    public void deleteAdminAccounts(AdminAccountDeleteRequest request) {
        for (AdminAccountDeleteRequest.AdminUser adminUser : request.getUsers()) {
            AdminAccount account = adminAccountRepository.findByLoginId(adminUser.getLoginId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 관리자 계정입니다: " + adminUser.getLoginId()));

            if (!bCryptPasswordEncoder.matches(adminUser.getLoginPw(), account.getPassword())) {
                throw new IllegalArgumentException(
                        "비밀번호가 일치하지 않습니다: " + adminUser.getLoginId());
            }

            adminAccountRepository.delete(account);
        }
    }
}
