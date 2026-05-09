package com.cyh.Utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password Encoder Utility
 * Provides password encryption and verification using BCrypt
 */
@Component
public class PasswordEncodering {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Encode password using BCrypt
     * @param password the raw password
     * @return the encoded password
     */
    public String encodePassword(String password) {
        return encoder.encode(password);
    }
    
    /**
     * Verify password matches the encoded password
     * @param rawPassword the raw password
     * @param encodedPassword the encoded password
     * @return true if password matches, false otherwise
     */
    public boolean matchPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
