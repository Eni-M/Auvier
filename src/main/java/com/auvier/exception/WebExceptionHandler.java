package com.auvier.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.UUID;

/**
 * Handles exceptions for web/HTML requests (Thymeleaf views).
 * Returns error pages instead of JSON responses.
 */
@ControllerAdvice
@Slf4j
@Order(1) // Higher priority than GlobalExceptionHandler
public class WebExceptionHandler {

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api") ||
               (accept != null && accept.contains("application/json"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return null; // Let GlobalExceptionHandler handle API requests
        }

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Web - Resource not found: {}", traceId, ex.getMessage());

        return createErrorView("error/404", HttpStatus.NOT_FOUND, ex.getMessage(), traceId);
    }

    @ExceptionHandler(ForbiddenException.class)
    public Object handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return null;
        }

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Web - Forbidden: {}", traceId, ex.getMessage());

        return createErrorView("error/403", HttpStatus.FORBIDDEN, ex.getMessage(), traceId);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return null;
        }

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Web - Unauthorized: {}", traceId, ex.getMessage());

        return createErrorView("error/403", HttpStatus.UNAUTHORIZED, ex.getMessage(), traceId);
    }

    @ExceptionHandler({BusinessException.class, InvalidRequestException.class})
    public Object handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return null;
        }

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Web - Bad request: {}", traceId, ex.getMessage());

        return createErrorView("error/400", HttpStatus.BAD_REQUEST, ex.getMessage(), traceId);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return null;
        }

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Web - No handler found: {}", traceId, ex.getRequestURL());

        return createErrorView("error/404", HttpStatus.NOT_FOUND, "Page not found", traceId);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return null;
        }

        String traceId = generateTraceId();
        log.error("TraceId: {} | Web - Unexpected error: ", traceId, ex);

        return createErrorView("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", traceId);
    }

    private ModelAndView createErrorView(String viewName, HttpStatus status, String message, String traceId) {
        ModelAndView mav = new ModelAndView(viewName);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("traceId", traceId);
        return mav;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

