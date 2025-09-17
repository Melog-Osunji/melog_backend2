package com.osunji.melog.harmony.controlller;

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
		@PathVariable String harmonyId) {

		log.info("📝 controller line 100~ 하모니룸 게시글 조회 요청: {}", harmonyId);

		try {
			HarmonyRoomResponse.HarmonyRoomPosts response = harmonyService.getHarmonyRoomPosts(harmonyId);
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

	/**
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
	 * 19. 하모니룸 게시글 생성 - POST /api/harmony/{harmonyId}/posts
	 */
	@PostMapping("/harmony/{harmonyId}/posts")
	public ResponseEntity<ApiMessage<Void>> createHarmonyRoomPost(
		@PathVariable String harmonyId,
		@RequestBody PostRequest.Create request,
		@RequestHeader("Authorization") String authHeader) {

		log.info("📝 하모니룸 게시글 생성 요청: {} - 제목: {}", harmonyId, request.getTitle());

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


}
