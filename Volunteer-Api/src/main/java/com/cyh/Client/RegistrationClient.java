package com.cyh.Client;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyh.Res.ApiResponse;
import com.cyh.entity.Registration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("Registration-Service")
public interface RegistrationClient {
    @GetMapping("/api/registrations/getOne")
     Registration getRegistrationOne(@RequestParam("activityId") Long activityId,@RequestParam("userId") Long userId,@RequestParam("status") Integer status);
    @GetMapping("/api/registrations/getCount")
    Long getCount(@RequestParam("activityId") Long activityId,@RequestParam("status") Integer status,@RequestParam("deleted") Integer deleted);
    @GetMapping("/api/registrations/getList")
     List<Registration> getList(@RequestParam("activityId") Long activityId,@RequestParam("status") Integer status,@RequestParam("deleted") Integer deleted);
    @GetMapping("/api/registrations/{id}")
     ResponseEntity<ApiResponse<Registration>> getRegistrationDetail(@PathVariable Long id);
    @PutMapping("/api/registrations/update")
    Registration updateRegistration(@RequestBody Registration registration);
    @GetMapping("/api/registrations/getByQw")
     List<Registration> getRegistration(@RequestParam("activityIds")List < Long> myActivityIds);
}
