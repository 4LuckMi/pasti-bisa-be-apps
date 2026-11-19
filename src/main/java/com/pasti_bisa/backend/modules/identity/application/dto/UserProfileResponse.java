package com.pasti_bisa.backend.modules.identity.application.dto;

import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.modules.identity.domain.CustomerAccount;

public record UserProfileResponse(UUID id, String email, String displayName, String userType, List<String> roles) {
    public static UserProfileResponse from(CustomerAccount account) {
        return new UserProfileResponse(
                account.id(),
                account.email(),
                account.displayName(),
                account.userType(),
                account.roles());
    }
}
