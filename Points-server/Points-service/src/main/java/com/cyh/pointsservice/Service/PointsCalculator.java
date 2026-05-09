package com.cyh.pointsservice.Service;

import com.cyh.enums.CheckInStatus;
import org.springframework.stereotype.Component;

/**
 * Points Calculator
 *
 * Calculates volunteer points based on check-in status.
 * Requirement: 5
 */
@Component
public class PointsCalculator {

    /**
     * Calculate points based on base points and check-in status code.
     *
     * @param basePoints    the base points for the activity
     * @param checkInStatus the check-in status code (from CheckInStatus enum)
     * @return calculated points
     */
    public int calculate(int basePoints, int checkInStatus) {
        if (checkInStatus == CheckInStatus.CHECKED_IN.getCode()) {
            // Normal check-in: full base points
            return basePoints;
        } else if (checkInStatus == CheckInStatus.LATE.getCode()) {
            // Late check-in: 80% of base points
            return (int) (basePoints * 0.6);
        } else {
            // Absent or not checked in: 0 points
            return 0;
        }
    }
}
