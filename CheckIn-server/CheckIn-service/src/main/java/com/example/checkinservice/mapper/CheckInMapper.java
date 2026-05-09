package com.example.checkinservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CheckIn Mapper
 */
@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {
    
    /**
     * Find check-ins by activity id
     * @param activityId the activity id
     * @return list of check-ins
     */
    List<CheckIn> findByActivityId(Long activityId);
    
    /**
     * Find check-ins by user id
     * @param userId the user id
     * @return list of check-ins
     */
    List<CheckIn> findByUserId(Long userId);
    
    /**
     * Find check-in by registration id
     * @param registrationId the registration id
     * @return the check-in
     */
    CheckIn findByRegistrationId(Long registrationId);
}
