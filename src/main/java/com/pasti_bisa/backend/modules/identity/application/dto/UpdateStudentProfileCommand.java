package com.pasti_bisa.backend.modules.identity.application.dto;

/** Normalized values sent to the persistence port. */
public record UpdateStudentProfileCommand(
                String displayName,
                String schoolName,
                String schoolGrade,
                String targetKind,
                String targetInstitution,
                String targetProgram,
                Integer targetScore) {
}
