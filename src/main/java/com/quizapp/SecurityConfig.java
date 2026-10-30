package com.quizapp;

import com.quizapp.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
            .map(u -> User.withUsername(u.getUsername()).password(u.getPasswordHash())
                .roles(u.getRole()).build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/login", "/css/**", "/error").permitAll()
                .requestMatchers("/teacher/**").hasRole("TEACHER")
                .requestMatchers("/student/**", "/attempt/**").hasRole("STUDENT")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
        return http.build();
    }
}
