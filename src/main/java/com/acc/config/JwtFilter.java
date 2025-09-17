package com.acc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // Import UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Import UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService; 

    
    private static final List<String> EXCLUDE_URL_PREFIXES = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/otp/generate",
            "/api/auth/otp/verify",
            "/api/customers/email/",
            "/api/categories",
            "/api/products/" 
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        System.out.println("DEBUG JWT Filter: Incoming Request URI: " + requestUri);

       
        if (EXCLUDE_URL_PREFIXES.stream().anyMatch(uriPrefix -> requestUri.startsWith(uriPrefix))) {
            System.out.println("DEBUG JWT Filter: Skipping JWT validation for public URI: " + requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        System.out.println("DEBUG JWT Filter: Authorization Header: " + authorizationHeader);

        String token = null;
        String userName = null; 

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            System.out.println("DEBUG JWT Filter: Extracted Token (first 20 chars): " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
            try {
                userName = jwtUtil.extractUsername(token); 
                System.out.println("DEBUG JWT Filter: Extracted Username/Email from Token: " + userName);
            } catch (Exception e) {
                System.err.println("ERROR JWT Filter: Failed to extract username/email from token: " + e.getMessage());
               
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            System.out.println("DEBUG JWT Filter: No Bearer token found in Authorization header for protected URI: " + requestUri);
           
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        
        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = null;
            try {
               
                userDetails = userDetailsService.loadUserByUsername(userName);
                System.out.println("DEBUG JWT Filter: UserDetails loaded for " + userName + ": " + (userDetails != null ? userDetails.getUsername() : "null"));
                if (userDetails != null) {
                    System.out.println("DEBUG JWT Filter: UserDetails Authorities: " + userDetails.getAuthorities());
                }
            } catch (UsernameNotFoundException e) {
                System.err.println("ERROR JWT Filter: User " + userName + " not found via UserDetailsService: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (Exception e) {
                System.err.println("ERROR JWT Filter: Exception loading UserDetails for " + userName + ": " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            if (userDetails != null && jwtUtil.validateToken(token, userDetails)) {
                System.out.println("DEBUG JWT Filter: Token valid for user: " + userName);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("DEBUG JWT Filter: SecurityContextHolder populated for user: " + userName);
            } else {
                System.out.println("DEBUG JWT Filter: Token validation failed or userDetails is null/invalid for " + userName);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            if (userName == null) {
                System.out.println("DEBUG JWT Filter: Username extracted from token was null.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println("DEBUG JWT Filter: Authentication already exists in context for: " + SecurityContextHolder.getContext().getAuthentication().getName());
               
            }
        }
        filterChain.doFilter(request, response);
    }
}
