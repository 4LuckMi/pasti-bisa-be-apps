package com.pasti_bisa.backend.modules.identity.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository {
    record Account(UUID id, String status, String canonicalEmail) {}
    Optional<Account> findLinkedAccount(String provider, String subject);
    Optional<Account> findByEmail(String canonicalEmail);
    Account createCustomer(String email, String canonicalEmail, String displayName);
    void link(UUID userId, String provider, String subject);
    List<String> roles(UUID userId);
}
