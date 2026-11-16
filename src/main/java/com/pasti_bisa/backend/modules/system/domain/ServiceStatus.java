package com.pasti_bisa.backend.modules.system.domain;

import java.time.Instant;

public record ServiceStatus(String status, String service, Instant timestamp) {
}
