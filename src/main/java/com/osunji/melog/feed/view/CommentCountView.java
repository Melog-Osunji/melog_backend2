package com.osunji.melog.feed.view;

import java.util.UUID;

public interface CommentCountView {
    UUID getPostId();
    long getCnt();
}
