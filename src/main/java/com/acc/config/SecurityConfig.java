package com.acc.config;

import com.acc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Lazy
    @Autowired
    private UserService userService;

    @Lazy
    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors() 
            .and()
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/customers/**", "/api/admin/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/carts/customer/*/items/*")
                .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/carts/customer/*/items").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/carts/customer/{customerId}").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders/customer/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orders/customer/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payments").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/addresses/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/customers/email/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/profiles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/customers/all").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/payments/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/payments/**").hasAuthority("ROLE_ADMIN")





                .requestMatchers(HttpMethod.GET, "/api/orders").hasAuthority("ROLE_ADMIN")

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
