package com.pasti_bisa.backend.modules.identity.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pasti_bisa.backend.modules.identity.application.UserRepository;
import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;

@Repository
public class JdbcUserRepository implements UserRepository {
	private final JdbcTemplate jdbc;

	public JdbcUserRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public CustomerAccount createCustomer(String email, String canonicalEmail, String displayName, String passwordHash) {
		UUID id = UUID.randomUUID();
		try {
			jdbc.update("""
					INSERT INTO app_user(id, email, email_canonical, password_hash, display_name)
					VALUES (?::uuid, ?, ?, ?, ?)
					""", id.toString(), email, canonicalEmail, passwordHash, displayName);
			jdbc.update("INSERT INTO user_role(user_id, role_code) VALUES (?::uuid, 'CUSTOMER')", id.toString());
			return new CustomerAccount(id, canonicalEmail, displayName);
		} catch (DuplicateKeyException exception) {
			throw new EmailAlreadyRegisteredException();
		}
	}

	@Override
	public Optional<CustomerAccount> findByCanonicalEmail(String canonicalEmail) {
		return jdbc.query("""
				SELECT id, email_canonical, display_name
				FROM app_user WHERE email_canonical = ? AND status = 'ACTIVE'
				""", (result, row) -> new CustomerAccount(
						result.getObject("id", UUID.class), result.getString("email_canonical"),
						result.getString("display_name")), canonicalEmail)
			.stream().findFirst();
	}

	public static class EmailAlreadyRegisteredException extends RuntimeException {
	}
}
