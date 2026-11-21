package com.pasti_bisa.backend.shared.security;

import java.util.Optional;

/** Identity port for checking revocation without exposing password/OAuth material. */
public interface AccountSecurityState {
    record State(String status, long revision,java.util.List<String> roles) {}
    Optional<State> find(String email);
}
