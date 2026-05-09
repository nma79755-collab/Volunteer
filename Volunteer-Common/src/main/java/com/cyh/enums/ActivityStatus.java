package com.cyh.enums;

import lombok.Getter;

/**
 * Activity Status Enumeration
 * 
 * Represents the lifecycle states of a volunteer activity.
 * 
 * States:
 * - DRAFT: Activity is in draft state, not yet published
 * - PUBLISHED: Activity is published and accepting registrations
 * - IN_PROGRESS: Activity is currently ongoing
 * - COMPLETED: Activity has ended
 * - CANCELLED: Activity has been cancelled
 * 
 * @author Volunteer Service Platform
 * @version 1.0
 */
@Getter
public enum ActivityStatus {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "发布中"),
    IN_PROGRESS(2, "进行中"),
    COMPLETED(3, "已结束"),
    CANCELLED(4, "已取消");
    
    private final int code;
    private final String desc;
    
    /**
     * Constructor for ActivityStatus
     * 
     * @param code the numeric code for database storage
     * @param desc the description for display
     */
    ActivityStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * Get ActivityStatus enum by code
     * 
     * @param code the numeric code
     * @return the corresponding ActivityStatus enum, or null if not found
     */
    public static ActivityStatus getByCode(int code) {
        for (ActivityStatus status : ActivityStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Returns a string representation of the ActivityStatus
     * 
     * @return string in format "ActivityStatus{code=X, desc='description'}"
     */
    @Override
    public String toString() {
        return "ActivityStatus{" +
                "code=" + code +
                ", desc='" + desc + '\'' +
                '}';
    }
}
