package com.osunji.melog.search.dto.request;

import lombok.Data;

public class SearchRequest {

	/**
	 * 검색어 쿼리 dto
	 */
	@Data
	public static class SearchQuery {
		private String q; // 검색어
	}


}
