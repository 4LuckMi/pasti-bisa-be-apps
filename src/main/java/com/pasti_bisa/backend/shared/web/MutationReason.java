package com.pasti_bisa.backend.shared.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MutationReason(@NotBlank @Size(max=500) String reason) {}
