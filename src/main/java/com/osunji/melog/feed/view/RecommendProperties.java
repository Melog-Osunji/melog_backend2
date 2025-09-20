package com.osunji.melog.feed.view;

import lombok.Getter;
import lombok.Setter;

public class RecommendProperties {
    private Boost boost = new Boost();
    private String freshScale;

    @Getter @Setter
    public static class Boost {
        private double tag;
        private double followee;
    }
}

