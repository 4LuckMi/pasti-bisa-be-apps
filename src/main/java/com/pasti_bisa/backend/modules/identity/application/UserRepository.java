package com.pasti_bisa.backend.modules.identity.application;

import java.util.Optional;

import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;
import com.pasti_bisa.backend.modules.identity.application.dto.PasswordAccount;

public interface UserRepository {
	CustomerAccount createCustomer(String email, String canonicalEmail, String displayName, String passwordHash);

	Optional<CustomerAccount> findByCanonicalEmail(String canonicalEmail);

	Optional<PasswordAccount> findPasswordAccount(String canonicalEmail);
}
