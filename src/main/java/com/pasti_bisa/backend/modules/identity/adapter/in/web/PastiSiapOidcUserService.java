package com.pasti_bisa.backend.modules.identity.adapter.in.web;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.pasti_bisa.backend.modules.identity.application.OAuthAccountProvisioningService;
import com.pasti_bisa.backend.modules.identity.application.dto.OAuthSignInCommand;
import com.pasti_bisa.backend.modules.identity.application.dto.OAuthAccountIdentity;

@Service
public class PastiSiapOidcUserService extends OidcUserService {
	private final OAuthAccountProvisioningService accounts;

	public PastiSiapOidcUserService(OAuthAccountProvisioningService accounts) {
		this.accounts = accounts;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(request);
		String email = oidcUser.getEmail();
		if (email == null || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
			throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
					"OAuth provider must return a verified email address");
		}
		String name = oidcUser.getFullName();
		if (name == null || name.isBlank()) {
			name = email.substring(0, email.indexOf('@'));
		}
		String provider = request.getClientRegistration().getRegistrationId();
		OAuthAccountIdentity account;
		try {
			account = accounts.provision(new OAuthSignInCommand(provider, oidcUser.getSubject(), email, name));
		} catch (OAuthAccountProvisioningService.AccountUnavailableException exception) {
			throw new OAuth2AuthenticationException(new OAuth2Error("account_unavailable"),
					"This account is unavailable");
		}
		var authorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>(
				oidcUser.getAuthorities());
		account.roleCodes().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(authorities::add);
		return new AccountOidcUser(authorities, oidcUser, account.canonicalEmail());
	}

	private static final class AccountOidcUser extends DefaultOidcUser {
		private final String canonicalEmail;

		private AccountOidcUser(
				java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities,
				OidcUser source, String canonicalEmail) {
			super(authorities, source.getIdToken(), source.getUserInfo(), "email");
			this.canonicalEmail = canonicalEmail;
		}

		@Override
		public String getName() {
			return canonicalEmail;
		}
	}
}
