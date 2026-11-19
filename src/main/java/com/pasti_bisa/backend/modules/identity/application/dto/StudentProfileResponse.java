package com.pasti_bisa.backend.modules.identity.application.dto;

import java.util.UUID;

public record StudentProfileResponse(
        UUID id,
        String email,
        String displayName,
        String userType,
        String schoolName,
        String schoolGrade,
        boolean leaderboardOptIn,
        Integer targetScore,
        String targetKind,
        String targetInstitution,
        String targetProgram) {}
