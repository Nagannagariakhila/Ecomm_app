package com.acc.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acc.config.JwtUtil;
import com.acc.dto.AuthRequestDTO;
import com.acc.dto.AuthResponseDTO;
import com.acc.dto.UserDTO;
import com.acc.entity.User;
import com.acc.service.AuthService;
import com.acc.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
   
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired 
    private AuthService authService;

   
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Validated @RequestBody UserDTO userDto) {
        log.info("Attempting to register new user with username: {}", userDto.getUsername()); // Log start
        try {
            User registeredUser = userService.save(userDto);
            log.info("User registered successfully. User ID: {}", registeredUser.getId()); // Log success

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully.");
            response.put("userId", registeredUser.getId());
            response.put("username", registeredUser.getUsername());
            response.put("email", registeredUser.getEmail());

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed for username: {} - Reason: Username or email already exists.", userDto.getUsername()); // Log specific failure
            return new ResponseEntity<>("Registration failed: Username or email already exists.", HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed for user: {} - Reason: {}", userDto.getUsername(), e.getMessage()); // Log specific failure
            return new ResponseEntity<>("Registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
           
            log.error("Error during user registration for username: {}", userDto.getUsername(), e); 
            return new ResponseEntity<>("Registration failed due to an unexpected error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Validated @RequestBody AuthRequestDTO authRequestDTO) {
        String username = authRequestDTO.getUsername();
        log.info("Attempting to log in user: {}", username); 
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, authRequestDTO.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateToken(authentication);

            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            UserDTO userDTO = userService.getUserByUsername(username);

            AuthResponseDTO response = new AuthResponseDTO();
            response.setToken(jwt);
            response.setMessage("Login successful");
            response.setRole(role.replace("ROLE_", ""));
            response.setUser(userDTO);

            log.info("User logged in successfully with role: {}", response.getRole()); 

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {} - Invalid credentials.", username); 
            return new ResponseEntity<>("Invalid username or password.", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            
            log.error("Error during user login for user: {}", username, e);
            return new ResponseEntity<>("An internal error occurred during login.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


   
    @GetMapping("/admin/welcome")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> welcomeAdmin() {
        
        log.info("Admin resource accessed by user: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        return new ResponseEntity<>("Welcome, Admin! You have access to administrative resources.", HttpStatus.OK);
    }

    @GetMapping("/user/welcome")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<String> welcomeUser() {
        log.info("User resource accessed by user: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        return new ResponseEntity<>("Welcome, User! You have access to general user resources.", HttpStatus.OK);
    }


   
    @PostMapping("/otp/generate")
    public ResponseEntity<?> generateOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Request received to generate OTP for email: {}", email);
        
        if (email == null) {
            log.warn("OTP generation failed: Email is missing in the request.");
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
        }
        try {
            authService.generateAndSendOtp(email);
            log.info("OTP generated and sent successfully to email: {}", email);
            return ResponseEntity.ok(Collections.singletonMap("message", "OTP sent to your email."));
        } catch (IllegalArgumentException e) {
            log.warn("OTP generation failed for email: {} - Reason: {}", email, e.getMessage()); 
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

 
    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponseDTO> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        log.info("Request received to verify OTP for email: {}", email); 

        if (email == null || otp == null) {
            log.warn("OTP verification failed for email: {} - Reason: Email or OTP is missing.", email);
            
            AuthResponseDTO errorResponse = new AuthResponseDTO();
            errorResponse.setMessage("Email and OTP are required.");
            errorResponse.setToken(null); 
            errorResponse.setUser(null);  
            return ResponseEntity.badRequest().body(errorResponse);
        }
        try {
            AuthResponseDTO response = authService.verifyOtp(email, otp);
            log.info("OTP verified successfully for email: {}", email); // Log success
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("OTP verification failed for email: {} - Reason: {}", email, e.getMessage()); // Log specific failure
            AuthResponseDTO errorResponse = new AuthResponseDTO();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setToken(null);
            errorResponse.setUser(null);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) { 
           
            log.error("An internal server error occurred during OTP verification for email: {}", email, e);
            AuthResponseDTO errorResponse = new AuthResponseDTO();
            errorResponse.setMessage("An internal server error occurred during OTP verification.");
            errorResponse.setToken(null);
            errorResponse.setUser(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}