package com.osunji.melog.user.service;

import com.osunji.melog.user.dto.response.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleUserInfoResponse fetchUserInfo(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            log.error("❌ Google access token is null or blank");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_access_token");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "https://www.googleapis.com/oauth2/v2/userinfo";

        ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                GoogleUserInfoResponse.class
        );

        GoogleUserInfoResponse body = response.getBody();
        if (body == null || body.getEmail() == null) {
            log.error("❌ Failed to fetch Google user info. body={}", body);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "google_userinfo_failed");
        }

        log.info("✅ Google userinfo fetched: {}", body.getEmail());
        return body;
    }
}
