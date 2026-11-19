package com.pasti_bisa.backend.modules.identity.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.pasti_bisa.backend.modules.identity.application.UserRepository;
import com.pasti_bisa.backend.modules.identity.application.dto.PasswordAccount;
import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CustomerAccount createCustomer(String email, String canonicalEmail, String displayName,
            String passwordHash) {
        UUID id = UUID.randomUUID();
        var params = new MapSqlParameterSource("id", id).addValue("email", email)
                .addValue("canonicalEmail", canonicalEmail)
                .addValue("displayName", displayName).addValue("passwordHash", passwordHash);
        try {
            jdbc.update("""
                    INSERT INTO app_user(id, email, email_canonical, password_hash, display_name, user_type)
                    VALUES (:id, :email, :canonicalEmail, :passwordHash, :displayName, 'SISWA')
                    """, params);
            jdbc.update("INSERT INTO user_role(user_id, role_code) VALUES (:id, 'CUSTOMER')", params);
            return new CustomerAccount(id, canonicalEmail, displayName, "SISWA", List.of("CUSTOMER"));
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Override
    public Optional<CustomerAccount> findByCanonicalEmail(String canonicalEmail) {
        var accounts = jdbc.query("""
                SELECT id, email_canonical, display_name, user_type FROM app_user
                WHERE email_canonical = :email AND status = 'ACTIVE'
                """, new MapSqlParameterSource("email", canonicalEmail), (rs, row) -> new CustomerAccount(
                rs.getObject("id", UUID.class), rs.getString("email_canonical"), rs.getString("display_name"),
                rs.getString("user_type"), List.of()));
        return accounts.stream().findFirst().map(account -> new CustomerAccount(account.id(), account.email(),
                account.displayName(), account.userType(), roles(account.id())));
    }

    @Override
    public Optional<PasswordAccount> findPasswordAccount(String canonicalEmail) {
        var accounts = jdbc.query("""
                SELECT id, email_canonical, password_hash FROM app_user
                WHERE email_canonical = :email AND status = 'ACTIVE' AND password_hash IS NOT NULL
                """, new MapSqlParameterSource("email", canonicalEmail), (rs, row) -> new PasswordAccount(
                rs.getObject("id", UUID.class), rs.getString("email_canonical"), rs.getString("password_hash"),
                List.of()));
        return accounts.stream().findFirst().map(account -> new PasswordAccount(
                account.id(), account.email(), account.passwordHash(), roles(account.id())));
    }

    private List<String> roles(UUID userId) {
        return jdbc.queryForList("SELECT role_code FROM user_role WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId), String.class);
    }

    public static class EmailAlreadyRegisteredException extends RuntimeException {
    }
}
