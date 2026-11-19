package com.pasti_bisa.backend.modules.identity.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStudentProfileRequest(
                @NotBlank @Size(min = 2, max = 120) String displayName,
                @NotBlank @Size(max = 180) String schoolName,
                @NotBlank @Size(max = 32) String schoolGrade,
                @NotBlank @Size(max = 24) String targetCategory,
                @NotBlank @Size(max = 180) String targetInstitution,
                @Size(max = 180) String targetProgram,
                @Min(0) @Max(1000) Integer targetScore) {
}
