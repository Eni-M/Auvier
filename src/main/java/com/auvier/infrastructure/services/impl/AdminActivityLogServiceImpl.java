package com.auvier.infrastructure.services.impl;

import com.auvier.entities.AdminActivityLogEntity;
import com.auvier.infrastructure.services.AdminActivityLogService;
import com.auvier.repositories.AdminActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActivityLogServiceImpl implements AdminActivityLogService {

    private final AdminActivityLogRepository repository;

    @Override
    public void log(String action, String entityType, Long entityId, String entityName, String details) {
        // Generate a readable description
        String description = generateDescription(action, entityType, entityName, entityId);
        logDetailed(action, entityType, entityId, entityName, description, details);
    }

    @Override
    public void logDetailed(String action, String entityType, Long entityId, String entityName,
                            String description, String changesDetail) {
        logWithAudit(action, entityType, entityId, entityName, description, changesDetail, null, null);
    }

    @Override
    public void logWithAudit(String action, String entityType, Long entityId, String entityName,
                             String description, String changesDetail,
                             String previousValues, String newValues) {
        try {
            AdminActivityLogEntity logEntry = new AdminActivityLogEntity();
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            logEntry.setEntityName(entityName);
            logEntry.setDescription(description);
            logEntry.setChangesDetail(changesDetail);
            logEntry.setPreviousValues(previousValues);
            logEntry.setNewValues(newValues);
            logEntry.setAdminUsername(getCurrentUsername());
            logEntry.setAdminDisplayName(getCurrentUsername()); // Could be enhanced to get full name
            logEntry.setIpAddress(getClientIp());
            logEntry.setUserAgent(getUserAgent());
            logEntry.setSessionId(getSessionId());

            repository.save(logEntry);
            log.info("Admin activity: {} - {}", action, description);
        } catch (Exception e) {
            log.error("Failed to log admin activity", e);
        }
    }

    private String generateDescription(String action, String entityType, String entityName, Long entityId) {
        String actionVerb = switch (action) {
            case "CREATE" -> "Created";
            case "UPDATE" -> "Updated";
            case "DELETE" -> "Deleted";
            default -> action;
        };

        String entityLabel = entityType.toLowerCase();
        if (entityName != null && !entityName.isEmpty()) {
            return String.format("%s %s '%s' (ID: %d)", actionVerb, entityLabel, entityName, entityId);
        } else {
            return String.format("%s %s with ID %d", actionVerb, entityLabel, entityId);
        }
    }

    @Override
    public List<AdminActivityLogEntity> getRecentLogs() {
        return repository.findTop50ByOrderByTimestampDesc();
    }

    @Override
    public List<AdminActivityLogEntity> getLogsByAdmin(String username) {
        return repository.findByAdminUsernameOrderByTimestampDesc(username);
    }

    @Override
    public List<AdminActivityLogEntity> getLogsByEntityType(String entityType) {
        return repository.findByEntityTypeOrderByTimestampDesc(entityType);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP", e);
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not get user agent", e);
        }
        return null;
    }

    private String getSessionId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    return session.getId();
                }
            }
        } catch (Exception e) {
            log.debug("Could not get session ID", e);
        }
        return null;
    }
}
