package com.auvier.repositories;

import com.auvier.entities.AdminActivityLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLogEntity, Long> {

    List<AdminActivityLogEntity> findTop50ByOrderByTimestampDesc();

    List<AdminActivityLogEntity> findByAdminUsernameOrderByTimestampDesc(String username);

    List<AdminActivityLogEntity> findByEntityTypeOrderByTimestampDesc(String entityType);

    List<AdminActivityLogEntity> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
