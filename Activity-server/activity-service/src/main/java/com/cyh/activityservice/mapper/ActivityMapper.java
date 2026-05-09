package com.cyh.activityservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.Activity;
import com.cyh.entity.ActivityImg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Activity Mapper
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    
    /**
     * Find activities by status
     * @param status the activity status
     * @return list of activities
     */
    List<Activity> findByStatus(Integer status);
    
    /**
     * Find activities by time range
     * @return list of activities
     */
    List<Activity> findByTimeRange(Map<String, LocalDateTime> params);
    
    /**
     * Find activities by created by
     * @param createdBy the creator id
     * @return list of activities
     */
    List<Activity> findByCreatedBy(Long createdBy);
    int insertImag(Long activityId,List<String> imageUrl);
   List<ActivityImg>  getImg(List<Long> activityId);
    // 删除活动的所有图片
    int deleteImg(@Param("activityId") Long activityId);

    // 批量插入图片
    int insertImags(@Param("activityId") Long activityId, @Param("imageUrl") List<String> imageUrl);
}
