package com.cyh.Client;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cyh.Res.ApiResponse;
import com.cyh.entity.Activity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("Activity-Service")
public interface ActivityClient {
    @GetMapping("/api/activities/{id}")
     ApiResponse<Activity> getActivityDetail(@PathVariable Long id);
    @GetMapping("/api/activities/getList")
     List<Activity> getActivitylist(@RequestParam("status") Integer status,@RequestParam("deleted") Integer delete);
    @PutMapping("/api/activities/updateById")
    void updateActivityById(@RequestBody Activity activity);
    @PutMapping("/api/activities/updateACById")
     void updateAcById(@RequestParam("id")Long id,@RequestParam("registered_count")int registered_count);
    @GetMapping("/api/activities/getListByCreat")
     List<Activity> getActivitylistByCreat(@RequestParam("createdBy") Long createdBy,@RequestParam("delete") Integer delete);
}
