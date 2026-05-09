package com.cyh.Controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Aspect.OperationLogAnnotation;
import com.cyh.Dto.*;
import com.cyh.Res.ApiResponse;
import com.cyh.Service.AuthService;
import com.cyh.Utils.HttpClientUtil;
import com.cyh.Utils.UserContext;
import com.cyh.entity.User;
import com.cyh.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * Authentication Controller
 * Handles user registration, login, password change, and current user retrieval
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;
    
    /**
     * Register a new user
     * @param request the registration request
     * @return the registration response
     */
    @PostMapping("/register")
    @OperationLogAnnotation(type = "REGISTER", object = "USER")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register endpoint called with username: {}", request.getUsername());
        
        User user = authService.register(request);
        
        UserResponse userResponse = UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .totalPoints(user.getTotalPoints())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userResponse));
    }
    
    /**
     * User login
     * @param request the login request
     * @return the login response with JWT token
     */
    @PostMapping("/login")
    @OperationLogAnnotation(type = "LOGIN", object = "USER")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called with username: {}", request.getUsername());
        
        LoginResponse loginResponse = authService.login(request);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }
    
    /**
     * Change user password
     * Requires authentication
     * @param request the change password request
     * @return the response
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @OperationLogAnnotation(type = "CHANGE_PASSWORD", object = "USER")
    public ResponseEntity<ApiResponse<?>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change password endpoint called");
        
        authService.changePassword(request);
        
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
    @GetMapping("/getUserById")
    public User getUserById(@RequestParam Long userId) {
      return userMapper.selectById(userId);
    }
    @PutMapping("/updateUserById")
    public void UpdateUserById(@RequestBody User user) {
         userMapper.updateById(user);
    }
    @GetMapping("/getAdmins")
    public List<User> getAdmins(){
        return userMapper.selectList(new QueryWrapper<User>().eq("role", 1));
    }
    @GetMapping("/getUserByIdPage")
    public List<User> getUserByIdPage() {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("is_deleted", 0)
                .orderByDesc("total_points")
                .orderByAsc("id");
        List<User> users = userMapper.selectList(qw);
        System.out.println("===== 查询用户数量: " + users.size());
        return users;
    }
    @GetMapping("/getUserName")
    public User getUserName(@RequestParam Long userId) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("id", userId).select("username","Avatar","id"));
        return user;
    }
    @PostMapping("/addSign")
    public void addSign(@RequestParam String sign) {
        userMapper.update(new UpdateWrapper<User>().set("sign",sign).eq("id", UserContext.getUser()));
    }
    @PostMapping("/addsex")
    public void addSex(@RequestParam int sex) {
        userMapper.update(new UpdateWrapper<User>().set("sex",sex).eq("id", UserContext.getUser()));
    }
    @PostMapping("/addage")
    public void addAge(@RequestParam int age) {
        userMapper.update(new UpdateWrapper<User>().set("age",age).eq("id", UserContext.getUser()));
    }
    @PostMapping("/addcity")
    public void addCity(@RequestParam String city) {
        userMapper.update(new UpdateWrapper<User>().set("city",city).eq("id", UserContext.getUser()));
    }
    @GetMapping("getUserDetail")
    public User getUserDetail(@RequestParam Long userId) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("id", userId).select("id","Avatar","username","age","sex","level","city","sign"));
    }

    @GetMapping("/getLocation")
    public LocationDto getLocation(@RequestParam String ip) {
        System.out.println(ip);
        HashMap<String, String> map = new HashMap<>();
        map.put("key","c060cf3dc2a148e9e8eba034a12efc57");
        map.put("ip",ip);
        String s = HttpClientUtil.doGet("https://restapi.amap.com/v3/ip", map);
        JSONObject entries = JSONUtil.parseObj(s);
        LocationDto dto = entries.toBean(LocationDto.class);
        return dto;
    }
    /**
     * Get current authenticated user information
     * Requires authentication
     * @return the current user information
     */
    @GetMapping("/current-user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getCurrentUser() {
        log.info("Get current user endpoint called");
        
        User user = authService.getCurrentUser();
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "User not authenticated"));
        }
        
        UserResponse userResponse = UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .totalPoints(user.getTotalPoints())
                .avatar(user.getAvatar())
                .sign(user.getSign())
                .checkcount(user.getCheckcount())
                .sex(user.getSex())
                .age(user.getAge())
                .city(user.getCity())
                .level(user.getLevel())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", userResponse));
    }
}
