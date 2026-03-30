package com.example.mysoftpos_backend.config;

import com.example.mysoftpos_backend.entity.PosAccount;
import com.example.mysoftpos_backend.repository.PosAccountRepository;
import com.example.mysoftpos_backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final PosAccountRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/forgot-password/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Admin-only endpoints (canonical + legacy compatibility)
                .requestMatchers("/api/users/**").hasRole("ADMIN") // deprecated compatibility route
                .requestMatchers("/api/pos-accounts/**").hasRole("ADMIN")
                .requestMatchers("/api/merchants/**").hasRole("ADMIN")
                .requestMatchers("/api/terminals/**").hasRole("ADMIN")
                .requestMatchers("/api/test-suites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/transactions/**").hasRole("ADMIN")
                // Authenticated endpoints
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Production security headers
            .headers(h -> h
                .frameOptions(fo -> fo.deny())
                .contentTypeOptions(cto -> {})              // X-Content-Type-Options: nosniff
                .httpStrictTransportSecurity(hsts -> hsts    // HSTS: enforce HTTPS
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .xssProtection(xss -> xss.headerValue(      // X-XSS-Protection: 1; mode=block
                    org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
                        .HeaderValue.ENABLED_MODE_BLOCK))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            PosAccount user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Pos account not found"));

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                    .disabled(!user.isActive())
                    .build();
        };
    }
}
