package com.cyh.Config;

import com.cyh.Filter.UserInfoFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Bean
    public UserInfoFilter userInfoFilter(){
        return new UserInfoFilter();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cor->{})
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/points/**").permitAll()
                .requestMatchers("/api/messages/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                // Activity endpoints
                .requestMatchers(HttpMethod.GET, "/api/activities/**").permitAll()
                // Activity write operations - authenticated users (ownership checked in service)
                .requestMatchers(HttpMethod.POST, "/api/activities/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/activities/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/activities/**").authenticated()
                .requestMatchers("/api/reviews/**").hasAnyRole("ADMIN", "VOLUNTEER")
                .requestMatchers("/api/operation-logs/**").hasRole("ADMIN")
                // Volunteer endpoints
                .requestMatchers("/api/registrations/**").hasAnyRole("VOLUNTEER", "ADMIN")
                .requestMatchers("/api/check-ins/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(userInfoFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
