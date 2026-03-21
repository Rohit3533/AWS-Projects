package com.devops.userservice.dto.admin;

public class RegisterAdminRequest {
    private String adminName;
    private String email;
    private String password;

    public RegisterAdminRequest() {}

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
