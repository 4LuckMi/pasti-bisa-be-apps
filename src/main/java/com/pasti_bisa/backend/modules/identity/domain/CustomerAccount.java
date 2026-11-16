package com.pasti_bisa.backend.modules.identity.domain;

import java.util.UUID;

public record CustomerAccount(UUID id, String email, String displayName) {
}
