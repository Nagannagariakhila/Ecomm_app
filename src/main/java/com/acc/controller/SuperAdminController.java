package com.acc.controller;

import com.acc.entity.Role;
import com.acc.entity.SuperAdmin;
import com.acc.repository.RoleRepository;
import com.acc.repository.UserRepository;
import com.acc.service.AuthService;
import com.acc.service.UserService; // Import UserService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For @PreAuthorize
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.acc.dto.AuthResponseDTO;
import com.acc.dto.UserDTO; // Import UserDTO (if needed for other methods, not directly used in this snippet's new method)
import com.acc.dto.UpdateUserRolesRequest; // Import the new DTO

@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired(required = false) 
    private PasswordEncoder passwordEncoder; 

    @Autowired 
    private UserService userService;

    
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody SuperAdmin superAdmin) {
        if (userRepository.findByEmail(superAdmin.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_SUPER_ADMIN not found in database. Please ensure it is seeded."));

        superAdmin.setRoles(Collections.singleton(superAdminRole));

       
        if (passwordEncoder != null && superAdmin.getPassword() != null && !superAdmin.getPassword().isBlank()) {
            superAdmin.setPassword(passwordEncoder.encode(superAdmin.getPassword()));
        } else {
            
            superAdmin.setPassword("");
        }

        userRepository.save(superAdmin);

        authService.generateAndSendOtp(superAdmin.getEmail());

        return ResponseEntity.ok("Super Admin registered. OTP sent to email.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestParam String email, @RequestParam String otp) {
        AuthResponseDTO authResponse = authService.verifyOtp(email, otp);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')") 
    public ResponseEntity<String> superAdminDashboard() {
        return ResponseEntity.ok("Welcome, Super Admin! You have access to the Super Admin Dashboard.");
    }

    
    @PutMapping("/users/roles") 
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')") 
    public ResponseEntity<?> updateUserRoles(@RequestBody UpdateUserRolesRequest request) {
        System.out.println("DEBUG: Received request to update roles for identifier: " + request.getIdentifier() + " with roles: " + request.getRoles());
        try {
            UserDTO updatedUser = userService.updateUserRoles(request.getIdentifier(), request.getRoles());
            return ResponseEntity.ok(updatedUser);
        } catch (UsernameNotFoundException e) {
            System.err.println("ERROR: User not found for role update: " + request.getIdentifier());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: Invalid role assignment attempt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("ERROR: An unexpected error occurred during role update: " + e.getMessage());
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "An internal server error occurred."));
        }
    }
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')") 
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        
        List<UserDTO> users = userService.getAllUsers();
        
       
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}
