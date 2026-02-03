package com.lontri.lighttherapy.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Jws<Claims> jws = tokenProvider.parse(token);
                Long userId = Long.valueOf(jws.getBody().getSubject());
                String role = String.valueOf(jws.getBody().get("role"));
                UserPrincipal principal = new UserPrincipal(
                        userId, "user-" + userId, "N/A",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)),
                        true
                );
                UsernamePasswordAuthenticationToken at =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(at);
            } catch (Exception e) {
                // Clear authentication if token is invalid
                SecurityContextHolder.clearContext();
                // Optionally log the error
                // logger.error("Invalid JWT token: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
