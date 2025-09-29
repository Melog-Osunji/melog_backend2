package com.osunji.melog.imageSave;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;
import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.repository.HarmonyRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

	private final AmazonS3 amazonS3;
	private final AuthHelper authHelper;
	private final UserRepository userRepository;
	private final HarmonyRoomRepository harmonyRoomRepository;

	@Value("${aws.s3.bucket}")
	private String bucket;

	// 허용되는 이미지 타입
	private static final List<String> ALLOWED_TYPES = Arrays.asList(
		"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
	);

	// 최대 파일 크기 (10MB)
	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

	/**
	 * 1. 프로필 이미지 업로드 + DB 저장 (한방에 처리)
	 */
	@PostMapping("/profile")
	public ResponseEntity<ApiMessage<String>> uploadAndUpdateProfileImage(
		@RequestParam("file") MultipartFile file,
		@RequestHeader("Authorization") String authHeader) {

		log.info("📸 프로필 이미지 업로드 및 변경 요청: {}", file.getOriginalFilename());

		try {
			// 파일 유효성 검사
			validateFile(file);

			// 사용자 인증
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

			// 기존 프로필 이미지 삭제
			String oldImageUrl = user.getProfileImageUrl();
			if (oldImageUrl != null && oldImageUrl.contains(bucket)) {
				deleteImageFromS3(oldImageUrl);
				log.info("🗑️ 기존 프로필 이미지 삭제: {}", oldImageUrl);
			}

			// 새 이미지 S3 업로드
			String fileName = generateFileName(file.getOriginalFilename());
			String key = "profiles/" + fileName;
			String newImageUrl = uploadImageToS3(file, key);

			// DB에 새 URL 저장
			user.updateProfileImage(newImageUrl);
			userRepository.save(user);

			log.info("✅ 프로필 이미지 변경 완료: {}", newImageUrl);
			return ResponseEntity.ok(ApiMessage.success(200, "프로필 이미지 변경 완료", newImageUrl));

		} catch (IllegalArgumentException e) {
			log.error("프로필 이미지 업로드 실패 - 유효하지 않은 파일: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("💥 프로필 이미지 변경 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "프로필 이미지 변경에 실패했습니다"));
		}
	}

	/**
	 * 2. 게시글 이미지 업로드 (URL만 반환)
	 */
	@PostMapping("/post")
	public ResponseEntity<ApiMessage<String>> uploadPostImage(
		@RequestParam("file") MultipartFile file,
		@RequestHeader("Authorization") String authHeader) {

		log.info("📷 게시글 이미지 업로드 요청: {}", file.getOriginalFilename());

		try {
			// 파일 유효성 검사
			validateFile(file);

			// 사용자 인증 (권한 체크용)
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// S3 업로드
			String fileName = generateFileName(file.getOriginalFilename());
			String key = "posts/" + fileName;
			String imageUrl = uploadImageToS3(file, key);

			log.info("✅ 게시글 이미지 업로드 완료: {}", imageUrl);
			return ResponseEntity.ok(ApiMessage.success(200, "게시글 이미지 업로드 완료", imageUrl));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (Exception e) {
			log.error("💥 게시글 이미지 업로드 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "게시글 이미지 업로드에 실패했습니다"));
		}
	}

	/**
	 * 3. 하모니룸 이미지 업로드 + DB 저장 (한방에 처리)
	 */
	@PostMapping("/harmony/{harmonyId}")
	public ResponseEntity<ApiMessage<String>> uploadAndUpdateHarmonyImage(
		@PathVariable String harmonyId,
		@RequestParam("file") MultipartFile file,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🎵 하모니룸 이미지 업로드 및 변경 요청: {} - {}", harmonyId, file.getOriginalFilename());

		try {
			// 파일 유효성 검사
			validateFile(file);

			// 사용자 인증
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 하모니룸 조회 및 권한 확인
			UUID harmonyRoomId = UUID.fromString(harmonyId);
			HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
				.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

			// 하모니룸 소유자 권한 체크
			if (!harmonyRoom.isOwner(user)) {
				throw new SecurityException("하모니룸 소유자만 이미지를 변경할 수 있습니다");
			}

			// 기존 하모니룸 이미지 삭제
			String oldImageUrl = harmonyRoom.getProfileImageUrl();
			if (oldImageUrl != null && oldImageUrl.contains(bucket)) {
				deleteImageFromS3(oldImageUrl);
				log.info("🗑️ 기존 하모니룸 이미지 삭제: {}", oldImageUrl);
			}

			// 새 이미지 S3 업로드
			String fileName = generateFileName(file.getOriginalFilename());
			String key = "harmony/" + fileName;
			String newImageUrl = uploadImageToS3(file, key);

			// DB에 새 URL 저장 (HarmonyRoom 엔티티에 updateProfileImage 메서드 필요)
			harmonyRoom.updateProfileImage(newImageUrl);
			harmonyRoomRepository.save(harmonyRoom);

			log.info("✅ 하모니룸 이미지 변경 완료: {}", newImageUrl);
			return ResponseEntity.ok(ApiMessage.success(200, "하모니룸 이미지 변경 완료", newImageUrl));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, e.getMessage()));
		} catch (SecurityException e) {
			return ResponseEntity.status(403).body(ApiMessage.fail(403, "권한이 없습니다"));
		} catch (Exception e) {
			log.error("💥 하모니룸 이미지 변경 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "하모니룸 이미지 변경에 실패했습니다"));
		}
	}

	/**
	 * 4. 하모니룸 이미지 업로드만 (URL만 반환, 하모니룸 생성 시 사용)
	 */
	@PostMapping("/harmony-create")
	public ResponseEntity<ApiMessage<String>> uploadHarmonyImageOnly(
		@RequestParam("file") MultipartFile file,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🎵 하모니룸 생성용 이미지 업로드 요청: {}", file.getOriginalFilename());

		try {
			validateFile(file);

			// 사용자 인증
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// S3 업로드
			String fileName = generateFileName(file.getOriginalFilename());
			String key = "harmony/" + fileName;
			String imageUrl = uploadImageToS3(file, key);

			log.info("✅ 하모니룸 생성용 이미지 업로드 완료: {}", imageUrl);
			return ResponseEntity.ok(ApiMessage.success(200, "하모니룸 이미지 업로드 완료", imageUrl));

		} catch (Exception e) {
			log.error("💥 하모니룸 이미지 업로드 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "이미지 업로드에 실패했습니다"));
		}
	}

	/**
	 * 5. 이미지 삭제 (범용)
	 */
	@DeleteMapping
	public ResponseEntity<ApiMessage<Void>> deleteImage(
		@RequestParam("imageUrl") String imageUrl,
		@RequestHeader("Authorization") String authHeader) {

		log.info("🗑️ 이미지 삭제 요청: {}", imageUrl);

		try {
			// 사용자 인증
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// S3에서 삭제
			deleteImageFromS3(imageUrl);

			log.info("✅ 이미지 삭제 완료: {}", imageUrl);
			return ResponseEntity.ok(ApiMessage.success(200, "이미지 삭제 완료", null));

		} catch (Exception e) {
			log.error("💥 이미지 삭제 실패: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiMessage.fail(500, "이미지 삭제에 실패했습니다"));
		}
	}

	// ==================== 헬퍼 메서드들 ==================== //

	/**
	 * 파일 유효성 검사
	 */
	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("파일이 비어있습니다");
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다");
		}

		if (!ALLOWED_TYPES.contains(file.getContentType())) {
			throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, png, gif, webp만 허용)");
		}
	}

	/**
	 * 고유한 파일명 생성
	 */
	private String generateFileName(String originalFilename) {
		String extension = "";
		if (originalFilename != null && originalFilename.contains(".")) {
			extension = originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		return UUID.randomUUID().toString() + extension;
	}

	/**
	 * S3에 이미지 업로드
	 */
	private String uploadImageToS3(MultipartFile file, String key) throws IOException {
		try {
			// 메타데이터 설정
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(file.getContentType());
			metadata.setContentLength(file.getSize());
			metadata.setCacheControl("max-age=31536000"); // 1년 캐시

			// S3에 업로드
			// ✅ ACL 없이 업로드
			PutObjectRequest putObjectRequest = new PutObjectRequest(
				bucket, key, file.getInputStream(), metadata
			);

			amazonS3.putObject(putObjectRequest);

			// 업로드된 파일의 URL 반환
			String imageUrl = amazonS3.getUrl(bucket, key).toString();

			log.info("✅ S3 업로드 성공: {}", imageUrl);
			return imageUrl;

		} catch (Exception e) {
			log.error("💥 S3 업로드 실패: {}", e.getMessage(), e);
			throw new RuntimeException("이미지 업로드에 실패했습니다", e);
		}
	}

	/**
	 * S3에서 이미지 삭제
	 */
	private void deleteImageFromS3(String imageUrl) {
		try {
			// URL에서 key 추출
			String key = extractKeyFromUrl(imageUrl);

			if (key != null && amazonS3.doesObjectExist(bucket, key)) {
				amazonS3.deleteObject(bucket, key);
				log.info("✅ S3 삭제 성공: {}", key);
			}
		} catch (Exception e) {
			log.error("💥 S3 삭제 실패: {}", e.getMessage(), e);
		}
	}

	/**
	 * URL에서 S3 key 추출
	 */
	private String extractKeyFromUrl(String imageUrl) {
		try {
			String bucketUrl = "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/";
			if (imageUrl.startsWith(bucketUrl)) {
				return imageUrl.substring(bucketUrl.length());
			}
		} catch (Exception e) {
			log.error("URL에서 key 추출 실패: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * 이미지 존재 여부 확인
	 */
	private boolean doesImageExist(String imageUrl) {
		try {
			String key = extractKeyFromUrl(imageUrl);
			return key != null && amazonS3.doesObjectExist(bucket, key);
		} catch (Exception e) {
			return false;
		}
	}
}
