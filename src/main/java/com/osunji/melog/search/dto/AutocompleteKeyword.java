package com.osunji.melog.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AutocompleteKeyword {
	private String keyword;
	private String type;
	private Integer priority;
}
