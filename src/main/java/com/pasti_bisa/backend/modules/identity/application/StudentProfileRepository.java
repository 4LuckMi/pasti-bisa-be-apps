package com.pasti_bisa.backend.modules.identity.application;

import com.pasti_bisa.backend.modules.identity.application.dto.StudentProfileResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.UpdateStudentProfileCommand;

public interface StudentProfileRepository {
    void lock(String email);
    com.pasti_bisa.backend.modules.identity.application.dto.StudentPrivacy.PrivacyResponse privacy(String email);
    void updatePrivacy(String email,boolean optedIn,String requestId);
    StudentProfileResponse findByEmail(String email);
    void updateByEmail(String email, UpdateStudentProfileCommand command);
}
