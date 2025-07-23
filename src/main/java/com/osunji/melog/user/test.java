package com.osunji.melog.user;

import java.util.List;

public class test {

	public static void main(String[] args) {
		System.out.println("=== User 엔티티 테스트 ===");

		// User 생성 테스트
		User user = User.createUser("test@example.com", "local", "테스트유저");

		System.out.println("사용자 ID: " + user.getUserId());
		System.out.println("플랫폼: " + user.getPlatform());
		System.out.println("닉네임: " + user.getNickname());

		// Onboarding 생성 테스트
		Onboarding onboarding = Onboarding.createOnboarding(
			user,
			List.of("모차르트", "베토벤"),
			List.of("고전주의", "낭만주의"),
			List.of("피아노", "바이올린")
		);

		System.out.println("\n=== Onboarding 정보 ===");
		System.out.println("온보딩 사용자 ID: " + onboarding.getUserId());
		System.out.println("선호 작곡가: " + onboarding.getComposers());
		System.out.println("선호 시대: " + onboarding.getEras());
		System.out.println("선호 악기: " + onboarding.getInstruments());

		// Agreement 생성 테스트
		Agreement agreement = Agreement.createAgreement(user, true);

		System.out.println("\n=== Agreement 정보 ===");
		System.out.println("약관 동의 사용자 ID: " + agreement.getUserId());
		System.out.println("마케팅 동의: " + agreement.getMarketing());
		System.out.println("동의 일시: " + agreement.getCreatedAt());

		System.out.println("\n모든 엔티티 생성 완료!");
	}
}
