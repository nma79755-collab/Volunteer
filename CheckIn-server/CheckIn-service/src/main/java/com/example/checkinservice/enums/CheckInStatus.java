package com.example.checkinservice.enums;

import lombok.Getter;

/**
 * Check-in Status Enumeration
 * 
 * Represents the check-in states of a volunteer for an activity.
 * 
 * States:
 * - NOT_CHECKED_IN: Volunteer has not checked in
 * - CHECKED_IN: Volunteer checked in on time
 * - LATE: Volunteer checked in late
 * - ABSENT: Volunteer did not check in (marked after activity ends)
 * 
 * @author Volunteer Service Platform
 * @version 1.0
 */
@Getter
public enum CheckInStatus {
    NOT_CHECKED_IN(0, "未签到"),
    CHECKED_IN(1, "已签到"),
    LATE(2, "迟到"),
    ABSENT(3, "缺席");
    
    private final int code;
    private final String desc;
    
    /**
     * Constructor for CheckInStatus
     * 
     * @param code the numeric code for database storage
     * @param desc the description for display
     */
    CheckInStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * Get CheckInStatus enum by code
     * 
     * @param code the numeric code
     * @return the corresponding CheckInStatus enum, or null if not found
     */
    public static CheckInStatus getByCode(int code) {
        for (CheckInStatus status : CheckInStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Returns a string representation of the CheckInStatus
     * 
     * @return string in format "CheckInStatus{code=X, desc='description'}"
     */
    @Override
    public String toString() {
        return "CheckInStatus{" +
                "code=" + code +
                ", desc='" + desc + '\'' +
                '}';
    }
}
