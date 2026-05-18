package com.tallersoft.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT Authentication Filter
 * Extracts JWT from Authorization header, validates it, and sets SecurityContext
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractTokenFromRequest(request);

            log.debug("Processing request: {} - Token present: {}", request.getRequestURI(), jwt != null);
            
            if (jwt != null) {
                log.debug("Token found: {} (length: {})", jwt.substring(0, Math.min(20, jwt.length())) + "...", jwt.length());
                boolean isValid = jwtUtil.validateToken(jwt);
                log.debug("Token valid: {}", isValid);
                
                if (isValid) {
                    String email = jwtUtil.extractEmail(jwt);
                    String rol = jwtUtil.extractRole(jwt);
                    log.debug("Token email: {}, role: {}", email, rol);
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    log.debug("UserDetails authorities: {}", userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set Spring Security authentication for user: {} with authorities: {}", 
                            email, userDetails.getAuthorities());
                } else {
                    log.warn("Invalid JWT token");
                }
            } else {
                log.debug("No JWT token found in request");
            }
        } catch (Exception ex) {
            log.error("Cannot set user authentication: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     *
     * @param request HTTP request
     * @return Token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
