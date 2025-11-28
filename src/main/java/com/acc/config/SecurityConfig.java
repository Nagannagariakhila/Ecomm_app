package com.acc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Lazy
    @Autowired
    private UserDetailsService userDetailsService;

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
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        String roles = """
                ROLE_SUPER_ADMIN > ROLE_ADMIN
                ROLE_ADMIN > ROLE_CUSTOMER
                ROLE_CUSTOMER > ROLE_USER
                """;
        hierarchy.setHierarchy(roles);
        return hierarchy;
    }

    private DefaultWebSecurityExpressionHandler webSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy());
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .withObjectPostProcessor(new ObjectPostProcessor<DefaultWebSecurityExpressionHandler>() {
                    @Override
                    public <O extends DefaultWebSecurityExpressionHandler> O postProcess(O object) {
                        object.setRoleHierarchy(roleHierarchy());
                        return object;
                    }
                })

                
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/otp/generate",
                    "/api/auth/otp/verify",
                    "/api/superadmin/register",
                    "/api/superadmin/login",
                    "/api/customers/email/**",
                    "/api/profiles/**",
                    "/error"
                ).permitAll()

                // *** FIX APPLIED HERE: Allowing ROLE_CUSTOMER to use /api/coupons/apply ***
                .requestMatchers(HttpMethod.POST, "/api/coupons/apply").hasAnyAuthority("ROLE_ADMIN", "ROLE_CUSTOMER")
                // Other coupon rules
                .requestMatchers(HttpMethod.GET, "/api/coupons").hasAnyAuthority("ROLE_ADMIN", "ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/coupons").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/coupons/**").hasAuthority("ROLE_ADMIN")

                
                .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/upload").hasAuthority("ROLE_ADMIN")

                
                .requestMatchers(HttpMethod.GET, "/api/customers/all").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/auth/admin/welcome").hasAuthority("ROLE_ADMIN")

            
                .requestMatchers("/api/auth/superadmin/welcome").hasAuthority("ROLE_SUPER_ADMIN")
                .requestMatchers("/api/superadmin/dashboard").hasAuthority("ROLE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/superadmin/users").hasAuthority("ROLE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/superadmin/users/roles").hasAuthority("ROLE_SUPER_ADMIN")

                
                .requestMatchers(HttpMethod.PUT, "/api/carts/customer/*/items/*")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/carts/customer/*/items")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/carts/customer/{customerId}")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders/customer/**")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/customer/**")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payments")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                .requestMatchers("/api/auth/user/welcome")
                        .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_USER", "ROLE_SUPER_ADMIN")
                
                
                .requestMatchers(HttpMethod.POST, "/api/reviews/products/**")
                .hasAnyAuthority("ROLE_CUSTOMER", "ROLE_ADMIN")
                        
                .requestMatchers(HttpMethod.GET, "/api/reviews/products/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}