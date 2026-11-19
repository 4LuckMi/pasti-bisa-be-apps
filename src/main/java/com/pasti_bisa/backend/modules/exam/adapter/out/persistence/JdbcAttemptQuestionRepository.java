package com.pasti_bisa.backend.modules.exam.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.exam.application.AttemptQuestionRepository;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.*;

@Repository
public class JdbcAttemptQuestionRepository implements AttemptQuestionRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcAttemptQuestionRepository(NamedParameterJdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public QuestionRow findQuestion(QuestionQuery query) {
        return jdbc.queryForObject("""
                SELECT item.question_version_id, item.position, item.option_order, version.stem, version.explanation,
                       answer.selected_option_id, answer.flagged, answer.revision,
                       EXISTS (SELECT 1 FROM score_run WHERE attempt_id = attempt.id) AS finalized
                FROM attempt
                JOIN app_user account ON account.id = attempt.user_id
                JOIN attempt_item item ON item.attempt_id = attempt.id
                JOIN question_version version ON version.id = item.question_version_id
                LEFT JOIN answer ON answer.attempt_item_id = item.id
                WHERE attempt.id = :attemptId AND account.email_canonical = :email AND item.position = :position
                """, params(query), (rs, row) -> new QuestionRow(
                    rs.getObject("question_version_id", UUID.class), rs.getInt("position"), parseJson(rs.getString("stem")),
                    parseJson(rs.getString("option_order")), rs.getObject("selected_option_id", UUID.class),
                    rs.getBoolean("flagged"), rs.getInt("revision"), rs.getBoolean("finalized"), rs.getString("explanation")));
    }

    @Override
    public List<OptionRow> findOptions(UUID questionVersionId) {
        return jdbc.query("""
                SELECT id, option_code, content, correct_key FROM question_option
                WHERE question_version_id = :versionId ORDER BY original_position
                """, new MapSqlParameterSource("versionId", questionVersionId), (rs, row) -> new OptionRow(
                    rs.getObject("id", UUID.class), rs.getString("option_code"), parseJson(rs.getString("content")), rs.getBoolean("correct_key")));
    }

    @Override
    public Optional<AssetFile> findAsset(QuestionQuery query, UUID assetId) {
        return jdbc.query("""
                SELECT file.relative_path, file.mime_type FROM attempt
                JOIN app_user account ON account.id = attempt.user_id
                JOIN attempt_item item ON item.attempt_id = attempt.id AND item.position = :position
                JOIN question_asset asset ON asset.question_version_id = item.question_version_id AND asset.id = :assetId
                JOIN stored_file file ON file.id = asset.file_id
                WHERE attempt.id = :attemptId AND account.email_canonical = :email AND file.visibility = 'PRIVATE'
                  AND file.purpose = 'QUESTION_ASSET'
                """, params(query).addValue("assetId", assetId),
                (rs, row) -> new AssetFile(rs.getString("relative_path"), rs.getString("mime_type"))).stream().findFirst();
    }

    private MapSqlParameterSource params(QuestionQuery query) {
        return new MapSqlParameterSource("attemptId", query.attemptId())
                .addValue("position", query.position()).addValue("email", query.email());
    }

    private JsonNode parseJson(String value) {
        try { return json.readTree(value); }
        catch (JacksonException exception) { throw new IllegalStateException("Invalid stored question JSON", exception); }
    }
}
