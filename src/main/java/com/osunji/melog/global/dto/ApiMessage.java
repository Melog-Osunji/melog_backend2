package com.osunji.melog.global.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiMessage<T> {
	private boolean success;
	private int code;
	private String message;
	private T data;

	// 성공 응답 생성기
	public static <T> ApiMessage<T> success(int code, String message, T data) {
		return ApiMessage.<T>builder()
			.success(true)
			.code(code)
			.message(message)
			.data(data)
			.build();
	}

	// 실패 응답 생성기
	public static <T> ApiMessage<T> fail(int code, String message) {
		return ApiMessage.<T>builder()
			.success(false)
			.code(code)
			.message(message)
			.data(null)
			.build();
	}
}
