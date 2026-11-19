package com.pasti_bisa.backend.modules.identity.application.dto;

import java.util.List;

public record OAuthAccountIdentity(String canonicalEmail, List<String> roleCodes) {}
