package com.univerliga.crm.repository;

import com.univerliga.crm.model.OutboxEventEntity;
import com.univerliga.crm.model.OutboxStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {

    Page<OutboxEventEntity> findAllByStatus(OutboxStatus status, Pageable pageable);

    @Query("""
            select o from OutboxEventEntity o
            where o.status in :statuses
              and (o.nextAttemptAt is null or o.nextAttemptAt <= :now)
            order by o.createdAt asc
            """)
    List<OutboxEventEntity> findForProcessing(@Param("statuses") Collection<OutboxStatus> statuses,
                                              @Param("now") OffsetDateTime now,
                                              Pageable pageable);
}
