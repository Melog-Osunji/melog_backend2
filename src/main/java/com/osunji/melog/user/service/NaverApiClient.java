package com.osunji.melog.user.service;

import com.osunji.melog.user.dto.response.NaverUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public NaverUserInfoResponse.Response fetchUserInfo(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            log.error("❌ Naver access token is null or blank");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_access_token");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<NaverUserInfoResponse> response = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                entity,
                NaverUserInfoResponse.class
        );

        NaverUserInfoResponse body = response.getBody();
        if (body == null || body.getResponse() == null || !"00".equals(body.getResultcode())) {
            log.error("❌ Failed to fetch Naver user info. body={}", body);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "naver_userinfo_failed");
        }

        return body.getResponse();
    }
}
