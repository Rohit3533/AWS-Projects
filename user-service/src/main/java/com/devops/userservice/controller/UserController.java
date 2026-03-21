package com.devops.userservice.controller;

import com.devops.userservice.dto.common.ApiRequest;
import com.devops.userservice.dto.common.ApiResponse;
import com.devops.userservice.dto.user.*;
import com.devops.userservice.model.User;
import com.devops.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody ApiRequest<RegisterUserRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/user/register", correlationId);

        User user = userService.register(request.getData());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(correlationId, Map.of(
                        "userId", user.getId(),
                        "userName", user.getUserName(),
                        "email", user.getEmail()
                )));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody ApiRequest<LoginRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/user/login", correlationId);

        LoginResponse response = userService.login(request.getData());
        return ResponseEntity.ok(ApiResponse.success(correlationId, response));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable Long userId) {
        log.info("GET /api/user/profile/{}", userId);
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(null, profile));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @PathVariable Long userId,
            @RequestBody ApiRequest<RegisterUserRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] PUT /api/user/profile/{}", correlationId, userId);

        UserProfileResponse profile = userService.updateProfile(userId, request.getData());
        return ResponseEntity.ok(ApiResponse.success(correlationId, profile));
    }

    @PostMapping("/profile/{userId}/address")
    public ResponseEntity<ApiResponse<AddressDTO>> addAddress(
            @PathVariable Long userId,
            @RequestBody ApiRequest<AddressDTO> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/user/profile/{}/address", correlationId, userId);

        AddressDTO address = userService.addAddress(userId, request.getData());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(correlationId, address));
    }

    @PutMapping("/profile/{userId}/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressDTO>> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @RequestBody ApiRequest<AddressDTO> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] PUT /api/user/profile/{}/address/{}", correlationId, userId, addressId);

        AddressDTO address = userService.updateAddress(userId, addressId, request.getData());
        return ResponseEntity.ok(ApiResponse.success(correlationId, address));
    }

    @DeleteMapping("/profile/{userId}/address/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        log.info("DELETE /api/user/profile/{}/address/{}", userId, addressId);
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @GetMapping("/profile/{userId}/address")
    public ResponseEntity<ApiResponse<?>> getAddresses(@PathVariable Long userId) {
        log.info("GET /api/user/profile/{}/address", userId);
        return ResponseEntity.ok(ApiResponse.success(null, userService.getAddresses(userId)));
    }
}
