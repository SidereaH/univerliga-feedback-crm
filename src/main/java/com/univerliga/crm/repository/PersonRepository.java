package com.univerliga.crm.repository;

import com.univerliga.crm.model.PersonEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<PersonEntity, String> {

    Optional<PersonEntity> findByEmail(String email);

    @Query("""
            select p from PersonEntity p
            where (:hasQuery = false or lower(p.displayName) like :queryPattern or lower(p.email) like :queryPattern)
              and (:departmentId is null or p.departmentId = :departmentId)
              and (:teamId is null or p.teamId = :teamId)
              and (:active is null or p.active = :active)
            """)
    Page<PersonEntity> search(
            @Param("hasQuery") boolean hasQuery,
            @Param("queryPattern") String queryPattern,
            @Param("departmentId") String departmentId,
            @Param("teamId") String teamId,
            @Param("active") Boolean active,
            Pageable pageable);

    boolean existsByIdAndActiveTrue(String id);
}
