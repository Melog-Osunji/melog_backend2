package com.osunji.melog.feed;


import java.util.*;
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
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> seen // 클라이언트가 이미 본 글
    ) {
        var items = feedService.recommend(userId, size, seen==null?List.of():seen);
        var meta = Map.of(
                "alg", "es-hybrid-v1",
                "components", List.of("tags","follow","freshness","popularity"),
                "size", size
        );
        return ResponseEntity.ok(Map.of("items", items, "meta", meta));
    }
}

