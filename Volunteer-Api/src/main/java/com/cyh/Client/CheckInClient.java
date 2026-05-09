package com.cyh.Client;

import com.cyh.entity.CheckIn;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("CheckIn-Service")
public interface CheckInClient {
    @GetMapping("/api/check-ins/getList")
     List<CheckIn> getList(@RequestParam("activityId") Long activityId);
}
