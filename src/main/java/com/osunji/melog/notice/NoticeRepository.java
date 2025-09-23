package com.osunji.melog.notice;

import com.osunji.melog.notice.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, UUID> {

	/**
	 * 전체 공지사항 목록 조회 (중요 공지 우선, 최신순)
	 */
	@Query("SELECT n FROM Notice n ORDER BY n.isImportant DESC, n.createdAt DESC")
	List<Notice> findAllNotices(Pageable pageable);

	/**
	 * 중요 공지사항만 조회
	 */
	@Query("SELECT n FROM Notice n WHERE n.isImportant = true ORDER BY n.createdAt DESC")
	List<Notice> findImportantNotices();

	/**
	 * 카테고리별 공지사항 조회
	 */
	@Query("SELECT n FROM Notice n WHERE n.category = :category " +
		"ORDER BY n.isImportant DESC, n.createdAt DESC")
	List<Notice> findNoticesByCategory(@Param("category") String category, Pageable pageable);

	/**
	 * 전체 공지사항 수 조회
	 */
	@Query("SELECT COUNT(n) FROM Notice n")
	Long countAllNotices();

	/**
	 * 카테고리별 공지사항 수 조회
	 */
	@Query("SELECT COUNT(n) FROM Notice n WHERE n.category = :category")
	Long countNoticesByCategory(@Param("category") String category);
}
