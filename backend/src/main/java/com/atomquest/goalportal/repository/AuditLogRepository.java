package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, UUID entityId);
    List<AuditLog> findAllByOrderByChangedAtDesc();
}
