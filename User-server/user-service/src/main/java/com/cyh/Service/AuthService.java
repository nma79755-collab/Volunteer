package com.cyh.Service;

import com.alibaba.fastjson.JSONObject;
import com.cyh.Dto.ChangePasswordRequest;
import com.cyh.Dto.LoginRequest;
import com.cyh.Dto.LoginResponse;
import com.cyh.Dto.RegisterRequest;
import com.cyh.Utils.JwtTokenProvider;
import com.cyh.Utils.PasswordEncodering;
import com.cyh.entity.User;
import com.cyh.exception.BusinessException;
import com.cyh.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Authentication Service
 * Handles user registration, login, password change, and current user retrieval
 */
@Slf4j
@Service
public class AuthService {
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncodering passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Register a new user
     * @param request the registration request
     * @return the registered user
     * @throws BusinessException if username or email already exists
     */
    @Transactional
    public User register(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());
        
        // Validate username uniqueness
        User existingUser = userMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new BusinessException("Username already exists");
        }
        
        // Validate email uniqueness
        User existingEmail = userMapper.findByEmail(request.getEmail());
        if (existingEmail != null) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new BusinessException("Email already exists");
        }
        
        // Validate phone uniqueness
        User existingPhone = userMapper.findByPhone(request.getPhone());
        if (existingPhone != null) {
            log.warn("Phone already exists: {}", request.getPhone());
            throw new BusinessException("Phone already exists");
        }
        
        // Validate password strength
        validatePasswordStrength(request.getPassword());
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encodePassword(request.getPassword()))
                .realName(request.getRealName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(0) // Default role: volunteer
                .status(0) // Default status: active
                .totalPoints(0L)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(0)
                .build();
        userMapper.insert(user);
        log.info("User registered successfully with username: {}", request.getUsername());
        
        return user;
    }
    
    /**
     * Authenticate user and generate JWT token
     * @param request the login request
     * @return the login response with JWT token
     * @throws BusinessException if credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt with username: {}", request.getUsername());
        
        // Find user by username
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            log.warn("User not found: {}", request.getUsername());
            throw new BusinessException("Username or password is incorrect");
        }
        
        // Check if user is active
        if (user.getStatus() != 0) {
            log.warn("User account is disabled: {}", request.getUsername());
            throw new BusinessException("User account is disabled");
        }
        
        // Verify password
        if (!passwordEncoder.matchPassword(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getUsername());
            throw new BusinessException("Username or password is incorrect");
        }
        
        // Generate JWT token
        String roleName = user.getRole() == 1 ? "ADMIN" : "VOLUNTEER";
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roleName);
        long expiresIn = 3600; // 1 hour in seconds
        
        log.info("User logged in successfully: {}", request.getUsername());
        
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .totalPoints(user.getTotalPoints())
                .expiresIn(expiresIn)
                .build();
    }
    
    /**
     * Change user password
     * @param request the change password request
     * @throws BusinessException if old password is incorrect
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        log.info("User attempting to change password");
        
        // Get current user
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("Current user not found");
            throw new BusinessException("User not authenticated");
        }
        
        // Verify old password
        if (!passwordEncoder.matchPassword(request.getOldPassword(), currentUser.getPassword())) {
            log.warn("Old password is incorrect for user: {}", currentUser.getUsername());
            throw new BusinessException("Old password is incorrect");
        }
        
        // Validate new password
        validatePasswordStrength(request.getNewPassword());
        
        // Verify new password matches confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("New password and confirm password do not match");
            throw new BusinessException("New password and confirm password do not match");
        }
        
        // Update password
        currentUser.setPassword(passwordEncoder.encodePassword(request.getNewPassword()));
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);
        
        log.info("Password changed successfully for user: {}", currentUser.getUsername());
    }
    
    /**
     * Get current authenticated user
     * @return the current user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String username = authentication.getName();
        return userMapper.findByUsername(username);
    }
    
    /**
     * Validate password strength
     * Password must be at least 6 characters long
     * @param password the password to validate
     * @throws BusinessException if password is too weak
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters long");
        }
    }
}
