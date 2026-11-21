package com.pasti_bisa.backend.modules.operations.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.operations.application.JobQueue;

@Repository
public class JdbcJobQueue implements JobQueue {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcJobQueue(NamedParameterJdbcTemplate jdbc,JsonMapper json) { this.jdbc=jdbc;this.json=json; }
    @Override public Optional<Job> claim() {
        jdbc.update("""
                UPDATE job SET status='FAILED',last_error='LEASE_EXHAUSTED',lease_until=NULL,lease_token=NULL
                WHERE status='PROCESSING' AND lease_until<now() AND retry_count>=max_attempts
                """,new MapSqlParameterSource());
        UUID token=UUID.randomUUID();
        return jdbc.query("""
                WITH next AS (
                    SELECT id FROM job WHERE retry_count<max_attempts
                    AND ((status IN ('QUEUED','RETRY_WAIT') AND run_after<=now()) OR (status='PROCESSING' AND lease_until<now()))
                    ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
                ) UPDATE job SET status='PROCESSING',lease_token=:token,lease_owner=:owner,
                    lease_until=now()+interval '15 minutes',retry_count=retry_count+1
                  FROM next WHERE job.id=next.id RETURNING job.id,job.type,job.payload
                """,new MapSqlParameterSource("token",token).addValue("owner",token.toString()),
                (rs,row)->new Job(rs.getObject("id",UUID.class),rs.getString("type"),json.readTree(rs.getString("payload")),token)).stream().findFirst();
    }
    private MapSqlParameterSource params(Job job) { return new MapSqlParameterSource("id",job.id()).addValue("token",job.token()); }
    @Override public void succeeded(Job job) {
        jdbc.update("UPDATE job SET status='SUCCEEDED',lease_until=NULL,lease_token=NULL,last_error=NULL WHERE id=:id AND lease_token=:token",params(job));
    }
    @Override public void failed(Job job,Exception exception) {
        jdbc.update("""
                UPDATE job SET status=CASE WHEN :terminal OR retry_count>=max_attempts THEN 'FAILED' ELSE 'RETRY_WAIT' END,
                  run_after=now()+interval '10 seconds',lease_until=NULL,lease_token=NULL,last_error=:error
                WHERE id=:id AND lease_token=:token
                """,params(job).addValue("error",exception.getClass().getSimpleName())
                    .addValue("terminal",job.type().equals("IMPORT_XLSX")&&exception instanceof com.pasti_bisa.backend.shared.web.BusinessException b&&b.status()<500));
    }
    @Override public void requireLease(Job job) {
        var owned=jdbc.query("SELECT id FROM job WHERE id=:id AND lease_token=:token AND status='PROCESSING' AND lease_until>now() FOR UPDATE",
                params(job),(rs,row)->rs.getObject(1,UUID.class));
        if(owned.isEmpty()) throw new IllegalStateException("Job lease no longer belongs to this worker");
    }
}
