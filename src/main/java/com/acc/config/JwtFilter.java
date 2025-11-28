package com.acc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");
        String requestUri = request.getRequestURI();
        
        logger.debug("Processing request URI: {}", requestUri);
        logger.debug("Authorization Header: {}", authorizationHeader);

        String token = null;
        String userName = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            logger.debug("Extracted Token (first 20 chars): {}", (token.length() > 20 ? token.substring(0, 20) + "..." : token));
            
            try {
                
                userName = jwtUtil.extractUsername(token);
                logger.debug("Extracted Username/Email from Token: {}", userName);
            } catch (Exception e) {
               
                logger.warn("Failed to extract username/email from token (Malformed or Expired). Allowing anonymous access for now if permitted: {}", e.getMessage());
                userName = null;
            }
        } else {
           
            logger.debug("No Bearer token found in Authorization header. Proceeding as anonymous.");
        }

        
        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = null;
            try {
                userDetails = userDetailsService.loadUserByUsername(userName);
                
                if (userDetails != null && jwtUtil.validateToken(token, userDetails)) {
                    logger.debug("Token valid for user: {}", userName);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("SecurityContextHolder populated for user: {}", userName);
                } else {
                   
                    logger.debug("Token validation failed for user: {}", userName);
                }
            } catch (UsernameNotFoundException e) {
                logger.error("User {} not found via UserDetailsService: {}", userName, e.getMessage());
            } catch (Exception e) {
                logger.error("Exception during UserDetails loading or token validation for {}: {}", userName, e.getMessage(), e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
