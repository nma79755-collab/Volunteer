package com.example.registrationservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.Registration;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * Registration Mapper
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
    
    /**
     * Find registration by user and activity
     * @param params map containing userId and activityId
     * @return the registration
     */
    Registration findByUserAndActivity(Map<String, Long> params);
    
    /**
     * Find registrations by status
     * @param status the registration status
     * @return list of registrations
     */
    List<Registration> findByStatus(Integer status);
    
    /**
     * Find registrations by activity id
     * @param activityId the activity id
     * @return list of registrations
     */
    List<Registration> findByActivityId(Long activityId);
}
