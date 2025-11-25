package com.osunji.melog.report;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports_all")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reports {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID reportingUserId;    // 신고자 ID

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 100)
	private ReportReason reason;     // 신고 사유

	@Column(name = "post_id")
	private UUID postId;              // 게시글 ID (선택)

	@Column(name = "comment_id")
	private UUID commentId;           // 댓글 ID (선택)

	@Column(name = "reported_user_id")
	private UUID reportedUserId;      // 유저 신고 시 신고 대상자 ID (선택)

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

}

