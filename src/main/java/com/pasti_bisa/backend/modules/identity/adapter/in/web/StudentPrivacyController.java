package com.pasti_bisa.backend.modules.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pasti_bisa.backend.modules.identity.application.StudentProfileService;
import com.pasti_bisa.backend.modules.identity.application.dto.StudentPrivacy.*;
import com.pasti_bisa.backend.shared.web.ApiRequestLoggingFilter;

@RestController
@RequestMapping("/api/v1/users/current/privacy")
@PreAuthorize("hasRole('CUSTOMER')")
public class StudentPrivacyController {
    private final StudentProfileService profiles;
    public StudentPrivacyController(StudentProfileService profiles){this.profiles=profiles;}
    @GetMapping public PrivacyResponse get(Authentication a){return profiles.privacy(a.getName());}
    @PatchMapping public PrivacyResponse update(Authentication a,@Valid @RequestBody PrivacyRequest body,HttpServletRequest request){
        return profiles.privacy(a.getName(),body.leaderboardOptIn(),ApiRequestLoggingFilter.requestId(request));
    }
}
