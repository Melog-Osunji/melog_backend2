package com.osunji.melog.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.FollowStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.osunji.melog.user.domain.Follow;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    @Query("""
       select f.following.id
       from Follow f
       where f.follower.id = :userId
         and f.status = com.osunji.melog.user.domain.enums.FollowStatus.ACCEPTED
    """)
    List<UUID> findFolloweeIds(@Param("userId") UUID userId);

//    boolean existsByFollower_IdAndFollowing_IdAndStatus(UUID followerId, UUID followingId);
    @Query("""
           select f.following.id
           from Follow f
           where f.follower.id = :userId
             and f.status = :status
        """)
    List<UUID> findFolloweeIdsByStatus(@Param("userId") UUID userId,
                                       @Param("status") FollowStatus status);

    /**
     * 나에게 팔로우를 '신청'한(또는 원하는 상태) 유저(=follower) 목록
     * 기존 메서드 유지 (status 파라미터로 전달)
     */
    @Query("""
        select f.follower
        from Follow f
        where f.following.id = :userId
          and f.status = :status
    """)
    List<User> findApplicantsByFollowingIdAndStatus(@Param("userId") UUID userId,
                                                    @Param("status") FollowStatus status);

    /**
     * 두 사용자 간 Follow 레코드 단건 조회
     */
    Optional<Follow> findByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);

    /**
     * 두 사용자 간 Follow 레코드 존재 여부
     */
    boolean existsByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);

    /**
     * 두 사용자 간 특정 상태의 Follow 존재 여부 (요청 중인지, 이미 수락했는지 등 체크)
     */
    boolean existsByFollower_IdAndFollowing_IdAndStatus(UUID followerId,
                                                        UUID followingId,
                                                        FollowStatus status);
    // follower와 following으로 단건 조회
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    // 이미 존재하는지 체크(선택)
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    /**
     * 내가 팔로우 '받는' 입장일 때(=following == me), 특정 상태의 신청/관계 목록 조회
     */
    List<Follow> findByFollowing_IdAndStatus(UUID followingId, FollowStatus status);

    /**
     * 내가 팔로우 '하는' 입장일 때(=follower == me), 특정 상태의 관계 목록 조회
     */
    List<Follow> findByFollower_IdAndStatus(UUID followerId, FollowStatus status);

    long countByFollowing_IdAndStatus(UUID followingUserId, FollowStatus status); // followers
    long countByFollower_IdAndStatus(UUID followerUserId, FollowStatus status);   // followings

}