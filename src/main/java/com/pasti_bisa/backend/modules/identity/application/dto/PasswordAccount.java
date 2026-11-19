package com.pasti_bisa.backend.modules.identity.application.dto;

import java.util.List;
import java.util.UUID;

/** Internal authentication projection; never returned by an API controller. */
public record PasswordAccount(UUID id, String email, String passwordHash, List<String> roles) {
    @Override public String toString() { return "PasswordAccount[credentials=REDACTED]"; }
}
