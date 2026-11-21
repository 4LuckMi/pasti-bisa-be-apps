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

    @Transactional(readOnly=true)
    public com.pasti_bisa.backend.modules.identity.application.dto.StudentPrivacy.PrivacyResponse privacy(String email){return repository.privacy(email);}

    @Transactional
    public com.pasti_bisa.backend.modules.identity.application.dto.StudentPrivacy.PrivacyResponse privacy(String email,boolean optedIn,String requestId){
        repository.lock(email);repository.updatePrivacy(email,optedIn,requestId);return repository.privacy(email);
    }

    @Transactional
    public StudentProfileResponse update(String email, UpdateStudentProfileRequest request) {
        repository.lock(email);
        var current=repository.findByEmail(email);
        String name=request.provided("displayName")?clean(request.getDisplayName()):current.displayName();
        if(name==null||name.length()<2)invalid("displayName","At least two non-blank characters are required");
        String category=clean(request.getTargetCategory());
        String kind = !request.provided("targetCategory")?current.targetKind():category==null?null:switch (category.toUpperCase(Locale.ROOT)) {
            case "UTBK" -> "CAMPUS";
            case "CPNS" -> "CIVIL_SERVICE";
            case "KEDINASAN" -> "ACADEMY";
            case "SEKOLAH" -> "SCHOOL";
            default -> {invalid("targetCategory","Use UTBK, CPNS, KEDINASAN or SEKOLAH");yield null;}
        };
        String institution=request.provided("targetInstitution")?clean(request.getTargetInstitution()):current.targetInstitution();
        String program=request.provided("targetProgram")?clean(request.getTargetProgram()):current.targetProgram();
        if(request.provided("targetCategory")&&!java.util.Objects.equals(kind,current.targetKind())){
            if(!request.provided("targetInstitution"))institution=null;
            if(!request.provided("targetProgram"))program=null;
        }
        if(kind!=null&&institution==null)invalid("targetInstitution","Institution is required when selecting a target");
        if(kind==null&&(institution!=null||program!=null)){
            if(request.provided("targetCategory")&&category==null){institution=null;program=null;}
            else invalid("targetCategory","Select a category before setting a target");
        }
        String school=request.provided("schoolName")?clean(request.getSchoolName()):current.schoolName();
        String grade=request.provided("schoolGrade")?clean(request.getSchoolGrade()):current.schoolGrade();
        if("CAMPUS".equals(kind)||"SCHOOL".equals(kind)){
            if(school==null)invalid("schoolName","School is required for this target category");
            if(grade==null)invalid("schoolGrade","Grade is required for this target category");
        }
        var command = new UpdateStudentProfileCommand(
                name,school,grade,kind,institution,program,
                request.provided("targetScore")?request.getTargetScore():current.targetScore());
        repository.updateByEmail(email, command);
        return repository.findByEmail(email);
    }
    private static String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private static void invalid(String field,String message){throw new com.pasti_bisa.backend.shared.web.BusinessException(400,"VALIDATION_FAILED","Request validation failed",java.util.Map.of(field,message));}
}
