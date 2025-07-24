package com.osunji.melog.review;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {
	// 기본 CRUD는 JpaRepository가 자동 제공
	// 필요하면 커스텀 메서드 추가 가능
}
