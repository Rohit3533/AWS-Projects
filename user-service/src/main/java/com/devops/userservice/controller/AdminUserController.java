package com.devops.userservice.controller;

import com.devops.userservice.dto.admin.RegisterAdminRequest;
import com.devops.userservice.dto.common.ApiRequest;
import com.devops.userservice.dto.common.ApiResponse;
import com.devops.userservice.dto.user.LoginRequest;
import com.devops.userservice.dto.user.LoginResponse;
import com.devops.userservice.model.User;
import com.devops.userservice.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody ApiRequest<RegisterAdminRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/admin/register", correlationId);

        var admin = adminService.register(request.getData());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(correlationId, Map.of(
                        "adminId", admin.getId(),
                        "adminName", admin.getAdminName(),
                        "email", admin.getEmail()
                )));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody ApiRequest<LoginRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/admin/login", correlationId);

        LoginResponse response = adminService.login(request.getData());
        return ResponseEntity.ok(ApiResponse.success(correlationId, response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        log.info("GET /api/admin/users");
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(null, users));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        log.info("GET /api/admin/users/{}", userId);
        User user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(null, user));
    }
}
