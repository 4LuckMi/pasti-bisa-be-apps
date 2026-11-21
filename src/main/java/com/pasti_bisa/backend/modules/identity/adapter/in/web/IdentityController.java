package com.pasti_bisa.backend.modules.identity.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pasti_bisa.backend.modules.identity.application.IdentityService;
import com.pasti_bisa.backend.modules.identity.application.dto.AuthResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.LoginRequest;
import com.pasti_bisa.backend.modules.identity.application.dto.RegisterRequest;
import com.pasti_bisa.backend.modules.identity.application.dto.UserProfileResponse;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {
	private final IdentityService identities;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final com.pasti_bisa.backend.shared.security.AccountSecurityState accountStates;

	public IdentityController(IdentityService identities, AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository, com.pasti_bisa.backend.shared.security.AccountSecurityState accountStates) {
		this.identities = identities;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.accountStates=accountStates;
	}

	@PostMapping("/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		identities.register(request.email(), request.displayName(), request.password());
		return authenticate(request.email(), request.password(), servletRequest, servletResponse);
	}

	@PostMapping("/auth/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		return authenticate(request.email(), request.password(), servletRequest, servletResponse);
	}

	@GetMapping("/users/current")
	public UserProfileResponse me(Authentication authentication) {
		return UserProfileResponse.from(identities.getProfile(authentication.getName()));
	}

	private AuthResponse authenticate(String email, String password, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
				email.trim().toLowerCase(java.util.Locale.ROOT), password));
		if (request.getSession(false) != null)
			request.changeSessionId();
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		com.pasti_bisa.backend.shared.security.AccountSessionGuard.bind(request,authentication,accountStates);
		securityContextRepository.saveContext(context, request, response);
		return new AuthResponse(UserProfileResponse.from(identities.getProfile(authentication.getName())));
	}

}
