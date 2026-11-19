package com.pasti_bisa.backend.modules.identity.domain;

import java.util.UUID;
import java.util.List;

public record CustomerAccount(UUID id, String email, String displayName, String userType, List<String> roles) {
}
