package com.pasti_bisa.backend.modules.exam.adapter.in.web;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pasti_bisa.backend.modules.exam.application.ExamService;
import com.pasti_bisa.backend.modules.exam.application.AttemptQuestionService;
import com.pasti_bisa.backend.modules.exam.application.dto.ExamDtos.*;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.QuestionQuery;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.QuestionResponse;
import com.pasti_bisa.backend.shared.web.BusinessException;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CUSTOMER')")
public class ExamController {
    private final ExamService exams;
    private final AttemptQuestionService questions;
    public ExamController(ExamService exams,AttemptQuestionService questions) { this.exams=exams;this.questions=questions; }
    @GetMapping("/tryouts/{id}") public TryoutDetail detail(@PathVariable UUID id) { return exams.detail(id); }
    @PostMapping("/attempts") public SessionResponse start(Authentication auth,@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody StartTryoutRequest request) { return exams.startTryout(auth.getName(),key,request); }
    @PostMapping("/practice/attempts") public SessionResponse practice(Authentication auth,@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody StartPracticeRequest request) { return exams.startPractice(auth.getName(),key,request); }
    @GetMapping("/users/current/attempts/active") public SessionResponse active(Authentication auth) { return exams.active(auth.getName()); }
    @GetMapping("/users/current/attempts/{id}") public SessionResponse get(Authentication auth,@PathVariable UUID id) { return exams.get(auth.getName(),id); }
    @PutMapping("/users/current/attempts/{id}/answers/{position}") public AnswerResponse answer(Authentication auth,@PathVariable UUID id,
            @PathVariable int position,@Valid @RequestBody AnswerRequest request) { return exams.answer(auth.getName(),id,position,request); }
    @PostMapping("/users/current/attempts/{id}/finalize") public SessionResponse finish(Authentication auth,@PathVariable UUID id,
            @RequestBody(required=false) FinalizeRequest request) { return exams.finalize(auth.getName(),id,request!=null&&Boolean.TRUE.equals(request.abandon())); }
    @GetMapping("/users/current/attempts/{id}/result") public ResultResponse result(Authentication auth,@PathVariable UUID id) { return exams.result(auth.getName(),id); }
    @GetMapping("/users/current/attempts/{id}/review") public List<QuestionResponse> review(Authentication auth,@PathVariable UUID id) {
        var result=exams.result(auth.getName(),id);
        if(!result.status().equals("COMPLETED")) throw new BusinessException(409,"RESULT_PROCESSING","Review is available after scoring completes");
        return exams.get(auth.getName(),id).questions().stream().map(q->questions.question(new QuestionQuery(id,q.position(),auth.getName()))).toList();
    }
}
