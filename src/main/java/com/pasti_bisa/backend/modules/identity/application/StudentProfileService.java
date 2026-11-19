package com.pasti_bisa.backend.modules.identity.application;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.identity.application.dto.StudentProfileResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.UpdateStudentProfileCommand;
import com.pasti_bisa.backend.modules.identity.application.dto.UpdateStudentProfileRequest;

@Service
public class StudentProfileService {
    private final StudentProfileRepository repository;
    public StudentProfileService(StudentProfileRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public StudentProfileResponse get(String email) { return repository.findByEmail(email); }

    @Transactional
    public StudentProfileResponse update(String email, UpdateStudentProfileRequest request) {
        String kind = switch (request.targetCategory().trim().toUpperCase(Locale.ROOT)) {
            case "UTBK" -> "CAMPUS";
            case "CPNS" -> "CIVIL_SERVICE";
            case "KEDINASAN" -> "ACADEMY";
            case "SEKOLAH" -> "SCHOOL";
            default -> throw new IllegalArgumentException("Unsupported target category");
        };
        var command = new UpdateStudentProfileCommand(
                request.displayName().trim(), request.schoolName().trim(), request.schoolGrade().trim(), kind,
                request.targetInstitution().trim(), request.targetProgram(), request.targetScore());
        repository.updateByEmail(email, command);
        return repository.findByEmail(email);
    }
}
