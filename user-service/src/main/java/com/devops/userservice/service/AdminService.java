package com.devops.userservice.service;

import com.devops.userservice.dto.admin.RegisterAdminRequest;
import com.devops.userservice.dto.user.LoginRequest;
import com.devops.userservice.dto.user.LoginResponse;
import com.devops.userservice.model.Admin;
import com.devops.userservice.model.User;
import com.devops.userservice.repository.AdminRepository;
import com.devops.userservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public AdminService(AdminRepository adminRepository, JwtUtil jwtUtil, UserService userService) {
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    public Admin register(RegisterAdminRequest request) {
        log.info("Registering new admin with email: {}", request.getEmail());

        if (adminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        Admin admin = new Admin(request.getAdminName(), request.getEmail(), request.getPassword());
        Admin saved = adminRepository.save(admin);
        log.info("Admin registered — id: {}", saved.getId());
        return saved;
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Admin login attempt for email: {}", request.getEmail());

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Admin not found: " + request.getEmail()));

        if (!admin.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                admin.getId().toString(), "ADMIN", admin.getEmail(), admin.getAdminName());

        log.info("Admin login successful — adminId: {}", admin.getId());
        return new LoginResponse(token, jwtExpirationMs, admin.getId(), admin.getAdminName(), "ADMIN");
    }

    public List<User> getAllUsers() {
        return userService.findAll();
    }

    public User getUserById(Long userId) {
        return userService.findById(userId);
    }
}
