package com.univerliga.crm.repository;

import com.univerliga.crm.model.TaskEntity;
import com.univerliga.crm.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {

    @Query("""
            select distinct t from TaskEntity t
            left join t.participantIds participant
            where (:status is null or t.status = :status)
              and (:ownerId is null or t.ownerId = :ownerId)
              and (:assigneeId is null or t.assigneeId = :assigneeId)
              and (:participantId is null or participant = :participantId)
              and (:periodFrom is null or t.periodFrom >= :periodFrom)
              and (:periodTo is null or t.periodTo <= :periodTo)
            """)
    Page<TaskEntity> search(
            @Param("status") TaskStatus status,
            @Param("ownerId") String ownerId,
            @Param("assigneeId") String assigneeId,
            @Param("participantId") String participantId,
            @Param("periodFrom") java.time.LocalDate periodFrom,
            @Param("periodTo") java.time.LocalDate periodTo,
            Pageable pageable);

    @Query("""
            select distinct t from TaskEntity t
            left join t.participantIds participant
            where t.ownerId = :personId or t.assigneeId = :personId or participant = :personId
            """)
    Page<TaskEntity> searchForEmployee(@Param("personId") String personId, Pageable pageable);

    @Query("""
            select (count(t) > 0) from TaskEntity t
            left join t.participantIds participant
            where t.id = :taskId and (t.ownerId = :personId or t.assigneeId = :personId or participant = :personId)
            """)
    boolean hasAccess(@Param("taskId") String taskId, @Param("personId") String personId);
}
