package com.pasti_bisa.backend.modules.exam.application;

import com.pasti_bisa.backend.modules.exam.application.dto.StudentExperienceDtos.Processing;

/** Public progress deliberately excludes worker exception details and lease information. */
public final class ScoringProgress {
    private ScoringProgress(){}
    public static Processing from(String status,String jobState){
        if("COMPLETED".equals(status))return new Processing("READY",false,0,null);
        if(!"FINALIZING".equals(status))return new Processing("NOT_STARTED",false,0,null);
        if(jobState==null||"FAILED".equals(jobState)||"SUCCEEDED".equals(jobState))
            return new Processing("FAILED",false,0,"SCORING_UNAVAILABLE");
        return new Processing(jobState,true,"RETRY_WAIT".equals(jobState)?10:3,null);
    }
}
