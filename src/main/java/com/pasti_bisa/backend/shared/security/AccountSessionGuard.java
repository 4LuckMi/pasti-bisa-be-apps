package com.pasti_bisa.backend.shared.security;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revocation is persisted in PostgreSQL; applies to password and Google sessions on every request. */
public final class AccountSessionGuard extends OncePerRequestFilter {
    public static final String REVISION=AccountSessionGuard.class.getName()+".revision";
    private final AccountSecurityState states;
    private final ApiSecurityErrorHandler errors;
    public AccountSessionGuard(AccountSecurityState states,ApiSecurityErrorHandler errors) { this.states=states;this.errors=errors; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getMethod().equals("POST") && java.util.List.of("/api/v1/auth/login","/api/v1/auth/register").contains(request.getRequestURI());
    }
    public static void bind(HttpServletRequest request,org.springframework.security.core.Authentication authentication,AccountSecurityState states) {
        var state=states.find(authentication.getName()).orElseThrow(()->new BadCredentialsException("Account unavailable"));
        if(!state.status().equals("ACTIVE")) throw new BadCredentialsException("Account unavailable");
        var roles=authentication.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).map(a->a.substring(5)).sorted().toList();
        if(!roles.equals(state.roles())) throw new BadCredentialsException("Account access changed during sign in; retry");
        request.getSession(true).setAttribute(REVISION,state.revision());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var session=request.getSession(false);
            var state=states.find(auth.getName());
            boolean valid=state.isPresent() && state.get().status().equals("ACTIVE") && session!=null
                && Long.valueOf(state.get().revision()).equals(session.getAttribute(REVISION));
            if(!valid) {
                if(session!=null) session.invalidate();
                SecurityContextHolder.clearContext();
                if(request.getRequestURI().equals("/api/v1/csrf")) { chain.doFilter(request,response);return; }
                errors.commence(request,response,new CredentialsExpiredException("Session revoked"));
                return;
            }
        }
        chain.doFilter(request,response);
    }
}
