package com.pasti_bisa.backend.shared.web;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pasti_bisa.backend.modules.identity.adapter.out.persistence.JdbcUserRepository.EmailAlreadyRegisteredException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.collect(java.util.stream.Collectors.toMap(
						error -> error.getField(),
						error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
						(first, ignored) -> first));
		return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED", "Request validation failed", fieldErrors, UUID.randomUUID().toString()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception) {
		return ResponseEntity.internalServerError().body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred", Map.of(), UUID.randomUUID().toString()));
	}

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ResponseEntity<ApiError> handleDuplicateEmail() {
		return ResponseEntity.status(409).body(new ApiError("EMAIL_ALREADY_REGISTERED", "An account already exists for this email", Map.of("email", "Already registered"), UUID.randomUUID().toString()));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> handleAuthenticationFailure() {
		return ResponseEntity.status(401).body(new ApiError("INVALID_CREDENTIALS", "Email or password is incorrect", Map.of(), UUID.randomUUID().toString()));
	}

	public record ApiError(String code, String message, Map<String, String> fieldErrors, String requestId) {
	}
}
