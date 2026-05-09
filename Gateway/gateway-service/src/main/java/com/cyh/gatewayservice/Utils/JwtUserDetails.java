package com.cyh.gatewayservice.Utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT User Details
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtUserDetails {
    private Long userId;
    private String username;
    private String role;
}
