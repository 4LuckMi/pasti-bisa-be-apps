package com.pasti_bisa.backend.modules.identity.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pasti_bisa.backend.modules.identity.application.IdentityService;
import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {
	private final IdentityService identities;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;

	public IdentityController(IdentityService identities, AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.identities = identities;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/auth/register")
	@org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
	public Profile register(@Valid @RequestBody RegisterRequest request) {
		return profile(identities.register(request.email(), request.displayName(), request.password()));
	}

	@PostMapping("/auth/login")
	public Profile login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(java.util.Locale.ROOT), request.password()));
		if (servletRequest.getSession(false) != null) {
			servletRequest.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, servletRequest, servletResponse);
		return profile(identities.getProfile(authentication.getName()));
	}

	@GetMapping("/me")
	public Profile me(Authentication authentication) {
		return profile(identities.getProfile(authentication.getName()));
	}

	private Profile profile(CustomerAccount account) {
		return new Profile(account.id().toString(), account.email(), account.displayName());
	}

	public record RegisterRequest(@NotBlank @Email @Size(max = 320) String email,
			@NotBlank @Size(min = 2, max = 120) String displayName,
			@NotBlank @Size(min = 12, max = 72) String password) {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record Profile(String id, String email, String displayName) {
	}
}
