package com.pasti_bisa.backend.modules.system.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pasti_bisa.backend.modules.system.application.GetServiceStatus;
import com.pasti_bisa.backend.modules.system.domain.ServiceStatus;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
	private final GetServiceStatus getServiceStatus;

	public HealthController(GetServiceStatus getServiceStatus) {
		this.getServiceStatus = getServiceStatus;
	}

	@GetMapping
	public ServiceStatus get() {
		return getServiceStatus.execute();
	}
}
