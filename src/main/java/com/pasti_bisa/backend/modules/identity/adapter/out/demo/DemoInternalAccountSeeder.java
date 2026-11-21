package com.pasti_bisa.backend.modules.identity.adapter.out.demo;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit development bootstrap only. Never promotes an account found by email. */
@Component
@Profile("demo-accounts")
public class DemoInternalAccountSeeder {
    public static final String ADMIN_EMAIL="admin.demo@pastisiap.test";
    public static final String STAFF_EMAIL="staff.demo@pastisiap.test";
    private final NamedParameterJdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public DemoInternalAccountSeeder(NamedParameterJdbcTemplate jdbc,PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
    public static UUID id(String role){return UUID.nameUUIDFromBytes(("pastisiap-demo-internal:"+role).getBytes(StandardCharsets.UTF_8));}
    public record SeededAccount(UUID id,String email,String userType,String role,boolean created){}
    private record Existing(UUID id,String type,String status,String passwordHash){
        @Override public String toString(){return "Existing[credentials=REDACTED]";}
    }
    @Transactional
    public List<SeededAccount> seed(String adminPassword,String staffPassword){
        validate(adminPassword);validate(staffPassword);
        if(adminPassword.equals(staffPassword))throw new IllegalArgumentException("Use different passwords for the two demo accounts");
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(149242)",new MapSqlParameterSource(),Object.class);
        return List.of(seedOne(ADMIN_EMAIL,"Demo Platform Admin","PLATFORM_ADMIN",adminPassword),
            seedOne(STAFF_EMAIL,"Demo Petugas Bank Soal","QUESTION_STAFF",staffPassword));
    }
    private static void validate(String password){
        if(password==null||password.length()<12||password.getBytes(StandardCharsets.UTF_8).length>72||password.isBlank())
            throw new IllegalArgumentException("Configure demo passwords with 12 to 72 UTF-8 bytes before running the development seed");
    }
    private SeededAccount seedOne(String email,String name,String role,String password){
        UUID expectedId=id(role);var p=new MapSqlParameterSource("id",expectedId).addValue("email",email).addValue("role",role);
        var existing=jdbc.query("SELECT id,user_type,status,password_hash FROM app_user WHERE email_canonical=:email OR id=:id FOR UPDATE",p,
            (r,n)->new Existing(r.getObject("id",UUID.class),r.getString("user_type"),r.getString("status"),r.getString("password_hash")));
        if(!existing.isEmpty()){
            var row=existing.getFirst();
            String storedEmail=jdbc.queryForObject("SELECT email_canonical FROM app_user WHERE id=:id",new MapSqlParameterSource("id",row.id()),String.class);
            var roles=jdbc.query("SELECT role_code FROM user_role WHERE user_id=:id ORDER BY role_code",new MapSqlParameterSource("id",row.id()),(r,n)->r.getString(1));
            if(existing.size()!=1||!row.id().equals(expectedId)||!email.equals(storedEmail)||!"INTERNAL".equals(row.type()))
                throw new IllegalStateException("Reserved demo identity conflicts with an existing account; no account was promoted");
            if(!"ACTIVE".equals(row.status())||!roles.equals(List.of(role)))
                throw new IllegalStateException("Existing demo account permissions or status were changed; seed will not overwrite them");
            if(row.passwordHash()==null||!encoder.matches(password,row.passwordHash()))
                throw new IllegalStateException("Existing demo account uses different credentials; supply the original local credential file");
            return new SeededAccount(expectedId,email,"INTERNAL",role,false);
        }
        jdbc.update("""
            INSERT INTO app_user(id,email,email_canonical,password_hash,display_name,user_type,status)
            VALUES(:id,:email,:email,:hash,:name,'INTERNAL','ACTIVE')
            """,p.addValue("hash",encoder.encode(password)).addValue("name",name));
        jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(:id,:role)",p);
        jdbc.update("""
            INSERT INTO audit_event(id,actor_id,action,entity_type,entity_id,reason,after_redacted)
            VALUES(gen_random_uuid(),:id,'DEMO_INTERNAL_ACCOUNT_CREATED','USER',:id,'LOCAL_DEMO_BOOTSTRAP',
                   jsonb_build_object('userType','INTERNAL','role',:role::text,'demo',true))
            """,p);
        return new SeededAccount(expectedId,email,"INTERNAL",role,true);
    }
}
