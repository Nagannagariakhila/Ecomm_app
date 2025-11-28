package com.acc.util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.acc.exception.ForbiddenException;
import com.acc.repository.CustomerRepository;

@Component
public class SecurityUtils {

    private final CustomerRepository customerRepository; 

    @Autowired
    public SecurityUtils(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Long getAuthenticatedCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenException("User is not authenticated or session is invalid (403).");
        }

        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            throw new ForbiddenException("Authentication principal is not a recognizable UserDetails object (403).");
        }
        
        return customerRepository.findByEmail(username)
                .map(customer -> customer.getId())
                .orElseThrow(() -> new ForbiddenException(
                    "Authenticated user '" + username + "' is not registered as a Customer in the database (403)."
                ));
    }

    public Long getAuthenticatedUserId() {
        return getAuthenticatedCustomerId();
    }
}