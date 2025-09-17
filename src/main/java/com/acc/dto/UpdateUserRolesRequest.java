package com.acc.dto;

import java.util.Set;

public class UpdateUserRolesRequest {
    private String identifier; 
    private Set<String> roles; 

    public UpdateUserRolesRequest() {
    }

    public UpdateUserRolesRequest(String identifier, Set<String> roles) {
        this.identifier = identifier;
        this.roles = roles;
    }

    
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "UpdateUserRolesRequest{" +
               "identifier='" + identifier + '\'' +
               ", roles=" + roles +
               '}';
    }
}
