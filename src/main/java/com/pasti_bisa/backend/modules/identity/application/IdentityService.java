package com.pasti_bisa.backend.modules.identity.application;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;

@Service
public class IdentityService {
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public IdentityService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CustomerAccount register(String email, String displayName, String password) {
		String normalizedEmail = email.trim();
		String canonicalEmail = normalizedEmail.toLowerCase(Locale.ROOT);
		return users.createCustomer(normalizedEmail, canonicalEmail, displayName.trim(), passwordEncoder.encode(password));
	}

	public boolean passwordMatches(String rawPassword, String passwordHash) {
		return passwordEncoder.matches(rawPassword, passwordHash);
	}

	@Transactional(readOnly = true)
	public CustomerAccount getProfile(String email) {
		return users.findByCanonicalEmail(email.toLowerCase(Locale.ROOT))
				.orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Account not found"));
	}
}
