package com.pasti_bisa.backend.modules.identity.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.pasti_bisa.backend.modules.identity.application.OAuthAccountRepository;

@Repository
public class JdbcOAuthAccountRepository implements OAuthAccountRepository {
        private static final RowMapper<Account> ACCOUNT_MAPPER = (rs, row) -> new Account(
                        rs.getObject("id", UUID.class), rs.getString("status"), rs.getString("email_canonical"));
        private final NamedParameterJdbcTemplate jdbc;

        public JdbcOAuthAccountRepository(NamedParameterJdbcTemplate jdbc) {
                this.jdbc = jdbc;
        }

        @Override
        public Optional<Account> findLinkedAccount(String provider, String subject) {
                return jdbc.query("""
                                SELECT account.id, account.status, account.email_canonical FROM oauth_account identity
                                JOIN app_user account ON account.id = identity.user_id
                                WHERE identity.provider = :provider AND identity.provider_subject = :subject
                                """, new MapSqlParameterSource("provider", provider).addValue("subject", subject),
                                ACCOUNT_MAPPER).stream().findFirst();
        }

        @Override
        public Optional<Account> findByEmail(String canonicalEmail) {
                return jdbc.query("SELECT id, status, email_canonical FROM app_user WHERE email_canonical = :email",
                                new MapSqlParameterSource("email", canonicalEmail), ACCOUNT_MAPPER).stream()
                                .findFirst();
        }

        @Override
        public Account createCustomer(String email, String canonicalEmail, String displayName) {
                UUID id = UUID.randomUUID();
                var params = new MapSqlParameterSource("id", id).addValue("email", email)
                                .addValue("canonicalEmail", canonicalEmail).addValue("displayName", displayName);
                jdbc.update("""
                                INSERT INTO app_user(id, email, email_canonical, password_hash, display_name, email_verified_at)
                                VALUES (:id, :email, :canonicalEmail, NULL, :displayName, now())
                                """,
                                params);
                jdbc.update("INSERT INTO user_role(user_id, role_code) VALUES (:id, 'CUSTOMER')", params);
                return new Account(id, "ACTIVE", canonicalEmail);
        }

        @Override
        public void link(UUID userId, String provider, String subject) {
                jdbc.update("INSERT INTO oauth_account(user_id, provider, provider_subject) VALUES (:userId, :provider, :subject)",
                                new MapSqlParameterSource("userId", userId).addValue("provider", provider)
                                                .addValue("subject", subject));
        }

        @Override
        public List<String> roles(UUID userId) {
                return jdbc.queryForList("SELECT role_code FROM user_role WHERE user_id = :userId",
                                new MapSqlParameterSource("userId", userId), String.class);
        }
}
