package com.osunji.melog.global.admin;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/secure")
public class SecureController {
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("ping");
        return ResponseEntity.ok("pong");
    }
}
