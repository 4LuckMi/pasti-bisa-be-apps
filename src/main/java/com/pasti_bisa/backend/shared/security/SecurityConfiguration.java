package com.pasti_bisa.backend.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
	private record UserAccount(String id, String email, String passwordHash, String status) {
	}
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(JdbcTemplate jdbc) {
		return email -> {
			var users = jdbc.query("""
					SELECT id, email_canonical, password_hash, status
					FROM app_user WHERE email_canonical = ?
					""", (result, row) -> new UserAccount(
						result.getString("id"), result.getString("email_canonical"),
						result.getString("password_hash"), result.getString("status")), email);
			if (users.isEmpty()) {
				throw new UsernameNotFoundException("Account not found");
			}
			var account = users.getFirst();
			if (!"ACTIVE".equals(account.status())) {
				throw new UsernameNotFoundException("Account is unavailable");
			}
			var roles = jdbc.queryForList("SELECT role_code FROM user_role WHERE user_id = ?::uuid", String.class, account.id());
			return org.springframework.security.core.userdetails.User.withUsername(account.email())
					.password(account.passwordHash()).roles(roles.toArray(String[]::new)).build();
		};
	}

	@Bean
	DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		var provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
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
	SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider,
			SecurityContextRepository securityContextRepository) throws Exception {
		return http
				.authenticationProvider(provider)
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health", "/actuator/health/**", "/api/v1/health", "/api/v1/csrf", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout
						.logoutUrl("/api/v1/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
				.build();
	}
}
