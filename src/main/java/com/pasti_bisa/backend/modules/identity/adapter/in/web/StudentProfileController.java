package com.pasti_bisa.backend.modules.identity.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pasti_bisa.backend.modules.identity.application.StudentProfileService;
import com.pasti_bisa.backend.modules.identity.application.dto.StudentProfileResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.UpdateStudentProfileRequest;

@RestController
@RequestMapping("/api/v1/users/current/profile")
public class StudentProfileController {
    private final StudentProfileService profiles;

    public StudentProfileController(StudentProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public StudentProfileResponse get(Authentication auth) {
        return profiles.get(auth.getName());
    }

    @PatchMapping
    public StudentProfileResponse update(Authentication auth, @Valid @RequestBody UpdateStudentProfileRequest request) {
        return profiles.update(auth.getName(), request);
    }
}
