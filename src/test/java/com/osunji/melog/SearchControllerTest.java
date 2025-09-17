package com.osunji.melog;

import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.search.controller.SearchController;
import com.osunji.melog.search.dto.response.SearchResponse;
import com.osunji.melog.search.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 자체는 체인에 추가 안 함
class SearchControllerTest {

    @Autowired MockMvc mockMvc;

    // --- 여기 추가: 보안 관련 의존성들을 목으로 막아 컨텍스트 로딩 문제 제거 ---
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JWTUtil jwtUtil;

    @MockBean SearchService searchService;

    @Nested
    @DisplayName("통합 검색")
    class AllSearch {
        @Test
        @DisplayName("[31] GET /api/search/all → 200 & ApiMessage 래핑")
        void getAllSearch_ok() throws Exception {
            when(searchService.getAllSearch()).thenReturn(Mockito.mock(SearchResponse.AllSearch.class));

            mockMvc.perform(get("/api/search/all"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists());

            verify(searchService).getAllSearch();
        }
    }

    @Nested
    @DisplayName("검색 전 페이지")
    class BeforeSearch {
        @Test
        @DisplayName("[32] 인기 작곡가")
        void getPopularComposers_ok() throws Exception {
            when(searchService.getPopularComposers())
                    .thenReturn(java.util.List.of(Mockito.mock(SearchResponse.Composer.class)));

            mockMvc.perform(get("/api/search/composer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("[33] 인기 연주가")
        void getPopularPlayers_ok() throws Exception {
            when(searchService.getPopularPlayers())
                    .thenReturn(java.util.List.of(Mockito.mock(SearchResponse.Player.class)));

            mockMvc.perform(get("/api/search/player"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("[34] 장르")
        void getGenres_ok() throws Exception {
            when(searchService.getGenres())
                    .thenReturn(java.util.List.of(Mockito.mock(SearchResponse.Genre.class)));

            mockMvc.perform(get("/api/search/genre"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("[35] 인기 시대")
        void getPeriods_ok() throws Exception {
            when(searchService.getPeriods())
                    .thenReturn(Mockito.mock(SearchResponse.Period.class));

            mockMvc.perform(get("/api/search/period"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("[36] 인기 악기")
        void getInstruments_ok() throws Exception {
            when(searchService.getInstruments())
                    .thenReturn(Mockito.mock(SearchResponse.Instrument.class));

            mockMvc.perform(get("/api/search/instrument"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Nested
    @DisplayName("검색 결과")
    class SearchResults {
        @Test
        @DisplayName("[37] 게시글+미디어")
        void searchAll_ok() throws Exception {
            when(searchService.searchAll("beethoven"))
                    .thenReturn(Mockito.mock(SearchResponse.SearchResultAll.class));

            mockMvc.perform(get("/api/search").param("q", "beethoven"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("[38] 프로필")
        void searchProfile_ok() throws Exception {
            when(searchService.searchProfile("mozart"))
                    .thenReturn(Mockito.mock(SearchResponse.SearchProfile.class));

            mockMvc.perform(get("/api/search/profile").param("q", "mozart"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("[39] 피드")
        void searchFeed_ok() throws Exception {
            when(searchService.searchFeed("chopin"))
                    .thenReturn(Mockito.mock(SearchResponse.SearchFeed.class));

            mockMvc.perform(get("/api/search/feed").param("q", "chopin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());
        }
    }
}
