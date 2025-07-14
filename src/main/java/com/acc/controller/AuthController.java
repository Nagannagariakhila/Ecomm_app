package com.acc.controller;
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
import com.acc.service.UserService;
import org.springframework.dao.DataIntegrityViolationException; 
@RestController
@RequestMapping("/api/auth") 
public class AuthController { 
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil; 
    @Autowired
    private UserService userService; 
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Validated @RequestBody UserDTO userDto) {
        try {
            User registeredUser = userService.save(userDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully.");
            response.put("userId", registeredUser.getId());
            response.put("username", registeredUser.getUsername());
            response.put("email", registeredUser.getEmail());

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity<>("Registration failed: Username or email already exists.", HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("Error during user registration: " + e.getMessage()); 
            return new ResponseEntity<>("Registration failed due to an unexpected error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Validated @RequestBody AuthRequestDTO authRequestDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequestDTO.getUsername(), authRequestDTO.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateToken(authentication);

            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

           
            UserDTO userDTO = userService.getUserByUsername(authRequestDTO.getUsername());

            
            AuthResponseDTO response = new AuthResponseDTO();
            response.setToken(jwt);
            response.setMessage("Login successful");
            response.setRole(role.replace("ROLE_", ""));
            response.setUser(userDTO);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (AuthenticationException e) {
            return new ResponseEntity<>("Invalid username or password.", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.err.println("Error during user login: " + e.getMessage());
            return new ResponseEntity<>("An internal error occurred during login.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @GetMapping("/admin/welcome")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") 
    public ResponseEntity<String> welcomeAdmin() {
        return new ResponseEntity<>("Welcome, Admin! You have access to administrative resources.", HttpStatus.OK);
    }
    @GetMapping("/user/welcome")
    @PreAuthorize("hasAuthority('ROLE_USER')") 
    public ResponseEntity<String> welcomeUser() {
        return new ResponseEntity<>("Welcome, User! You have access to general user resources.", HttpStatus.OK);
    }
}