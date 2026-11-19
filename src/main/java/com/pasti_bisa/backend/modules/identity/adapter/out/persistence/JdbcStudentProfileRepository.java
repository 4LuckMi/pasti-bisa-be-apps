package com.pasti_bisa.backend.modules.identity.adapter.out.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.pasti_bisa.backend.modules.identity.application.StudentProfileRepository;
import com.pasti_bisa.backend.modules.identity.application.dto.StudentProfileResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.UpdateStudentProfileCommand;

@Repository
public class JdbcStudentProfileRepository implements StudentProfileRepository {
        private final NamedParameterJdbcTemplate jdbc;

        public JdbcStudentProfileRepository(NamedParameterJdbcTemplate jdbc) {
                this.jdbc = jdbc;
        }

        @Override
        public StudentProfileResponse findByEmail(String email) {
                return jdbc.queryForObject(
                                """
                                                SELECT account.id, account.email, account.display_name, account.user_type,
                                                       profile.school_name, profile.school_grade, profile.leaderboard_opt_in, profile.target_score,
                                                       target.kind AS target_kind, target.institution AS target_institution, target.program AS target_program
                                                FROM app_user account
                                                JOIN user_profile profile ON profile.user_id = account.id
                                                LEFT JOIN target ON target.id = profile.target_id
                                                WHERE account.email_canonical = :email AND account.status = 'ACTIVE'
                                                """,
                                new MapSqlParameterSource("email", email), (rs, row) -> new StudentProfileResponse(
                                                rs.getObject("id", UUID.class), rs.getString("email"),
                                                rs.getString("display_name"),
                                                rs.getString("user_type"), rs.getString("school_name"),
                                                rs.getString("school_grade"),
                                                rs.getBoolean("leaderboard_opt_in"),
                                                rs.getObject("target_score", Integer.class),
                                                rs.getString("target_kind"), rs.getString("target_institution"),
                                                rs.getString("target_program")));
        }

        @Override
        public void updateByEmail(String email, UpdateStudentProfileCommand command) {
                UUID userId = jdbc.queryForObject(
                                "SELECT id FROM app_user WHERE email_canonical = :email AND status = 'ACTIVE'",
                                new MapSqlParameterSource("email", email), UUID.class);
                var params = new MapSqlParameterSource()
                                .addValue("userId", userId)
                                .addValue("displayName", command.displayName())
                                .addValue("schoolName", command.schoolName())
                                .addValue("schoolGrade", command.schoolGrade())
                                .addValue("targetKind", command.targetKind())
                                .addValue("targetInstitution", command.targetInstitution())
                                .addValue("targetProgram", command.targetProgram(), java.sql.Types.VARCHAR)
                                .addValue("targetScore", command.targetScore(), java.sql.Types.INTEGER);
                UUID targetId = jdbc.query("""
                                SELECT id FROM target WHERE kind = :targetKind AND institution = :targetInstitution
                                AND program IS NOT DISTINCT FROM :targetProgram
                                """, params, (rs, row) -> rs.getObject("id", UUID.class)).stream().findFirst()
                                .orElseGet(() -> {
                                        UUID id = UUID.randomUUID();
                                        jdbc.update("""
                                                        INSERT INTO target(id, kind, institution, program, source_status)
                                                        VALUES (:targetId, :targetKind, :targetInstitution, :targetProgram, 'USER_SELECTED')
                                                        """,
                                                        params.addValue("targetId", id));
                                        return id;
                                });
                params.addValue("targetId", targetId);
                jdbc.update("UPDATE app_user SET display_name = :displayName, updated_at = now() WHERE id = :userId",
                                params);
                jdbc.update("""
                                UPDATE user_profile SET display_name = :displayName, school_name = :schoolName, school_grade = :schoolGrade,
                                       target_id = :targetId, target_score = :targetScore, updated_at = now()
                                WHERE user_id = :userId
                                """,
                                params);
        }
}
