package com.osunji.melog.feed.controller;

import com.osunji.melog.feed.service.ElkIndexService;
import com.osunji.melog.feed.service.ElkSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev/elk")
@RequiredArgsConstructor
public class DevElkAdminController {

    private final ElkIndexService elkIndexService; // (있다면)
    private final ElkSeedService elkSeedService;

    @PostMapping("/ensure")
    public ResponseEntity<?> ensure() throws Exception {
        var r = elkIndexService.ensureAll();

        return ResponseEntity.ok(r);
    }

    @PostMapping("/seed/users")
    public ResponseEntity<?> seedUsers(
            @RequestParam(defaultValue = "500") int count,
            @RequestParam(required = false) Long seed
    ) throws Exception {
        return ResponseEntity.ok(elkSeedService.seedUsers(count, seed));
    }

    @PostMapping("/seed/user-logs")
    public ResponseEntity<?> seedUserLogs(
            @RequestParam(defaultValue = "500") int users,
            @RequestParam(defaultValue = "1500") int events,
            @RequestParam(required = false) Long seed
    ) throws Exception {
        return ResponseEntity.ok(elkSeedService.seedUserLogs(users, events, seed));
    }

    /** ⬅️ 신규: posts 시드 */
    @PostMapping("/seed/posts")
    public ResponseEntity<?> seedPosts(
            @RequestParam(defaultValue = "500") int users,
            @RequestParam(defaultValue = "3000") int count,
            @RequestParam(required = false) Long seed
    ) throws Exception {
        return ResponseEntity.ok(elkSeedService.seedPosts(users, count, seed));
    }
}
