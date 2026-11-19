package com.pasti_bisa.backend.shared.security;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import com.pasti_bisa.backend.shared.web.ApiExceptionHandler.ApiError;
import com.pasti_bisa.backend.shared.web.ApiRequestLoggingFilter;

@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
	private final JsonMapper json;
	public ApiSecurityErrorHandler(JsonMapper json) { this.json = json; }

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
		write(request, response, exception, 401, "AUTHENTICATION_REQUIRED", "Please sign in to continue");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
		boolean csrf = exception instanceof CsrfException;
		write(request, response, exception, 403, csrf ? "CSRF_INVALID" : "ACCESS_DENIED",
				csrf ? "CSRF token is missing or invalid; fetch a new token with the same session" : "Access denied");
	}

	private void write(HttpServletRequest request, HttpServletResponse response, Exception exception,
			int status, String code, String message) throws IOException {
		request.setAttribute(ApiRequestLoggingFilter.ERROR, exception);
		request.setAttribute(ApiRequestLoggingFilter.ERROR_CODE, code);
		String id = ApiRequestLoggingFilter.requestId(request);
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("X-Request-ID", id);
		json.writeValue(response.getOutputStream(), new ApiError(code, message, Map.of(), id));
	}
}
