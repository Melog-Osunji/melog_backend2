package com.osunji.melog.inquirySettings.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inquiries",
        indexes = {
                @Index(name = "idx_inquiry_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_inquiry_parent_child", columnList = "parent_type, child_type")
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    /** 문의 ID (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** 상위 문의 유형 */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "parent_type", nullable = false, length = 32)
    private InquiryParentType parentType;

    /** 하위 문의 유형 (8개 고정) */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "child_type", nullable = false, length = 32)
    private InquiryChildType childType;

    /** 제목 */
    @NotNull
    @Column(nullable = false, length = 150)
    private String title;

    /** 본문 */
    @Lob
    @NotNull
    @Column(nullable = false)
    private String content;

    /** 작성자 (User UUID FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", columnDefinition = "uuid", nullable = false)
    private User user;

    /** 작성일시 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성자
    public Inquiry(InquiryParentType parentType,
                   InquiryChildType childType,
                   String title,
                   String content,
                   User user) {
        this.parentType = parentType;
        this.childType = childType;
        this.title = title;
        this.content = content;
        this.user = user;

        // 런타임 가드: 상하위 조합 검증 (유효하지 않으면 IllegalArgumentException)
        ensureValidPair(parentType, childType);
    }

    // 팩토리 메서드
    public static Inquiry create(InquiryParentType parentType,
                                 InquiryChildType childType,
                                 String title,
                                 String content,
                                 User user) {
        return new Inquiry(parentType, childType, title, content, user);
    }

    /** 상·하위 유형 매핑 검증 */
    private static void ensureValidPair(InquiryParentType parent, InquiryChildType child) {
        if (!parent.getAllowedChildren().contains(child)) {
            throw new IllegalArgumentException(
                    "Invalid inquiry type pair: parent=" + parent + ", child=" + child
            );
        }
    }
}
