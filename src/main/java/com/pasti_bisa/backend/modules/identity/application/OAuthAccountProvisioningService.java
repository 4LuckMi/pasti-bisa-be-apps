package com.pasti_bisa.backend.modules.identity.application;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.identity.application.dto.OAuthAccountIdentity;
import com.pasti_bisa.backend.modules.identity.application.dto.OAuthSignInCommand;

@Service
public class OAuthAccountProvisioningService {
    private final OAuthAccountRepository repository;
    public OAuthAccountProvisioningService(OAuthAccountRepository repository) { this.repository = repository; }

    @Transactional
    public OAuthAccountIdentity provision(OAuthSignInCommand command) {
        var linked = repository.findLinkedAccount(command.provider(), command.subject());
        if (linked.isPresent()) return identity(linked.get());

        String email = command.email().trim();
        String canonicalEmail = email.toLowerCase(Locale.ROOT);
        var account = repository.findByEmail(canonicalEmail)
                .orElseGet(() -> repository.createCustomer(email, canonicalEmail, command.displayName().trim()));
        requireActive(account);
        repository.link(account.id(), command.provider(), command.subject());
        return identity(account);
    }

    private OAuthAccountIdentity identity(OAuthAccountRepository.Account account) {
        requireActive(account);
        return new OAuthAccountIdentity(account.canonicalEmail(), repository.roles(account.id()));
    }

    private void requireActive(OAuthAccountRepository.Account account) {
        if (!"ACTIVE".equals(account.status())) throw new AccountUnavailableException();
    }

    public static class AccountUnavailableException extends RuntimeException {}
}
