package com.osunji.melog.feed;


import java.util.*;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/recommend")
    public ResponseEntity<Map<String,Object>> recommend(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> seen,
            HttpServletRequest request
    ) {
        String userId = (String) request.getAttribute(JwtAuthFilter.USER_ID_ATTR);
        var items = feedService.recommend(userId, size, seen==null?List.of():seen);
        var meta = Map.of(
                "alg", "es-hybrid-v1",
                "components", List.of("tags","follow","freshness","popularity"),
                "size", size
        );
        return ResponseEntity.ok(Map.of("items", items, "meta", meta));
    }
}

