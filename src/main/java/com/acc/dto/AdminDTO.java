package com.acc.dto;
public class AdminDTO {
    private String email;
    private String password;
    private String userName; 
    private Long id; 

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUserName() { return userName; } 
    public void setUserName(String userName) { this.userName = userName; } 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}