package com.pasti_bisa.backend.shared.web;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pasti_bisa.backend.modules.identity.adapter.out.persistence.JdbcUserRepository.EmailAlreadyRegisteredException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleMalformedJson(Exception exception, HttpServletRequest request) {
		return error(request, exception, 400, "MALFORMED_JSON", "Request JSON is missing required fields or has invalid types", Map.of());
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiError> handleBusiness(BusinessException exception, HttpServletRequest request) {
		return error(request, exception, exception.status(), exception.code(), exception.getMessage(), Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.collect(java.util.stream.Collectors.toMap(
						error -> error.getField(),
						error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
						(first, ignored) -> first));
		return error(request, exception, 400, "VALIDATION_FAILED", "Request validation failed", fieldErrors);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
		return error(request, exception, 400, "INVALID_REQUEST", "Request contains an invalid value", Map.of());
	}

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ResponseEntity<ApiError> handleDuplicateEmail(EmailAlreadyRegisteredException exception, HttpServletRequest request) {
		return error(request, exception, 409, "EMAIL_ALREADY_REGISTERED", "An account already exists for this email", Map.of("email", "Already registered"));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> handleAuthenticationFailure(AuthenticationException exception, HttpServletRequest request) {
		return error(request, exception, 401, "INVALID_CREDENTIALS", "Email or password is incorrect", Map.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		if (exception instanceof org.springframework.web.ErrorResponse response) {
			int status = response.getStatusCode().value();
			return error(request, exception, status, "HTTP_" + status, "Request could not be processed", Map.of());
		}
		if (exception instanceof org.springframework.dao.EmptyResultDataAccessException) {
			return error(request, exception, 404, "NOT_FOUND", "Requested resource was not found", Map.of());
		}
		if (exception instanceof org.springframework.security.access.AccessDeniedException) {
			return error(request, exception, 403, "ACCESS_DENIED", "Access denied", Map.of());
		}
		return error(request, exception, 500, "INTERNAL_ERROR", "An unexpected error occurred", Map.of());
	}

	private ResponseEntity<ApiError> error(HttpServletRequest request, Exception exception, int status,
			String code, String message, Map<String, String> fields) {
		request.setAttribute(ApiRequestLoggingFilter.ERROR, exception);
		request.setAttribute(ApiRequestLoggingFilter.ERROR_CODE, code);
		String requestId = ApiRequestLoggingFilter.requestId(request);
		return ResponseEntity.status(status).header("X-Request-ID", requestId)
				.body(new ApiError(code, message, fields, requestId));
	}

	public record ApiError(String code, String message, Map<String, String> fieldErrors, String requestId) {
	}
}
