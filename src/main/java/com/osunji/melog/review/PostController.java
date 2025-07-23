package com.osunji.melog.review;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.osunji.melog.user.User;
import com.osunji.melog.user.UserRepository;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@GetMapping
	public List<Post> findAll() {
		try {
			List<Post> posts = postRepository.findAll();

			// 모든 지연 로딩 객체를 강제로 초기화
			posts.forEach(post -> {
				try {
					// User 프록시 초기화
					if (post.getUser() != null) {
						post.getUser().getUserId();
						post.getUser().getNickname();
						post.getUser().getPlatform();
					}
					// 좋아요 사용자 목록 초기화
					if (post.getLikedUsers() != null) {
						post.getLikedUsers().size();
					}
					// 태그 목록 초기화
					if (post.getTags() != null) {
						post.getTags().size();
					}
				} catch (Exception e) {
					System.out.println("프록시 초기화 오류: " + e.getMessage());
				}
			});

			return posts;
		} catch (Exception e) {
			System.out.println("전체 오류: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	// PostController에 추가할 메서드
	@GetMapping("/simple")
	public List<String> findAllSimple() {
		return postRepository.findAll()
			.stream()
			.map(post -> "제목: " + post.getTitle() + ", 작성자: " + post.getUser().getNickname())
			.collect(Collectors.toList());
	}

	@GetMapping("/create-test-data")
	public String createTestData() {
		try {
			// 중복 생성 방지
			if (userRepository.existsById("test@example.com")) {
				return "테스트 데이터가 이미 존재합니다!";
			}

			// 1. 테스트 사용자 생성
			User user = User.createUser("test@example.com", "local", "테스트유저");
			userRepository.save(user);

			// 2. 테스트 게시물 생성
			Post post1 = Post.createPost(user, "첫 번째 게시물", "이것은 테스트 게시물입니다.");
			Post post2 = Post.createPost(user, "두 번째 게시물", "또 다른 테스트 게시물입니다.");

			postRepository.save(post1);
			postRepository.save(post2);

			return "테스트 데이터 생성 완료!";
		} catch (Exception e) {
			return "데이터 생성 실패: " + e.getMessage();
		}
	}
}
