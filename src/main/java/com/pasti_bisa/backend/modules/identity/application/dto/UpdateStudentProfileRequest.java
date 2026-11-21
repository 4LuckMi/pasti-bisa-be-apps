package com.pasti_bisa.backend.modules.identity.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.HashSet;
import java.util.Set;

/** PATCH distinguishes omitted fields from explicit null. */
public class UpdateStudentProfileRequest {
    private final Set<String> provided=new HashSet<>();
    @Size(min=2,max=120) private String displayName;
    @Size(max=180) private String schoolName;
    @Size(max=32) private String schoolGrade;
    @Size(max=24) private String targetCategory;
    @Size(max=180) private String targetInstitution;
    @Size(max=180) private String targetProgram;
    @Min(0) @Max(1000) private Integer targetScore;
    @JsonIgnore public boolean provided(String key){return provided.contains(key);}
    public String getDisplayName(){return displayName;}
    public String getSchoolName(){return schoolName;}
    public String getSchoolGrade(){return schoolGrade;}
    public String getTargetCategory(){return targetCategory;}
    public String getTargetInstitution(){return targetInstitution;}
    public String getTargetProgram(){return targetProgram;}
    public Integer getTargetScore(){return targetScore;}
    @JsonSetter public void setDisplayName(String value){provided.add("displayName");displayName=value;}
    @JsonSetter public void setSchoolName(String value){provided.add("schoolName");schoolName=value;}
    @JsonSetter public void setSchoolGrade(String value){provided.add("schoolGrade");schoolGrade=value;}
    @JsonSetter public void setTargetCategory(String value){provided.add("targetCategory");targetCategory=value;}
    @JsonSetter public void setTargetInstitution(String value){provided.add("targetInstitution");targetInstitution=value;}
    @JsonSetter public void setTargetProgram(String value){provided.add("targetProgram");targetProgram=value;}
    @JsonSetter public void setTargetScore(Integer value){provided.add("targetScore");targetScore=value;}
}
