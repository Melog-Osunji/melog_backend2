package com.osunji.melog.feed.controller;


import java.util.*;

import com.osunji.melog.feed.service.RecommendationFacade;

import com.osunji.melog.feed.dto.FeedResponse;
import com.osunji.melog.global.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final RecommendationFacade feedFacade;

    @GetMapping("/recommend")
    public ResponseEntity<FeedResponse> recommend(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> seen,
            HttpServletRequest request
    ) {
        String userIdStr = (String) request.getAttribute(JwtAuthFilter.USER_ID_ATTR);
        UUID userId = UUID.fromString(userIdStr);

        FeedResponse response = feedFacade.recommend(userId, size, (seen == null ? List.of() : seen));
        return ResponseEntity.ok(response);
    }
}