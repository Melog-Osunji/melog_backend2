package com.osunji.melog.global.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
public class GetTokenController {
    // 확인용 controller
    @GetMapping("/token")
    public String getToken() {
        return null;
    }
}
