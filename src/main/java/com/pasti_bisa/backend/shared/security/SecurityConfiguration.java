package com.pasti_bisa.backend.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.pasti_bisa.backend.modules.identity.adapter.in.web.PastiSiapOidcUserService;
import com.pasti_bisa.backend.modules.identity.application.UserRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
	@Bean
	PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	@Bean
	UserDetailsService userDetailsService(UserRepository users) {
		return email -> {
			var account = users.findPasswordAccount(email)
					.orElseThrow(() -> new UsernameNotFoundException("Password login unavailable"));
			return org.springframework.security.core.userdetails.User.withUsername(account.email())
					.password(account.passwordHash()).roles(account.roles().toArray(String[]::new)).build();
		};
	}

	@Bean
	DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
		var provider = new DaoAuthenticationProvider(users);
		provider.setPasswordEncoder(encoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			SecurityContextRepository securityContextRepository,
			PastiSiapOidcUserService oidcUserService,
			DaoAuthenticationProvider daoProvider,
			ObjectProvider<ClientRegistrationRepository> oauthClients,
			ApiSecurityErrorHandler apiErrors,
			@Value("${pastisiap.auth.success-redirect:/}") String successRedirect) throws Exception {
		http
				.authenticationProvider(daoProvider)
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health", "/actuator/health/**", "/api/v1/health", "/api/v1/csrf", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/oauth2/**", "/login/oauth2/**").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(errors -> errors
						.defaultAuthenticationEntryPointFor(apiErrors,
								request -> request.getRequestURI().startsWith("/api/"))
						.accessDeniedHandler(apiErrors))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout
						.logoutUrl("/api/v1/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
		if (oauthClients.getIfAvailable() != null) {
			http.oauth2Login(oauth -> oauth
						.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
						.defaultSuccessUrl(successRedirect, true));
		}
		return http.build();
	}
}
