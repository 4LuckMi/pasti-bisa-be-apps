package com.pasti_bisa.backend.modules.system.application;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.pasti_bisa.backend.modules.system.domain.ServiceStatus;

@Service
public class GetServiceStatus {
	private final Clock clock;

	public GetServiceStatus(Clock clock) {
		this.clock = clock;
	}

	public ServiceStatus execute() {
		return new ServiceStatus("UP", "pastisiap-backend", clock.instant());
	}
}
