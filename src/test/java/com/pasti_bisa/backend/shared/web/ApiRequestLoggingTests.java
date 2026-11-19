package com.pasti_bisa.backend.shared.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import tools.jackson.databind.json.JsonMapper;

import com.pasti_bisa.backend.shared.security.ApiSecurityErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRequestLoggingTests {
	@Test
	void unexpectedErrorHasSameIdInResponseAndLogAndDoesNotLogRequestSecrets() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
		var captured = new ListAppender<ILoggingEvent>();
		captured.start();
		logger.addAppender(captured);
		var request = new MockHttpServletRequest("PATCH", "/api/v1/users/current/profile");
		request.setQueryString("token=QUERY_SECRET");
		request.addHeader("Authorization", "Bearer HEADER_SECRET");
		request.setContent("{\"password\":\"BODY_SECRET\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		var response = new MockHttpServletResponse();
		MDC.put("requestId", "previous-context");
		try {
			new ApiRequestLoggingFilter().doFilter(request, response, (req, res) -> {
				var result = new ApiExceptionHandler().handleUnexpected(new IllegalStateException("diagnostic failure"), request);
				response.setStatus(result.getStatusCode().value());
				assertThat(result.getBody().requestId()).isEqualTo(response.getHeader("X-Request-ID"));
			});
			assertThat(MDC.get("requestId")).isEqualTo("previous-context");
			assertThat(captured.list).hasSize(1);
			var event = captured.list.getFirst();
			assertThat(event.getFormattedMessage()).contains("status=500", "method=PATCH", response.getHeader("X-Request-ID"));
			assertThat(event.getFormattedMessage()).doesNotContain("QUERY_SECRET", "HEADER_SECRET", "BODY_SECRET");
			assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
		} finally {
			MDC.remove("requestId");
			logger.detachAppender(captured);
			captured.stop();
		}
	}

	@Test
	void csrfRejectionUsesSameRequestIdAndDoesNotReturnToken() throws Exception {
		var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		var response = new MockHttpServletResponse();
		var json = JsonMapper.builder().build();
		var errors = new ApiSecurityErrorHandler(json);
		new ApiRequestLoggingFilter().doFilter(request, response, (req, res) ->
				errors.handle(request, response, new MissingCsrfTokenException("SECRET_CSRF")));
		assertThat(response.getStatus()).isEqualTo(403);
		var body = json.readTree(response.getContentAsString());
		assertThat(body.get("code").asString()).isEqualTo("CSRF_INVALID");
		assertThat(body.get("requestId").asString()).isEqualTo(response.getHeader("X-Request-ID"));
		assertThat(response.getContentAsString()).doesNotContain("SECRET_CSRF");
		assertThat(MDC.get("requestId")).isNull();
	}
}
