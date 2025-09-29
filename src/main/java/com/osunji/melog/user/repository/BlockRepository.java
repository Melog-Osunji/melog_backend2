package com.osunji.melog.user.repository;


import com.osunji.melog.user.domain.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {

    /** 차단한 사람이 나(blocker)인 레코드 전체 + N+1 방지용 blocked(상대) 즉시 로드 */
    @Query("""
           select b
           from Block b
           join fetch b.blocked u
           where b.blocker.id = :blockerId
           """)
    List<Block> findAllByBlockerIdFetchBlocked(@Param("blockerId") UUID blockerId);

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

}
