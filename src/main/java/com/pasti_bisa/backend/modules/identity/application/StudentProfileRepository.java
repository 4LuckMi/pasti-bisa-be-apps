package com.pasti_bisa.backend.modules.identity.application;

import com.pasti_bisa.backend.modules.identity.application.dto.StudentProfileResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.UpdateStudentProfileCommand;

public interface StudentProfileRepository {
    StudentProfileResponse findByEmail(String email);
    void updateByEmail(String email, UpdateStudentProfileCommand command);
}
