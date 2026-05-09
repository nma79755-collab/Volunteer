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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Value("${github.deploy.token}")
    private String githubToken;

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

        @PostMapping("/trigger-deploy")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<?> triggerDeploy() {

            // 1. 设置 GitHub API 地址：你的仓库 + /dispatches
            String url = "https://api.github.com/repos/nma79755-collab/Volunteer/dispatches";

            // 2. 设置请求体：event_type 必须和 deploy.yml 里 repository_dispatch 的 types 一致
            Map<String, Object> body = Map.of(
                    "event_type", "deploy-from-admin" // 这个自定义事件和工作流配置里要一致
            );

            // 3. 设置请求头：带 Bearer Token 认证
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + githubToken); // GitHub 的鉴权方式
            headers.set("Accept", "application/vnd.github+json");

            // 4. 发送请求
            // RestTemplate 是 Spring 自带的轻量级 HTTP 客户端
            new RestTemplate().postForEntity(url, new HttpEntity<>(body, headers), String.class);

            // 5. 返回结果给管理员
            return ApiResponse.success("部署已触发，请稍后查看 GitHub Actions 进度");
        }
    @GetMapping("/deploy-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> getDeployStatus() {
        String url = "https://api.github.com/repos/nma79755-collab/Volunteer/actions/runs?per_page=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Accept", "application/vnd.github+json");

        ResponseEntity<Map> response = new RestTemplate().exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
        );

        Map<String, Object> body = response.getBody();
        // 从返回的 JSON 里提取 status：queued / in_progress / completed
        return ApiResponse.success(body);
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
