package com.cyh.Client;
import com.cyh.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("User-Service")
public interface UserClient {
    @GetMapping("/api/auth/getUserById")
     User getUserById(@RequestParam Long userId);
    @PutMapping("/api/auth/updateUserById")
    void UpdateUserById(@RequestBody User user);
    @GetMapping("/api/auth/getUserByIdPage")
     List<User> getUserByIdPage();
    @GetMapping("/api/auth/getAdmins")
     List<User> getAdmins();
    @GetMapping("/api/auth/getUserName")
     User getUserName(@RequestParam Long userId);
}
