package com.pasti_bisa.backend.shared.web;

/** A controlled domain failure with a stable API code. */
public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    public BusinessException(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    public int status() { return status; }
    public String code() { return code; }
}
