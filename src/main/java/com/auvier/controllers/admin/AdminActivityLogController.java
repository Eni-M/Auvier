package com.auvier.controllers.admin;

import com.auvier.entities.AdminActivityLogEntity;
import com.auvier.infrastructure.services.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminActivityLogController {

    private final AdminActivityLogService activityLogService;

    @GetMapping("/admin/activity")
    public String viewLogs(Model model) {
        model.addAttribute("logs", activityLogService.getRecentLogs());
        return "admin/activity/list";
    }

    @GetMapping("/api/admin/my-activity")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMyActivity(Authentication authentication) {
        String username = authentication.getName();
        List<AdminActivityLogEntity> logs = activityLogService.getLogsByAdmin(username);

        // Limit to 10 most recent and format for frontend
        List<Map<String, Object>> result = logs.stream()
                .limit(10)
                .map(log -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", log.getId());
                    map.put("action", log.getAction());
                    map.put("entityType", log.getEntityType());
                    map.put("entityId", log.getEntityId());
                    map.put("entityName", log.getEntityName());
                    // Use the detailed description if available, otherwise generate one
                    String description = log.getDescription();
                    if (description == null || description.isEmpty()) {
                        description = formatDescription(log);
                    }
                    map.put("description", description);
                    map.put("timeAgo", formatTimeAgo(log.getTimestamp()));
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private String formatDescription(AdminActivityLogEntity log) {
        String actionVerb = switch (log.getAction()) {
            case "CREATE" -> "Created";
            case "UPDATE" -> "Updated";
            case "DELETE" -> "Deleted";
            default -> log.getAction();
        };

        if (log.getEntityName() != null && !log.getEntityName().isEmpty()) {
            return String.format("%s %s '%s'", actionVerb, log.getEntityType().toLowerCase(), log.getEntityName());
        } else {
            return String.format("%s %s #%d", actionVerb, log.getEntityType().toLowerCase(), log.getEntityId());
        }
    }

    private String formatTimeAgo(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";

        long hours = ChronoUnit.HOURS.between(timestamp, now);
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";

        long days = ChronoUnit.DAYS.between(timestamp, now);
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";

        return timestamp.toLocalDate().toString();
    }
}
