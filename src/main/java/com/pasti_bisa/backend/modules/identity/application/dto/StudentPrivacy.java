package com.pasti_bisa.backend.modules.identity.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class StudentPrivacy {
    private StudentPrivacy(){}
    public record PrivacyRequest(@NotNull Boolean leaderboardOptIn){}
    public record PrivacyResponse(boolean leaderboardOptIn,Instant updatedAt){}
}
