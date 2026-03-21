package com.devops.userservice.service;

import com.devops.userservice.dto.user.*;
import com.devops.userservice.model.User;
import com.devops.userservice.model.UserAddress;
import com.devops.userservice.repository.UserAddressRepository;
import com.devops.userservice.repository.UserRepository;
import com.devops.userservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
//testing for flow
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public UserService(UserRepository userRepository, UserAddressRepository addressRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.jwtUtil = jwtUtil;
    }

    public User register(RegisterUserRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User(request.getUserName(), request.getEmail(),
                request.getPassword(), request.getPhoneNumber());
        User saved = userRepository.save(user);
        log.info("User registered — id: {}", saved.getId());
        return saved;
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                user.getId().toString(), "USER", user.getEmail(), user.getUserName());

        log.info("Login successful for userId: {}", user.getId());
        return new LoginResponse(token, jwtExpirationMs, user.getId(), user.getUserName(), "USER");
    }

    @Cacheable(value = "users", key = "#userId")
    public UserProfileResponse getProfile(Long userId) {
        log.info("Fetching profile for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserProfileResponse profile = new UserProfileResponse();
        profile.setUserId(user.getId());
        profile.setUserName(user.getUserName());
        profile.setEmail(user.getEmail());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setAddresses(user.getAddresses().stream()
                .map(this::toAddressDTO)
                .collect(Collectors.toList()));
        return profile;
    }

    @CacheEvict(value = "users", key = "#userId")
    public UserProfileResponse updateProfile(Long userId, RegisterUserRequest request) {
        log.info("Updating profile for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (request.getUserName() != null) user.setUserName(request.getUserName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        return getProfile(userId);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public AddressDTO addAddress(Long userId, AddressDTO dto) {
        log.info("Adding address for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setLabel(dto.getLabel());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);

        UserAddress saved = addressRepository.save(address);
        log.info("Address added — id: {}", saved.getId());
        return toAddressDTO(saved);
    }

    @CacheEvict(value = "users", key = "#userId")
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        log.info("Updating address {} for userId: {}", addressId, userId);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user: " + userId);
        }

        if (dto.getLabel() != null) address.setLabel(dto.getLabel());
        if (dto.getAddressLine1() != null) address.setAddressLine1(dto.getAddressLine1());
        if (dto.getAddressLine2() != null) address.setAddressLine2(dto.getAddressLine2());
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getState() != null) address.setState(dto.getState());
        if (dto.getPincode() != null) address.setPincode(dto.getPincode());
        if (dto.getIsDefault() != null) address.setIsDefault(dto.getIsDefault());

        UserAddress saved = addressRepository.save(address);
        return toAddressDTO(saved);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address {} for userId: {}", addressId, userId);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user: " + userId);
        }

        addressRepository.delete(address);
        log.info("Address deleted — id: {}", addressId);
    }

    public List<AddressDTO> getAddresses(Long userId) {
        log.info("Fetching addresses for userId: {}", userId);
        return addressRepository.findByUserId(userId).stream()
                .map(this::toAddressDTO)
                .collect(Collectors.toList());
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    private AddressDTO toAddressDTO(UserAddress address) {
        AddressDTO dto = new AddressDTO();
        dto.setAddressId(address.getId());
        dto.setLabel(address.getLabel());
        dto.setAddressLine1(address.getAddressLine1());
        dto.setAddressLine2(address.getAddressLine2());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPincode(address.getPincode());
        dto.setIsDefault(address.getIsDefault());
        return dto;
    }
}
