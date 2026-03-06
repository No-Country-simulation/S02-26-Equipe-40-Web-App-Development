package com.nocountry.conversionflow.conversionflow_api.controller.error;

import com.nocountry.conversionflow.conversionflow_api.application.exception.AppConfigurationException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.DuplicateLeadException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.InvalidInputException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.LeadNotFoundException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.StripeIntegrationException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.StripeWebhookException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(LeadNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLeadNotFound(LeadNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateLeadException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateLead(DuplicateLeadException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({InvalidInputException.class, StripeWebhookException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(StripeIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleStripeIntegration(StripeIntegrationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AppConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> handleConfiguration(AppConfigurationException ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                OffsetDateTime.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }

    public record ApiErrorResponse(
            int status,
            String error,
            String message,
            String path,
            String timestamp
    ) {
    }
}
