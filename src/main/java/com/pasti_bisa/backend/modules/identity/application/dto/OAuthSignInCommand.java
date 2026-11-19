package com.pasti_bisa.backend.modules.identity.application.dto;

public record OAuthSignInCommand(String provider, String subject, String email, String displayName) {}
