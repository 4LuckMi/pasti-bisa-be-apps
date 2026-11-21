package com.pasti_bisa.backend.shared.web;

/** A controlled domain failure with a stable API code. */
public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    private final java.util.Map<String,String> fieldErrors;
    public BusinessException(int status, String code, String message) { this(status,code,message,java.util.Map.of()); }
    public BusinessException(int status,String code,String message,java.util.Map<String,String> fieldErrors){super(message);this.status=status;this.code=code;this.fieldErrors=java.util.Map.copyOf(fieldErrors);}
    public int status() { return status; }
    public String code() { return code; }
    public java.util.Map<String,String> fieldErrors(){return fieldErrors;}
}
