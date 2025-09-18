package com.osunji.melog.feed;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.yml")
public class RecommendProperties {
    private Boost boost = new Boost();
    private String freshScale;

    @Getter @Setter
    public static class Boost {
        private double tag;
        private double followee;
    }
}

