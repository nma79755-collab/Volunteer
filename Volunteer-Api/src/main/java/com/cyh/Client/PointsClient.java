package com.cyh.Client;

import com.cyh.entity.CheckIn;
import com.cyh.entity.Points;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("Points-Service")
public interface PointsClient {
    @GetMapping("/api/points/allocatePoints")
    Points allocatePoints(@RequestBody CheckIn checkIn);
}
