package com.auvier.controllers.admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Custom error controller to display Thymeleaf error pages
 * instead of Spring Boot's default whitelabel error page.
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        String traceId = generateTraceId();

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        String errorMessage = message != null ? message.toString() : "An error occurred";

        // Log the error
        if (statusCode >= 500) {
            log.error("TraceId: {} | Error {} at {}: {}",
                    traceId, statusCode, request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                    exception != null ? exception : errorMessage);
        } else {
            log.warn("TraceId: {} | Error {} at {}: {}",
                    traceId, statusCode, request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), errorMessage);
        }

        model.addAttribute("status", statusCode);
        model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        model.addAttribute("message", errorMessage);
        model.addAttribute("traceId", traceId);

        // Return appropriate error page
        return switch (statusCode) {
            case 400 -> "error/400";
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 500 -> "error/500";
            default -> {
                if (statusCode >= 400 && statusCode < 500) {
                    yield "error/400";
                }
                yield "error/500";
            }
        };
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

