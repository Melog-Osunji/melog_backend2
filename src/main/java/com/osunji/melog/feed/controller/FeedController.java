package com.osunji.melog.feed.controller;


import java.util.*;

import com.osunji.melog.feed.service.RecommendationFacade;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final RecommendationFacade feedFacade;

    @GetMapping("/recommend")
    public ResponseEntity<ApiMessage<FilterPostResponse.FeedList>> recommend(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> seen,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<FilterPostResponse.FeedList> response = feedFacade.recommend(userId, size, (seen == null ? List.of() : seen));
        return ResponseEntity.status(response.getCode()).body(response);
    }

//    @GetMapping("/recommend/{post_id}")
//    public ResponseEntity<FeedResponse> recommendPost(
//            @PathVariable("post_id") UUID postId,
//            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
//    ){
//        FeedResponse response =
//        return ResponseEntity.status(response.getCode()).body(response);
//
//    }
}