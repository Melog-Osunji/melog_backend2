package com.osunji.melog.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
         and f.status = true
    """)
    List<UUID> findFolloweeIds(@Param("userId") UUID userId);


    Optional<Follow> findByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);

    boolean existsByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);

//    boolean existsByFollower_IdAndFollowing_IdAndStatus(UUID followerId, UUID followingId);
}