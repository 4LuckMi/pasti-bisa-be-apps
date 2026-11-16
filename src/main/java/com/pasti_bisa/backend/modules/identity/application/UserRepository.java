package com.pasti_bisa.backend.modules.identity.application;

import java.util.Optional;

import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;

public interface UserRepository {
	CustomerAccount createCustomer(String email, String canonicalEmail, String displayName, String passwordHash);
	Optional<CustomerAccount> findByCanonicalEmail(String canonicalEmail);
}
