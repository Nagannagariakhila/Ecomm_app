package com.acc.controller;
import com.acc.dto.AdminDTO;
import com.acc.dto.AuthResponseDTO;
import com.acc.dto.UserDTO;
import com.acc.exception.ResourceNotFoundException;
import com.acc.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // Import AuthenticationException
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/admin") 
public class AdminController {
    @Autowired
    private AdminService adminService;
    @PostMapping("/register") 
    public ResponseEntity<AdminDTO> registerAdmin(@Validated @RequestBody AdminDTO adminDto) {
        try {
            AdminDTO registeredAdmin = adminService.registerAdmin(adminDto);
            return new ResponseEntity<>(registeredAdmin, HttpStatus.CREATED); 
        } catch (IllegalArgumentException e) {
            System.err.println("Admin registration failed: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.CONFLICT); 
        } catch (ResourceNotFoundException e) {
            System.err.println("Admin registration failed (role not found): " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during admin registration: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginAdmin(@Validated @RequestBody UserDTO userDto) {
        try {
            AuthResponseDTO authResponse = adminService.loginAdmin(userDto);
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (AuthenticationException e) { 
            System.err.println("Admin login failed: Invalid username or password. " + e.getMessage());
            return new ResponseEntity<>(new AuthResponseDTO(null, "Invalid username or password."), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during admin login: " + e.getMessage());
            e.printStackTrace(); 
            return new ResponseEntity<>(new AuthResponseDTO(null, "An internal error occurred during login."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}