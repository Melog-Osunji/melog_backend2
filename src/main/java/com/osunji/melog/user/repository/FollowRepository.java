package com.osunji.melog.user.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.osunji.melog.user.domain.Follow;

public interface FollowRepository extends JpaRepository<Follow, String> {

    @Query("""
       select f.following.id
       from Follow f
       where f.follower.id = :userId
         and f.status = true
    """)
    List<String> findFolloweeIds(@Param("userId") String userId);
}