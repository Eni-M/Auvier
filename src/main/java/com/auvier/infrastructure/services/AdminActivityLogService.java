package com.auvier.infrastructure.services;

import com.auvier.entities.AdminActivityLogEntity;

import java.util.List;

public interface AdminActivityLogService {

    /**
     * Log an admin action (simple)
     */
    void log(String action, String entityType, Long entityId, String entityName, String details);

    /**
     * Log an admin action with detailed description
     */
    void logDetailed(String action, String entityType, Long entityId, String entityName,
                     String description, String changesDetail);

    /**
     * Log an admin action with full audit trail (including previous/new values)
     */
    void logWithAudit(String action, String entityType, Long entityId, String entityName,
                      String description, String changesDetail,
                      String previousValues, String newValues);

    /**
     * Get recent activity logs
     */
    List<AdminActivityLogEntity> getRecentLogs();

    /**
     * Get logs by admin username
     */
    List<AdminActivityLogEntity> getLogsByAdmin(String username);

    /**
     * Get logs by entity type
     */
    List<AdminActivityLogEntity> getLogsByEntityType(String entityType);
}
