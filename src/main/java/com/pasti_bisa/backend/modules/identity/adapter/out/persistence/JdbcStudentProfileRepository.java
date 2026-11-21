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
        public void lock(String email){jdbc.queryForObject("SELECT id FROM app_user WHERE email_canonical=:email AND status='ACTIVE' FOR UPDATE",new MapSqlParameterSource("email",email),UUID.class);}

        @Override public com.pasti_bisa.backend.modules.identity.application.dto.StudentPrivacy.PrivacyResponse privacy(String email){
                return jdbc.queryForObject("""
                        SELECT p.leaderboard_opt_in,(p.consent->>'leaderboardUpdatedAt')::timestamptz AS consent_updated
                        FROM user_profile p JOIN app_user u ON u.id=p.user_id WHERE u.email_canonical=:email
                        """,new MapSqlParameterSource("email",email),(r,n)->{
                            var updated=r.getObject("consent_updated",java.time.OffsetDateTime.class);
                            return new com.pasti_bisa.backend.modules.identity.application.dto.StudentPrivacy.PrivacyResponse(r.getBoolean("leaderboard_opt_in"),updated==null?null:updated.toInstant());
                        });
        }
        @Override public void updatePrivacy(String email,boolean optedIn,String requestId){
                var p=new MapSqlParameterSource("email",email).addValue("opted",optedIn).addValue("request",requestId);
                jdbc.update("""
                        INSERT INTO audit_event(id,actor_id,action,entity_type,entity_id,request_id,before_redacted,after_redacted)
                        SELECT gen_random_uuid(),u.id,'STUDENT_PRIVACY_UPDATED','USER_PROFILE',u.id,:request,
                        jsonb_build_object('leaderboardOptIn',p.leaderboard_opt_in),jsonb_build_object('leaderboardOptIn',:opted::boolean)
                        FROM app_user u JOIN user_profile p ON p.user_id=u.id WHERE u.email_canonical=:email
                        """,p);
                jdbc.update("""
                        UPDATE user_profile SET leaderboard_opt_in=:opted,
                        consent=consent||jsonb_build_object('leaderboardOptIn',:opted::boolean,'leaderboardUpdatedAt',now()),updated_at=now()
                        WHERE user_id=(SELECT id FROM app_user WHERE email_canonical=:email)
                        """,p);
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
                UUID targetId = command.targetKind()==null?null:jdbc.query("""
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
