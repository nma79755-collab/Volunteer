package com.cyh.pointsservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.pointsservice.entity.Points;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Points Mapper
 */
@Mapper
public interface PointsMapper extends BaseMapper<Points> {
    
    /**
     * Find points by user id
     * @param userId the user id
     * @return list of points
     */
    List<Points> findByUserId(Long userId);
    
    /**
     * Find points by activity id
     * @param activityId the activity id
     * @return list of points
     */
    List<Points> findByActivityId(Long activityId);
    
    /**
     * Find points by create time range
     * @param params map containing startTime and endTime
     * @return list of points
     */
    List<Points> findByCreateTime(Map<String, LocalDateTime> params);
}
