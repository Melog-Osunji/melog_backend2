package com.osunji.melog.notice;

import com.osunji.melog.notice.NoticeRequest;
import com.osunji.melog.notice.NoticeResponse;
import com.osunji.melog.notice.Notice;
import com.osunji.melog.notice.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

	private final NoticeRepository noticeRepository;

	/**
	 * 1. 공지사항 생성
	 */
	public void createNotice(NoticeRequest.Create request) {
		try {
			log.info("📢 공지사항 생성 시작: {}", request.getTitle());

			Notice notice = Notice.create(
				request.getTitle(),
				request.getContent(),
				request.getIsImportant(),
				request.getCategory(),
				request.getImageUrl(),
				null  // expiredAt 제거
			);

			noticeRepository.save(notice);
			log.info("✅ 공지사항 생성 완료: {} (ID: {})", notice.getTitle(), notice.getId());

		} catch (Exception e) {
			log.error("💥 공지사항 생성 실패: {}", e.getMessage(), e);
			throw new RuntimeException("공지사항 생성에 실패했습니다", e);
		}
	}

	/**
	 * 2. 공지사항 목록 조회 (토큰 불필요)
	 */
	@Transactional(readOnly = true)
	public NoticeResponse.NoticeList getNotices(int page, int size, String category) {
		try {
			log.info("📢 공지사항 목록 조회: page={}, size={}, category={}", page, size, category);

			PageRequest pageRequest = PageRequest.of(page, size);

			List<Notice> notices;
			if (category != null && !category.isEmpty()) {
				notices = noticeRepository.findNoticesByCategory(category, pageRequest);
			} else {
				notices = noticeRepository.findAllNotices(pageRequest);
			}

			List<NoticeResponse.NoticeList.NoticeInfo> noticeInfos = notices.stream()
				.map(this::convertToNoticeInfo)
				.collect(Collectors.toList());

			// 더 있는지 확인
			boolean hasMore = notices.size() == size;

			log.info("📢 공지사항 목록 조회 완료: {}개 (더 있음: {})", noticeInfos.size(), hasMore);

			return NoticeResponse.NoticeList.builder()
				.notices(noticeInfos)
				.totalCount(noticeInfos.size())
				.hasMore(hasMore)
				.build();

		} catch (Exception e) {
			log.error("💥 공지사항 목록 조회 실패: {}", e.getMessage(), e);
			throw new RuntimeException("공지사항 조회에 실패했습니다", e);
		}
	}

	/**
	 * 3. 중요 공지사항만 조회 (토큰 불필요)
	 */
	@Transactional(readOnly = true)
	public NoticeResponse.NoticeList getImportantNotices() {
		try {
			log.info("🔥 중요 공지사항 조회");

			List<Notice> notices = noticeRepository.findImportantNotices();

			List<NoticeResponse.NoticeList.NoticeInfo> noticeInfos = notices.stream()
				.map(this::convertToNoticeInfo)
				.collect(Collectors.toList());

			log.info("🔥 중요 공지사항 조회 완료: {}개", noticeInfos.size());

			return NoticeResponse.NoticeList.builder()
				.notices(noticeInfos)
				.totalCount(noticeInfos.size())
				.hasMore(false)
				.build();

		} catch (Exception e) {
			log.error("💥 중요 공지사항 조회 실패: {}", e.getMessage(), e);
			throw new RuntimeException("중요 공지사항 조회에 실패했습니다", e);
		}
	}

	/**
	 * 4. 공지사항 상세 조회 (토큰 불필요)
	 */
	@Transactional(readOnly = true)
	public NoticeResponse.NoticeDetail getNoticeDetail(String noticeId) {
		try {
			log.info("📄 공지사항 상세 조회: {}", noticeId);

			UUID noticeUuid = UUID.fromString(noticeId);
			Notice notice = noticeRepository.findById(noticeUuid)
				.orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다"));

			NoticeResponse.NoticeDetail detail = NoticeResponse.NoticeDetail.builder()
				.id(notice.getId().toString())
				.title(notice.getTitle())
				.content(notice.getContent())
				.isImportant(notice.getIsImportant())
				.category(notice.getCategory())
				.imageUrl(notice.getImageUrl())
				.createdAt(notice.getCreatedAt())
				.updatedAt(notice.getUpdatedAt())
				.build();

			log.info("📄 공지사항 상세 조회 완료: {}", notice.getTitle());
			return detail;

		} catch (Exception e) {
			log.error("💥 공지사항 상세 조회 실패: {}", e.getMessage(), e);
			throw new RuntimeException("공지사항 상세 조회에 실패했습니다", e);
		}
	}

	/**
	 * 5. 공지사항 수정
	 */
	public void updateNotice(String noticeId, NoticeRequest.Update request) {
		try {
			log.info("📝 공지사항 수정 시작: {}", noticeId);

			UUID noticeUuid = UUID.fromString(noticeId);
			Notice notice = noticeRepository.findById(noticeUuid)
				.orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다"));

			notice.update(
				request.getTitle(),
				request.getContent(),
				request.getIsImportant(),
				request.getCategory(),
				request.getImageUrl(),
				null  // expiredAt 제거
			);

			noticeRepository.save(notice);
			log.info("✅ 공지사항 수정 완료: {}", notice.getTitle());

		} catch (Exception e) {
			log.error("💥 공지사항 수정 실패: {}", e.getMessage(), e);
			throw new RuntimeException("공지사항 수정에 실패했습니다", e);
		}
	}

	/**
	 * 6. 공지사항 삭제
	 */
	public void deleteNotice(String noticeId) {
		try {
			log.info("🗑️ 공지사항 삭제 시작: {}", noticeId);

			UUID noticeUuid = UUID.fromString(noticeId);
			Notice notice = noticeRepository.findById(noticeUuid)
				.orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다"));

			String title = notice.getTitle();
			noticeRepository.delete(notice);

			log.info("✅ 공지사항 삭제 완료: {}", title);

		} catch (Exception e) {
			log.error("💥 공지사항 삭제 실패: {}", e.getMessage(), e);
			throw new RuntimeException("공지사항 삭제에 실패했습니다", e);
		}
	}

	/**
	 * Notice → NoticeInfo 변환 헬퍼 메서드
	 */
	private NoticeResponse.NoticeList.NoticeInfo convertToNoticeInfo(Notice notice) {
		return NoticeResponse.NoticeList.NoticeInfo.builder()
			.id(notice.getId().toString())
			.title(notice.getTitle())
			.content(notice.getContent())
			.isImportant(notice.getIsImportant())
			.category(notice.getCategory())
			.imageUrl(notice.getImageUrl())
			.createdAt(notice.getCreatedAt())
			.updatedAt(notice.getUpdatedAt())
			.build();
	}
}
