package com.pasti_bisa.backend.shared.web;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs before security, so rejected requests also have a correlated log entry. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
	public static final String REQUEST_ID = ApiRequestLoggingFilter.class.getName() + ".requestId";
	public static final String ERROR = ApiRequestLoggingFilter.class.getName() + ".error";
	public static final String ERROR_CODE = ApiRequestLoggingFilter.class.getName() + ".errorCode";
	private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);

	public static String requestId(HttpServletRequest request) {
		if (request.getAttribute(REQUEST_ID) instanceof String id) return id;
		String id = UUID.randomUUID().toString();
		request.setAttribute(REQUEST_ID, id);
		return id;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String previous = MDC.get("requestId");
		String id = requestId(request);
		MDC.put("requestId", id);
		response.setHeader("X-Request-ID", id);
		long started = System.nanoTime();
		Throwable escaped = null;
		try {
			chain.doFilter(request, response);
		} catch (IOException | ServletException | RuntimeException exception) {
			escaped = exception;
			throw exception;
		} finally {
			try {
				int status = escaped == null ? response.getStatus() : 500;
				Throwable cause = escaped != null ? escaped : (Throwable) request.getAttribute(ERROR);
				String path = request.getRequestURI().split(";", 2)[0].replaceAll("[\\r\\n\\t]", "_");
				long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
				Object code = request.getAttribute(ERROR_CODE);
				String summary = "requestId=" + id + " method=" + request.getMethod() + " path=" + path
						+ " status=" + status + " durationMs=" + duration + " code=" + (code == null ? "-" : code);
				if (status >= 500) log.error(summary, cause);
				else if (status >= 400) log.warn("{} exception={}", summary, cause == null ? "-" : cause.getClass().getSimpleName());
				else log.info(summary);
			} finally {
				if (previous == null) MDC.remove("requestId"); else MDC.put("requestId", previous);
			}
		}
	}
}
