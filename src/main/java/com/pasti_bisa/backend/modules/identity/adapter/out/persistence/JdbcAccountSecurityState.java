package com.pasti_bisa.backend.modules.identity.adapter.out.persistence;

import java.util.Optional;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import com.pasti_bisa.backend.shared.security.AccountSecurityState;

@Repository
public class JdbcAccountSecurityState implements AccountSecurityState {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcAccountSecurityState(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Optional<State> find(String email) {
        return jdbc.query("SELECT status,auth_revision FROM app_user WHERE email_canonical=:email",
            new MapSqlParameterSource("email",email.toLowerCase(java.util.Locale.ROOT)),
            (rs,row)->new State(rs.getString("status"),rs.getLong("auth_revision"),jdbc.queryForList("SELECT r.role_code FROM user_role r JOIN app_user u ON u.id=r.user_id WHERE u.email_canonical=:email ORDER BY r.role_code",new MapSqlParameterSource("email",email.toLowerCase(java.util.Locale.ROOT)),String.class))).stream().findFirst();
    }
}
