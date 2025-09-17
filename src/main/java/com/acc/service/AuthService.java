package com.acc.service;

import com.acc.entity.JwtTokenUtil; 
import com.acc.entity.Otp;
import com.acc.entity.Role;
import com.acc.entity.User;
import com.acc.repository.OtpRepository;
import com.acc.repository.UserRepository;
import com.acc.dto.AuthResponseDTO; 
import com.acc.dto.UserDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService; 

    @Transactional
    public void generateAndSendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));

        otpRepository.deleteByEmail(email);

        String otp = generateOtp();

        Otp otpEntity = new Otp();
        otpEntity.setEmail(email);
        otpEntity.setCode(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpEntity);

        System.out.println("DEBUG: OTP generated for " + email + ". Code: " + otp + ". Expiry Time: " + otpEntity.getExpiryTime());

        String emailBody = "Your one-time password (OTP) for login is: " + otp + "\n\nThis code is valid for 5 minutes.";
        emailService.sendEmail(email, "Login OTP", emailBody);
    }

    @Transactional
    public AuthResponseDTO verifyOtp(String email, String otp) { 
        System.out.println("DEBUG: Attempting to verify OTP for " + email);
        System.out.println("DEBUG: Current Time (server): " + LocalDateTime.now());

        Otp otpEntity = otpRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found for this email."));

        System.out.println("DEBUG: Stored OTP Expiry Time: " + otpEntity.getExpiryTime());

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otpEntity);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        System.out.println("DEBUG: Comparing OTPs:");
        System.out.println("DEBUG:    Stored OTP: '" + otpEntity.getCode() + "'");
        System.out.println("DEBUG:    Received OTP: '" + otp + "'");

        if (!otpEntity.getCode().equals(otp)) {
            System.out.println("DEBUG: OTP mismatch detected. Stored: '" + otpEntity.getCode() + "', Received: '" + otp + "'");
            throw new IllegalArgumentException("Invalid OTP.");
        }

        System.out.println("DEBUG: OTP matched successfully. Deleting OTP from database.");

        otpRepository.delete(otpEntity);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

       
        String token = jwtTokenUtil.generateToken(user); 

        
        UserDTO userDTO = userService.convertToDTO(user);

        
        String roleName = user.getRoles().stream()
                               .findFirst()
                               .map(Role::getName)
                               .orElse("ROLE_USER"); 

       
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setMessage("OTP verified successfully and logged in.");
        response.setRole(roleName.replace("ROLE_", "")); 
        response.setUser(userDTO); 

        return response;
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}