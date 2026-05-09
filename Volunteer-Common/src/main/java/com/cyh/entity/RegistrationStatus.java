package com.cyh.entity;

import lombok.Getter;

/**
 * Registration Status Enumeration
 * 
 * Represents the states of a volunteer's registration for an activity.
 * 
 * States:
 * - PENDING_REVIEW: Registration submitted, awaiting admin review
 * - APPROVED: Registration approved by admin
 * - REJECTED: Registration rejected by admin
 * - CANCELLED: Registration cancelled by volunteer
 * 
 * @author Volunteer Service Platform
 * @version 1.0
 */
@Getter
public enum RegistrationStatus {
    PENDING_REVIEW(0, "待审核"),
    APPROVED(1, "已批准"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已取消");
    
    private final int code;
    private final String desc;
    
    /**
     * Constructor for RegistrationStatus
     * 
     * @param code the numeric code for database storage
     * @param desc the description for display
     */
    RegistrationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * Get RegistrationStatus enum by code
     * 
     * @param code the numeric code
     * @return the corresponding RegistrationStatus enum, or null if not found
     */
    public static RegistrationStatus getByCode(int code) {
        for (RegistrationStatus status : RegistrationStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Returns a string representation of the RegistrationStatus
     * 
     * @return string in format "RegistrationStatus{code=X, desc='description'}"
     */
    @Override
    public String toString() {
        return "RegistrationStatus{" +
                "code=" + code +
                ", desc='" + desc + '\'' +
                '}';
    }
}
