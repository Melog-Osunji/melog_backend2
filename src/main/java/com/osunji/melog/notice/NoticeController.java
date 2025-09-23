package com.osunji.melog.notice;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.notice.NoticeRequest;
import com.osunji.melog.notice.NoticeResponse;
import com.osunji.melog.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/secretMelog/notices0128")
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeService noticeService;

	/**
	 * 1. 공지사항 생성 - POST /api/notice (토큰 불필요)
	 */
	@PostMapping
	public ResponseEntity<ApiMessage<Void>> createNotice(
		@RequestBody NoticeRequest.Create request) {

		log.info("📢 공지사항 생성 요청: {}", request.getTitle());

		try {
			noticeService.createNotice(request);
			return ResponseEntity.ok(ApiMessage.success(201, "공지사항 생성 완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("공지사항 생성 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "공지사항 생성에 실패했습니다"));
		}
	}

	/**
	 * 2. 공지사항 목록 조회 - GET /api/notice (토큰 불필요)
	 */
	@GetMapping
	public ResponseEntity<ApiMessage<NoticeResponse.NoticeList>> getNotices(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) String category) {

		log.info("📢 공지사항 목록 조회 요청: page={}, size={}, category={}", page, size, category);

		try {
			NoticeResponse.NoticeList response = noticeService.getNotices(page, size, category);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 완료", response));
		} catch (Exception e) {
			log.error("공지사항 목록 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 3. 중요 공지사항만 조회 - GET /api/notice/important (토큰 불필요)
	 */
	@GetMapping("/important")
	public ResponseEntity<ApiMessage<NoticeResponse.NoticeList>> getImportantNotices() {

		log.info("🔥 중요 공지사항 조회 요청");

		try {
			NoticeResponse.NoticeList response = noticeService.getImportantNotices();
			return ResponseEntity.ok(ApiMessage.success(200, "중요 공지사항 조회 완료", response));
		} catch (Exception e) {
			log.error("중요 공지사항 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 4. 공지사항 상세 조회 - GET /api/notice/{noticeId} (토큰 불필요)
	 */
	@GetMapping("/{noticeId}")
	public ResponseEntity<ApiMessage<NoticeResponse.NoticeDetail>> getNoticeDetail(
		@PathVariable String noticeId) {

		log.info("📄 공지사항 상세 조회 요청: {}", noticeId);

		try {
			NoticeResponse.NoticeDetail response = noticeService.getNoticeDetail(noticeId);
			return ResponseEntity.ok(ApiMessage.success(200, "상세 조회 완료", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("공지사항 상세 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 5. 공지사항 수정 - PUT /api/notice/{noticeId} (토큰 불필요)
	 */
	@PutMapping("/{noticeId}")
	public ResponseEntity<ApiMessage<Void>> updateNotice(
		@PathVariable String noticeId,
		@RequestBody NoticeRequest.Update request) {

		log.info("📝 공지사항 수정 요청: {}", noticeId);

		try {
			noticeService.updateNotice(noticeId, request);
			return ResponseEntity.ok(ApiMessage.success(200, "공지사항 수정 완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("공지사항 수정 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "수정에 실패했습니다"));
		}
	}

	/**
	 * 6. 공지사항 삭제 - DELETE /api/notice/{noticeId} (토큰 불필요)
	 */
	@DeleteMapping("/{noticeId}")
	public ResponseEntity<ApiMessage<Void>> deleteNotice(
		@PathVariable String noticeId) {

		log.info("🗑️ 공지사항 삭제 요청: {}", noticeId);

		try {
			noticeService.deleteNotice(noticeId);
			return ResponseEntity.ok(ApiMessage.success(200, "공지사항 삭제 완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("공지사항 삭제 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "삭제에 실패했습니다"));
		}
	}
}
