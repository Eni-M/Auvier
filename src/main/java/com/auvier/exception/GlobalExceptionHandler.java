package com.auvier.exception;

import com.auvier.dtos.errors.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles exceptions for REST API requests.
 * Returns JSON error responses.
 */
@RestControllerAdvice
@Slf4j
@Order(2) // Lower priority - WebExceptionHandler handles web requests first
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Resource not found: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Duplicate resource: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Business error: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Unauthorized: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDto> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Forbidden: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRequest(
            InvalidRequestException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Invalid request: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        BindingResult result = ex.getBindingResult();

        List<ErrorResponseDto.FieldError> fieldErrors = result.getFieldErrors()
                .stream()
                .map(f -> new ErrorResponseDto.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();

        log.warn("TraceId: {} | Validation failed: {}", traceId, fieldErrors);

        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.error("TraceId: {} | Data integrity violation: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.CONFLICT, "Data Conflict",
                "Database constraint violation occurred", request.getRequestURI(), traceId);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Method not supported: {}", traceId, ex.getMethod());

        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                request.getRequestURI(), traceId);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | No handler found: {}", traceId, ex.getRequestURL());

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                "The requested endpoint does not exist", request.getRequestURI(), traceId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("TraceId: {} | Illegal argument: {}", traceId, ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI(), traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.error("TraceId: {} | Unexpected error: ", traceId, ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", "An unexpected error occurred",
                request.getRequestURI(), traceId);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            HttpStatus status, String error, String message, String path, String traceId) {

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(status).body(errorDto);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

