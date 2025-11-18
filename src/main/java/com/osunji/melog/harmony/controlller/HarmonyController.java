package com.osunji.melog.harmony.controlller;

import java.util.List;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.harmony.dto.request.HarmonyRoomRequest;
import com.osunji.melog.harmony.dto.response.HarmonyRoomResponse;
import com.osunji.melog.harmony.service.HarmonyService;
import com.osunji.melog.review.dto.request.PostRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HarmonyController {

	private final HarmonyService harmonyService;

	/**
	 * 1. 하모니룸 생성 - POST /api/posts/harmony
	 */
	@PostMapping("/posts/harmony")
	public ResponseEntity<ApiMessage<Void>> createHarmonyRoom(
		@RequestBody HarmonyRoomRequest.Create request,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🎵 하모니룸 생성 요청: {}", request.getName());

		try {
			harmonyService.createHarmonyRoom(request, authHeader);
			return ResponseEntity.ok(ApiMessage.success(201, "생성완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 생성 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "하모니룸 생성에 실패했습니다"));
		}
	}

	/**
	 * 2. 나의 하모니룸 조회 - GET /api/harmony/my
	 */
	@GetMapping("/harmony/my")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.MyHarmony>> getMyHarmony(
		@RequestHeader("Authorization") String authHeader) {

		log.info("🏠 Controller Line 46번째줄 나의 하모니룸 조회요청");

		try {
			HarmonyRoomResponse.MyHarmony response = harmonyService.getMyHarmony(authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (Exception e) {
			log.error("나의 하모니룸 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 3. 최근 업로드 미디어 조회 - GET /api/harmony/recentMedia
	 */
	@GetMapping("/harmony/recentMedia")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.RecentMedia>> getRecentMedia(
		@RequestHeader("Authorization") String authHeader) {

		log.info("📺 controller line 64~ 최근 업로드 미디어 조회 ");

		try {
			HarmonyRoomResponse.RecentMedia response = harmonyService.getRecentMedia(authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (Exception e) {
			log.error("최근 미디어 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 4. 추천 하모니룸 조회 - GET /api/harmony/recommendHarmony
	 */
	@GetMapping("/harmony/recommendHarmony")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.RecommendHarmony>> getRecommendHarmony(
		@RequestHeader("Authorization") String authHeader) {

		log.info("⭐ controller lnie 78~ 추천 하모니룸 조회 요청");

		try {
			HarmonyRoomResponse.RecommendHarmony response = harmonyService.getRecommendHarmony(authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (Exception e) {
			log.error("추천 하모니룸 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}
	/**
	 * 5. 하모니룸 게시글 조회 - GET /api/harmony/{harmonyID}/posts
	 */
	@GetMapping("/harmony/{harmonyId}/posts")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.HarmonyRoomPosts>> getHarmonyRoomPosts(
		@PathVariable String harmonyId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

		log.info("📝 controller line 100~ 하모니룸 게시글 조회 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.HarmonyRoomPosts response = harmonyService.getHarmonyRoomPosts(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 게시글 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}


	/**
	 * 6. 하모니룸 범용 정보 조회 - GET /api/harmony/{harmonyID}/information
	 */
	@GetMapping("/harmony/{harmonyId}/information")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.Information>> getHarmonyRoomInformation(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("ℹ️Controller line 120~  하모니룸 정보 조회 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.Information response = harmonyService.getHarmonyRoomInformation(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 정보 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**ddddddd
	 * 7. 하모니룸 상세 정보 조회 - GET /api/harmony/{harmonyID}/detail
	 */
	@GetMapping("/harmony/{harmonyId}/detail")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.Detail>> getHarmonyRoomDetail(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("📋 Controller line 140~ 하모니룸 상세정보 조회 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.Detail response = harmonyService.getHarmonyRoomDetail(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 상세정보 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 8. 멤버 여부 확인 - GET /api/harmony/{harmonyID}/isMember
	 */
	@GetMapping("/harmony/{harmonyId}/isMember")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.IsMember>> checkMembership(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("👥 Controller line 162~ 멤버 여부 확인 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.IsMember response = harmonyService.checkMembership(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("멤버 여부 확인 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}

	/**
	 * 9. 하모니룸 정보 수정 - PATCH /api/harmony/{harmonyID}/update
	 */
	@PatchMapping("/harmony/{harmonyId}/update")
	public ResponseEntity<ApiMessage<Void>> updateHarmonyRoom(
		@PathVariable String harmonyId,
		@RequestBody HarmonyRoomRequest.Update request,
		@RequestHeader("Authorization") String authHeader) {

		log.info("✏️ controller line 180~ 하모니룸 수정 요청: {}", harmonyId);

		try {
			harmonyService.updateHarmonyRoom(harmonyId, request, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "수정완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (SecurityException e) {
			return ResponseEntity.status(403).body(ApiMessage.fail(403, "권한이 없습니다"));
		} catch (Exception e) {
			log.error("하모니룸 수정 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "수정에 실패했습니다"));
		}
	}

	/**
	 * 10. 하모니룸 삭제 - DELETE /api/harmony/{harmonyID}/delete
	 */
	@DeleteMapping("/harmony/{harmonyId}/delete")
	public ResponseEntity<ApiMessage<Void>> deleteHarmonyRoom(
		@PathVariable String harmonyId,
		@RequestBody HarmonyRoomRequest.Delete request,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🗑️ controller line 203~ 하모니룸 삭제 요청: {}", harmonyId);

		try {
			harmonyService.deleteHarmonyRoom(harmonyId, request, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "폐쇄완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (SecurityException e) {
			return ResponseEntity.status(403).body(ApiMessage.fail(403, "권한이 없습니다"));
		} catch (Exception e) {
			log.error("하모니룸 삭제 실패: {}", e.getMessage(), e);
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, "폐쇄실패"));
		}
	}

	/**
	 * 11. 가입 승인 대기 유저 리스트 - GET /api/harmony/{harmonyID}/waitingUser
	 */
	@GetMapping("/harmony/{harmonyId}/waitingUser")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.WaitingUsers>> getWaitingUsers(
		@PathVariable String harmonyId) {

		log.info("⏳ controller line 231~ 가입 대기 유저 조회 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.WaitingUsers response = harmonyService.getWaitingUsers(harmonyId);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("가입 대기 유저 조회 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}
	/**
	 * 11-2 나 가입중? 하모니룸에? - GET /api/harmony/{harmonyID}/isWaiting
	 */
	@GetMapping("/harmony/{harmonyId}/isWaiting")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.IsWaiting>> isWaitingUser(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {
		try {
			log.info("🔍 하모니룸 대기 상태 확인: " , harmonyId);
			HarmonyRoomResponse.IsWaiting response = harmonyService.isWaitingUser(harmonyId, authHeader);

			return ResponseEntity.ok(ApiMessage.success(200, "대기 상태 조회 완료", response));

		} catch (IllegalArgumentException e) {
			log.warn("하모니룸 대기 상태 확인 실패: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));

		} catch (Exception e) {
			log.error("하모니룸 대기 상태 확인 오류: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}


	/**
	 * 12-1. 가입 승인 - PATCH /api/harmony/{harmonyID}/approve
	 */
	@PatchMapping("/harmony/{harmonyId}/approve")
	public ResponseEntity<ApiMessage<Void>> approveUser(
		@PathVariable String harmonyId,
		@RequestBody HarmonyRoomRequest.ApproveOrDeny request) {

		log.info("✅ controllerLine 251~  가입 승인 요청: {} - 사용자: {}", harmonyId, request.getUserID());

		try {
			harmonyService.approveUser(harmonyId, request);
			return ResponseEntity.ok(ApiMessage.success(200, "승인되었습니다", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("가입 승인 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "승인에 실패했습니다"));
		}
	}

	/**
	 * 12-2. 가입 거절 - PATCH /api/harmony/{harmonyID}/deny
	 */
	@PatchMapping("/harmony/{harmonyId}/deny")
	public ResponseEntity<ApiMessage<Void>> denyUser(
		@PathVariable String harmonyId,
		@RequestBody HarmonyRoomRequest.ApproveOrDeny request) {

		log.info("❌ 가입 거절 요청: {} - 사용자: {}", harmonyId, request.getUserID());

		try {
			harmonyService.denyUser(harmonyId, request);
			return ResponseEntity.ok(ApiMessage.success(200, "거절되었습니다", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("가입 거절 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "거절에 실패했습니다"));
		}
	}



	/**
	 * 13. 하모니룸 즐겨찾기 추가/제거 (토글) - POST /api/harmony/{harmonyID}/bookmark
	 */
	@PostMapping("/harmony/{harmonyId}/bookmark")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.BookmarkResult>> toggleBookmark(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🔖 하모니룸 즐겨찾기 토글 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.BookmarkResult result = harmonyService.toggleBookmark(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, result.getMessage(), result));
		} catch (IllegalArgumentException e) {
			log.error("즐겨찾기 토글 실패 - 잘못된 요청: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (IllegalStateException e) {
			log.error("즐겨찾기 토글 실패 - 인증 오류: {}", e.getMessage());
			return ResponseEntity.status(401).body(ApiMessage.fail(401, "인증이 필요합니다"));
		} catch (Exception e) {
			log.error("하모니룸 즐겨찾기 토글 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "즐겨찾기 처리에 실패했습니다"));
		}
	}

	/**
	 * 14. 하모니룸 공유 - POST /api/harmony/{harmonyID}/share
	 */
	@PostMapping("/harmony/{harmonyId}/share")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.Share>> shareHarmonyRoom(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {  // ✅ RequestBody 제거

		log.info("🔗 하모니룸 공유 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.Share response = harmonyService.shareHarmonyRoom(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "공유 링크 생성완료", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 공유 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "공유에 실패했습니다"));
		}
	}

	/**
	 * 15. 하모니룸 신고 - POST /api/harmony/{harmonyID}/report
	 */
	@PostMapping("/harmony/{harmonyId}/report")
	public ResponseEntity<ApiMessage<Void>> reportHarmony(
		@PathVariable String harmonyId,
		@RequestBody HarmonyRoomRequest.Report reportRequest,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🚨 하모니룸 신고 요청: {} - 사유: {}, 카테고리: {}",
			harmonyId, reportRequest.getReason(), reportRequest.getCategory());

		try {
			harmonyService.reportHarmony(harmonyId, reportRequest, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "신고가 접수되었습니다", null));
		} catch (IllegalArgumentException e) {
			log.error("하모니룸 신고 실패 - 잘못된 요청: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (IllegalStateException e) {
			log.error("하모니룸 신고 실패 - 인증 오류: {}", e.getMessage());
			return ResponseEntity.status(401).body(ApiMessage.fail(401, "인증이 필요합니다"));
		} catch (Exception e) {
			log.error("하모니룸 신고 처리 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "신고 처리에 실패했습니다"));
		}
	}


	/**
	 * 16. 하모니룸 가입 신청 - POST /api/harmony/{harmonyID}/join
	 */
	@PostMapping("/harmony/{harmonyId}/join")
	public ResponseEntity<ApiMessage<Void>> joinHarmonyRoom(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🚪controller line 315~ 하모니룸 가입 신청 요청: {}", harmonyId);

		try {
			String message = harmonyService.joinHarmonyRoom(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, message, null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 가입 신청 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "가입 신청에 실패했습니다"));
		}
	}






	/**
	 * 17. 하모니룸 탈퇴 - DELETE /api/harmony/{harmonyID}/leave
	 */
	@DeleteMapping("/harmony/{harmonyId}/leave")
	public ResponseEntity<ApiMessage<Void>> leaveHarmonyRoom(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🚪 controller line 336~ 하모니룸 탈퇴 요청: {}", harmonyId);

		try {
			harmonyService.leaveHarmonyRoom(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "탈퇴완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (SecurityException e) {
			return ResponseEntity.status(403).body(ApiMessage.fail(403, "탈퇴실패"));
		} catch (Exception e) {
			log.error("하모니룸 탈퇴 실패: {}", e.getMessage(), e);
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, "탈퇴실패"));
		}
	}

	/**
	 * 18. 하모니룸 내부 피드 추천 - GET /api/harmony/{harmonyID}/recommendPosts
	 */
	@GetMapping("/harmony/{harmonyId}/recommendPosts")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.HarmonyRoomPosts>> getRecommendPosts(
		@PathVariable String harmonyId,
		@RequestHeader("Authorization") String authHeader) {

		log.info("📱 하모니룸 내부 피드 추천 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.HarmonyRoomPosts response = harmonyService.getRecommendPosts(harmonyId, authHeader);
			return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("하모니룸 내부 피드 추천 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "조회에 실패했습니다"));
		}
	}






	/**
	 *
	 *
	 *
	 * 하모니룸 전용 post관련 api
	 *
	 *
	 *
	 * /
	/**
	 * 19. 하모니룸 게시글 생성 - POST /api/harmony/{harmonyId}/posts
	 */
	@PostMapping("/harmony/{harmonyId}/posts")
	public ResponseEntity<ApiMessage<Void>> createHarmonyRoomPost(
		@PathVariable String harmonyId,
		@RequestBody HarmonyRoomRequest.CreateHarmonyPost request,  // ✅ 타입 변경
		@RequestHeader("Authorization") String authHeader) {

		log.info("📝 하모니룸 게시글 생성 요청: {} - 내용: {}", harmonyId, request.getContent());

		try {
			harmonyService.createHarmonyRoomPost(harmonyId, request, authHeader);
			return ResponseEntity.ok(ApiMessage.success(201, "하모니룸 게시글 생성완료", null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (SecurityException e) {
			return ResponseEntity.status(403).body(ApiMessage.fail(403, "하모니룸 멤버만 게시글을 작성할 수 있습니다"));
		} catch (Exception e) {
			log.error("하모니룸 게시글 생성 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "게시글 생성에 실패했습니다"));
		}
	}



	// ========== 하모니룸 게시글 단건 상세 ==========
	@GetMapping("/harmony/posts/{harmonyPostId}")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.PostDetail>> getHarmonyPostDetail(
		@PathVariable String harmonyPostId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {
		ApiMessage<HarmonyRoomResponse.PostDetail> response = harmonyService.getHarmonyPostDetail(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 전체 댓글 조회 ==========
	@GetMapping("/harmony/posts/{harmonyPostId}/comments")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.HarmonyRoomPostComments>> getHarmonyPostComments(
		@PathVariable String harmonyPostId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {
		ApiMessage<HarmonyRoomResponse.HarmonyRoomPostComments> response = harmonyService.getHarmonyPostComments(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 베스트 댓글 조회 ==========
	@GetMapping("/harmony/posts/{harmonyPostId}/bestComment")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.HarmonyRoomBestComment>> getBestHarmonyPostComment(
		@PathVariable String harmonyPostId) {
		ApiMessage<HarmonyRoomResponse.HarmonyRoomBestComment> response = harmonyService.getBestHarmonyPostComment(harmonyPostId);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 좋아요/취소 ==========
	@PostMapping("/harmony/posts/{harmonyPostId}/like")
	public ResponseEntity<ApiMessage<Void>> likeHarmonyPost(
		@PathVariable String harmonyPostId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.likeOrUnlikeHarmonyPost(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 좋아요 여부 ==========
	@GetMapping("/harmony/posts/{harmonyPostId}/islike")
	public ResponseEntity<ApiMessage<Boolean>> isHarmonyPostLiked(
		@PathVariable String harmonyPostId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Boolean> response = harmonyService.isHarmonyPostLiked(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 북마크 추가 ==========
	@PostMapping("/harmony/posts/{harmonyPostId}/bookmark")
	public ResponseEntity<ApiMessage<Void>> bookmarkHarmonyPost(
		@PathVariable String harmonyPostId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.addHarmonyPostBookmark(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 북마크 제거 ==========
	@DeleteMapping("/harmony/posts/{harmonyPostId}/bookmark")
	public ResponseEntity<ApiMessage<Void>> unbookmarkHarmonyPost(
		@PathVariable String harmonyPostId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.removeHarmonyPostBookmark(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 삭제 ==========
	@DeleteMapping("/harmony/posts/{harmonyPostId}")
	public ResponseEntity<ApiMessage<Void>> deleteHarmonyPost(
		@PathVariable String harmonyPostId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.deleteHarmonyPost(harmonyPostId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 수정 ==========
	@PatchMapping("/harmony/posts/{harmonyPostId}")
	public ResponseEntity<ApiMessage<Void>> updateHarmonyPost(
		@PathVariable String harmonyPostId,
		@RequestBody HarmonyRoomRequest.UpdateHarmonyPost request,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.updateHarmonyPost(harmonyPostId, request, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 댓글 생성 ==========
	@PostMapping("/harmony/posts/{harmonyPostId}/comment")
	public ResponseEntity<ApiMessage<Void>> createHarmonyPostComment(
		@PathVariable String harmonyPostId,
		@RequestBody HarmonyRoomRequest.CreateComment request,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.createHarmonyPostComment(harmonyPostId, request, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 별도 특정 댓글 삭제 ==========
	@DeleteMapping("/harmony/posts/{harmonyPostId}/comments/{commentId}")
	public ResponseEntity<ApiMessage<Void>> deleteHarmonyPostComment(
		@PathVariable String harmonyPostId,
		@PathVariable String commentId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.deleteHarmonyPostComment(harmonyPostId, commentId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 하모니룸 게시글 특정 댓글 좋아요/취소 ==========
	@PatchMapping("/harmony/posts/{harmonyPostId}/comments/{commentId}")
	public ResponseEntity<ApiMessage<Void>> toggleHarmonyCommentLike(
		@PathVariable String harmonyPostId,
		@PathVariable String commentId,
		@RequestHeader("Authorization") String authHeader) {
		ApiMessage<Void> response = harmonyService.likeOrUnlikeHarmonyComment(harmonyPostId, commentId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 유저별 하모니룸 게시글 목록 ==========
	@GetMapping("/harmony/users/{userId}/posts")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.UserHarmonyPosts>> getUserHarmonyPosts(
		@PathVariable String userId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {
		ApiMessage<HarmonyRoomResponse.UserHarmonyPosts> response = harmonyService.getUserHarmonyPosts(userId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	// ========== 유저별 북마크 하모니룸 게시글 목록 ========ff==
	@GetMapping("/harmony/posts/{userId}/bookmarks")
	public ResponseEntity<ApiMessage<HarmonyRoomResponse.UserHarmonyBookmarks>> getHarmonyUserBookmarks(
		@PathVariable String userId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {
		ApiMessage<HarmonyRoomResponse.UserHarmonyBookmarks> response = harmonyService.getHarmonyUserBookmarks(userId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}












	@GetMapping("/harmony/roomSearch")
	public ResponseEntity<ApiMessage<List<HarmonyRoomResponse.Simple>>> searchHarmonyRoom(
		@RequestParam("keyword") String keyword) {

		try {
			List<HarmonyRoomResponse.Simple> list = harmonyService.searchHarmonyRooms(keyword);
			return ResponseEntity.ok(ApiMessage.success(200, "하모니룸 검색 성공", list));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		}
	}


}
