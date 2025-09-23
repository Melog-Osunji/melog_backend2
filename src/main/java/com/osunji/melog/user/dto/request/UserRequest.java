package com.osunji.melog.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

public class UserRequest {

    @Getter
    @Setter
    public static class agreement {
        private boolean marketing;
    }

    @Getter
    @Setter
    public static class onboarding {
        List<String> composer;
        List<String> period;
        List<String> instrument;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 직렬화에서 제외(가독성)
    public static class onboardingPatch {
        private List<String> composer;
        private List<String> period;
        private List<String> instrument;
    }

    @Getter
    @Builder
    public static class profilePatch {
        @NotNull
        private Map<String, String> updates; // e.g. {"intro":"안녕하세요", "nickName":"모차르트러버"}
    }

    @Getter
    @Builder
    public static class following {
        private String follower;
    }
}
