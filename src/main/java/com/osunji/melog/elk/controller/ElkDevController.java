package com.osunji.melog.elk.controller;


import com.osunji.melog.elk.repository.ELKUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/dev/elk")
@RequiredArgsConstructor
public class ElkDevController {

    private final ELKUserRepository repo;

    // 1) 로그 적재
    // 예) GET /api/dev/elk/log?uid=u1&type=LOGIN&ip=1.2.3.4&ua=Chrome&meta={"key":"v"}
    @GetMapping("/log")
    public String log(@RequestParam("uid") String userId,
                      @RequestParam("type") String eventType,
                      @RequestParam(value = "ip", required = false) String ip,
                      @RequestParam(value = "ua", required = false) String userAgent,
                      @RequestParam(value = "meta", required = false) String metaJson) {
        repo.logUserEvent(userId, eventType, ip, userAgent, metaJson);
        return "ok";
    }

    // 2) DAU 시계열
    // 예) GET /api/dev/elk/dau?days=7
    @GetMapping("/dau")
    public LinkedHashMap<String, Long> dau(@RequestParam(defaultValue = "7") int days) {
        return repo.getDauSeries(days);
    }

    // 3) 최근 가입자
    // 예) GET /api/dev/elk/recent-signups?days=7&size=20
    @GetMapping("/recent-signups")
    public List<String> recentSignups(@RequestParam(defaultValue = "7") int days,
                                      @RequestParam(defaultValue = "20") int size) {
        return repo.getRecentSignups(days, size);
    }

    // 4) 특정 유저 최근 이벤트
    // 예) GET /api/dev/elk/user-events?userId=u1&days=7&size=20
    @GetMapping("/user-events")
    public List<String> userEvents(@RequestParam String userId,
                                   @RequestParam(defaultValue = "7") int days,
                                   @RequestParam(defaultValue = "20") int size) {
        return repo.getUserRecentEvents(userId, days, size);
    }
}
