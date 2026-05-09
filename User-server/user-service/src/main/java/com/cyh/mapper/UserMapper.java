package com.cyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * User Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * Find user by username
     * @param username the username
     * @return the user
     */
    User findByUsername(String username);
    
    /**
     * Find user by email
     * @param email the email
     * @return the user
     */
    User findByEmail(String email);
    
    /**
     * Find user by phone
     * @param phone the phone
     * @return the user
     */
    User findByPhone(String phone);
    int insertAvatar(String avatar, Long userId);
}
